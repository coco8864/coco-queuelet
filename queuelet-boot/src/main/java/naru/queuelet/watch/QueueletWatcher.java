package naru.queuelet.watch;

import java.io.IOException;

public class QueueletWatcher {
	private WatchInfo watchInfo;
	private String vmOption;
	private int xmx;
	private String[] args;
	
	//JAVA_HOME/java -DQUEUELET_HOME=xxx 
	// -XmsYYm -XmxYYm  
	// -DZZZZ -DESS 
	//-jar xxx/bin/xxx.jar 
	//arg[0] arg[1] arg[2]
	
	public static void deamon(String[] args){
		WatchDeamon watchDeamon=WatchDeamon.create(name, commandLine, env, heartBeatLimit, restartMax);
		synchronized(watchDeamon){
			while(true){
				if(!watchDeamon.isWatching()){
					break;
				}
				watchDeamon.wait();
			}
		}
	}
	
	public static QueueletWatcher setup(){
		QueueletWatcher queueletWatcher=new QueueletWatcher(name);
		return queueletWatcher;
	}
	
	private QueueletWatcher(String name) throws IOException{
		watchInfo=WatchInfo.create(name);
	}
	
	public void end(){
		watchInfo.setIsRestart(false);
	}
	public void restart(String vmOption,int xmx){
		watchInfo.setCommandLine(commandLine);
		watchInfo.setIsRestart(true);
	}
	public void heartbeat(){
		watchInfo.heartBeat();
	}

}
