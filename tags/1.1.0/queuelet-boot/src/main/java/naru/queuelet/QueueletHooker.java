/*
 * 作成日: 2004/09/01
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
public interface QueueletHooker {
	public boolean callMethod(QueueletCallInfo req);
	public QueueletCallInfo returnMethod(Object returnValue,Throwable returnThrowable);
	public byte[] getByteCode(String className);
	public byte[] getByteCode(String className,byte[] b,int off,int len);
	public void registerClass(String className,Class clazz);
}
