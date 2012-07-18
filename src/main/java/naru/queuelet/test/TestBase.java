package naru.queuelet.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
/**
 * Queuelet������Unit�e�X�g����ہA���̃��\�b�h��extends���Ďg�p
 * 
 * 1)����Class��extends����B
 * 2)@BeforeClass����setupContainer���Ăяo��
 * 3)@AfterClass����stopContainer���Ăяo��
 * 4)�ȉ��̋L�q��queuelet��`�t�@�C���ɋL�q
====================================
<!-- for test start -->
<loader name="main" callStack="true" delegate="false" resouceLoader="parent">
</loader>
<terminal name="queuelet.test" threadCount="1">
  <queuelet loaderName="main" className="${queuelet.test.class}">
 </queuelet>
</terminal>
<!-- for test end -->
====================================
 * 
 * @author naru
 *
 */
public abstract class TestBase implements Queuelet {
	private static final String INIT_QUEUELET_TEST_PROPERTIES="QueueletTest.properties";
	private static final long INIT_METHOD_TIMEOUT=10000;
	private static final String TEST_TERMINAL_NAME="queuelet.test";
	private static final String TEST_CLASS="queuelet.test.class";
	private static final String TEST_CONTAINER_STATUS="queuelet.test.container.startus";//�V�X�e���v���p�e�B�ɐݒ�,
	private static final String MARK_BOOT="boot";
	private static final String MARK_START="start";
	private static final String MARK_END="end";
	
	private static final int TEST_PARAM_NUM=6;
	private static final int PRAM_METHOD_NAME=0;
	private static final int PRAM_TYPES=1;
	private static final int PRAM_PARMS=2;
	private static final int PRAM_RETURN_VALURE=3;
	private static final int PRAM_RETURN_EXCEPTION=4;
	private static final int PRAM_QUEUELET_TEST_PROP=5;
	private static final Class[] NO_TYPE=new Class[0];
	private static final Object[] NO_ARG=new Object[0];
	
	private static Object container;
	private static Properties queueletTestProp;
	private static String startupQueueletXmlDef;
	private QueueletContext context;
	
	protected static Object callMethod(Object obj,String methodName) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException{
		return callMethod(obj,methodName,NO_TYPE,NO_ARG);
	}
	
	protected static Object callMethod(Object obj,String methodName,Class types[],Object args[]) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException{
		Method method=null;
		if(types==null){
			types=NO_TYPE;
		}
		if(args==null){
			args=NO_ARG;
		}
		method=obj.getClass().getMethod(methodName, types);
		Object ret=method.invoke(obj, args);
		if(method.getReturnType()==Void.TYPE){
			return Void.TYPE;
		}
		return ret;
	}
	
	protected static String getTestContainerStatus(){
		return (String)System.getProperty(TEST_CONTAINER_STATUS);
	}
	
	//���ۂɋN������̂́A�R���X�g���N�^���쎞
	//junit��@BeforeClass�́Astatic���\�b�h�Ȃ̂ŁAstatic���\�b�h�Ŏ���
	protected static void setupContainer() throws IOException{
		setupContainer(INIT_QUEUELET_TEST_PROPERTIES,null);
	}
	protected static void setupContainer(String startupDefKey) throws IOException{
		setupContainer(INIT_QUEUELET_TEST_PROPERTIES,startupDefKey);
	}
	protected static void setupContainer(String envProp,String startupDefKey) throws IOException{
		//Loader����ǂݍ���
		InputStream is=TestBase.class.getClassLoader().getResourceAsStream(envProp);
		if(is==null){
			//�Ȃ����File�Ƃ��ēǂݍ���
			File envFile=new File(envProp);
			if(!envFile.exists()){
				throw new RuntimeException("not fount envProp:"+envProp);
			}
			is=new FileInputStream(envProp);
		}
		setupContainer(is,startupDefKey);
	}
	protected static void setupContainer(InputStream is) throws IOException{
		setupContainer(is,null);
	}
	protected static void setupContainer(InputStream is,String startupDefKey) throws IOException{
		queueletTestProp = new Properties();
		queueletTestProp.load(is);
		is.close();
		Iterator itr=queueletTestProp.keySet().iterator();
		while(itr.hasNext()){
			String key=(String)itr.next();
			String value=queueletTestProp.getProperty(key);
			System.setProperty(key,resolveProperty(value));
		}
		System.setProperty(TEST_CONTAINER_STATUS,MARK_BOOT);
		if(startupDefKey==null){
			startupQueueletXmlDef=null;
			return;
		}
		startupQueueletXmlDef=queueletTestProp.getProperty(startupDefKey);
		if(startupQueueletXmlDef==null){
			throw new RuntimeException("not found in env defKey:" + startupDefKey);
		}
	}
	
