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

/* WatchDeamonとWatchInfoは、汎用ツールとして使用できるように設計
 * javaやqueuelet containerに限らず、ダウン監視、再起動ができるようにしている。
 */
public class WatchDeamon implements Runnable{
	private static final long INTERVAL=5000;//監視間隔
	
	private static final int NOMAL=0;
	private static final int RESTART=1;
	private static final int FORCE_END=2;
	private static final int DEMON_STOP=3;
	private static final int RETRY_OVER=4;
	
	private String name;
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
		this.name=name;
		if(commandLine==null){
			watchInfo=WatchInfo.create(name,true);
		}else{
			watchInfo=WatchInfo.create(name,true,commandLine,env,heartBeatLimit,restartMax);
		}
		if(watchInfo!=null && !name.equals(watchInfo.getName())){
			System.out.println("Deamon:name error."+name +":"+watchInfo.getName());
			watchInfo.term();
			watchInfo=null;
		}
		thread=new Thread(this);
		thread.start();
	}
	
	private int check(WatchInfo watchInfo,int retryCount){
		if(watchInfo==null){
			System.out.println("Deamon:fail to entry:"+name);//音信不通
			return DEMON_STOP;
		}
		if(watchInfo.isRun()){//起動中
			if(watchInfo.isForceEnd()){//強制停止要求
				System.out.println("Deamon:forceEnd:"+name);
				watchInfo.setIsForceEnd(false);
				watchInfo.termanateChild();
				return FORCE_END;
			}
			long interval=System.currentTimeMillis()-watchInfo.getLastHeartBeat();
			long heatBeatLimit=watchInfo.getHeartBeatLimist();
			if(heatBeatLimit>0 && interval>heatBeatLimit){
				System.out.println("Deamon:hangup:"+name);//音信不通
				watchInfo.termanateChild();
				return FORCE_END;
			}
			return NOMAL;
		}
		//停止中
		if(watchInfo.isRestart()){
			int restartLimit=watchInfo.getRestartLimit();
			if( restartLimit>0 && retryCount>=restartLimit){
				System.out.println("Deamon:retry over:"+name);//音信不通
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
			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
			}
		}
		System.out.println("Deamon:end:"+name);
		if(watchInfo!=null){
			watchInfo.term();
		}
		synchronized(this){//終了を待ち合わせる事ができる
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
		System.exit(0);
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
	 * 1)watchDirを監視して、処理対象がみつかれば、起動および監視を行う。
	 * 2)watchDirにstopファイルがみつかれば処理をやめる
	 * 3)watchDirに処理対象をエントリーするには、WatchInfo.cleateメソッドでできる
	 * 4)watchInfoコマンドで処理対象をエントリーができる
	 * 5)watchInfoコマンドで監視デーモンの停止ができる
	 * 6)デーモン停止時には、監視対象を終了させる
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
