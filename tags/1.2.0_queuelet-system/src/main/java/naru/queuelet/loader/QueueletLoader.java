/*
 * 作成日: 2004/07/30
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import naru.queuelet.QueueletCallInfo;
import naru.queuelet.QueueletHooker;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class QueueletLoader extends ClassLoader implements QueueletHooker {
	static private Logger logger=Logger.getLogger(QueueletLoader.class);

//	static List systemLoaderClasses=new ArrayList();
	static Pattern systemLoaderClassesPattern;
	static{
		Properties prop=new Properties();;
		InputStream is=null;
		try {
			is=QueueletLoader.class.getResourceAsStream("systemLoaderClasses.properties");
			prop.load(is);
		} catch (IOException e) {
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
		StringBuffer sb=new StringBuffer();
		Iterator itr=prop.keySet().iterator();
		while(itr.hasNext()){
			if(sb.length()!=0){
				sb.append("|");
			}
			String className=(String)itr.next();
			sb.append(className);
			/*
			className=className.replaceAll("\\.", "\\\\.");
			sb.append(className);
			if(className.endsWith(".")){
				sb.append(".*");
			}
			*/
		}
		systemLoaderClassesPattern=Pattern.compile(sb.toString());
//		systemLoaderClasses.addAll(prop.keySet());
	}
	
	private URLClassLoader urlClassLoader;
	private QueueletHooker queueletHooker;
	private boolean delegate=true;
	private ClassLoader parent;

	public QueueletLoader() {
		//自分のクラスローダ(system)の親、つまり(common)を取り出している
		super(QueueletLoader.class.getClassLoader().getParent());
		ClassLoader common=QueueletLoader.class.getClassLoader().getParent();
		logger.debug("parent loader:" + common);
	}
		
	public void setup(QueueletHooker queueletHooker,boolean delegate, List urlList,ClassLoader resouceLoader){
		this.queueletHooker=queueletHooker;
		this.delegate=delegate;
		logger.debug("start");
		
		urlClassLoader =URLClassLoader.newInstance(
					(URL[]) urlList.toArray(new URL[urlList.size()]),resouceLoader);
		logger.debug("end");
	}

	public  Class loadClass(String className)throws ClassNotFoundException {
		Class cl=null;
		cl=findLoadedClass(className);
		if( cl!=null ){
			return cl;
		}
		logger.debug("loadClass:"+ className);
		if(delegate){
			return super.loadClass(className);
		}
		Matcher matcher=null;
		synchronized(systemLoaderClassesPattern){
			matcher=systemLoaderClassesPattern.matcher(className);
		}
		if(matcher.matches()){
//			System.out.println("matchers:"+className);
			cl=super.loadClass(className);
			return cl;
		}
//		System.out.println("unmatchers:"+className);
		/*
		Iterator itr=systemLoaderClasses.iterator();
		while(itr.hasNext()){
			Object sysLoaderClass=itr.next();
			if( className.startsWith((String)sysLoaderClass) ){
				cl=super.loadClass(className);
				return cl;
			}
		}
		*/
		try {
			cl=findClass(className);
		} catch (ClassNotFoundException e) {
			logger.debug("findClass from loadClass");
			cl=super.loadClass(className);
		}
//		logger.info("loadClass load from me:"+ className,new Exception());
		return cl;
	}

	/* (非 Javadoc)
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	protected Class findClass(String className) throws ClassNotFoundException {
		logger.debug("findClass:"+ className);
		
		byte[] b=queueletHooker.getByteCode(className);
		if( b==null){
			return super.findClass(className);
		}
		logger.debug("rewrite class:"+ className);
		Class c = defineClass(className,b, 0, b.length);
		queueletHooker.registerClass(className,c);
		return c;
	}

	/* (非 Javadoc)
	 * @see java.lang.ClassLoader#findResource(java.lang.String)
	 */
	protected URL findResource(String arg0) {
		return urlClassLoader.findResource(arg0);
	}

	/* (非 Javadoc)
	 * @see java.lang.ClassLoader#findResources(java.lang.String)
	 */
	protected Enumeration findResources(String arg0) throws IOException {
		return urlClassLoader.findResources(arg0);
	}

	/* (非 Javadoc)
	 * @see java.lang.ClassLoader#getResource(java.lang.String)
	 */
	public URL getResource(String arg0) {
		return urlClassLoader.getResource(arg0);
	}

	/* (非 Javadoc)
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String arg0) {
		return urlClassLoader.getResourceAsStream(arg0);
	}


	/* (非 Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "class:" + getClass().getName()+"@"+ hashCode();
	}

	
	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean callMethod(QueueletCallInfo qci) {
		return queueletHooker.callMethod(qci);
	}
	
	/* (非 Javadoc)
	 * @see naru.queuelet.QueueletHooker#returnMethod(naru.queuelet.QueueletCallInfo)
	 */
	public QueueletCallInfo returnMethod(Object returnValue,Throwable returnThrowable) {
		return queueletHooker.returnMethod(returnValue,returnThrowable);
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.QueueletHooker#getByteCode(java.lang.String)
	 */
	public byte[] getByteCode(String className) {
		throw new IllegalArgumentException("getByteCode");
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.QueueletHooker#getByteCode(java.lang.String, byte[], int, int)
	 */
	public byte[] getByteCode(String className, byte[] b, int off, int len) {
		throw new IllegalArgumentException("getByteCode");
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.QueueletHooker#registerClass(java.lang.String, java.lang.Class)
	 */
	public void registerClass(String className, Class clazz) {
		throw new IllegalArgumentException("registerClass");
	}
}
