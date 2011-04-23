/*
 * 作成日: 2004/07/22
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet;

import java.util.Properties;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public interface QueueletContext {
	int queueLength();
	int queueLength(String terminal);
	
	void enque(Object req);
	void enque(Object req,String terminal);
	Object deque();
	Object deque(String terminal);
	void finish();
	/**
	 * @param string
	 * @param file
	 */
	void callASync(String method,Object param);
	void callASync(String method);
	
	/* ${xxx}形式のproperty値を解決する */
	String resolveProperty(String value,Properties prop);
}
