package naru.queuelet.startup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class WatchInfo implements Runnable{
	private static final int IS_RUN_OFFSET=0;
	private static final int LAST_HEAT_BEAT_OFFSET=IS_RUN_OFFSET+4;
	private static final int IS_RESTART_OFFSET=LAST_HEAT_BEAT_OFFSET+8;
	private static final int IS_FORCE_END_OFFSET=IS_RESTART_OFFSET+4;
	
	private static final int COMMAND_LINE_OFFSET=IS_FORCE_END_OFFSET+4;
	private static final int COMMAND_LINE_MAX=8192;
	
	private static final int ENVIRONMENT_OFFSET=COMMAND_LINE_OFFSET+4+COMMAND_LINE_MAX;
	private static final int ENVIRONMENT_MAX=8192;
	
	private static final int WATCH_FILE_SIZE=ENVIRONMENT_OFFSET+4+ENVIRONMENT_MAX;
	
	private MappedByteBuffer watchFileBuffer;
	private FileChannel watchFileChannel;
	private RandomAccessFile watchAccessFile;
	
	private int readInt(int offset){
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
	private void writeInt(int offset,int value){
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
	
	private long readLong(int offset){
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
	
	private void writeLong(int offset,long value){
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
	
	private String readString(int offset,int max){
		byte[] buf=new byte[max];
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock(0, Long.MAX_VALUE, false);
			watchFileBuffer.position(offset);
			int size=watchFileBuffer.getInt();
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
	
	private void writeString(int offset,int maxSize,String value){
		byte[] buf=new byte[maxSize];
		FileLock lock=null;
		try {
			lock=watchFileChannel.lock();
			byte[] src=value.getBytes("utf-8");
			int size=Math.min(src.length, maxSize);
			watchFileBuffer.position(offset);
			watchFileBuffer.putInt(size);
			System.arraycopy(src, 0, buf, 0,size);
			watchFileBuffer.put(buf);
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

	/*
	 * éqÇ©ÇÁåƒÇ—èoÇ≥ÇÍÇÈèÍçá
	 */
	public WatchInfo(File watchFile) throws IOException{
		this(watchFile,null,null);
	}

	/*
	 * deamonÇ©ÇÁåƒÇ—èoÇ≥ÇÍÇÈèÍçá
	 */
	public WatchInfo(File watchFile,String commandLine,String[] env) throws IOException{
		watchAccessFile=new RandomAccessFile(watchFile,"rw");
		watchFileChannel=watchAccessFile.getChannel();
		watchFileBuffer=watchFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, (long)WATCH_FILE_SIZE);
		if(commandLine!=null){
			setIsRun(false);
			setIsRestart(true);
			setIsForceEnd(false);
			setCommandLine(commandLine);
			setEnvironment(env);
		}
	}
	
	public void term(){
		terminateChild();
		if( childRunThread!=null ){
			try {
				childRunThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
	}
	
	public boolean isRun(){
		int isRun=readInt(IS_RUN_OFFSET);
		return (isRun!=0);
	}
	public long getLastHeatBeat(){
		return readLong(LAST_HEAT_BEAT_OFFSET);
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
		return readString(ENVIRONMENT_OFFSET, ENVIRONMENT_MAX).split(ENV_DELIMITER);
	}
	
	public void heatBeat(){
		writeLong(LAST_HEAT_BEAT_OFFSET,System.currentTimeMillis());
	}
	
	public void setCommandLine(String commandLine){
		writeString(COMMAND_LINE_OFFSET, COMMAND_LINE_MAX,commandLine);
	}
	
	public void setEnvironment(String[] env){
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<env.length;i++){
			sb.append(env[i]);
			sb.append(ENV_DELIMITER);
		}
		writeString(COMMAND_LINE_OFFSET, COMMAND_LINE_MAX,sb.toString());
	}
	
	public void setIsRun(boolean isRun){
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
	
	private static class Reader implements Runnable{
		private InputStream is;
		Reader(InputStream is){
			this.is=is;
			Thread thread=new Thread(this);
			thread.start();
		}
		public void run() {
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
				System.out.println(new String(buf,0,len));
			}
			try {
				is.close();
			} catch (IOException ignore) {
			}
			is=null;
		}
	}
	
	private Thread childRunThread;
	private Process process;
	public void run() {
		process=null;
		try {
			setIsRun(true);
			Runtime runtime=Runtime.getRuntime();
			process=runtime.exec(getCommandLine(),getEnvironment());
			new Reader(process.getInputStream());
			new Reader(process.getErrorStream());
			process.waitFor();
			process=null;
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}finally{
			if(process!=null){
				process.destroy();
			}
			setIsRun(false);
			childRunThread=null;
		}
	}
	
	public void executeChild(){
		childRunThread=new Thread(this);
		childRunThread.start();
	}
	public void terminateChild(){
		Process p=process;
		if(p!=null){
			p.destroy();
		}
	}
	
	public static void main(String arg[]) throws IOException{
		WatchInfo watchInfo=new WatchInfo(new File(arg[0]),"notepad.exe",null);
		watchInfo.setCommandLine("commandLine");
		System.out.println("commandLine:"+watchInfo.getCommandLine());
	}
	
}
