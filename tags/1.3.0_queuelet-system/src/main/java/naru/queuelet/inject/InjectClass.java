/*
 * 作成日: 2004/09/01
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.inject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import naru.queuelet.QueueletHookClassLoader;
import naru.queuelet.QueueletHookUrlClassLoader;
import naru.queuelet.QueueletHooker;

import org.apache.log4j.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class InjectClass {
	static private Logger logger=Logger.getLogger(InjectClass.class);

	static private Properties addMethods;
	static private Hashtable replaceCalls;

	static class QueueletEditor extends ExprEditor{
		/* (非 Javadoc)
		 * @see javassist.expr.ExprEditor#edit(javassist.expr.MethodCall)
		 */
		public void edit(MethodCall methodCall) throws CannotCompileException {
			QueueletReplaceMethod qrm=new QueueletReplaceMethod(methodCall);
			try {
				logger.debug(methodCall.getClassName() + "##" + methodCall.getMethodName() +"##" + methodCall.getMethod().getSignature());
			} catch (NotFoundException e) {
				logger.debug("methodCall.getMethod() error",e);
			}
			String replaceCode=(String)replaceCalls.get(qrm);
			if( replaceCode!=null){
				logger.debug("edit:" + replaceCode);
				methodCall.replace(replaceCode);
			}
		}
	}
	
	static class QueueletReplaceMethod{
		String className;
		String methodName;
		String signature;
		
		public QueueletReplaceMethod(String classMethodSignature){
			StringTokenizer st=new StringTokenizer(classMethodSignature,",");
			setClassName(st.nextToken());
			setMethodName(st.nextToken());
			setSignature(st.nextToken());
		}

		public QueueletReplaceMethod(MethodCall methodCall){
			setClassName(methodCall.getClassName());
			setMethodName(methodCall.getMethodName());
			CtMethod method=null;
			try {
				method=methodCall.getMethod();
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
			if( method==null){
				return;
			}
			setSignature(method.getSignature());

/*			
			logger.debug("-----------------------");
			logger.debug("getClassName:" + methodCall.getClassName());
			logger.debug("getMethodName:" + methodCall.getMethodName());
			logger.debug("isSuper:" + methodCall.isSuper());
			logger.debug("getName:" + method.getName());
			logger.debug("getSignature:" + method.getSignature());
*/
		}
		
		/**
		 * @return
		 */
		public String getClassName() {
			return className;
		}

		/**
		 * @return
		 */
		public String getMethodName() {
			return methodName;
		}

		/**
		 * @return
		 */
		public String getSignature() {
			return signature;
		}

		/**
		 * @param string
		 */
		public void setClassName(String string) {
			className = string;
		}

		/**
		 * @param string
		 */
		public void setMethodName(String string) {
			methodName = string;
		}

		/**
		 * @param string
		 */
		public void setSignature(String string) {
			signature = string;
		}

		/* (非 Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if( obj==null){
				return false;
			}
			if(!(obj instanceof QueueletReplaceMethod)){
				return false;
			}
			QueueletReplaceMethod qrm=(QueueletReplaceMethod)obj;
			
			if( !qrm.className.equals(className) ){
				return false;
			}
			if( !qrm.methodName.equals(methodName) ){
				return false;
			}
			
			if( qrm.signature!=null){
				if( !qrm.signature.equals(signature) ){
					return false;
				}
			}else if(signature!=null){
				return false;
			}
			return true;
		}
		/* (非 Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			if(signature==null){
				return className.hashCode()+methodName.hashCode();
			}
			return className.hashCode()+methodName.hashCode()+signature.hashCode();
		}

	}
	
	private static String getJavaCode(String codeFileName){
		InputStream is=null;
		try {
			is=InjectClass.class.getResourceAsStream(codeFileName);
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			byte[] buf=new byte[1024];
			while(true){
				int len=is.read(buf);
				if(len<0){
					break;
				}
				baos.write(buf,0,len);
			}
			baos.close();
			byte[] javaCodeByte=baos.toByteArray();
			return new String(javaCodeByte);
		} catch (IOException e) {
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
		return null;
	}
	
	static{
		InputStream is=null;
		try {
			addMethods=new Properties();
			is=InjectClass.class.getResourceAsStream("addMethods.properties");
			addMethods.load(is);

			Iterator itr=addMethods.keySet().iterator();
			while(itr.hasNext()){
				String name=(String)itr.next();
				String codeFileName=(String)addMethods.get(name);
				String javaCode=getJavaCode(codeFileName);
				addMethods.put(name,javaCode);
//				itr.remove();
			}
			is.close();
			
			Properties replaceCallsProp=new Properties();
			replaceCalls=new Hashtable();
			is=InjectClass.class.getResourceAsStream("replaceCalls.properties");
			replaceCallsProp.load(is);
			itr=replaceCallsProp.keySet().iterator();
			while(itr.hasNext()){
				String name=(String)itr.next();
				String code=(String)replaceCallsProp.get(name);
				QueueletReplaceMethod qrm=new QueueletReplaceMethod(name);
				replaceCalls.put(qrm,code);
			}
		} catch (IOException e) {
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
	
	private static void addMethod(CtClass cc,String javaCode) throws CannotCompileException{
		logger.debug(javaCode);
		CtMethod m=CtNewMethod.make(javaCode,cc);
		cc.addMethod(m);
	}
	
	private static void addMethods(CtClass cc) throws CannotCompileException{
		Iterator itr=addMethods.values().iterator();
		while(itr.hasNext()){
			String javaCode=(String)itr.next();
			addMethod(cc,javaCode);
		}
	}
	
	private static void addField(CtClass loaderCc) throws NotFoundException, CannotCompileException{
		ClassPool cp=loaderCc.getClassPool();
		CtClass queueletHooker=cp.get("naru.queuelet.QueueletHooker");
		CtField cf=new CtField(queueletHooker,"__queueletHooker",loaderCc);
		loaderCc.addField(cf,"(naru.queuelet.QueueletHooker)" +
			"naru.queuelet.startup.Startup.getQueuletHooker((ClassLoader)this)");
	}
	
	private static void changeMethodsCode(CtClass loaderCc) throws CannotCompileException{
		CtMethod[] methods=loaderCc.getDeclaredMethods();
		for(int i=0;i<methods.length;i++){
			CtMethod method=methods[i];
			/* addMethodsで追加したメソッドには作用させない */
			if( addMethods.get(method.getName())!=null ){
				continue;
			}
			method.instrument(new QueueletEditor());
		}
		if( true ){
			return;
		}
		
		String className=loaderCc.getName();
		CtConstructor[] constructors=loaderCc.getConstructors();
		for(int i=0;i<constructors.length;i++){
			CtConstructor method=constructors[i];
//			if( addMethods.get(method.getName())!=null ){
//				continue;
//			}
			method.instrument(new QueueletEditor());
		}
		
	}

	private static void addInterface(CtClass loaderCc) throws NotFoundException, CannotCompileException{
		ClassPool cp=loaderCc.getClassPool();
		logger.debug("addInterface classPool:" + cp);
		CtClass queueletHooker=cp.get(QueueletHooker.class.getName());		
		loaderCc.addInterface(queueletHooker);

		/* JDKから提供されるクラスローダをフックしてシステムクラスローダを
		 * 直接親クラスローダにさせない */
		String superClassName=loaderCc.getSuperclass().getName();
		if(URLClassLoader.class.getName().equals(superClassName) ){
			CtClass queueletUrlClassLoader=cp.get(QueueletHookUrlClassLoader.class.getName());
			loaderCc.setSuperclass(queueletUrlClassLoader);
		}else if(ClassLoader.class.getName().equals(superClassName)){
			CtClass queueletClassLoader=cp.get(QueueletHookClassLoader.class.getName());
			loaderCc.setSuperclass(queueletClassLoader);
		}
	}
	
	
	public static void modifyLoaderClass(CtClass loaderCc){
		/* 重複処理を行わないための対処 */
		try {
			loaderCc.getField("__queueletHooker");
			return;
		} catch (Throwable e1) {
		}
		
		try {
			addField(loaderCc);
			addInterface(loaderCc);
			addMethods(loaderCc);
			changeMethodsCode(loaderCc);
		} catch (NotFoundException e) {
			logger.warn("modifyLoaderClass error",e);
		} catch (CannotCompileException e) {
			logger.warn("modifyLoaderClass error",e);
		}
	}

	public static void main(String[] args) throws Exception{
		String outDir=".";
		String className;
		
		if( args.length<=0 ){
			System.out.println("Usage: [-d outdir] className(a.b.c)");
			System.exit(1);
		}
		if( "-d".equals(args[0])){
			outDir=args[1];
			className=args[2];
		}else{
			className=args[0];
		}
		
		ClassPool cp=ClassPool.getDefault();
		
		CtClass loaderCc=cp.get(className);
		modifyLoaderClass(loaderCc);
		loaderCc.writeFile(outDir);
	}
	
}
