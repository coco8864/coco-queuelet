package naru.queuelet.watch;

import java.io.File;
import java.io.IOException;

/*
 * コマンドライン解析
 * 1)"で囲まれているところは、１つの引数
 * 2)1)以外のところは、スペース区切で引数を解釈
 * [(".*")|(.*)]*
 */

public class QueueletWatcher {
	private WatchInfo watchInfo;
	private String vmOption;
	private int xmx;
	private String[] args;
	
	/*
	private String buildCommandLine(){
		String javaHome=System.getenv("JAVA_HOME");
		String queueletHome=System.getenv("QUEUELET_HOME");
		StringBuffer sb=new StringBuffer();
		sb.append(javaHome);
		sb.append(File.separatorChar);
		sb.append("bin");
		sb.append(File.separatorChar);
		sb.append("java");
		sb.append(" ");
		sb.append("-Xmx");
		sb.append(xmx);
		sb.append("m -Xms");
		sb.append(xmx);
		sb.append("m ");
	}
	*/
	
	
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
