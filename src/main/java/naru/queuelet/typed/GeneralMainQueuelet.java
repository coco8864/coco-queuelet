/*
 * 作成日: 2004/08/03
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.util.CallThreadUtil;
import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class GeneralMainQueuelet implements Queuelet {
	static private Logger logger=Logger.getLogger(GeneralMainQueuelet.class);

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		return false;
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
		try {
			String className=(String)param.get("className");
			logger.info("className=" + className);
			String[] args=(String [])param.get("QueueletArgs");
			for(int i=0;i<args.length;i++){
				logger.info("arg[" + i + "]=" + args[i]);
			}
			
			ClassLoader loader=(ClassLoader)param.get("QueueletLoader");
			if( loader==null){
				loader=getClass().getClassLoader();
			}
			String methodName = (String)param.get("methodName");
			if(methodName==null){
				methodName="main";
			}
			
			if( className==null ){
				if( args.length==0 ){
					logger.error("args.length=0");
					return;
				}
				className=args[0];
				String[] args2=new String[args.length-1];
				System.arraycopy(args, 1, args2, 0, args.length-1);
				args=args2;
			}
			logger.info("className=" + className);
			Class clazz=loader.loadClass(className);

			Class[] paramTypes = new Class[1];
			paramTypes[0] = args.getClass(); 
			Object[] paramValues = new Object[1];
			paramValues[0] = args;
			CallThreadUtil.callStaticASync(clazz,methodName,paramTypes,paramValues);
		} catch (SecurityException e) {
			logger.error(e);
		} catch (IllegalArgumentException e) {
			logger.error(e);
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
