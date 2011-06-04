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
 * Queuelet内部をUnitテストする際、このメソッドをextendsして使用
 * 
 * 1)このClassをextendsする。
 * 2)@BeforeClassからsetupContainerを呼び出す
 * 3)@AfterClassからstopContainerを呼び出す
 * 4)以下の記述がqueuelet定義ファイルに記述
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
	private static final String TEST_CONTAINER_STATUS="queuelet.test.container.startus";//システムプロパティに設定,
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
	
	//実際に起動するのは、コンストラクタ動作時
	//junitの@BeforeClassは、staticメソッドなので、staticメソッドで実装
	protected static void setupContainer() throws IOException{
		setupContainer(INIT_QUEUELET_TEST_PROPERTIES,null);
	}
	protected static void setupContainer(String startupDefKey) throws IOException{
		setupContainer(INIT_QUEUELET_TEST_PROPERTIES,startupDefKey);
	}
	protected static void setupContainer(String envProp,String startupDefKey) throws IOException{
		//Loaderから読み込み
		InputStream is=TestBase.class.getClassLoader().getResourceAsStream(envProp);
		if(is==null){
			//なければFileとして読み込み
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
		value=System.getProperty(key);//-Dがあれば最優先
		if(value!=null){
			return value;
		}
		value=System.getenv(key);//環境変数にあれば採用
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
			/* 何れかの括弧がなく対応が取れていない場合 */
			key=in.substring(startReplace+1);
			start=new StringBuffer(in.substring(0,startReplace));
		}else{
			key=in.substring(startPos+1,endPos);
			start=new StringBuffer(in.substring(0,startReplace));
			end=in.substring(endPos+1);
		}
		key=key.trim();
		String value=getValue(key,runtimeProperties);
		if(value==null){//該当する値がなかった場合、空白に変換
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
				pos++;//変換されなかった場合の読み飛ばし
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
		//ここが使われる事はない
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
			//通常停止
			container=null;
			//再起動可能
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
	 * テストコードからContainer内のterminalにqueueする場合に使用
	 * @param req
	 * @param terminal
	 */
	protected void enqueContainer(Object req,String terminal){
		Throwable t=null;
		try {
			callMethod(container,"enque",
					new Class[]{Object.class,String.class},
					new Object[]{req, terminal});
		} catch (IllegalArgumentException e) {//argment間違い、内部エラー
			t=e;
		} catch (SecurityException e) {//環境間違い
			t=e;
		} catch (IllegalAccessException e) {//メソッドのスコープ間違い、内部エラー
			t=e;
		} catch (InvocationTargetException e) {//メソッドが例外、内部エラー
			if(e.getCause()!=null){
				t=e.getCause();
			}else{
				t=e;
			}
		} catch (NoSuchMethodException e) {//メソッドなし、内部エラー
			t=e;
		}
		if(t!=null){
			throw new RuntimeException("Internal error",t);
		}
	}
	
	/**
	 * 問題があった場合例外を返却する。
	 * @param callMethod
	 * @return 呼び出したメソッドの復帰値、voidの場合は、Void.type
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
			
			//処理終了待ち
			try {
				params.wait(timeout);
			} catch (InterruptedException e) {
			}
			
			//メソッドが復帰した
			if(params[PRAM_RETURN_VALURE]!=null){
				return params[PRAM_RETURN_VALURE];
			}
			//メソッドが例外復帰した
			if(params[PRAM_RETURN_EXCEPTION]!=null){
				throw (Throwable)params[PRAM_RETURN_EXCEPTION];
			}
			//タイムアウト
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
	 * ２種の呼び出しがある
	 * 1)JUnitからのtestメソッド呼び出し毎にnewされる
	 * 2)QueueletContainerからの呼び出し
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
			//環境誤り
		} catch (IllegalArgumentException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//arg誤り
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//スコープ誤り
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			params[PRAM_RETURN_EXCEPTION]=e;
			//メソッド名誤り
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
