package naru.queuelet.watch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* WatchDeamon��WatchInfo�́A�ėp�c�[���Ƃ��Ďg�p�ł���悤�ɐ݌v
 * java��queuelet container�Ɍ��炸�A�_�E���Ď��A�ċN�����ł���悤�ɂ��Ă���B
 */
public class WatchDeamon implements Runnable{
	private static final long INTERVAL=5000;//�Ď��Ԋu
	
	private static final int NOMAL=0;
	private static final int RESTART=1;
	private static final int FORCE_END=2;
	private static final int DEMON_STOP=3;
	private static final int RETRY_OVER=4;
	
	private WatchInfo watchInfo;
	private Thread thread;
	
	public static WatchDeamon create(String name) throws IOException{
		return new WatchDeamon(name,null,null,-1,-1);
	}
	public static WatchDeamon create(String name,String commandLine,String[] env,long heartBeatLimit,int restartMax) throws IOException{
		return new WatchDeamon(name,commandLine,env,heartBeatLimit,restartMax);
	}
	
	public boolean isWatching(){
		return (watchInfo!=null);
	}
	
	private WatchDeamon(String name,String commandLine,String[] env,long heartBeatLimit,int restartMax) throws IOException{
		if(commandLine==null){
			watchInfo=WatchInfo.create(name,true);
		}else{
			watchInfo=WatchInfo.create(name,true,commandLine,env,heartBeatLimit,restartMax);
		}
		thread=new Thread(this);
		thread.start();
	}
	
	private int check(WatchInfo watchInfo,int retryCount){
		if(watchInfo.isRun()){//�N����
			if(watchInfo.isForceEnd()){//������~�v��
				System.out.println("Deamon:forceEnd:"+watchInfo.getName());
				watchInfo.setIsForceEnd(false);
				watchInfo.termanateChild();
				return FORCE_END;
			}
			long interval=System.currentTimeMillis()-watchInfo.getLastHeartBeat();
			long heatBeatLimit=watchInfo.getHeartBeatLimist();
			if(heatBeatLimit>0 && interval>heatBeatLimit){
				System.out.println("Deamon:hangup:"+watchInfo.getName());//���M�s��
				watchInfo.termanateChild();
				return FORCE_END;
			}
			return NOMAL;
		}
		//��~��
		if(watchInfo.isRestart()){
			if(retryCount>=watchInfo.getRestartLimit()){
				System.out.println("Deamon:retry over:"+watchInfo.getName());//���M�s��
				return RETRY_OVER;
			}
			watchInfo.executeChild();
			return RESTART;
		}else{
			watchInfo.term();
			return DEMON_STOP;
		}
	}
	
	public void run() {
		int retryCount=0;
		while(true){
			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
			}
			int ret=check(watchInfo,retryCount);
			if(ret==DEMON_STOP){
				break;
			}
			if(ret==RETRY_OVER){
				break;
			}
			if(ret==RESTART){
				retryCount++;
			}else if(ret!=FORCE_END){
				retryCount=0;
			}
		}
		System.out.println("Deamon:end:"+watchInfo.getName());
		watchInfo.term();
		synchronized(this){//�I����҂����킹�鎖���ł���
			watchInfo=null;
			notify();
		}
	}
	
	private static String getName(File watchFile){
		String fileName=watchFile.getName();
		int length=fileName.length();
		return fileName.substring(0, length-WatchInfo.WATCH_FILE_EXT.length());
	}
	
	private static void entry(Map childlen,File[] watchFiles){
		for(int i=0;i<watchFiles.length;i++){
			File watchFile=watchFiles[i];
			String name=getName(watchFile);
			if(childlen.get(name)!=null){
				continue;
			}
			WatchDeamon deamon=null;
			try {
				deamon=WatchDeamon.create(name);
			} catch (IOException e) {
				System.out.println("Deamon:fail to create:"+name);
				e.printStackTrace();
			}
			if(deamon!=null){
				System.out.println("Deamon:new entry:"+name);
				childlen.put(name, deamon);
			}else{
				childlen.put(name, new Object());
			}
		}
	}

	private static void unentry(Map childlen,File[] watchFiles){
		Set names=new HashSet();
		for(int i=0;i<watchFiles.length;i++){
			names.add(getName(watchFiles[i]));
		}
		Iterator itr=childlen.keySet().iterator();
		while(itr.hasNext()){
			String name=(String)itr.next();
			if(!names.contains(name)){
				itr.remove();
			}
		}
	}
	
	private static void deamon(){
		Map childlen=new HashMap();
		File watchDir=WatchInfo.getWatchDir();
		File stopFile=WatchInfo.getStopFlagFile();
		System.out.println("Deamon:start.watchDir:"+watchDir.getAbsolutePath());
		while(!stopFile.exists()){
			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
			}
			File[] watchFiles=watchDir.listFiles(watchFileFilter);
			entry(childlen,watchFiles);
			unentry(childlen,watchFiles);
		}
		stopFile.delete();
		System.out.println("Deamon:stop");
	}
	
	private static FilenameFilter watchFileFilter=new FilenameFilter() {  
			public boolean accept(File file, String name) {  
			return name.endsWith(WatchInfo.WATCH_FILE_EXT);
			}  
		};
    
	private static void usage(){
		System.out.println("usage:java "+ WatchDeamon.class.getName()+ " [${name}|stop|start] commandLine interval count");
	}
	
		
	/**
	 * 1)watchDir���Ď����āA�����Ώۂ��݂���΁A�N������ъĎ����s���B
	 * 2)watchDir��stop�t�@�C�����݂���Ώ�������߂�
	 * 3)watchDir�ɏ����Ώۂ��G���g���[����ɂ́AWatchInfo.cleate���\�b�h�łł���
	 * 4)watchInfo�R�}���h�ŏ����Ώۂ��G���g���[���ł���
	 * 5)watchInfo�R�}���h�ŊĎ��f�[�����̒�~���ł���
	 * 6)�f�[������~���ɂ́A�Ď��Ώۂ��I��������
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length==0){
			usage();
			return;
		}
		String name=args[0];
		if("stop".endsWith(name)){
			File stopFile=WatchInfo.getStopFlagFile();
			(new FileOutputStream(stopFile)).close();
			return;
		}else if("start".endsWith(name)){
			deamon();
			return;
		}
		if(args.length<2){
			usage();
			return;
		}
		String commandLine=args[1];
		long interval=-1;
		int count=-1;
		if(args.length>=3){
			interval=Long.parseLong(args[2]);
		}
		if(args.length>=4){
			count=Integer.parseInt(args[3]);
		}
		WatchInfo watchInfo=WatchInfo.create(name,false,commandLine,null,interval,count);
		if(watchInfo==null){
			System.out.println("fail to watch:"+name);
		}else{
			System.out.println("OK:"+name);
		}
	}
}
