package naru.queuelet.startup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

public class StartupProperties {
	/* 以降の項目はユーザが明示的に設定...なければないで問題ない?'*/
	public static final String QUEUELET_HOME="QUEUELET_HOME";
	/* queueletコンテナ開発用...通常使用しない */
//	public static final String QUEUELET_CLASSPATH="queuelet.classpath";
	
	public static final String QUEUELET_SYSTEM_CLASSPATHS="queuelet.directory.system.classpaths";
	public static final String QUEUELET_SYSTEM_LIBS="queuelet.directory.system.libs";
	public static final String QUEUELET_COMMON_CLASSPATHS="queuelet.directory.common.classpaths";
	public static final String QUEUELET_COMMON_LIBS="queuelet.directory.common.libs";

	public static final String QUEUELET_CONF_DIR="queuelet.directory.config";
	public static final String QUEUELET_LOG_DIR="queuelet.directory.log";
	public static final String QUEUELET_STORE_DIR="queuelet.directory.store";
	
	private File queueletHome;
	private File[] commonClasses;
	private File[] commonLibs;
	private File[] systemClasses;
	private File[] systemLibs;
	
	private File storeDir;//queuelet内部DB格納ディレクトリ
	private File confDir;//xml定義格納ディレクトリ
	private File logDir;//log出力ディレクトリ
	
	private boolean useNio=true;
	private boolean useLoader=false;
	
	private String getQueueletProperty(Properties loaderProp,String key){
		String paramValue;
		paramValue=System.getProperty(key);//java -Dオプションが一番強い
		if(paramValue!=null){
			return paramValue;
		}
		paramValue=loaderProp.getProperty(key);//queueletStartup.propertiesが次に強い
		if(paramValue!=null){
			return paramValue;
		}
		paramValue=System.getenv(key);//環境変数があれば採用
		if(paramValue!=null){
			return paramValue;
		}
		return paramValue;
	}
	
	public StartupProperties(){
		Properties prop=new Properties();
		ClassLoader cl=Startup.class.getClassLoader();
		InputStream is=null;
		try {
//			URL propertiesUrl=cl.getResource("queueletStartup.properties");
//			System.out.println(propertiesUrl.toString());
			is=cl.getResourceAsStream("queueletStartup.properties");
			prop.load(is);
		} catch (Exception e) {
			System.out.println("startup fail to read properties");
			e.printStackTrace();
		}finally{
			if( is!=null ){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
		String paramValue;

		paramValue=getQueueletProperty(prop,QUEUELET_HOME);
		if( paramValue==null){
			throw new RuntimeException("fail to get QUEUELTE_HOME");
		}
		queueletHome=new File(paramValue);
		if( !queueletHome.exists() || !queueletHome.canRead() ){
			throw new RuntimeException("QUEUELTE_HOME not found."+ queueletHome.getAbsolutePath());
		}
		/* log4j.propertiesで参照するためシステムプロパティに設定 */
		System.setProperty(QUEUELET_HOME,queueletHome.getAbsolutePath());
		
		
		paramValue=getQueueletProperty(prop,"queuelet.mode.loader");
		this.useLoader=("true".equalsIgnoreCase(paramValue));
		
		paramValue=getQueueletProperty(prop,"queuelet.mode.nio");
		this.useNio=("true".equalsIgnoreCase(paramValue));
		
		setupSystemClasses(prop);
		setupSystemLibs(prop);
		setupCommonClasses(prop);
		setupCommonLibs(prop);
		
		paramValue=getQueueletProperty(prop,QUEUELET_CONF_DIR);
		this.confDir=queueletPath(paramValue);
		
		paramValue=getQueueletProperty(prop,QUEUELET_LOG_DIR);
		this.logDir=queueletPath(paramValue);
		
		paramValue=getQueueletProperty(prop,QUEUELET_STORE_DIR);
		this.storeDir=queueletPath(paramValue);
	}
	
	public File[] getCommonClasses() {
		return commonClasses;
	}
	
	public File queueletPath(String path){
		if( path==null){
			return getQueueletHome();
		}else if(path.indexOf(":/")>0 || path.indexOf(":\\")>0){
			/* windows系でドライブから指定された->絶対が指定された場合 */
			return new File(path);
		}else if(path.startsWith("/") || path.startsWith("\\")){
			/* 絶対が指定された場合 */
			return new File(path);
		}
		/* 相対パスが指定された場合はQUEUELET_HOMEからの相対 */
		return new File(getQueueletHome(),path);
	}
	
	public File queueletSystemPath(){
		return queueletPath("system");
	}
	
	private File[] queueletPaths(Properties loaderProp,String key,String homeDir){
		ArrayList paths=new ArrayList();
		String classpaths=getQueueletProperty(loaderProp,key);
		if( classpaths!=null ){
			StringTokenizer st=new StringTokenizer(classpaths,",");
			while(st.hasMoreTokens()){
				paths.add(queueletPath(st.nextToken()));
			}
		}
		paths.add(queueletPath(homeDir));
		return (File[])paths.toArray(new File[paths.size()]);
	}
	
	private void setupCommonClasses(Properties loaderProp) {
		this.commonClasses=queueletPaths(loaderProp,QUEUELET_COMMON_CLASSPATHS,"common" + File.separator +"classes");
	}
	
	public File[] getCommonLibs() {
		return commonLibs;
	}
	private void setupCommonLibs(Properties loaderProp) {
		this.commonLibs = queueletPaths(loaderProp,QUEUELET_COMMON_LIBS,"common" + File.separator +"lib");
	}
	
	public File[] getSystemClasses() {
		return systemClasses;
	}
	public void setupSystemClasses(Properties loaderProp) {
		this.systemClasses = queueletPaths(loaderProp,QUEUELET_SYSTEM_CLASSPATHS,"system" + File.separator +"classes");
	}
	public File[] getSystemLibs() {
		return systemLibs;
	}
	private void setupSystemLibs(Properties loaderProp) {
		this.systemLibs = queueletPaths(loaderProp,QUEUELET_SYSTEM_LIBS,"system" + File.separator +"lib");
	}
	
	public boolean isUseLoader() {
		return useLoader;
	}
	
	public boolean isUseNio() {
		return useNio;
	}

	public File getConfDir() {
		return confDir;
	}

	public File getLogDir() {
		return logDir;
	}

	public File getQueueletHome() {
		return queueletHome;
	}

	public File getStoreDir() {
		return storeDir;
	}

}