	private static String getValue(String key,Properties runtimeProperties){
		String value=null;
		if( runtimeProperties!=null){
			value=runtimeProperties.getProperty(key);
			if( value!=null ){
				return value;
			}
		}
		value=System.getProperty(key);//-D������΍ŗD��
		if(value!=null){
			return value;
		}
		value=System.getenv(key);//���ϐ��ɂ���΍̗p
		if( value!=null){
			return value;
		}
		return null;
	}
	
	private static String replaceOne(String in,int pos,Properties runtimeProperties){
		int startReplace=in.indexOf("$",pos);
		if( startReplace<0){
			return in;
		}
		StringBuffer start=null;
		String end="";
		int startPos=in.indexOf("{");
		int endPos=in.indexOf("}");
		String key=null;
		if( startPos<0 || endPos<0 ){
			/* ���ꂩ�̊��ʂ��Ȃ��Ή������Ă��Ȃ��ꍇ */
			key=in.substring(startReplace+1);
			start=new StringBuffer(in.substring(0,startReplace));
		}else{
			key=in.substring(startPos+1,endPos);
			start=new StringBuffer(in.substring(0,startReplace));
			end=in.substring(endPos+1);
		}
		key=key.trim();
		String value=getValue(key,runtimeProperties);
		if(value==null){//�Y������l���Ȃ������ꍇ�A�󔒂ɕϊ�
			value="";
		}
		return start.append(value).append(end).toString();
		
		
	}
	/* aaa${xxx}bbb -> aaayyybbb */
	private static String resolveProperty(String in,Properties runtimeProperties){
		if( in==null ){
			return null;
		}
		int pos=0;
		String out=in;
		while(true){
			pos=in.indexOf("$",pos);
			if( pos<0){
				break;
			}
			out=replaceOne(in,pos,runtimeProperties);
			if( in.equals(out) ){
				pos++;//�ϊ�����Ȃ������ꍇ�̓ǂݔ�΂�
			}
			in=out;
		}
		return out;
	}
	
	public static String getProperty(String key){
		if(queueletTestProp==null){
			return null;
		}
		String value=getValue(key, queueletTestProp);
		if(value==null){
			value=key;
		}
		value=resolveProperty(value);
		return value;
	}
	
