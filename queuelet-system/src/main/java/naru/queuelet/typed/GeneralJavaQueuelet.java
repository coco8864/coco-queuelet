/*
 * 作成日: 2004/08/03
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import naru.queuelet.util.CallThreadUtil;
import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class GeneralJavaQueuelet implements Queuelet {
	private QueueletContext context;
	private static String[]argsKey=
		{"arg0","arg1","arg2","arg3","arg4","arg5","arg6","arg7",
		"arg8","arg9","arg10","arg11","arg12","arg13","arg14","arg15"};

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
			List argsList=new ArrayList();
			for(int i=0;i<argsKey.length;i++){
				String arg=(String)param.get(argsKey[i]);
				if( arg==null ){
					break;
				}
				argsList.add(arg);
			}
			ClassLoader loader=(ClassLoader)param.get("QueueletLoader");
			if( loader==null){
				loader=getClass().getClassLoader();
			}
			Class clazz=loader.loadClass(className);

			String methodName = (String)param.get("methodName");
			if(methodName==null){
				methodName="main";
			}
			String[] args=(String [])argsList.toArray(new String[argsList.size()]);

			Class[] paramTypes = new Class[1];
			paramTypes[0] = args.getClass(); 
			Object[] paramValues = new Object[1];
			paramValues[0] = args;
			CallThreadUtil.callStaticASync(clazz,methodName,paramTypes,paramValues);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}