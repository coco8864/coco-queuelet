/*
 * �쐬��: 2004/08/03
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
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
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
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
	//�N����A���̎��Ԍo�߂����retryCount�����Z�b�g����
	private int retryResetInterval=60000;
	
	private String name;
	private int restartLimit;
	private long heartBeatLimit;
	private String queueletConf;
	
	/* �N���ȍ~�ɂ͑O��̋N�������i�[�A�ُ�ċN���ɔ����� */
	private String queueletArgs[];
	private String javaArgs[];
	private String[] javaVmOptions;
	private int javaHeapSize;
	
	/* �ُ�ċN�����鎞�̃I�v�V���� */
	private String recoverQueueletArgs[];
	private String recoverJavaArgs[];
	private String[] recoverJavaVmOptions;
	private int recoverJavaHeapSize;

	/* (�� Javadoc)
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
	
	/* (�� Javadoc)
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
			this.javaHeapSize=64;//�w�肪�Ȃ����64m�Ƃ���
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
			this.recoverJavaHeapSize=-1;//�w�肪�Ȃ���ΑO��N����
		}
		this.recoverJavaVmOptions=arryParam(param,"java.recoverVmOption");
		this.recoverQueueletArgs=arryParam(param,"queuelet.recoverArg");
		this.recoverJavaArgs=arryParam(param,"java.recoverArg");
		thread=new Thread(this);
		thread.start();
	}

	/* (�� Javadoc)
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
		if(watchFile.isRun()){//�N����
			if(watchFile.isForceEnd()){//������~�v��
				logger.info("forceEnd:"+name);
				watchFile.setForceEnd(false);
				return FORCE_END;
			}
			long lastHeartBeat=watchFile.getLastHeartBeat();
			long heartBeartInterval=System.currentTimeMillis()-lastHeartBeat;
			if(heartBeatLimit>0 && heartBeatLimit<heartBeartInterval){
				logger.info("hungup:"+name+":" + new Date(lastHeartBeat) +":"+heartBeartInterval);//���M�s��
				return FORCE_END;
			}
			return NOMAL;
		}
		//��~��
		if(watchFile.isRestart()){
			if( restartLimit>0 && retryCount>=restartLimit){
				logger.info("retry over:"+name);//���M�s��
				return RETRY_OVER;
			}
			logger.info("restart:"+name);//�ċN��
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
				//type���ႤstartupInfo�͖���
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
		
		//cmd���\�z
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
		
		StartupInfo startupInfo=new StartupInfo();//���v���Z�X�ɒʒm����startupInfo
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
	
	//�Ď��X���b�h
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
