package naru.queuelet.watch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Map;

public class WatchProcess implements Runnable{
	private class StdReader implements Runnable{
		private InputStream is;
		private PrintStream out;
		StdReader(InputStream is,PrintStream out){
			this.is=is;
			this.out=out;
			Thread thread=new Thread(this);
			thread.start();
		}
		public void run() {
//			System.out.println("WatchInfo:StdReader start:"+out);
			byte[] buf=new byte[4096];
			while(true){
				int len;
				try {
					len = is.read(buf);
				} catch (IOException e) {
					break;
				}
				if(len<=0){
					break;
				}
				out.println(new String(buf,0,len));
			}
			try {
				is.close();
			} catch (IOException ignore) {
			}
			is=null;
//			System.out.println("WatchInfo:StdReader end:"+out);
		}
	}
	private class TerminateHook implements Runnable{
		public void run() {
			System.out.println("WatchProcess:TerminateHook:"+name);
			shutdownHook=null;
			stop();
		}
	}
	private WatchFile watchFile;
	private String name;
	private String[] cmd;
	private String[] env;
	
	private Thread runThread;
	private Process process;
	private Thread shutdownHook;
	
	//監視対象が再起動時に指定した起動情報
	private StartupInfo responseStartupInfo=null;
	
	public WatchProcess(WatchFile watchFile,String[] cmd,String[] env){
		this.watchFile=watchFile;
		this.name=watchFile.getName();
		this.cmd=cmd;
		if(env==null){
			Map selfEnv=System.getenv();
			env=new String[selfEnv.size()];
			Iterator itr=selfEnv.keySet().iterator();
			int i=0;
			while(itr.hasNext()){
				String key=(String)itr.next();
				String value=System.getenv(key);
				env[i]=key+"="+value;
				i++;
			}
		}
		int envLength=0;
		envLength=env.length;
		this.env=new String[envLength+2];
		System.arraycopy(env, 0, this.env, 0, envLength);
		this.env[envLength]=WatchFile.QUEUELET_WATCH_TOKEN_ENV+"="+watchFile.getToken();
		this.env[envLength+1]=WatchFile.QUEUELET_WATCH_NAME_ENV+"="+watchFile.getName();
	}
	
	private void readResponseStartupInfo() throws IOException{
		File responseFile=watchFile.getResponseFile();
		if(!responseFile.exists()){
			return;
		}
		System.out.println("WatchProcess:responseStartupInfo exists:"+name);
		int length=(int)responseFile.length();
		RandomAccessFile response=new RandomAccessFile(responseFile,"r");
		responseStartupInfo=WatchFile.deserializseStartupInfo(response,length);
		response.close();
		responseFile.delete();
	}
	
	public void run() {
		Runtime runtime=Runtime.getRuntime();
		synchronized(this){
			watchFile.setRun(true);
			watchFile.setWatched(false);
			shutdownHook=new Thread(new TerminateHook());
			runtime.addShutdownHook(shutdownHook);
			notify();
		}
		process=null;
		try {
			process=runtime.exec(cmd,env);
			Process p=process;
			new StdReader(process.getInputStream(),System.out);
			new StdReader(process.getErrorStream(),System.err);
			watchFile.heartBeat();//初回heart beatはdeamonから実施
			process.waitFor();
			//プロセス終了、responseファイルを探す
			readResponseStartupInfo();
			System.out.println("exitValue:"+p.exitValue());
			process=null;
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}finally{
			if(shutdownHook!=null){
				runtime.removeShutdownHook(shutdownHook);
				shutdownHook=null;
			}
			if(process!=null){
				process.destroy();
				process=null;
			}
			watchFile.setRun(false);
			runThread=null;
		}
	}
	public void start(){
		System.out.println("WatchProcess:startChild:"+name);
		runThread=new Thread(this);
		runThread.start();
	}
	
	public void stop(){
		System.out.println("WatchProcess:stopChild:"+name);
		Thread s=shutdownHook;
		if(s!=null){
			Runtime runtime=Runtime.getRuntime();
			runtime.removeShutdownHook(s);
			shutdownHook=null;
		}
		Process p=process;
		if(p!=null){
			p.destroy();
			process=null;
		}
		Thread t=runThread;
		if( t!=null ){
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runThread=null;
		}
	}

	public StartupInfo getResponseStartupInfo() {
		return responseStartupInfo;
	}
}
