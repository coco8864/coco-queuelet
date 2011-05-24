package naru.queuelet.watch;

import java.io.IOException;

public class WatchDeamon implements Runnable{
	private static final long INTERVAL=5000;//監視間隔
	
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
		if(watchInfo.isRun()){//起動中
			if(watchInfo.isForceEnd()){//強制停止要求
				System.out.println("Deamon:forceEnd:"+watchInfo.getName());
				watchInfo.setIsForceEnd(false);
				watchInfo.termanateChild();
				return FORCE_END;
			}
			long interval=System.currentTimeMillis()-watchInfo.getLastHeartBeat();
			long heatBeatLimit=watchInfo.getHeartBeatLimist();
			if(heatBeatLimit>0 && interval>heatBeatLimit){
				System.out.println("Deamon:hangup:"+watchInfo.getName());//音信不通
				watchInfo.termanateChild();
				return FORCE_END;
			}
			return NOMAL;
		}
		//停止中
		if(watchInfo.isRestart()){
			if(retryCount>=watchInfo.getRestartLimit()){
				System.out.println("Deamon:retry over:"+watchInfo.getName());//音信不通
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
		synchronized(this){//終了を待ち合わせる事ができる
			watchInfo=null;
			notify();
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		WatchDeamon deamon=WatchDeamon.create("test","c:/windows/notepad.exe a.txt",null,-1,3);
	}
}
