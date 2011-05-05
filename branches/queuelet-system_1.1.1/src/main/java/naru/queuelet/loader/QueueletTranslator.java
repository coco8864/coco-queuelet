/*
 * 作成日: 2004/08/31
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.Properties;

import naru.queuelet.inject.InjectClass;

import org.apache.log4j.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.Translator;
import javassist.bytecode.AccessFlag;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class QueueletTranslator {
	static private Logger logger=Logger.getLogger(QueueletTranslator.class);
	static private Properties hookerCode;
	static{
		InputStream is=null;
		try {
			hookerCode=new Properties();
			is=QueueletLoader.class.getResourceAsStream("hookerCode.properties");
			hookerCode.load(is);
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

	private LoaderWrapper loaderWrapper;
//	private HookerControler hookerControler;
	private CtClass ccThrowable;
	
	public QueueletTranslator(LoaderWrapper loaderWrapper){
		this.loaderWrapper=loaderWrapper;
		ClassPool defaultPool=ClassPool.getDefault();
		try {
			ccThrowable=defaultPool.get("java.lang.Throwable");
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}
	

/*
	public QueueletTranslator(HookerControler hookerControl){
		this.hookerControler=hookerControl;
		ClassPool defaultPool=ClassPool.getDefault();
		try {
			ccThrowable=defaultPool.get("java.lang.Throwable");
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}
*/

	String makeBeforeCode(MethodHooker hooker,CtMethod cm) throws NotFoundException{
		StringBuffer sb=new StringBuffer();
		sb.append("Class thisClass=Class.forName(\"" + cm.getDeclaringClass().getName() + "\");");
		sb.append("java.lang.ClassLoader thisClassLoader=thisClass.getClassLoader();");
		sb.append("naru.queuelet.QueueletHooker queuelet=(naru.queuelet.QueueletHooker)thisClassLoader;");
		sb.append("naru.queuelet.QueueletCallInfo qci=new naru.queuelet.QueueletCallInfo(");
		sb.append(hooker.getMethodNumber());
		if( (cm.getMethodInfo().getAccessFlags()&AccessFlag.STATIC)!=0 ){
			sb.append(",(Object)null");
		}else{
			sb.append(",(Object)this");
		}
		sb.append(",$args");
		if( hooker.isDetectCallStack() ){
			sb.append(",new Throwable()");
		}
		sb.append(");");

		/* before ,connurrentがなければ不要、最適化の余地あり */
		sb.append("boolean isReturnNow=queuelet.callMethod(qci);");
		
		/* before がなければ不要、最適化の余地あり */
		//hooker.getMethod().getReturnType();
		String returnValue=hooker.getReturnValue();
		if( returnValue!=null ){
			sb.append("if( isReturnNow ){");
//			sb.append("System.out.println(\"######################" + returnValue + "\");");
			sb.append("return " + returnValue);
			sb.append(";}");
		}else{
			sb.append("if( isReturnNow ){");
			CtClass returnType=cm.getReturnType();
			if( returnType.isPrimitive() ){
				/* 復帰値がプリミティブ型の場合 */
				if(CtClass.voidType.equals(returnType)){
					sb.append("return;");
				}else if(CtClass.booleanType.equals(returnType)){
					sb.append("return (boolean)false;");
				}else if(CtClass.byteType.equals(returnType)){
					sb.append("return (byte)0;");
				}else if(CtClass.shortType.equals(returnType)){
					sb.append("return (short)0;");
				}else if(CtClass.intType.equals(returnType)){
					sb.append("return (int)0;");
				}else if(CtClass.longType.equals(returnType)){
					sb.append("return (long)0;");
				}else if(CtClass.charType.equals(returnType)){
					sb.append("return (char)'\0';");
				}else if(CtClass.floatType.equals(returnType)){
					sb.append("return (float)0.0f;");
				}else if(CtClass.doubleType.equals(returnType)){
					sb.append("return (double)0.0;");
				}
			}else{
				/* 復帰値がオブジェクトの場合nullで復帰 */
				sb.append("return null;");
			}
			sb.append("}");
		}
		return sb.toString();
	}

	String makeAfterCode(MethodHooker hooker,CtMethod cm,boolean throwableCatch){
		StringBuffer sb=new StringBuffer();
		sb.append("Class thisClass=Class.forName(\"" + cm.getDeclaringClass().getName() + "\");");
		sb.append("java.lang.ClassLoader thisClassLoader=thisClass.getClassLoader();");
		sb.append("naru.queuelet.QueueletHooker queuelet=(naru.queuelet.QueueletHooker)thisClassLoader;");
		if(throwableCatch){
			sb.append("naru.queuelet.QueueletCallInfo qci=queuelet.returnMethod((Object)null,(Throwable)$e);");
			sb.append("throw($e);");
		}else if(hooker.getReturnValue()==null){
			sb.append("naru.queuelet.QueueletCallInfo qci=queuelet.returnMethod(($w)$_,(Throwable)null);");
//			sb.append("System.out.println(\"#####\" + qci.getReturnValue());");
			sb.append("if( qci.isShortcut() ){");
			sb.append("$_ = ($r)qci.getReturnValue();");
			sb.append("}");
		}
		return sb.toString();
	}
	
	private void modifyMethod(MethodHooker hooker,CtMethod cm) throws CannotCompileException, NotFoundException{
		hooker.setCtMethod(cm);
		/* before call */
		StringBuffer sb=new StringBuffer("{");
		String code=hookerCode.getProperty("insertBefore");
		if( code!=null){
			sb.append(code);
		}
		sb.append(makeBeforeCode(hooker,cm));
		sb.append("}");
		cm.insertBefore(sb.toString());
		if(logger.isDebugEnabled()){
			logger.debug("modifyMethod:" + cm.getDeclaringClass().getName()+"," + cm.getName());
			logger.debug("insertBefore:" + sb.toString());
		}
		
//		if( hooker.isAsync()){
		/* 非同期の場合、外から呼び出すのでパブリックにしないと駄目 */
		int modifier=cm.getModifiers();
		cm.setModifiers(Modifier.setPublic(modifier));
		
		/* nomal return */
		sb.setLength(0);
		sb.append("{");
		sb.append(makeAfterCode(hooker,cm,false));
		sb.append("}");
		cm.insertAfter(sb.toString(),false);/* 実return */
		if(logger.isDebugEnabled()){
			logger.debug("insertAfter nomal:" + sb.toString());
		}
		
		/* exception return */
		sb.setLength(0);
		sb.append("{");
		sb.append(makeAfterCode(hooker,cm,true));
		sb.append("}");
		cm.addCatch(sb.toString(),ccThrowable);
		if(logger.isDebugEnabled()){
			logger.debug("insertAfter exception:" + sb.toString());
		}
	}

	private void hookClass(CtClass cc,ClassHooker classHooker){
		String realClassName=cc.getName();
//		logger.debug("hookerClass:realClassName=" +realClassName,new Exception());
		logger.info("Class hook loader:" + loaderWrapper + " class:" + realClassName);
		MethodHooker[] mhs=classHooker.getMethodHookers();
		for(int i=0;i<mhs.length;i++){
			MethodHooker hooker=mhs[i];
			try {
				String methodName=hooker.getName();
				String signature=hooker.getSignature();
				CtMethod cm=cc.getMethod(methodName,signature);
				
//				logger.debug("cm.getName()=" + cm.getName());
//				logger.debug("cm.getDeclaringClass().getName()=" + cm.getDeclaringClass().getName());
//				logger.debug("cm.isEmpty()=" + cm.isEmpty());
				if(realClassName.equals(cm.getDeclaringClass().getName())){
					/* メソッド書き換え */
					logger.info("Method hoook name:" + cm.getName() + " signature:" + cm.getSignature());
					modifyMethod(hooker,cm);
				}
			} catch (CannotCompileException e) {
				logger.error("hooker CannotCompile. "+realClassName +"#" + hooker.getName()+"#" + hooker.getSignature(),e);
			} catch (NotFoundException e) {
				logger.error("hooker NotFound. "+realClassName +"#" + hooker.getName()+"#"+ hooker.getSignature(),e);
			}
		}
	}


	/* (非 Javadoc)
	 * @see javassist.Translator#start(javassist.ClassPool)
	 */
	public void start(ClassPool arg0)
		throws NotFoundException, CannotCompileException {
	}
	
	private void doHook(ClassPool classPool, CtClass targetCc,CtClass parentCc){
		if(parentCc==null){
			return;
		}
		String parentName=parentCc.getName();
		/* JDK提供のクラスローダの子だった場合の処理 */
		if( ClassLoader.class.getName().equals(parentName) || 
				URLClassLoader.class.getName().equals(parentName)){
			logger.debug("modifyLoaderClass:" + targetCc.getName());
			
			InjectClass.modifyLoaderClass(targetCc);
//			return;/*ClassLoaderクラスを重ねて書き換えるて大丈夫か? */
		}
		
		ClassHooker ch=loaderWrapper.getClassHooker(parentName);
		if(ch!=null){
			ch.resolve(parentCc);
			hookClass(targetCc,ch);
		}
		
		/* 親クラスを探す */
		try{
			CtClass supperClass=parentCc.getSuperclass();
			doHook(classPool,targetCc,supperClass);
		}catch(NotFoundException e){}
		
		/* インタフェースを探す */
		try{
			CtClass[] ifs=parentCc.getInterfaces();
			for(int i=0;i<ifs.length;i++){
				doHook(classPool,targetCc,ifs[i]);
			}
		}catch(NotFoundException e){}
	}

	/* (非 Javadoc)
	 * @see javassist.Translator#onWrite(javassist.ClassPool, java.lang.String)
	 */
	public void onWrite(ClassPool classPool, String className)
		throws NotFoundException, CannotCompileException {
		logger.debug("onWrite:" + className);
		CtClass targetCc = classPool.get(className);
		if( targetCc.isInterface()==true){
			return;
		}
		if( className.startsWith("java.") ||
			className.startsWith("naru.queuelet")){
			return;
		}
		
		doHook(classPool,targetCc,targetCc);
	}
	
	/* (非 Javadoc)
	 * @see javassist.Translator#onWrite(javassist.ClassPool, java.lang.String)
	 */
	public CtClass doRewrite(ClassPool classPool, String className)
		throws NotFoundException, CannotCompileException {
		logger.debug("onWrite:" + className);
		CtClass targetCc = classPool.get(className);
		if( targetCc.isInterface()==true){
			return targetCc;
		}
		if( className.startsWith("java.") ||
			className.startsWith("naru.queuelet")){
			return targetCc;
		}
		doHook(classPool,targetCc,targetCc);
		return targetCc;
	}
	

}
