/*
 * 作成日: 2004/08/05
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet;

import java.io.Serializable;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class QueueletCallInfo implements Serializable {
	public static final int STATUS_INIT = 0;
	public static final int STATUS_CALL = 1;
	public static final int STATUS_BEGIN = 2;
	public static final int STATUS_END = 3;

	private int status;

	private Class clazz;
	private Object thiz;
	private String methodName;
	private String signature;
	private Object[] args;
	private Object returnValue;
	private Throwable returnThrowable;
	/* 呼ばれた時刻 */
	private long callTime;

	/* 処理開始時刻 */
	private long beginTime;
	/* 処理終了時刻 */
	private long endTime;

	private String callThreadName;
	private String processThreadName;
	private Throwable callStack;

	/* 内部制御用 */
	private int magic;
	private QueueletCallInfo syncQci=null; /* 非同期時同期呼び出しのqci */
	private boolean shortcut=false; /* メソッド・ショートカット実行 */

	public QueueletCallInfo(int magic, Object thiz, Object[] args,Throwable callStack) {
		this.status = STATUS_INIT;
		this.magic = magic;
		this.thiz = thiz;
		this.args = args;
		this.callStack=callStack;

		this.callTime = System.currentTimeMillis();
		this.callThreadName = Thread.currentThread().getName();
	}

	public QueueletCallInfo(int magic, Object thiz, Object[] args) {
		this(magic,thiz,args,null);
	}

	/**
	 * @return
	 */
	public Object[] getArgs() {
		return args;
	}

	/**
	 * @return
	 */
	public long getBeginTime() {
		return beginTime;
	}

	/**
	 * @return
	 */
	public String getCallThreadName() {
		return callThreadName;
	}

	/**
	 * @return
	 */
	public long getCallTime() {
		return callTime;
	}

	/**
	 * @return
	 */
	public Class getClazz() {
		return clazz;
	}

	/**
	 * @return
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @return
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @return
	 */
	public String getProcessThreadName() {
		return processThreadName;
	}

	/**
	 * @return
	 */
	public Throwable getReturnThrowable() {
		return returnThrowable;
	}

	/**
	 * @return
	 */
	public Object getReturnValue() {
		return returnValue;
	}

	/**
	 * @return
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * @return
	 */
	public Throwable getCallStack() {
		return callStack;
	}

	/**
	 * @return
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @return
	 */
	public Object getThiz() {
		return thiz;
	}

	/**
	 * @param objects
	 */
	public void setArgs(Object[] objects) {
		args = objects;
	}

	/**
	 * @param string
	 */
	public void setCallThreadName(String string) {
		callThreadName = string;
	}

	/**
	 * @param class1
	 */
	public void setClazz(Class class1) {
		clazz = class1;
	}

	/**
	 * @param string
	 */
	public void setMethodName(String string) {
		methodName = string;
	}

	/**
	 * @param string
	 */
	public void setProcessThreadName(String string) {
		processThreadName = string;
	}

	/**
	 * @param throwable
	 */
	public void setReturnThrowable(Throwable throwable) {
		returnThrowable = throwable;
	}

	/**
	 * @param object
	 */
	public void setReturnValue(Object object) {
		returnValue = object;
	}

	/**
	 * @param string
	 */
	public void setSignature(String string) {
		signature = string;
	}

	/**
	 * @param i
	 */
	public void setStatus(int status) {
		if( status==STATUS_CALL){
			this.callTime = System.currentTimeMillis();
		}else if(status==STATUS_BEGIN){
			this.beginTime = System.currentTimeMillis();
		}else if(status==STATUS_END){
			this.endTime = System.currentTimeMillis();
		}
		this.status = status;
	}

	/**
	 * @param object
	 */
	public void setThiz(Object object) {
		thiz = object;
	}

	/**
	 * @return
	 */
	public int getMagic() {
		return magic;
	}

	/**
	 * @param i
	 */
	public void setMagic(int i) {
		magic = i;
	}

	/* (非 Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "QueueletCallInfo##status:"
			+ status
			+ ",methodName:"
			+ methodName;
	}

	/**
	 * @return
	 */
	public boolean isAsync() {
		return (syncQci!=null);
	}

	/**
	 * @return
	 */
	public boolean isSync() {
		return (syncQci==null);
	}

	/**
	 * @return
	 */
	public boolean isShortcut() {
		return shortcut;
	}

	/**
	 * @return
	 */
	public QueueletCallInfo getSyncQci() {
		return syncQci;
	}

	/**
	 * @param b
	 */
	public void setShortcut(boolean b) {
		shortcut = b;
	}

	/**
	 * @param info
	 */
	public void setSyncQci(QueueletCallInfo info) {
		syncQci = info;
	}

}
