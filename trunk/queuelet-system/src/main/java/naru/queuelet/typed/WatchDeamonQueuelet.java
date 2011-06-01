/*
 * 作成日: 2004/08/03
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.io.File;
import java.io.IOException;
import java.util.Date;
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
	//起動後、この時間経過すればretryCountをリセットする
	private int retryResetInterval=60000;
	
	private String name;
	private int restartLimit;
	private long heartBeatLimit;
	private String queueletConf;
	
	/* 起動以降には前回の起動情報を格納、異常再起動に備える */
	private String queueletArgs[];
	private String javaArgs[];
	private String[] javaVmOptions;
	private int javaHeapSize;
	
	/* 異常再起動する時のオプション */
	private String recoverQueueletArgs[];
	private String recoverJavaArgs[];
	private String[] recoverJavaVmOptions;
	private int recoverJavaHeapSize;

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
		String interval=(String)param.get("watch.interval");
		if(interval!=null){
			this.interval=Long.parseLong(interval);
		}
		String retryResetInterval=(String)param.get("watch.retryResetInterval");
		if(retryResetInterval!=null){
			this.retryResetInterval=Integer.parseInt(retryResetInterval);
		}
		
		this.name=(String)param.get("watch.name");
		String heartBeatLimit=(String)param.get("watch.heartBeatLimit");
		this.heartBeatLimit=Long.parseLong(heartBeatLimit);
		String restartLimit=(String)param.get("watch.restartLimit");
		this.restartLimit=Integer.parseInt(restartLimit);
		String javaHeapSize=(String)param.get("java.heapSize");
		if(javaHeapSize!=null){
			this.javaHeapSize=Integer.parseInt(javaHeapSize);
		}else{
			this.javaHeapSize=64;//指定がなければ64mとする
		}
		this.queueletConf=(String)param.get("queuelet.conf");
		this.javaVmOptions=arryParam(param,"java.vmOption");
		if(this.javaVmOptions==null){
			this.javaVmOptions=new String[0];
		}
		this.queueletArgs=arryParam(param,"queuelet.arg");
		if(this.queueletArgs==null){
			this.queueletArgs=new String[0];
		}
		this.javaArgs=arryParam(param,"java.arg");
		if(this.javaArgs==null){
			this.javaArgs=new String[0];
		}
		
		String recoverHeapSize=(String)param.get("java.recoverHeapSize");
		if(recoverHeapSize!=null){
			this.recoverJavaHeapSize=Integer.parseInt(recoverHeapSize);
		}else{
			this.recoverJavaHeapSize=-1;//指定がなければ前回起動時
		}
		this.recoverJavaVmOptions=arryParam(param,"java.recoverVmOption");
		this.recoverQueueletArgs=arryParam(param,"queuelet.recoverArg");
		this.recoverJavaArgs=arryParam(param,"java.recoverArg");
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
			long lastHeartBeat=watchFile.getLastHeartBeat();
			long heartBeartInterval=System.currentTimeMillis()-lastHeartBeat;
			if(heartBeatLimit>0 && heartBeatLimit<heartBeartInterval){
				logger.info("hungup:"+name+":" + new Date(lastHeartBeat) +":"+heartBeartInterval);//音信不通
				return FORCE_END;
			}
			return NOMAL;
		}
		//停止中
		if(watchFile.isRestart()){
			if( restartLimit>0 && retryCount>=restartLimit){
				logger.info("retry over:"+name);//音信不通
				return RETRY_OVER;
			}
			logger.info("restart:"+name);//再起動
			return RESTART;
		}else{
			return DEMON_STOP;
		}
	}

	private void execChild(WatchFile watchFile){
		StartupInfo resStartupInfo=watchFile.getResponseStartupInfo();
		if(resStartupInfo!=null){
			logger.info("receive startupInfo:"+name);
			if(resStartupInfo.getType()!=type){
				logger.warn("startupInfo type is missmatch:"+resStartupInfo.getType());
				//typeが違うstartupInfoは無効
				resStartupInfo=null;
			}
		}
		int cmdLength=1 + /* command */ /* javaVmOptions */
					  1 + /* -DQUEUELET_HOME="" */
					  2 + /*-XmsXXm -XmxXXm */ 
					  2 + /* -cp ssssssssssss */
					  1 + /* naru.queuelet.startup.Startup */
					  1;// conf.xml /* args */
		if(resStartupInfo!=null&&resStartupInfo.getJavaVmOptions()!=null){
			javaVmOptions=resStartupInfo.getJavaVmOptions();
		}else if(this.recoverJavaVmOptions!=null){
			javaVmOptions=recoverJavaVmOptions;
		}
		cmdLength+=javaVmOptions.length;
		
		if(resStartupInfo!=null&&resStartupInfo.getJavaHeapSize()>0){
			javaHeapSize=resStartupInfo.getJavaHeapSize();
		}else if(recoverJavaHeapSize>0){
			javaHeapSize=recoverJavaHeapSize;
		}
		if(resStartupInfo!=null&&resStartupInfo.getArgs()!=null){
			queueletArgs=resStartupInfo.getArgs();
		}else if(recoverQueueletArgs!=null){
			queueletArgs=this.recoverQueueletArgs;
		}
		cmdLength+=queueletArgs.length;
		
		//cmdを構築
		String[] cmd=new String[cmdLength];
		int pos=0;
		cmd[pos]=getJavaCommand();
		pos++;
		cmd[pos]="-DQUEUELET_HOME="+ System.getProperty("QUEUELET_HOME");
		pos++;
		if(javaVmOptions!=null){
			System.arraycopy(javaVmOptions, 0, cmd,pos,javaVmOptions.length);
			pos+=javaVmOptions.length;
		}
		cmd[pos]="-Xms" + javaHeapSize +"m";
		pos++;
		cmd[pos]="-Xms" + javaHeapSize +"m";
		pos++;
		cmd[pos]="-cp";
		pos++;
		cmd[pos]=getClassPath();
		pos++;
		cmd[pos]="naru.queuelet.startup.Startup";
		pos++;
		cmd[pos]=queueletConf;
		pos++;
		if(queueletArgs!=null){
			System.arraycopy(queueletArgs, 0, cmd,pos,queueletArgs.length);
			pos+=queueletArgs.length;
		}
		
		StartupInfo startupInfo=new StartupInfo();//次プロセスに通知するstartupInfo
		startupInfo.setType(type);
		startupInfo.setName(name);
		startupInfo.setJavaVmOptions(javaVmOptions);
		startupInfo.setJavaHeapSize(javaHeapSize);
		startupInfo.setArgs(queueletArgs);
		
		logger.info("restartCount:" + watchFile.getStartupInfo().getRestartCount() +" cmd:");
		for(int i=0;i<cmd.length;i++){
			logger.info(i +":" +cmd[i]);
		}
		watchFile.execChild(cmd, null,startupInfo);
	}
	
	private void watch(WatchFile watchFile,File stopFile) throws IOException{
		int retryCount=0;
		long retryResetTime=0;
		while(!stopFile.exists()){
			int result=check(watchFile,retryCount);
			if(result==FORCE_END){
				watchFile.terminateChild();
				retryResetTime=System.currentTimeMillis()+retryResetInterval;
			}else if(result==RESTART){
				execChild(watchFile);
				retryCount++;
			}else if(result==RETRY_OVER || result==DEMON_STOP){
				break;
			}else if(result==NOMAL){
				if(retryResetTime>0 && System.currentTimeMillis()>=retryResetTime){
					logger.info("start success:"+name);
					retryCount=0;
					retryResetTime=0;
				}
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
			if(watchFile==null){
				logger.error("fail to createWatchFile.name:"+name);
				return;//
			}
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
