/*
 * 作成日: 2004/07/22
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet;

import java.util.Properties;

import naru.queuelet.watch.StartupInfo;

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
	 * terminal配下ではない、Queueletで指定できる。
	 * isForceEnd:正常のシーケンスを経ずにデーモンから強制終了を試みる。（正常の終了シーケンスも走行させる）
	 * isRestart:終了時に再起動するか否かを指定,falseの場合以降は無視される。
	 * javaHeap:再起動時に指定する-XmxSS(m)の値を指定、負の値の場合、現状値
	 * javaVmOptions:再起動時に指定するjava VMオプションを指定、nullの場合、現状値
	 * args:再起動時に指定する引数を指定、nullの場合、現状値
	 */
	void finish(boolean isForceEnd,boolean isRestart,StartupInfo startupInfo);
	
	/**
	 * 生存信号を送信,長時間initを実行すると監視タイムアウトにかかるのを回避
	 */
	boolean heatBeat();
	
	/**
	 * @param string
	 * @param file
	 */
	void callASync(String method,Object param);
	void callASync(String method);
	
	/* ${xxx}形式のproperty値を解決する */
	String resolveProperty(String value,Properties prop);
}