	private static String resolveProperty(String src){
		String dest=resolveProperty(src,queueletTestProp);
		if(container==null){
			return dest;
		}
		//�������g���鎖�͂Ȃ�
		try {
			Object destDest = callMethod(container,"resolveProperty",
					new Class[]{String.class},
					new Object[]{dest});
			return (String)destDest;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected void startContainer(String defKey){
		String mark=getTestContainerStatus();
		if(!MARK_BOOT.equals(mark)){
			throw new IllegalStateException("illegal mark:"+mark);
		}
		String queueletXmlDef=queueletTestProp.getProperty(defKey);
		if(queueletXmlDef!=null){
			runContainer(queueletXmlDef);
		}else{
			runContainer(defKey);
		}
	}
	
	private void runContainer(String queueletXmlDef){
		String resolveDef=resolveProperty(queueletXmlDef);
		System.setProperty(TEST_CLASS,getClass().getName());
		System.setProperty(TEST_CONTAINER_STATUS,MARK_START);
		try {
			Class containerClass=Class.forName("naru.queuelet.Container");
			Class types[]={String.class};
			Constructor containerConstructor=containerClass.getConstructor(types);
			container=containerConstructor.newInstance(new Object[]{resolveDef});
			//System.out.println("container:"+container);
		} catch (Exception e) {
			System.setProperty(TEST_CONTAINER_STATUS,MARK_END);
			e.printStackTrace();
			throw new RuntimeException("not found naru.queuelet.Container");
		}
	}
	
	protected static void stopContainer(){
		if(container==null){
			return;
		}
		try {
			callMethod(container,"stop");
			//�ʏ��~
			container=null;
			//�ċN���\
			System.setProperty(TEST_CONTAINER_STATUS,MARK_BOOT);
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		System.setProperty(TEST_CONTAINER_STATUS,MARK_END);
		System.out.println("stopContainer internal error!");
		container=null;
	}

	protected Object callTest(String callMethod) throws Throwable{
		return callTest(callMethod,INIT_METHOD_TIMEOUT,NO_TYPE,NO_ARG);
	}
	protected Object callTest(String callMethod,long timeout) throws Throwable{
		return callTest(callMethod,timeout,NO_TYPE,NO_ARG);
	}
	protected Object callTest(String callMethod,Class[] types,Object args[]) throws Throwable{
		return callTest(callMethod,INIT_METHOD_TIMEOUT,types,args);
	}
	
	/**
	 * �e�X�g�R�[�h����Container����terminal��queue����ꍇ�Ɏg�p
	 * @param req
	 * @param terminal
	 */
	protected void enqueContainer(Object req,String terminal){
		Throwable t=null;
		try {
			callMethod(container,"enque",
					new Class[]{Object.class,String.class},
					new Object[]{req, terminal});
		} catch (IllegalArgumentException e) {//argment�ԈႢ�A�����G���[
			t=e;
		} catch (SecurityException e) {//���ԈႢ
			t=e;
		} catch (IllegalAccessException e) {//���\�b�h�̃X�R�[�v�ԈႢ�A�����G���[
			t=e;
		} catch (InvocationTargetException e) {//���\�b�h����O�A�����G���[
			if(e.getCause()!=null){
				t=e.getCause();
			}else{
				t=e;
			}
		} catch (NoSuchMethodException e) {//���\�b�h�Ȃ��A�����G���[
			t=e;
		}
		if(t!=null){
			throw new RuntimeException("Internal error",t);
		}
	}
	
	/**
	 * ��肪�������ꍇ��O��ԋp����B
	 * @param callMethod
	 * @return �Ăяo�������\�b�h�̕��A�l�Avoid�̏ꍇ�́AVoid.type
	 * @throws Throwable 
	 */
	protected Object callTest(String callMethod,long timeout,Class[] types,Object args[]) throws Throwable{
		runContainerIfNeed();
		if(container==null){
			throw new IllegalStateException("container is not setuped.");
		}
		
		Object[] params=new Object[TEST_PARAM_NUM];
		params[PRAM_METHOD_NAME]=callMethod;
		params[PRAM_TYPES]=types;
		params[PRAM_PARMS]=args;
		params[PRAM_QUEUELET_TEST_PROP]=queueletTestProp;
		synchronized(params){
			enqueContainer(params, TEST_TERMINAL_NAME);
			
			//�����I���҂�
			try {
				params.wait(timeout);
			} catch (InterruptedException e) {
			}
			
			//���\�b�h�����A����
			if(params[PRAM_RETURN_VALURE]!=null){
				return params[PRAM_RETURN_VALURE];
			}
			//���\�b�h����O���A����
			if(params[PRAM_RETURN_EXCEPTION]!=null){
				throw (Throwable)params[PRAM_RETURN_EXCEPTION];
			}
			//�^�C���A�E�g
			System.out.println("timeout:"+timeout+":this:"+this+":method:"+callMethod);
			Map<Thread,StackTraceElement[]> traceMap=Thread.getAllStackTraces();
			Iterator<Thread> itr=traceMap.keySet().iterator();
			while(itr.hasNext()){
				Thread thread=itr.next();
				StackTraceElement[] traces=traceMap.get(thread);
				dumpStackTrace(thread,traces);
			}
			throw new RuntimeException("timeout:"+timeout+":this:"+this+":method:"+callMethod);
		}
	}
	
	private void dumpStackTrace(Thread thread,StackTraceElement[] traces){
		StringBuilder sb=new StringBuilder("###:"+thread.getName());
		for(int i=0;i<traces.length;i++){
			sb.append("\r\nat ");
			sb.append(traces[i]);
		}
		System.out.println(sb.toString());
	}
	
	/**
	 * �Q��̌Ăяo��������
	 * 1)JUnit�����test���\�b�h�Ăяo������new�����
	 * 2)QueueletContainer����̌Ăяo��
	 */
	private void runContainerIfNeed(){
//		System.out.println("TestBase construct." + this.getClass().getClassLoader());
		String mark=getTestContainerStatus();
		if(MARK_BOOT.equals(mark)&&startupQueueletXmlDef!=null){
			runContainer(startupQueueletXmlDef);
			return;
		}
//		System.out.println("queuelet start.class:"+getClass().getName()+":mark:"+mark);
	}
	
	protected void enque(Object req,String terminal){
		context.enque(req,terminal);
	}
	protected Object deque(String terminal){
		return context.deque(terminal);
	}
	
	public void init(QueueletContext context, Map param) {
		this.context=context;
	}

	public boolean service(Object req) {
		Object[] params=(Object[])req;
		Object ret;
		try {
			queueletTestProp=(Properties)params[PRAM_QUEUELET_TEST_PROP];
			ret=callMethod(this,(String)params[PRAM_METHOD_NAME],(Class[])params[PRAM_TYPES],(Object[])params[PRAM_PARMS]);
			params[PRAM_RETURN_VALURE]=ret;
		} catch (InvocationTargetException e) {
			Throwable cause=e.getCause();
			if(cause!=null){
				params[PRAM_RETURN_EXCEPTION]=cause;
			}else{
				params[PRAM_RETURN_EXCEPTION]=e;
			}
		} catch (SecurityException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//�����
		} catch (IllegalArgumentException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//arg���
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//�X�R�[�v���
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//���\�b�h�����
			e.printStackTrace();
		}finally{
			synchronized(params){
				params.notify();
			}
		}
		return false;
	}

	public void term() {
	}
}
