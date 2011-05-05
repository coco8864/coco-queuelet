/*
 * 作成日: 2004/08/10
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.util;

import java.util.Stack;

import naru.queuelet.QueueletCallInfo;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class CallStackUtil extends ThreadLocal{
	private static CallStackUtil callStackThreadLocal=new CallStackUtil();
	private static ThreadLocal callCheckThreadLocal=new ThreadLocal();

	/* (非 Javadoc)
	 * @see java.lang.ThreadLocal#initialValue()
	 */
	protected Object initialValue() {
		return new Stack();
	}
	
	public static QueueletCallInfo pop(){
		Stack callStack=(Stack)callStackThreadLocal.get();
		return (QueueletCallInfo)callStack.pop();
	}

	public static void push(QueueletCallInfo info){
		Stack callStack=(Stack)callStackThreadLocal.get();
		callStack.push(info);
	}
	
	public static QueueletCallInfo peek(){
		Stack callStack=(Stack)callStackThreadLocal.get();
		return (QueueletCallInfo)callStack.peek();
	}

	/*　非同期呼び出し時に２重にキューしない仕組み */
	public static Object isAsync(){
		Object o=callCheckThreadLocal.get();
		callCheckThreadLocal.set(null);
		return o;
	}
	
	public static void setAsync(Object obj){
		if( obj==null ){
			throw new NullPointerException(CallStackUtil.class.getName() +"#setAsync");
		}
		callCheckThreadLocal.set(obj);
	}
}
