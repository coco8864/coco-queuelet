	/*
 * 作成日: 2004/07/29
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import naru.queuelet.QueueletHooker;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class Startup {
	public static final String MAINLOADER="queuelet.hooker.mainloader";
	public static final String DEFAULT_MINLOADER_NAME="main";
	
	/* 以降の項目は内部的に設定、但し明示的に指定された場合それを使用（基本) */
	/* 定義ファイルフルパス 例）'/aaa/bbb/ccc/?????.xml'*/
	private static final String QUEUELET_CONFIG="queuelet.configuration";

	/* 定義xmlのファイル名を設定しlog4j.propertiesで動的ログ名生成に使用 例）'?????' */
	private static final String QUEUELET_CONFIG_FILENAME="queuelet.configuration.filename";
	
	/* queueletコンテナで使用するlog4j定義ファイルフルパス */
	/* 1).../定義ファイル.log4j.xml */
	/* 2).../定義ファイル.log4j.properties */
	/* 3)${QUEULET_HOME}/conf/log4j.properties(存在前提) */
	public static final String LOG4J_CONFIG="queuelet.log4j.configuration";
	
	private static String defaultDefFile="queuelet.xml";
	private static String[] queuelet_args=new String[0];
	
	public static StartupProperties startupProperteis;
	static{
//		startupProperteis=new StartupProperties();
	}

	/**
	 * @return Returns the systemDir.
	 */
	public static String getDefaultDefFile() {
		return defaultDefFile;
	}
	/**
	 * @param systemDir The systemDir to set.
	 */
	public static void setDefaultDefFile(String defaultDefFile) {
		Startup.defaultDefFile = defaultDefFile;
	}
	
	public static String getQueueletConfiguration(){
		String conf=System.getProperty(QUEUELET_CONFIG);
		if( conf==null || "".equals(conf)){
			return null;
		}
		return conf;
	}	
	
	public static void setQueueletConfigration(File confXmlFile){
		String queueletConfig=getQueueletConfiguration();
		if( queueletConfig==null ){
			queueletConfig=confXmlFile.getAbsolutePath();
			System.setProperty(QUEUELET_CONFIG,queueletConfig);
		}else{
			confXmlFile=new File(queueletConfig);
		}
		
		String confXmlName=confXmlFile.getName();
		int dotPos=confXmlName.lastIndexOf(".");
		if( dotPos>0 ){
			confXmlName=confXmlName.substring(0,dotPos);
		}
		System.setProperty(QUEUELET_CONFIG_FILENAME,confXmlName);
		setLog4jConfiguration(confXmlFile);
	}
	
	public static String getLog4jConfiguration(){
		String conf=System.getProperty(LOG4J_CONFIG);
		if( conf==null || "".equals(conf)){
			return null;
		}
		return conf;
	}
	
	private static void setLog4jConfiguration(File confXmlFile){
		String log4jConfig=getLog4jConfiguration();
		if( log4jConfig!=null ){
			return;
		}
		String log4jconfXmlName=null;
		String log4jconfPropertiesName=null;
		String confXmlName=confXmlFile.getName();
		int dotPos=confXmlName.lastIndexOf(".");
		if( dotPos>0 ){
			confXmlName=confXmlName.substring(0,dotPos);
		}
		log4jconfXmlName=confXmlName + ".log4j.xml";
		log4jconfPropertiesName=confXmlName + ".log4j.properties";
		
		File log4jconfXmlFile=null;
		File log4jconfPropertiesFile=null;
		File parent=confXmlFile.getParentFile();

		log4jconfXmlFile=new File(parent,log4jconfXmlName);
		log4jconfPropertiesFile=new File(parent,log4jconfPropertiesName);

		if( log4jconfXmlFile.canRead() ){
			log4jConfig=log4jconfXmlFile.getAbsolutePath();
		}else if(log4jconfPropertiesFile.canRead()){
			log4jConfig=log4jconfPropertiesFile.getAbsolutePath();
		}else{
			log4jconfPropertiesFile=new File(startupProperteis.getConfDir(),"log4j.properties");
			log4jConfig=log4jconfPropertiesFile.getAbsolutePath();
		}
		
		if( log4jConfig!=null ){
			System.setProperty(LOG4J_CONFIG,log4jConfig);
		}
	}
	
	/* 内部で使用するプロパティ値を削除する */
	/* 連続でテストする時に初期化されないためログの出力先が変更されない */
	private static void clsInternalProperties(){
		System.setProperty(LOG4J_CONFIG,"");
		System.setProperty(QUEUELET_CONFIG,"");
		System.setProperty(QUEUELET_CONFIG_FILENAME,"");
	}
	
	public static String[] getArgs(){
		return queuelet_args;
	}
	
	private static void setArgs(String[] orgArgs,int start){
		queuelet_args=new String[orgArgs.length-start];
		for(int i=0;i<queuelet_args.length;i++){
			queuelet_args[i]=orgArgs[start+i];
		}
	}

	private static ClassLoader getQueueletSystemLoader(){
		ClassLoader parent=Startup.class.getClassLoader();
//		ClassLoader parent=Object.class.getClassLoader();
		StartupLoader commonLoader=new StartupLoader(
				"commonLoader",
				startupProperteis.getCommonClasses(),
				startupProperteis.getCommonLibs(),
				parent );
		
		StartupLoader sysLoader=new StartupLoader(
				"systemLoader",
				startupProperteis.getSystemClasses(),
				startupProperteis.getSystemLibs(),
				commonLoader );
		return sysLoader;
	}

	private static Class getContainerClass(ClassLoader queueletSystemLoader) throws ClassNotFoundException{
		return queueletSystemLoader.loadClass("naru.queuelet.core.Container");
	}

	private static Object mainContainerObject=null;
	private static Class mainContainerClass=null;

	public static Object start(InputStream queueletXml) {
		Object containerObject=null;
		Class containerClass=null;
		try {
			ClassLoader systemLoader=getQueueletSystemLoader();
			containerClass=getContainerClass(systemLoader);
			
			Thread.currentThread().setContextClassLoader(systemLoader);
			Class[] paramTypes = new Class[1];
			paramTypes[0] = InputStream.class; 
			Object[] paramValues = new Object[1];
			paramValues[0] = queueletXml;
			Method method = containerClass.getMethod("getInstance", paramTypes);
			containerObject = method.invoke(null, paramValues);
			
			method = containerClass.getMethod("start", new Class[0]);
			method.invoke(containerObject, new Object[0]);
		} catch (Throwable e) {
			throw new IllegalStateException("container fail to Startup#start",e);
		}
		mainContainerClass=containerClass;
		mainContainerObject=containerObject;
		return containerObject;
	}
	public static Object start(Object obj){
		InputStream is=obj.getClass().getResourceAsStream("queuelet.xml");
		if( is==null ){
			return null;
		}
		return start(is);
	}
	
	public static void stop(){
		if( mainContainerClass==null || mainContainerObject==null){
			return;
		}
		Throwable t=null;
		try {
			Method method = mainContainerClass.getMethod("stop", new Class[0]);
			method.invoke(mainContainerObject, new Object[0]);
		} catch (SecurityException e) {
			t=e;
		} catch (IllegalArgumentException e) {
			t=e;
		} catch (NoSuchMethodException e) {
			t=e;
		} catch (IllegalAccessException e) {
			t=e;
		} catch (InvocationTargetException e) {
			t=e;
		}
		if(t!=null){
			System.out.println("ontainer fail to Startup#stop");
			t.printStackTrace();
		}
	}

	private static ClassLoader getLoader(String loaderName) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		if(mainContainerObject==null){
			System.out.println("fail to getLoader. detail:not start queulet container");
			return null;
		}
		Class[] paramTypes = new Class[1];
		paramTypes[0] = String.class; 
		Object[] paramValues = new Object[1];
		paramValues[0] = loaderName;
		Method method = mainContainerClass.getMethod("getLoader", paramTypes);
		
		Object loader = method.invoke(mainContainerObject, paramValues);
		return (ClassLoader)loader;
	}

	/* クラスローダが初期化される時に呼ばれる */
	public static QueueletHooker getQueuletHooker(ClassLoader loader) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		if(mainContainerObject==null){
			start((Object)loader);
		}
		if(mainContainerObject==null){
			System.out.println("fail to getQueueletHooker. detail:not start queulet container");
			return null;
		}

		Class[] paramTypes = new Class[1];
		paramTypes[0] = ClassLoader.class; 
		Object[] paramValues = new Object[1];
		paramValues[0] = loader;
		Method method = mainContainerClass.getMethod("getQueueletHooker", paramTypes);
		
		Object queueletHooker = method.invoke(mainContainerObject, paramValues);
