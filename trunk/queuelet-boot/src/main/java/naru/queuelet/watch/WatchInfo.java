package naru.queuelet.watch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import naru.queuelet.startup.StartupProperties;

public class WatchInfo{
	/*
	 * 名前 name 4 +16　
	 * 監視中　isWatching 4 
	 * 起動中　isRun 4 
	 * 最新更新 lastHeartBeat 8
	 * 更新限界 heartBeatLimit 8
	 * 再起動限界 restartLimit 4
	 * 再起動指定 isRestart 4
	 * 強制終了指定 isForceEnd 4
	 * コマンドライン commandLine 4 + 8192
	 * 環境変数 environment 4 +8192
	 */
	private static final int NAME_OFFSET=0;
	private static final int NAME_MAX=16;
	private static final int IS_WATCHING_OFFSET=NAME_OFFSET+4+NAME_MAX;
	private static final int IS_RUN_OFFSET=IS_WATCHING_OFFSET+4+NAME_MAX;
	private static final int LAST_HEART_BEAT_OFFSET=IS_RUN_OFFSET+4;
	private static final int HEART_BEAT_LIMIT_OFFSET=LAST_HEART_BEAT_OFFSET+8;
	private static final int RESTART_LIMIT=HEART_BEAT_LIMIT_OFFSET+8;
	private static final int IS_RESTART_OFFSET=RESTART_LIMIT+4;
	private static final int IS_FORCE_END_OFFSET=IS_RESTART_OFFSET+4;
	private static final int COMMAND_LINE_OFFSET=IS_FORCE_END_OFFSET+4;
	private static final int COMMAND_LINE_MAX=8192;
	private static final int ENVIRONMENT_OFFSET=COMMAND_LINE_OFFSET+4+COMMAND_LINE_MAX;
	private static final int ENVIRONMENT_MAX=8192;
	private static final int WATCH_FILE_SIZE=ENVIRONMENT_OFFSET+4+ENVIRONMENT_MAX;
	private static final String WATCH_STOP_FLAG_FILE="watchStrop.flg";
	private static final String WATCH_DIR_NAME="watch";
	static final String WATCH_FILE_EXT=".wch";
	
	private File watchFile;
	private MappedByteBuffer watchFileBuffer;
	private FileChannel watchFileChannel;
	private RandomAccessFile watchAccessFile;
	private WatchProcess watchProcess;//強制停止用
	private boolean isDeamon=false;//deamonからの参照か否か
	private Thread shutdownHook=null;
	
