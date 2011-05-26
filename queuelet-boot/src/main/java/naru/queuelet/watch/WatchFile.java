package naru.queuelet.watch;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import naru.queuelet.startup.StartupProperties;

public class WatchFile {
	/**
	 * watchFileに保持するデータ
	 * 名前 name 4 +16　
	 * 監視中　isWatching 4 
	 * 最新更新 lastHeartBeat 8
	 * isForceEnd
	 * isRestart
	 * --------
	 * isRun
	 * この下childに通知する起動情報:UTF-8文字列
	 */
	private static final int NAME_OFFSET=0;
	private static final int NAME_MAX=16;
	private static final int IS_WATCHING_OFFSET=NAME_OFFSET+4+NAME_MAX;
	private static final int LAST_HEART_BEAT_OFFSET=IS_WATCHING_OFFSET+4;
	private static final int IS_FORCE_END_OFFSET=LAST_HEART_BEAT_OFFSET+4;
	private static final int IS_RESTART_OFFSET=IS_FORCE_END_OFFSET+4;
	private static final int WATCH_SHARE_OFFSET=IS_RESTART_OFFSET+4;
	
	private static final String EQ="=";
	private static final String RET="\n";
	
	private File watchFile;
	private MappedByteBuffer watchFileBuffer;
	private FileChannel watchFileChannel;
	private RandomAccessFile watchAccessFile;
	
	private Map startupInfo;//childに通知する起動情報
	private boolean isRun=false;//Deamonだけが知ればよい情報なので共用領域にはおかない
	
	public static Map readStartupInfo(RandomAccessFile file) throws IOException{
		Map startInfo=new HashMap();
		String optionsString=file.readUTF();
		String[] options=optionsString.split(RET);
		for(int i=0;i<options.length;i++){
			String option=options[i];
			String[] keyAndValue=option.split(EQ, 2);
			if(keyAndValue.length>2){
				startInfo.put(keyAndValue[0], keyAndValue[1]);
			}
		}
		return startInfo;
	}

	public static void wariteStartupInfo(RandomAccessFile file,Map startupInfo) throws IOException{
		Iterator itr=startupInfo.keySet().iterator();
		StringBuffer sb=new StringBuffer();
		while(itr.hasNext()){
			String key=(String)itr.next();
			String value=(String)startupInfo.get(key);
			if(value==null){
				continue;
			}
			sb.append(key);
			sb.append(EQ);
			sb.append(value);
			sb.append(RET);
		}
		file.writeUTF(sb.toString());
	}

	
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
	
	private static final String WATCH_DIR_NAME="watch";
	static final String WATCH_FILE_EXT=".wch";
	
	public static File getWatchDir(){
		String queueletHome=System.getProperty(StartupProperties.QUEUELET_HOME);
		File watchDir=new File(queueletHome,WATCH_DIR_NAME);
		if(watchDir.exists()&&watchDir.isDirectory()){
			return watchDir;
		}
		return null;
	}
	private static File getWatchFile(String name){
		File watchFile=new File(getWatchDir(),name+WATCH_FILE_EXT);
		return watchFile;
	}
	
	public static WatchFile loadWatcher(String name) throws IOException{
		WatchFile watcher=new WatchFile(name);
		if(!name.equals(watcher.getName())){
			//正しいwatchファイルじゃない
			return null;
		}
		watcher.loadStartupInfo();
		return watcher;
	}
	
	private WatchFile(String name) throws IOException{
		this.watchFile=getWatchFile(name);
		this.watchAccessFile=new RandomAccessFile(watchFile,"rw");
		this.watchFileChannel=watchAccessFile.getChannel();
		this.watchFileBuffer=watchFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, (long)WATCH_SHARE_OFFSET);
	}
	
	private void loadStartupInfo() throws IOException{
		watchAccessFile.seek(WATCH_SHARE_OFFSET);
		startupInfo=readStartupInfo(watchAccessFile);
	}
	
	private void saveStartupInfo() throws IOException{
		watchAccessFile.seek(WATCH_SHARE_OFFSET);
		wariteStartupInfo(watchAccessFile,startupInfo);
	}
	
	public Map getStartInfo() {
		return startupInfo;
	}
	public String getName() {
		return readString(NAME_OFFSET, NAME_MAX);
	}
	public boolean isWatching() {
		return (readInt(IS_WATCHING_OFFSET)!=0);
	}
	public long getLastHeartBeat() {
		return readLong(LAST_HEART_BEAT_OFFSET);
	}
	public boolean isForceEnd() {
		return (readInt(IS_FORCE_END_OFFSET)!=0);
	}
	public boolean isRestart() {
		return (readInt(IS_RESTART_OFFSET)!=0);
	}
	public void setStartInfo(Map startInfo) {
		this.startupInfo = startInfo;
	}
	public void setName(String name) {
		writeString(NAME_OFFSET, NAME_MAX, name);
	}
	public void setWatching(boolean isWatching) {
		writeInt(IS_WATCHING_OFFSET,isWatching?1:0);
	}
	
	public void heartBeat(){
		writeLong(LAST_HEART_BEAT_OFFSET,System.currentTimeMillis());
	}
	public void setForceEnd(boolean isForceEnd) {
		writeInt(IS_FORCE_END_OFFSET,isForceEnd?1:0);
	}
	public void setRestart(boolean isRestart) {
		writeInt(IS_RESTART_OFFSET,isRestart?1:0);
	}
	public boolean isRun() {
		return isRun;
	}
	public void setRun(boolean isRun) {
		this.isRun = isRun;
	}
}
