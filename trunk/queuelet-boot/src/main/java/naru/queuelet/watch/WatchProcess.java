package naru.queuelet.watch;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

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
			System.out.println("WatchInfo:StdReader start:"+out);
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
			System.out.println("WatchInfo:StdReader end:"+out);
		}
	}
	
	private WatchFile watchFile;
	private String[] cmd;
	private String[] env;
	
	private Thread runThread;
	private Process process;
//	private boolean isRun=false;
	public void run() {
		synchronized(this){
			watchFile.setRun(true);
			notify();
		}
		process=null;
		try {
			Runtime runtime=Runtime.getRuntime();
			process=runtime.exec(cmd,env);
			new StdReader(process.getInputStream(),System.out);
			new StdReader(process.getErrorStream(),System.err);
			watchFile.heartBeat();//‰‰ñheart beat‚Ídeamon‚©‚çŽÀŽ{
			process.waitFor();
			System.out.println("exitValue:"+process.exitValue());
			process=null;
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}finally{
			if(process!=null){
				process.destroy();
				process=null;
			}
			watchFile.setRun(false);
			runThread=null;
		}
	}
	public void start(){
		runThread=new Thread(this);
		runThread.start();
	}
	
	public void stop(){
		Process p=process;
		if(p!=null){
			p.destroy();
		}
		Thread t=runThread;
		if( t!=null ){
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
