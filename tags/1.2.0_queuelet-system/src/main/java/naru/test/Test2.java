/*
 * 作成日: 2004/10/04
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Category;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class Test2 extends Category{
/**
	 * @param arg0
	 */
	protected Test2(String arg0) {
		super(arg0);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	//	static private Category logger=Category.getInstance(Test2.class);
	private static Log LOG = LogFactory.getLog(Test2.class);

	static{
//		logger.info("aaa");
		LOG.info("bbb");
	}
}
