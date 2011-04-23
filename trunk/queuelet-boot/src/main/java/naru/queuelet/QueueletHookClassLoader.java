/*
 * 作成日: 2004/09/18
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class QueueletHookClassLoader extends ClassLoader {
	private static ClassLoader __queueletRootClassLoader;

	protected static ClassLoader queuelet_getParentClassLoader(ClassLoader cl){
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
	 * 
	 */
	public QueueletHookClassLoader() {
		super(__queueletRootClassLoader);
//		System.out.println("QueueletHookClassLoader():"+ __queueletRootClassLoader);
	}

	/**
	 * @param arg0
	 */
	public QueueletHookClassLoader(ClassLoader arg0) {
		super(queuelet_getParentClassLoader(arg0));
//		System.out.println("QueueletHookClassLoader(arg0):"+ arg0 + ":" + queuelet_getParentClassLoader(arg0));
	}
}
