/*
 * 作成日: 2004/09/18
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class QueueletHookUrlClassLoader extends URLClassLoader {

	private static ClassLoader __queueletRootClassLoader;

	protected static ClassLoader queuelet_getParentClassLoader(ClassLoader cl) {
		ClassLoader system=ClassLoader.getSystemClassLoader();
		if(cl==null || cl.equals(system)){
			return __queueletRootClassLoader;
		}
		return cl;
	}

	public static void queuelet_setParentClassLoader(ClassLoader cl){
		__queueletRootClassLoader=cl;
	}

	/**
	 * @param arg0
	 */
	public QueueletHookUrlClassLoader(URL[] arg0) {
		super(arg0,__queueletRootClassLoader);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public QueueletHookUrlClassLoader(URL[] arg0, ClassLoader arg1) {
		super(arg0, queuelet_getParentClassLoader(arg1));
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public QueueletHookUrlClassLoader(
		URL[] arg0,
		ClassLoader arg1,
		URLStreamHandlerFactory arg2) {
		super(arg0, queuelet_getParentClassLoader(arg1), arg2);
	}
}
