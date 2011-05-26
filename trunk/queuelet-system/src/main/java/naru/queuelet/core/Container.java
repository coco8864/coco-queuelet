/*
 * 作成日: 2004/07/21
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import naru.queuelet.QueueletHooker;
import naru.queuelet.loader.ClassHooker;
import naru.queuelet.loader.LoaderWrapper;
import naru.queuelet.loader.MethodHooker;
import naru.queuelet.startup.Startup;
import naru.queuelet.store.QueueletStore;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.xml.sax.SAXException;

/**
 * @author naru
 * 
 * この生成されたコメントの挿入されるテンプレートを変更するため ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class Container {
	static private Logger logger = Logger.getLogger(Container.class);

	/* 1VM上には、1つのコンテナとする */
	static private Container instance;

	private Map terminalMap = new HashMap();
	//起動停止時の初期化、終了か順番を規定する
	private List terminalOrderNames=new ArrayList();

	private Map loaderMap = new HashMap();

	private List queuelets = new ArrayList();

	private int checkInterval = 60;
	private String stopMode = "check";// 9999|check|manager

	private File properties = new File("queuelet.properties");

	private File sysPropertiesFile = new File("system/queuelet.properties");

	private QueueletStore store = null;

	private String managerPort;

	private QueueletProperties queueletProperties;

	private QueueletDaemon queueletDaemon;

	private QueueletAcceptChannel queueletAcceptChannel;

	private String[] queueletArgs=new String[0];
	
	/* 既存クラスローダをフック */
	private LoaderWrapper defaultDynamicLoader;

	private Map dynamicLoaderMap = new HashMap();

	private Container() {
	}

	private static Digester getQueueletDigester() {
		ClassLoader thisCl = Thread.currentThread().getContextClassLoader();
		Digester digester = new Digester();
		digester.setClassLoader(thisCl);
		digester.addSetProperties("queueApp");

		digester.addCallMethod("queueApp/properties", "setProperties", 1);
		digester.addCallParam("queueApp/properties", 0, "file");
		
		digester.addCallMethod("queueApp/sysProperty", "setSysProperty", 2);
		digester.addCallParam("queueApp/sysProperty", 0, "name");
		digester.addCallParam("queueApp/sysProperty", 1, "value");
		
		digester.addObjectCreate("queueApp/store", QueueletStore.class);
		digester.addSetProperties("queueApp/store");
		digester.addSetNext("queueApp/store", "setStore");

		digester.addObjectCreate("queueApp/terminal", Terminal.class);
		digester.addSetProperties("queueApp/terminal");
		digester.addSetNext("queueApp/terminal", "addTerminal");

		digester.addObjectCreate("queueApp/terminal/store", QueueletStore.class);
		digester.addSetProperties("queueApp/terminal/store");
		digester.addSetNext("queueApp/terminal/store", "setStore");

		digester.addObjectCreate("queueApp/terminal/queuelet",QueueletWrapper.class);
		digester.addSetProperties("queueApp/terminal/queuelet");
		digester.addSetNext("queueApp/terminal/queuelet", "addQuelet");
		digester.addSetTop("queueApp/terminal/queuelet", "setTerminal");
		digester.addCallMethod("queueApp/terminal/queuelet/param", "putParam",2);
		digester.addCallParam("queueApp/terminal/queuelet/param", 0, "name");
		digester.addCallParam("queueApp/terminal/queuelet/param", 1, "value");

		digester.addObjectCreate("queueApp/queuelet", QueueletWrapper.class);
		digester.addSetProperties("queueApp/queuelet");
		digester.addSetNext("queueApp/queuelet", "addQuelet");
		digester.addCallMethod("queueApp/queuelet/param", "putParam", 2);
		digester.addCallParam("queueApp/queuelet/param", 0, "name");
		digester.addCallParam("queueApp/queuelet/param", 1, "value");

		if (Startup.startupProperteis.isUseLoader()) {
			digester.addObjectCreate("queueApp/loader", LoaderWrapper.class);
			digester.addSetProperties("queueApp/loader");
			digester.addSetNext("queueApp/loader", "addLoader");

			digester.addObjectCreate("queueApp/loader/classHooker",ClassHooker.class);
			digester.addSetProperties("queueApp/loader/classHooker");
			digester.addSetNext("queueApp/loader/classHooker", "addClassHooker");

			digester.addObjectCreate("queueApp/loader/classHooker/methodHooker",MethodHooker.class);
			digester.addSetProperties("queueApp/loader/classHooker/methodHooker");
			digester.addSetNext("queueApp/loader/classHooker/methodHooker","addMethodHooker");

			digester.addCallMethod("queueApp/loader/lib", "addLib", 1);
			digester.addCallParam("queueApp/loader/lib", 0, "path");
			digester.addCallMethod("queueApp/loader/classpath", "addClasspath",1);
			digester.addCallParam("queueApp/loader/classpath", 0, "path");
		}
		return digester;
	}

	public static Container getInstance() throws IOException, SAXException {
		return getInstance(new File("queuelet.xml"));
	}

	public static Container getInstance(File file) throws IOException,
			SAXException {
		FileInputStream fis = new FileInputStream(file);
		return getInstance(fis);
	}

	public static Container getInstance(InputStream is) throws IOException,
			SAXException {
		if (instance != null) {
			return instance;
		}

		String log4jconfFile = Startup.getLog4jConfiguration();
		if (log4jconfFile != null) {
			if (log4jconfFile.endsWith(".xml")) {
				DOMConfigurator.configure(log4jconfFile);
			} else {
				PropertyConfigurator.configure(log4jconfFile);
			}
		}
		Container container = new Container();
		Digester digester = getQueueletDigester();
		digester.push(container);
		digester.parse(is);
		is.close();

		instance = container;
		return container;
	}

	private Map xmlSysProperties;
	/**
	 * queueletの定義xmlにしたがってdigesterから呼び出される
	 * -Dをシュミレート
	 * このときはまだresolveできないので後で実施
	 * @param name
	 * @param value
	 */
	public void setSysProperty(String name,String value){
		if(xmlSysProperties==null){
			xmlSysProperties=new HashMap();
		}
		xmlSysProperties.put(name,value);
	}
	
	private void executeSetSysProperties(){
		if(xmlSysProperties==null){
			return;
		}
		Iterator itr=xmlSysProperties.keySet().iterator();
		while(itr.hasNext()){
			String name=(String)itr.next();
			name=resolveProperty(name);
			String value=resolveProperty((String)xmlSysProperties.get(name));
			logger.debug("setProperty name:" +name +":value:" +value);
			System.setProperty(name, value);
		}
		xmlSysProperties=null;
	}
	
	private List userPropertiesFiles;
	/**
	 * queueletの定義xmlにしたがってdigesterから呼び出される
	 * @param file
	 */
	public void setProperties(String file){
		if(userPropertiesFiles==null){
			userPropertiesFiles=new ArrayList();
		}
		userPropertiesFiles.add(file);
	}
	
	private void executeAddUserProperties(QueueletProperties queueletProperties){
		if(userPropertiesFiles==null){
			return;
		}
		Iterator itr=userPropertiesFiles.iterator();
		while(itr.hasNext()){
			String fileName=(String)itr.next();
			fileName=resolveProperty(fileName);
			logger.debug("load user property fileName:" +fileName);
			queueletProperties.addUserProperties(new File(fileName));
		}
		userPropertiesFiles=null;
	}
	
	public String[] getArgs() {
		logger.debug("call getArgs:" + queueletArgs);
		return queueletArgs;
	}
	
	public QueueletProperties getQueueletProperties() {
		return queueletProperties;
	}

	/**
	 * @param nextQT
	 * @return
	 */
	public Terminal getTerminal(String nextQTName) {
		return (Terminal) terminalMap.get(nextQTName);
	}

	/**
	 * @param nextQT
	 * @return
	 */
	public ClassLoader getLoader(String loaderName) {
		if (loaderName == null) {
			return null;
		}
		LoaderWrapper lw = (LoaderWrapper) loaderMap.get(loaderName);
		return (ClassLoader) lw.getQueueletLoader();
	}

	/* Startupからローダの初期化時に呼び出される */
	public QueueletHooker getQueueletHooker(ClassLoader loader) {
		String className = loader.getClass().getName();

		LoaderWrapper lw = (LoaderWrapper) dynamicLoaderMap.get(className);
		if (lw == null) {
			if (defaultDynamicLoader == null) {
				logger.info("ClassLoader not hook:" + className);
				return null;
			} else {
				lw = defaultDynamicLoader;
			}
		}
		LoaderWrapper dupWrapper = lw.dup();
		dupWrapper.setName(className);
		dupWrapper.setup(this);
		dupWrapper.appendLoaderClasspath(loader);
		logger.info("ClassLoader hook name:" + className + " wrapper:"+dupWrapper);
//		logger.debug("loader toString:" + loader);
		return dupWrapper;
	}

	public void status() {
		queueletDaemon.status();
	}

	private void loadInnerDefinition(String defXmlFileName) {
		File defXmlFile = Startup.getQueueletPath(defXmlFileName);
		if (!defXmlFile.canRead()) {
			logger.error("can't read innerXml file."
					+ defXmlFile.getAbsolutePath());
			return;
		}
		Digester digester = getQueueletDigester();
		digester.push(this);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(defXmlFile);
			digester.parse(fis);
		} catch (IOException e) {
			logger.error("read innerXml error." + defXmlFile.getAbsolutePath(),
					e);
		} catch (SAXException e) {
			logger.error("read innerXml error." + defXmlFile.getAbsolutePath(),
					e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ignore) {
					ignore.printStackTrace();
				}
			}
		}
	}

	public void start() throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException {
		logger.debug("start in");
		logger.info("System SecurityManager:"+System.getSecurityManager());
		Properties sysProp=System.getProperties();
		Iterator sysPropItr=sysProp.keySet().iterator();
		while(sysPropItr.hasNext()){
			String key=(String)sysPropItr.next();
			String value=sysProp.getProperty(key);
			logger.info("System Property:"+key +":" +value);
		}
		Map envs=System.getenv();
		Iterator envItr=envs.keySet().iterator();
		while(envItr.hasNext()){
			Object key=envItr.next();
			Object value=envs.get(key);
			logger.info("System env:"+key +":" +value);
		}
		
//		ClassLoader ql = (ClassLoader) Container.class.getClassLoader();
		this.queueletArgs=Startup.getArgs();
		
		File homeDir=Startup.startupProperteis.getQueueletHome();
		this.properties=new File(homeDir,"queuelet.properties");
		/* プロパティファイルの読み込み */
		File systemDir=Startup.startupProperteis.queueletSystemPath();
		this.sysPropertiesFile=new File(systemDir,"queuelet.properties");
		queueletProperties = new QueueletProperties(this.properties,this.sysPropertiesFile);
		
		//Userプロパティの追加
		executeAddUserProperties(queueletProperties);
		
		//プロパティの設定
		executeSetSysProperties();
		
		/* 組み込み処理の追加 */
		if (managerPort != null) {
			queueletProperties.setSysProperty("queuelet.manager.httpPort",
					managerPort);
			loadInnerDefinition(systemDir +"/conf/httpManager.xml");
		}
		if (store != null) {
			store.init(queueletProperties);
			if (store.isStartupServer()) {
				loadInnerDefinition(systemDir + "/conf/storeServer.xml");
				QueueletWrapper wrapper = (QueueletWrapper) queuelets
						.get(queuelets.size() - 1);
				wrapper.init(this);
				queuelets.remove(wrapper);
			}
		}
		if (Startup.startupProperteis.isUseNio()) {
			/* socket接続端点の獲得 */
			queueletAcceptChannel = new QueueletAcceptChannel();
		}
		queueletDaemon = new QueueletDaemon(this);

		Iterator itr = loaderMap.values().iterator();
		while (itr.hasNext()) {
			LoaderWrapper loader = (LoaderWrapper) itr.next();
			loader.setup(this);
		}
		
		for(int i=0;i<terminalOrderNames.size();i++){
			Object name=terminalOrderNames.get(i);
			Terminal terminal = (Terminal) terminalMap.get(name);
			terminal.init(this);
		}
		/*
		itr = terminalMap.values().iterator();
		while (itr.hasNext()) {
			Terminal terminal = (Terminal) itr.next();
			terminal.init(this);
		}
		*/
		
		for(int i=0;i<terminalOrderNames.size();i++){
			Object name=terminalOrderNames.get(i);
			Terminal terminal = (Terminal) terminalMap.get(name);
			terminal.start();
		}
		/*
		itr = terminalMap.values().iterator();
		while (itr.hasNext()) {
			Terminal terminal = (Terminal) itr.next();
			terminal.start();
		}
		*/
		
		for(int i=0;i<queuelets.size();i++){
			QueueletWrapper wrapper = (QueueletWrapper) queuelets.get(i);
			wrapper.init(this);
		}
		/*
		itr = queuelets.iterator();
		while (itr.hasNext()) {
			QueueletWrapper wrapper = (QueueletWrapper) itr.next();
			wrapper.init(this);
		}
		*/

		queueletDaemon.start();
		logger.info("Queuelet Container start");
	}

	public void stop() {
		stop(false,-1,null);
	}
	
	public void stop(boolean restart, int xmx, String vmoption) {
		logger.debug("Queuelet Container stop secuence");
		if( !queueletDaemon.isStop() ){
			//本来Deamonから停止すべきだがStartUpから停止された場合
			queueletDaemon.stopRequest();
			return;
		}
		if(queueletAcceptChannel!=null){
			queueletAcceptChannel.end();
			queueletAcceptChannel=null;
		}
		if (store != null) {
			if (store.isStartupServer()) {
				store.shutdownSore();
			}
			store.term();
			store=null;
		}

		for(int i=queuelets.size()-1;i>=0;i--){
			QueueletWrapper wrapper = (QueueletWrapper) queuelets.get(i);
			wrapper.term();
		}
		/*
		Iterator itr;
		itr = queuelets.iterator();
		while (itr.hasNext()) {
			QueueletWrapper wrapper = (QueueletWrapper) itr.next();
			wrapper.term();
		}
		*/
		
		for(int i=terminalOrderNames.size()-1;i>=0;i--){
			Object name=terminalOrderNames.get(i);
			Terminal terminal = (Terminal) terminalMap.get(name);
			terminal.stop();//配信停止
			terminal.term();//term呼び出し
		}
		/*
		itr = terminalMap.values().iterator();
		while (itr.hasNext()) {
			Terminal terminal = (Terminal) itr.next();
			terminal.stop();
		}
		*/
		/*
		for(int i=terminalOrderNames.size()-1;i>=0;i--){
			Object name=terminalOrderNames.get(i);
			Terminal terminal = (Terminal) terminalMap.get(name);
			terminal.term();
		}
		*/
		/*
		itr = terminalMap.values().iterator();
		while (itr.hasNext()) {
			Terminal terminal = (Terminal) itr.next();
			terminal.term();
		}
		*/
		synchronized(this){
			this.notify();//mainスレッドを終了させる
		}
		logger.info("Queuelet Container stop");
	}

	/* Treminalからの呼び出し */
	/*
	 * (非 Javadoc)
	 * 
	 * @see naru.quelet.QueletCommand#enque(java.lang.Object, java.lang.String)
	 */
	public void enque(Object req, String terminal) {
		Terminal t = getTerminal(terminal);
		if (t == null) {
			if (store != null) {
				store.enque(req, terminal);
				return;
			}
			logger.warn("enque() no terminal name:" + terminal);
			return;
		}
		t.enque(req);
	}

	/*
	 * (非 Javadoc)
	 * 
	 * @see naru.quelet.QueletCommand#deque(java.lang.String)
	 */
	public Object deque(String terminal) {
		Terminal t = getTerminal(terminal);
		if (t == null) {
			if (store != null) {
				Object req = store.deque(terminal);
				return req;
			}
			logger.debug("deque() no terminal name:" + terminal);
			return null;
		}
		return t.deque();
	}

	/* Digester経由の呼び出しメソッド */
	public void setManagerPort(String managerPort) {
		this.managerPort = managerPort;
	}

	public void addTerminal(Terminal terminal) {
		terminalOrderNames.add(terminal.getName());
		terminalMap.put(terminal.getName(), terminal);
	}

	public void addQuelet(QueueletWrapper queletWrapper) {
		queuelets.add(queletWrapper);
	}

	public void addLoader(LoaderWrapper loader) {
		String name = loader.getName();
		String loaderClassName = loader.getLoaderClassName();
		if ("*".equals(loaderClassName)) {
			defaultDynamicLoader = loader;
		} else if (loaderClassName != null) {
			dynamicLoaderMap.put(loaderClassName, loader);
		} else if (name != null) {
			loaderMap.put(name, loader);
		} else {
			logger.warn("loader tag need name or loaderClassName");
		}
	}

	/**
	 * @param checkInterval
	 *            監視間隔.
	 */
	public void setCheckInterval(int checkInterval) {
		this.checkInterval = checkInterval;
	}

	/**
	 * @param property
	 *            The property to set.
	 */
	public void setProperties(File properties) {
		this.properties = properties;
	}

	/**
	 * @param stopMode The stopMode to set.
	 */
	public void setStopMode(String stopMode) {
		this.stopMode = stopMode;
	}

	/**
	 * @return Returns the stopMode.
	 */
	public String getStopMode() {
		return stopMode;
	}
	
	/**
	 * @param checkInterval
	 *            監視間隔.
	 */
	public int getCheckInterval() {
		return checkInterval;
	}

	public void setStore(QueueletStore store) {
		this.store = store;
	}

	public Iterator getTermianlNameIterator(){
		return terminalMap.keySet().iterator();
	}

	/**
	 * @return
	 */
	public QueueletDaemon getQueueletDaemon() {
		return queueletDaemon;
	}
	
	
	/* aaa${xxx}bbb -> aaayyybbb */
	public String resolveProperty(String in) {
		return queueletProperties.resolveProperty(in);
	}

	/* ソケット受信端点 */
	public void registerTerminalPort(int port, Terminal terminal)
			throws IOException {
		if( queueletAcceptChannel==null ){
			throw new IllegalArgumentException("can't use terminal port");
		}
		queueletAcceptChannel.register(port, terminal);
	}

	public static void main(String[] args) {
		if (args.length <= 0) {
			System.out.println("usage:java " + Container.class.getName()
					+ " queueAppXmlFile");
			return;
		}
		try {
			Container container = getInstance(new File(args[0]));
			container.start();
			container.status();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}