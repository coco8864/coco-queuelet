/*
 * 作成日: 2004/07/22
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.sample;

import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;


/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class SimpleQuelet implements Queuelet {
	private QueueletContext command;

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		System.out.println("this:" + this.toString() +" req:"+ req);
//		command.enqueNext(this.toString() + req);
//		command.enqueNext(req);
		command.enque("stop");
		command.finish();
		return true;
	}

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#init(naru.quelet.QueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
		this.command=command;
	}

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#term()
	 */
	public void term() {
	}

}