	private synchronized int readInt(int offset){
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock(0, Long.MAX_VALUE, false);
			return watchFileBuffer.getInt(offset);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fail to lock",e);
		} finally {
			if(lock!=null){
				try {
					lock.release();
				} catch (IOException ignore) {
				}
			}
		}
	}
	private synchronized void writeInt(int offset,int value){
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock();
			watchFileBuffer.putInt(offset,value);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fail to lock",e);
		} finally {
			if(lock!=null){
				try {
					lock.release();
				} catch (IOException ignore) {
				}
			}
		}
	}
	private synchronized long readLong(int offset){
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock(0, Long.MAX_VALUE, false);
			return watchFileBuffer.getLong(offset);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fail to lock",e);
		} finally {
			if(lock!=null){
				try {
					lock.release();
				} catch (IOException ignore) {
				}
			}
		}
	}
	private synchronized void writeLong(int offset,long value){
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock();
			watchFileBuffer.putLong(offset,value);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fail to lock",e);
		} finally {
			if(lock!=null){
				try {
					lock.release();
				} catch (IOException ignore) {
				}
			}
		}
	}
	private synchronized String readString(int offset,int max){
		byte[] buf=new byte[max];
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock(0, Long.MAX_VALUE, false);
			watchFileBuffer.position(offset);
			int size=watchFileBuffer.getInt();
			if(size<0){
				return null;
			}
			watchFileBuffer.get(buf,0,size);
			return new String(buf,0,size,"utf-8");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fail to lock",e);
		} finally {
			if(lock!=null){
				try {
					lock.release();
				} catch (IOException ignore) {
				}
			}
		}
	}
	private synchronized void writeString(int offset,int maxSize,String value){
		byte[] buf=new byte[maxSize];
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock();
			byte[] src=null;
			int size=-1;
			if(value!=null){
				src=value.getBytes("utf-8");
				size=Math.min(src.length, maxSize);
			}
			watchFileBuffer.position(offset);
			watchFileBuffer.putInt(size);
			if(src!=null){
				System.arraycopy(src, 0, buf, 0,size);
				watchFileBuffer.put(buf);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fail to lock",e);
		} finally {
			if(lock!=null){
				try {
					lock.release();
				} catch (IOException ignore) {
				}
			}
		}
	}
	
	public static File getWatchDir(){
		String queueletHome=System.getProperty(StartupProperties.QUEUELET_HOME);
		File watchDir=new File(queueletHome,WATCH_DIR_NAME);
		if(watchDir.exists()&&watchDir.isDirectory()){
			return watchDir;
		}
		return null;
	}
	public static File getStopFlagFile(){
		File watchDir=getWatchDir();
		return new File(watchDir,WATCH_STOP_FLAG_FILE);
	}
	private static File getWatchFile(String name){
		File watchFile=new File(getWatchDir(),name+WATCH_FILE_EXT);
		return watchFile;
	}
	public static WatchInfo create(String name) throws IOException{
		return create(name,false);
	}
	public static WatchInfo create(String name,boolean isWatching) throws IOException{
		File watchFile=getWatchFile(name);
		WatchInfo watchInfo=new WatchInfo(watchFile);
		/*
		if(!name.equals(watchInfo.getName())){
			//名前が不一致
			System.out.println("WatchInfo:name unmutch:"+name+":"+watchInfo.getName());
			watchInfo.isDeamon=true;
			watchInfo.term();
			return null;
		}
		*/
		watchInfo.isDeamon=false;
		if(isWatching){
			if(watchInfo.isWatching()){
				System.out.println("WatchInfo:aleady watching:"+name);
				watchInfo.term();
				return null;//既に監視中
			}
			watchInfo.setShutdownHook();
			watchInfo.isDeamon=true;
		}
		watchInfo.setIsWatching(isWatching);
		return watchInfo;
	}
	public static WatchInfo create(
			String name,
			boolean isWatching,//ファイルを作るだけの場合false,監視する場合true
			String commandLine,
			String[] env,
			long heartBeatLimit,
			int restartLimit
			) throws IOException{
		WatchInfo watchInfo=create(name,isWatching);
		if(watchInfo==null){
			return null;
		}
		watchInfo.setName(name);
		watchInfo.setIsRun(false);
		watchInfo.setIsRestart(true);
		watchInfo.setIsForceEnd(false);
		watchInfo.setCommandLine(commandLine);
		watchInfo.setEnvironment(env);
		watchInfo.setHeartBeatLimit(heartBeatLimit);
		watchInfo.setRestartLimit(restartLimit);
		return watchInfo;
	}
	private WatchInfo(File watchFile) throws IOException{
		this.watchFile=watchFile;
		this.watchAccessFile=new RandomAccessFile(watchFile,"rw");
		this.watchFileChannel=watchAccessFile.getChannel();
		this.watchFileBuffer=watchFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, (long)WATCH_FILE_SIZE);
	}
	
	private void setShutdownHook(){
		Runtime runtime=Runtime.getRuntime();
		shutdownHook=new Thread(new TerminateHook());
		runtime.addShutdownHook(shutdownHook);
	}
	
	public void term(){
		if(shutdownHook!=null){
			Runtime runtime=Runtime.getRuntime();
			runtime.removeShutdownHook(shutdownHook);
			shutdownHook=null;
		}
		WatchProcess w=watchProcess;
		if(w!=null){
			w.stop();
		}
		if(isDeamon){
			setIsWatching(false);
		}
		watchFileBuffer=null;
		if( watchFileChannel!=null ){
			try {
				watchFileChannel.close();
			} catch (IOException ignore) {
			}
			watchFileChannel=null;
		}
		if(watchAccessFile!=null){
			try {
				watchAccessFile.close();
			} catch (IOException ignore) {
			}
			watchAccessFile=null;
		}
		if(isDeamon){
			//watchFileBufferがgcされないと、ファイルは削除できない
			//Runtime.getRuntime().gc();
			watchFile.deleteOnExit();
		}
	}
	public String getName(){
		return readString(NAME_OFFSET,NAME_MAX);
	}
	public boolean isRun(){
		int isRun=readInt(IS_RUN_OFFSET);
		return (isRun!=0);
	}
	public boolean isWatching(){
		int isWatching=readInt(IS_WATCHING_OFFSET);
		return (isWatching!=0);
	}
	public long getHeartBeatLimist(){
		return readLong(HEART_BEAT_LIMIT_OFFSET);
	}
	public long getLastHeartBeat(){
		return readLong(LAST_HEART_BEAT_OFFSET);
	}
	public int getRestartLimit(){
		return readInt(RESTART_LIMIT);
	}
	public boolean isRestart(){
		int isRestart=readInt(IS_RESTART_OFFSET);
		return (isRestart!=0);
	}
	public boolean isForceEnd(){
		int isForceEnd=readInt(IS_FORCE_END_OFFSET);
		return (isForceEnd!=0);
	}
	
	public String getCommandLine(){
		return readString(COMMAND_LINE_OFFSET, COMMAND_LINE_MAX);
	}
	
	private static final String ENV_DELIMITER=";";
	
	public String[] getEnvironment(){
		String envs=readString(ENVIRONMENT_OFFSET, ENVIRONMENT_MAX);
		if(envs==null){
			return null;
		}
		return envs.split(ENV_DELIMITER);
	}
	
	
	public void setName(String name){
		writeString(NAME_OFFSET,NAME_MAX,name);
	}
	public void setHeartBeatLimit(long limit){
		writeLong(HEART_BEAT_LIMIT_OFFSET,limit);
	}
	
	public void setRestartLimit(int limit){
		writeInt(RESTART_LIMIT,limit);
	}
	
	public void heartBeat(){
		writeLong(LAST_HEART_BEAT_OFFSET,System.currentTimeMillis());
	}
	
	public void setCommandLine(String commandLine){
		writeString(COMMAND_LINE_OFFSET, COMMAND_LINE_MAX,commandLine);
	}
	
	public void setEnvironment(String[] env){
		if(env==null){
			writeString(ENVIRONMENT_OFFSET, ENVIRONMENT_MAX,null);
			return;
		}
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<env.length;i++){
			sb.append(env[i]);
			sb.append(ENV_DELIMITER);
		}
		writeString(COMMAND_LINE_OFFSET, COMMAND_LINE_MAX,sb.toString());
	}
	
	private void setIsWatching(boolean isWatching){
		int value=0;
		if(isWatching){
			value=1;
		}
		writeInt(IS_WATCHING_OFFSET,value);
	}
	
	private void setIsRun(boolean isRun){
		int value=0;
		if(isRun){
			value=1;
		}
		writeInt(IS_RUN_OFFSET,value);
	}
	
	public void setIsRestart(boolean isRestart){
		int value=0;
		if(isRestart){
			value=1;
		}
		writeInt(IS_RESTART_OFFSET,value);
	}
	
	public void setIsForceEnd(boolean isForceEnd){
		int value=0;
		if(isForceEnd){
			value=1;
		}
		writeInt(IS_FORCE_END_OFFSET,value);
	}
	
	public boolean executeChild(){
		if(!isDeamon){
			System.out.println("WatchInfo:can't execute:"+getName());
			return false;
		}
		if(watchProcess!=null){
			System.out.println("WatchInfo:aleady execute:"+getName());
			return false;
		}
		
		watchProcess=new WatchProcess();
		synchronized(watchProcess){
			watchProcess.start();
			try {
				watchProcess.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("WatchInfo:execute:"+getName());
		return true;
	}
	
	public boolean termanateChild(){
		WatchProcess w=watchProcess;
		if(w==null){
			System.out.println("WatchInfo:aleady termanate:"+getName());
			return false;
		}
		w.stop();
		System.out.println("WatchInfo:termanateChild:"+getName());
		return true;
	}

	private class TerminateHook implements Runnable{
		public void run() {
			System.out.println("WatchInfo:TerminateHook:"+getName());
			shutdownHook=null;
			term();
		}
	}
	
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
	
	private class WatchProcess implements Runnable{
		private Thread runThread;
		private Process process;
		public void run() {
			synchronized(this){
				setIsRun(true);
				notify();
			}
			process=null;
			try {
				Runtime runtime=Runtime.getRuntime();
				process=runtime.exec(getCommandLine(),getEnvironment());
				new StdReader(process.getInputStream(),System.out);
				new StdReader(process.getErrorStream(),System.err);
				heartBeat();//初回heart beatはdeamonから実施
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
				setIsRun(false);
				runThread=null;
				watchProcess=null;
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
			watchProcess=null;
		}
	}
	
	private static void usage(){
		System.out.println("usage:java "+ WatchInfo.class.getName()+ " [name|stop] commandLine interval count");
	}
	
	public static void main(String arg[]) throws IOException{
		if(arg.length==0){
			usage();
			return;
		}
		String name=arg[0];
		if("stop".endsWith(name)){
			File stopFile=getStopFlagFile();
			(new FileOutputStream(stopFile)).close();
			return;
		}
		if(arg.length<2){
			usage();
			return;
		}
		String commandLine=arg[1];
		long interval=-1;
		int count=-1;
		if(arg.length>=3){
			interval=Long.parseLong(arg[2]);
		}
		if(arg.length>=4){
			count=Integer.parseInt(arg[3]);
		}
		WatchInfo watchInfo=WatchInfo.create(name,false,commandLine,null,interval,count);
		if(watchInfo==null){
			System.out.println("fail to watch:"+name);
		}else{
			System.out.println("OK:"+name);
		}
	}
}
