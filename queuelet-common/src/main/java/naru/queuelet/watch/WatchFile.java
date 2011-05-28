package naru.queuelet.watch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


public class WatchFile {
	/**
	 * watchFileに保持するデータ
	 * 名前 name 4 +16　
	 * 監視中　isWatching 4 
	 * 監視対象認知 isWatched 4
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
	private static final int IS_WATCHED_OFFSET=IS_WATCHING_OFFSET+4;
	private static final int LAST_HEART_BEAT_OFFSET=IS_WATCHED_OFFSET+4;
	private static final int IS_FORCE_END_OFFSET=LAST_HEART_BEAT_OFFSET+4;
	private static final int IS_RESTART_OFFSET=IS_FORCE_END_OFFSET+4;
	private static final int WATCH_SHARE_OFFSET=IS_RESTART_OFFSET+4;
	
	/* 以下２つの環境変数が設定されていれば、監視対象のコンテナ */
	public static final String QUEUELET_WATCH_TOKEN_ENV="QUEUELET_WATCH_TOKEN";
	public static final String QUEUELET_WATCH_NAME_ENV="QUEUELET_WATCH_NAME";
	
	private static final String QUEUELET_HOME="QUEUELET_HOME";
	private static final String WATCH_DIR_NAME="watch";
	private static final String WATCH_STOP_FLAG_FILE="watchStrop.flg";
	static final String WATCH_FILE_EXT=".wch";
	
	private File watchFile;
	private MappedByteBuffer watchFileBuffer;
	private FileChannel watchFileChannel;
	private RandomAccessFile watchAccessFile;
	
	private StartupInfo startupInfo;//childに通知する起動情報

	/* parameterを変更して再起動するには、tokenが必要.tokenを知らなければ再起動はできるがparameterの変更は不可 */
	private String token;//ファイル上でやりとりされな、親と子だけが知る文字列
	private boolean isDeamon;//Deamonが使っているか、監視対象がつかっているか？
	private boolean isRun=false;//Deamonだけが知ればよい情報なので共用領域にはおかない
	private WatchProcess watchProcess=null;//Demonから使う

	public static StartupInfo deserializseStartupInfo(RandomAccessFile is,int length) throws IOException{
		byte[] data=new byte[length];
		int pos=0;
		while(pos<length){
			int readLen=is.read(data,pos,length-pos);
			if(readLen<=0){
				break;
			}
			pos+=readLen;
		}
		if(pos<length){
			return null;
		}
		return deserializseStartupInfo(data);
	}
	