//		System.out.println("%%%%%getQueuletHooker return:"+queueletHooker);
		return (QueueletHooker)queueletHooker;
	}
	
	
	private static File getHookConfigfile(Class targetClass){
		String confXmlFileFullPath=getQueueletConfiguration();
		if( confXmlFileFullPath!=null ){
			return new File(confXmlFileFullPath);
		}
		File confDir=startupProperteis.getConfDir();
		String fullClassName=targetClass.getName();
		String className=fullClassName;
		int pos=fullClassName.lastIndexOf(".");
		if( pos>0){
			className=fullClassName.substring(pos+1);
		}
		File confXmlFile=new File(confDir,className + ".xml");
		if( !confXmlFile.canRead()){
			System.out.println("fail to config file."+ confXmlFile);
			return null;
		}
		return confXmlFile;
	}
	
	/* メイン横取り口 */
	public static boolean mainHooker(Class targetClass,String[] args) {
		/* 横取り判定(QUEUELET_HOMEが前提) */
/*　本当はチェックを残したいが、初期化のタイミングの関係で無理
		File queueletHome=startupProperteis.getQueueletHome();
		if( queueletHome==null){
			System.out.println("failt to get QUEUELET_HOME");
			return false;
		}
*/
		/* システムクラスローダ上で動作している時のみ横取り */
		ClassLoader systemLoader=ClassLoader.getSystemClassLoader();
		ClassLoader targetLoader=targetClass.getClassLoader();
		if( !targetLoader.equals(systemLoader) ){
			System.out.println("queuelet hook start");
			return false;
		}
		
		/* プロパティ初期化処理 */
		clsInternalProperties();
		startupProperteis=new StartupProperties();
		
		File confXmlFile=getHookConfigfile(targetClass);
		setQueueletConfigration(confXmlFile);
		/* コンテナの起動 */
		try {
			mainContainerObject=start(new FileInputStream(confXmlFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		/* メインメソッドをコンテナ上で実行 */
		ClassLoader loader=null;
		Class clazz=null;
		try {
			String loaderName=System.getProperty(MAINLOADER);
			if( loaderName==null){
				loaderName=DEFAULT_MINLOADER_NAME;
			}
			loader=getLoader(loaderName);
			if( loader==null ){
				return false;
			}
			System.out.println("loader:"+loader);
			clazz=null;
			String fullClassName=targetClass.getName();
			clazz=loader.loadClass(fullClassName);
			if( clazz.getClassLoader()==systemLoader ){
				System.out.println("queuelet container:failt to load "+clazz.getName());
				return false;
			}
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return false;
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return false;
		}
		
		String methodName = "main";
		Class[] paramTypes = new Class[1];
		paramTypes[0] = args.getClass(); 
		Object[] paramValues = new Object[1];
		paramValues[0] = args;
		try {
			Method method =	clazz.getMethod(methodName, paramTypes);
			method.invoke(null, paramValues);
		} catch (SecurityException e2) {
			e2.printStackTrace();
			return false;
		} catch (IllegalArgumentException e2) {
			e2.printStackTrace();
			return false;
		} catch (NoSuchMethodException e2) {
			e2.printStackTrace();
			return false;
		} catch (IllegalAccessException e2) {
			e2.printStackTrace();
			return false;
		} catch (InvocationTargetException e2) {
			e2.printStackTrace();
			return false;
		}
		return true;
	}

	/* 普通の入り口 */
	public static void main(String[] args) throws FileNotFoundException {
		clsInternalProperties();
		startupProperteis=new StartupProperties();
		try {
			String confXml;
			File confXmlFile=null;
			if(args.length>=1){
				confXml=args[0];
				setArgs(args,1);
			}else{
				confXml=defaultDefFile;
				setArgs(args,0);
			}
			if( confXml.indexOf("\\")<0 && confXml.indexOf("/")<0){
				confXmlFile=new File(startupProperteis.getConfDir(), confXml);
			}else{
				confXmlFile=new File(confXml);
			}
			setQueueletConfigration(confXmlFile);
			mainContainerObject=start(new FileInputStream(confXmlFile));
			
			synchronized(mainContainerObject){
				try {
					mainContainerObject.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mainContainerObject=null;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	public static File getQueueletPath(String fileName) {
		return startupProperteis.queueletPath(fileName);
	}
	
	/* 普通の入り口 */
	public static Object startup(String confXml) throws FileNotFoundException {
		clsInternalProperties();
		startupProperteis=new StartupProperties();
		try {
			File confXmlFile=null;
			if( confXml.indexOf("\\")<0 && confXml.indexOf("/")<0){
				confXmlFile=new File(startupProperteis.getConfDir(), confXml);
			}else{
				confXmlFile=new File(confXml);
			}
			setQueueletConfigration(confXmlFile);
			mainContainerObject=start(new FileInputStream(confXmlFile));
			return mainContainerObject;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
