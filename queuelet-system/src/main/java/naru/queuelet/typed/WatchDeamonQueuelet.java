/*
 * 作成日: 2004/08/03
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.queuelet.watch.StartupInfo;
import naru.queuelet.watch.WatchFile;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class WatchDeamonQueuelet implements Queuelet,Runnable {
	static private Logger logger=Logger.getLogger(WatchDeamonQueuelet.class);
	private static final int NOMAL=0;
	private static final int RESTART=1;
	private static final int FORCE_END=2;
	private static final int DEMON_STOP=3;
	private static final int RETRY_OVER=4;

	private QueueletContext context;
	private Thread thread;
	
	private int type=StartupInfo.TYPE_QUEUELET;
	private long interval=5000;
	
	private String name;
	private int restartLimit;
	private long heartBeatLimit;
	private String queueletConf;
	private String queueletArgs[];
	private String javaArgs[];
	private String[] javaVmOptions;
	private int javaHepSize;

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		return false;
	}

	private String[] arryParam(Map param,String key){
		String lengthParam=(String)param.get(key+".length");
		if(lengthParam==null){
			return null;
		}
		int length=Integer.parseInt(lengthParam);
		String[] result=new String[length];
		for(int i=0;i<length;i++){
			result[i]=(String)param.get(key+"."+i);
		}
		return result;
	}
	
	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletCommand, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.name=(String)param.get("watch.name");
		String heartBeatLimit=(String)param.get("watch.heartBeatLimit");
		this.heartBeatLimit=Long.parseLong(heartBeatLimit);
		String restartLimit=(String)param.get("watch.restartLimit");
		this.restartLimit=Integer.parseInt(restartLimit);
		String javaHeapSize=(String)param.get("java.heapSize");
		this.javaHepSize=Integer.parseInt(javaHeapSize);
		this.queueletConf=(String)param.get("queuelet.conf");
		this.javaVmOptions=arryParam(param,"java.vmOption");
		this.queueletArgs=arryParam(param,"queuelet.arg");
		this.javaArgs=arryParam(param,"java.arg");
		thread=new Thread(this);
		thread.start();
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}
	
	
	private String getJavaCommand(){
		String javaHome=System.getenv("JAVA_HOME");
		if(javaHome==null){
			javaHome=System.getProperty("java.home");
			if(javaHome==null){
				logger.error("fail to getJavaHome");
			}
		}
		return javaHome+File.separator+"bin"+File.separator+"java";
	}
	private String getClassPath(){
		String bootJar=System.getProperty("java.class.path");
		return bootJar;
	}
	
	private int check(WatchFile watchFile,int retryCount){
		if(watchFile.isRun()){//起動中
			if(watchFile.isForceEnd()){//強制停止要求
				logger.info("forceEnd:"+name);
				watchFile.setForceEnd(false);
				return FORCE_END;
			}
			long interval=System.currentTimeMillis()-watchFile.getLastHeartBeat();
			if(heartBeatLimit>0 && interval>heartBeatLimit){
				logger.info("hungup:"+name);//音信不通
				return FORCE_END;
			}
			return NOMAL;
		}
		//停止中
		if(watchFile.isRestart()){
			if( restartLimit>0 && retryCount>=restartLimit){
				logger.info("hangup:"+name);//音信不通
				return RETRY_OVER;
			}
			return RESTART;
		}else{
			return DEMON_STOP;
		}
	}

	private void execChild(WatchFile watchFile){
		StartupInfo resStartupInfo=watchFile.getResponseStartupInfo();
		if(resStartupInfo!=null && resStartupInfo.getType()!=type){
			//typeが違うstartupInfoは無効
			resStartupInfo=null;
		}
		int cmdLength=1 + /* command */ /* javaVmOptions */ 
					  2 + /*-XmsXXm -XmxXXm */ 
					  2 + /* -cp ssssssssssss */
					  1 + /* naru.queuelet.startup.Startup */
					  1;// conf.xml /* args */
		String[] vmOption=javaVmOptions;
		if(resStartupInfo!=null&&resStartupInfo.getJavaVmOptions()!=null){
			vmOption=resStartupInfo.getJavaVmOptions();
		}
		if(vmOption!=null){
			cmdLength+=vmOption.length;
		}
		int heapSize=this.javaHepSize;
		if(resStartupInfo!=null&&resStartupInfo.getJavaHeapSize()>0){
			heapSize=resStartupInfo.getJavaHeapSize();
		}
		String[] args=queueletArgs;
		if(resStartupInfo!=null&&resStartupInfo.getArgs()!=null){
			args=resStartupInfo.getArgs();
		}
		if(args!=null){
			cmdLength+=args.length;
		}
		//cmdを構築
		String[] cmd=new String[cmdLength];
		int pos=0;
		cmd[pos]=getJavaCommand();
		pos++;
		if(vmOption!=null){
			System.arraycopy(vmOption, 0, cmd,pos,vmOption.length);
			pos+=vmOption.length;
		}
		cmd[pos]="-Xms" + heapSize +"m";
		pos++;
		cmd[pos]="-Xms" + heapSize +"m";
		pos++;
		cmd[pos]="-cp";
		pos++;
		cmd[pos]=getClassPath();
		pos++;
		cmd[pos]="naru.queuelet.startup.Startup";
		pos++;
		cmd[pos]=queueletConf;
		pos++;
		if(args!=null){
			System.arraycopy(args, 0, cmd,pos,args.length);
			pos+=args.length;
		}
		
		StartupInfo startupInfo=new StartupInfo();//次プロセスに通知するstartupInfo
		startupInfo.setName(name);
		startupInfo.setArgs(args);
		startupInfo.setJavaHeapSize(heapSize);
		startupInfo.setJavaVmOptions(vmOption);
		
		logger.info("execChild cmd");
		for(int i=0;i<cmd.length;i++){
			logger.info(i +":" +cmd[i]);
		}
		watchFile.execChild(cmd, null,startupInfo);
	}
	
	private void watch(WatchFile watchFile,File stopFile) throws IOException{
		int retryCount=0;
		while(!stopFile.exists()){
			int result=check(watchFile,retryCount);
			if(result==FORCE_END){
				watchFile.terminateChild();
			}else if(result==RESTART){
				execChild(watchFile);
				retryCount++;
			}else if(result==RETRY_OVER || result==DEMON_STOP){
				break;
			}else if(result==NOMAL){
				retryCount=0;
			}
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
		}
	}
	
	//監視スレッド
	public void run(){
		WatchFile watchFile=null;
		File stopFile=WatchFile.getStopFlagFile();
		try{
			watchFile=WatchFile.createWatchFile(name);
			watch(watchFile,stopFile);
		} catch (IOException e) {
			logger.error("watch error.",e);
		}finally{
			if(watchFile!=null && watchFile.isRun()){
				watchFile.terminateChild();
			}
			if(stopFile.exists()){
				stopFile.delete();
			}
			context.finish();
			logger.info("WatchDeamon end");
		}
	}

}
