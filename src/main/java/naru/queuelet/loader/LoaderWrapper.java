/*
 * �쐬��: 2004/09/08
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.apache.log4j.Logger;

import naru.queuelet.startup.Startup;
import naru.queuelet.util.*;
import naru.queuelet.QueueletCallInfo;
import naru.queuelet.QueueletHooker;
import naru.queuelet.core.Container;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class LoaderWrapper implements QueueletHooker,Cloneable {
	static private Logger logger=Logger.getLogger(LoaderWrapper.class);
	
	/* property�ϊ� */
	private List classpaths=new ArrayList();
	/* property�ϊ� */
	private List libs=new ArrayList();
	/* property�ϊ� */
	private String home=null;//"user";

	private String name;
	private String loaderClassName;
	private boolean delegate=true;
	
	private QueueletLoader queueletLoader;
	private QueueletTranslator queueletTranslator;
	private ClassPool classPool;
	private Container container;
	private HookerControler hookerControler=new HookerControler();

	/* �ÓI���A�l �i���[�_�P�ʂɎw�肷��ꍇ) */	
	private String returnValue;

	/* �Ăяo���O�ɒʒm����^�[�~�i���i���[�_�P�ʂɎw�肷��ꍇ) */	
	private String before;

	/* �Ăяo����ɒʒm����^�[�~�i���i���[�_�P�ʂɎw�肷��ꍇ) */	
	private String after;
	
	/* �f�o�b�O����container��hoook����ۂ�-cp(classpath)���w�肳�ꂽ�ꍇ�A���Y���[�_�őΏۂɂ��邽�� */	
	/* "system","parent"=>common�̐e,"null":�f�t�H���g */
	private String resouceLoader;
	
	/* �Ăяo�����ɌĂяo���X�^�b�N���܂߂邩�ۂ��i���[�_�P�ʂɎw�肷��ꍇ) */	
	private String callStack;/* ���w���m�邽�߂�String */
	
	public LoaderWrapper dup(){
		try {
			LoaderWrapper result=(LoaderWrapper)clone();
			return result;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public QueueletLoader getQueueletLoader(){
		return queueletLoader;
	}
	
	void collectClasspaths(List list){
		//property�ϊ�
		Iterator itr=classpaths.iterator();
		while(itr.hasNext()){
			String path=(String)itr.next();
			String convertPath=container.resolveProperty(path);
			list.add(new File(convertPath));
		}
	}
	
	void collectLists(List list){
		/* property�ϊ� */
		Iterator itr=libs.iterator();
		while(itr.hasNext()){
			String path=(String)itr.next();
			String convertPath=container.resolveProperty(path);
			File lib=(File)new File(convertPath);
			List jars=getJarPath(lib);
			list.addAll(jars);
		}
	}
	private List getJarPath(File libDir) {
		File directory = libDir;
		if (!directory.isDirectory()
			|| !directory.exists()
			|| !directory.canRead())
			return new ArrayList();
		String filenames[] = directory.list();
		List list = new ArrayList();
		for (int j = 0; j < filenames.length; j++) {
			String filename = filenames[j].toLowerCase();
			if (!filename.endsWith(".jar")&&!filename.endsWith(".zip"))
				continue;
			File file = new File(directory, filenames[j]);
			list.add(file);
		}
		return list;
	}
	
	public void appendLoaderClasspath(ClassLoader loader){
		LoaderClassPath lcp=new LoaderClassPath(loader);
		classPool.appendClassPath(lcp);
	}
	
	public void setup(Container container){
		logger.debug("start:"+name);
		this.container=container;

		/* ���[�U�N���X�i�[���ݒ�(default:${QUEUELET_HOME}/user */
		if(home!=null){
			String convertHome=container.resolveProperty(this.home);
			String userHome=Startup.getQueueletPath(convertHome).getAbsolutePath();
			addClasspath(userHome +"/classes");
			addLib(userHome+"/lib");
		}
		
		List urlList = new ArrayList();
		try {
//			Translator qt=new QueueletTranslator(hookerControler);
			queueletTranslator=new QueueletTranslator(this);
			classPool = new ClassPool(null);

			List fileList = new ArrayList();
			collectClasspaths(fileList);
			collectLists(fileList);

			Iterator itr=fileList.iterator();
			while(itr.hasNext()){
				File path=(File)itr.next();
				if( !path.exists() ){
					logger.warn("classpath not found."+path);
					continue;
				}
				urlList.add(path.toURL());
			}
		}catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if( name!=null){
			queueletLoader=new QueueletLoader();
			ClassLoader loader=null;
			if("system".equals(resouceLoader)){
				loader=ClassLoader.getSystemClassLoader();
			}else if("parent".equals(resouceLoader)){
				ClassLoader common=LoaderWrapper.class.getClassLoader().getParent();
				loader=common.getParent();
			}
			logger.debug(name +":resouceLoader:"+resouceLoader +":"+loader);
			queueletLoader.setup(this,isDelegate(),urlList,loader);
			appendLoaderClasspath(queueletLoader);
		}
		
		hookerControler.setup(container);
		logger.debug("LorderWrapper name:" + name + " classPool:" + classPool);
		logger.debug("end");
	}
	
	public void addClassHooker(ClassHooker hooker){
		hooker.setLoaderWrapper(this);
		hookerControler.addClassHooker(hooker);
	}
	
	public ClassHooker getClassHooker(String className){
		return hookerControler.getClassHooker(className);
	}
	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param string
	 */
	public void setHome(String path) {
		this.home=path;
	}

	/**
	 * @param string
	 */
	public void addLib(String path) {
		/* property�ϊ� */
		libs.add(path);
	}

	/**
	 * @param string
	 */
	public void addClasspath(String path) {
		classpaths.add(path);
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

	/* (�� Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean callMethod(QueueletCallInfo qci) {
		logger.debug("callMethod:"+qci);
		qci.setStatus(QueueletCallInfo.STATUS_CALL);
		/* �񓯊��Ăяo�� */
		QueueletCallInfo syncQci=(QueueletCallInfo)CallStackUtil.isAsync();
		if(syncQci!=null){
			logger.debug("return false:"+qci);
			qci.setSyncQci(syncQci);
			CallStackUtil.push(qci);
			qci.setStatus(QueueletCallInfo.STATUS_BEGIN);
			return false;
		}
		int methodNumber=qci.getMagic();
		MethodHooker hooker=MethodHooker.getMethodHooker(methodNumber);
		qci.setMethodName(hooker.getName());
		qci.setSignature(hooker.getSignature());
		String before=hooker.getBefore();
		CallStackUtil.push(qci);
		
		/* �Ăяo���O���� */
		if( before!=null ){
			if(hooker.getReturnValue()!=null){
				/* ���A�l���ÓI�Ɍ��܂��Ă���΂����ɕ��A */
				container.enque(qci,before);
				qci.setShortcut(true);/* �V���[�g�J�b�g���� */
			}else{
				/* ���A�l�����܂��Ă��Ȃ��A�����ɂ��؂�ւ� */			
				synchronized (qci) {/* �����҂� */
					container.enque(qci,before);
					try {
						qci.wait();
					} catch (InterruptedException e) {
						logger.error("wait Interrupted.",e);
					}
				}
				if(qci.getReturnValue()!=null){
					qci.setShortcut(true);
				}
				logger.debug("return Value:" + qci.getReturnValue());
			}
		}
		if( qci.isShortcut() ){
			qci.setStatus(QueueletCallInfo.STATUS_BEGIN);
			return true;
		}
		hooker.syncCall();/* concurent ���d�x���� */
		qci.setStatus(QueueletCallInfo.STATUS_BEGIN);
		return false;
	}
	
	/* (�� Javadoc)
	 * @see naru.queuelet.QueueletHooker#returnMethod(naru.queuelet.QueueletCallInfo)
	 */
	public QueueletCallInfo returnMethod(Object returnValue,Throwable returnThrowable) {
		logger.debug("returnMethod:"+ returnValue + ":"+returnThrowable,returnThrowable);
		QueueletCallInfo curQci=CallStackUtil.pop();
		curQci.setStatus(QueueletCallInfo.STATUS_END);
		int methodNumber=curQci.getMagic();
		MethodHooker hooker=MethodHooker.getMethodHooker(methodNumber);
		hooker.syncReturn();
		
		/* �V���[�g�J�b�g���́AreturnValue�́A�ݒ��� */
		if( !curQci.isShortcut() ){
			curQci.setReturnValue(returnValue);
		}
		curQci.setReturnThrowable(returnThrowable);
		String after=hooker.getAfter();
		if( after!=null){
			container.enque(curQci,after);
		}
		return curQci;
	}
	

	/* (�� Javadoc)
	 * @see naru.queuelet.QueueletHooker#getByteCode(java.lang.String)
	 */
	public byte[] getByteCode(String className) {
		logger.debug("name:" + name + " getByteCode:" + className);
		byte[] b=null;
		try {
//			CtClass cc = classPool.get(className);
			CtClass cc =queueletTranslator.doRewrite(classPool,className);
			b = cc.toBytecode();
			return b;
		} catch (NotFoundException e) {
			logger.debug("LoaderWrapper#getByteCode NotFoundException:"+className);
		} catch (IOException e) {
			logger.info("LoaderWrapper#getByteCode",e);
		} catch (CannotCompileException e) {
			logger.info("LoaderWrapper#getByteCode",e);
		} catch (Throwable e){
			logger.info("Thowable#getByteCode",e);
		}
		return null;
	}

	/* (�� Javadoc)
	 * @see naru.queuelet.QueueletHooker#getByteCode(java.lang.String, byte[], int, int)
	 */
	public byte[] getByteCode(String className, byte[] b, int off, int len) {
		return getByteCode(className);
	}

	/* (�� Javadoc)
	 * @see naru.queuelet.QueueletHooker#registerClass(java.lang.String, java.lang.Class)
	 */
	public void registerClass(String className, Class clazz) {
		try {
			if( ClassLoader.class.isAssignableFrom(clazz) && 
				QueueletHooker.class.isAssignableFrom(clazz) ){
				logger.debug("registerClass:"+clazz.getName());
//				logger.info("registerClass:"+clazz.getName() + ":" +queueletLoader);
				Class [] types=new Class[1];
				types[0]=ClassLoader.class;
				Method setter=clazz.getMethod("queuelet_setParentClassLoader",types);
				Object[] args=new Object[1];
				args[0]=queueletLoader;
				setter.invoke(null,args);
			}
		} catch (SecurityException e) {
			logger.info("registerClass",e);
		} catch (NoSuchMethodException e) {
			logger.info("registerClass",e);
		} catch (IllegalArgumentException e) {
			logger.info("registerClass",e);
		} catch (IllegalAccessException e) {
			logger.info("registerClass",e);
		} catch (InvocationTargetException e) {
			logger.info("registerClass",e);
		}
		
		ClassHooker ch=getClassHooker(className);
		if( ch==null ){
/*
			ch=new ClassHooker();
			ch.setName(className);
			hookerControler.addClassHooker(ch);
*/
			return;
		}
		ch.setClazz(clazz);
	}


	/**
	 * @return
	 */
	public String getLoaderClassName() {
		return loaderClassName;
	}

	/**
	 * @param string
	 */
	public void setLoaderClassName(String string) {
		loaderClassName = string;
	}

	/**
	 * @return
	 */
	public String getAfter() {
		return after;
	}

	/**
	 * @return
	 */
	public String getBefore() {
		return before;
	}

	/**
	 * @return
	 */
	public String getCallStack() {
		return callStack;
	}

	/**
	 * @param string
	 */
	public void setAfter(String string) {
		after = string;
	}

	/**
	 * @param string
	 */
	public void setBefore(String string) {
		before = string;
	}

	/**
	 * @param string
	 */
	public void setCallStack(String string) {
		callStack = string;
	}

	/**
	 * @return Returns the returnValue.
	 */
	public String getReturnValue() {
		return returnValue;
	}
	/**
	 * @param returnValue The returnValue to set.
	 */
	public void setReturnValue(String returnValue) {
		this.returnValue = returnValue;
	}
	/**
	 * @return Returns the delegate.
	 */
	public boolean isDelegate() {
		return delegate;
	}
	/**
	 * @param delegate The delegate to set.
	 */
	public void setDelegate(boolean delegate) {
		this.delegate = delegate;
	}
	
	public String toString(){
		return getName() + "#" + getLoaderClassName()+ "#" + super.toString();
	}

	public String getResouceLoader() {
		return resouceLoader;
	}

	public void setResouceLoader(String resouceLoader) {
		this.resouceLoader = resouceLoader;
	}
}