	public static StartupInfo deserializseStartupInfo(byte[] data) throws IOException{
		ByteArrayInputStream bais=new ByteArrayInputStream(data);
		ObjectInputStream ois=new ObjectInputStream(bais);
		try {
			return (StartupInfo)ois.readObject();
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	public static byte[] serializseStartupInfo(StartupInfo startupInfo) throws IOException{
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		ObjectOutputStream oos=new ObjectOutputStream(baos);
		oos.writeObject(startupInfo);
		oos.close();
		return baos.toByteArray();
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
	
	public static File getWatchDir(){
		String queueletHome=System.getProperty(QUEUELET_HOME);
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
	
	public static File getStopFlagFile(){
		File watchDir=getWatchDir();
		return new File(watchDir,WATCH_STOP_FLAG_FILE);
	}

	//Deamonが監視対象を管理するために使用
	public static WatchFile createWatchFile(String name) throws IOException{
		File file=getWatchFile(name);
		if(file.exists()){
			if(!file.delete()){
				return null;//既に監視中
			}
		}
		WatchFile watchFile=new WatchFile(name);
		watchFile.setName(name);
		watchFile.setForceEnd(false);
		watchFile.setRestart(true);
		watchFile.isDeamon=true;
        return watchFile;
	}
	
	//監視対象が自分のWatchFileを取得する時に使用
	public static WatchFile mapWatchFile() throws IOException{
		String name=System.getenv(QUEUELET_WATCH_NAME_ENV);
		if(name==null){
			return null;
		}
		WatchFile watchFile=new WatchFile(name);
		if(!name.equals(watchFile.getName())){
			//正しいwatchファイルじゃない
			return null;
		}
		if(watchFile.isWatched()){
			//既に監視中
			return null;
		}
		watchFile.setWatched(true);
		watchFile.loadStartupInfo();
		watchFile.token=System.getenv(QUEUELET_WATCH_TOKEN_ENV);
		watchFile.isDeamon=false;
		return watchFile;
	}
	
    private static final char[] hexmap = {
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'a', 'b', 'c', 'd', 'e', 'f'};
	
	private WatchFile(String name) throws IOException{
		this.watchFile=getWatchFile(name);
		this.watchAccessFile=new RandomAccessFile(watchFile,"rw");
		this.watchFileChannel=watchAccessFile.getChannel();
		this.watchFileBuffer=watchFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, (long)WATCH_SHARE_OFFSET);
	}

	public boolean execChild(String[] cmd,String[] env){
		if(!isDeamon || watchProcess!=null){
			return false;
		}
		//起動する前にtokenを作成する。
		SecureRandom random;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		random.setSeed(("$_23##"+System.currentTimeMillis()).getBytes());
		byte[] bytes=new byte[16];
		random.nextBytes(bytes);
        char[] buffer = new char[bytes.length*2];
        int pos=0;
        for (int i=0; i<bytes.length; i++) {
            int low = (int) (bytes[pos+i] & 0x0f);
            int high = (int) ((bytes[pos+i] & 0xf0) >> 4);
            buffer[i*2] = hexmap[high];
            buffer[i*2 + 1] = hexmap[low];
        }
        token=new String(buffer);
		WatchProcess watchProcess=new WatchProcess(this,cmd,env);
		watchProcess.start();
		return true;
	}
	
	public boolean terminateChild(){
		if(watchProcess==null){
			return false;
		}
		watchProcess.stop();
		return true;
	}
	
	public void term(){
		terminateChild();
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
	
	private void loadStartupInfo() throws IOException{
		watchAccessFile.seek(WATCH_SHARE_OFFSET);
		int length=(int)watchAccessFile.length()-WATCH_SHARE_OFFSET;
		startupInfo=deserializseStartupInfo(watchAccessFile,length);
	}
	
	public void save() throws IOException{
		saveStartupInfo();
	}
	
	private void saveStartupInfo() throws IOException{
		byte[] data=serializseStartupInfo(startupInfo);
		watchAccessFile.seek(WATCH_SHARE_OFFSET);
		watchAccessFile.write(data);
	}
	
	//監視対象終了時に次回起動時指定するレスポンスファイル
	public File getResponseFile(){
		File watchFile=new File(getWatchDir(),getName()+getToken()+WATCH_FILE_EXT);
		return watchFile;
	}
	
	public StartupInfo getResponseStartupInfo(){
		if(watchProcess==null){
			return null;
		}
		if(isRun){
			return null;
		}
		//Deamonがレスポンスファイルを確認すれば、watchProcessはいらない
		StartupInfo responseStartupInfo=watchProcess.getResponseStartupInfo();
		watchProcess=null;
		return responseStartupInfo;
	}
	
	public StartupInfo getStartupInfo() {
		return startupInfo;
	}
	public String getName() {
		return readString(NAME_OFFSET, NAME_MAX);
	}
	public boolean isWatching() {
		return (readInt(IS_WATCHING_OFFSET)!=0);
	}
	public boolean isWatched() {
		return (readInt(IS_WATCHED_OFFSET)!=0);
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
	public void setStartupInfo(StartupInfo startInfo) {
		this.startupInfo = startInfo;
	}
	public void setName(String name) {
		writeString(NAME_OFFSET, NAME_MAX, name);
	}
	public void setWatching(boolean isWatching) {
		writeInt(IS_WATCHING_OFFSET,isWatching?1:0);
	}
	public void setWatched(boolean isWatched) {
		writeInt(IS_WATCHED_OFFSET,isWatched?1:0);
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

	public String getToken() {
		return token;
	}
}
