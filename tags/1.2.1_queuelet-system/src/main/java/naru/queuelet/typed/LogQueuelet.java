/*
 * 作成日: 2004/08/31
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletCallInfo;
import naru.queuelet.QueueletContext;
import naru.queuelet.loader.MethodHooker;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class LogQueuelet implements Queuelet {
	static private Logger logger=Logger.getLogger(LogQueuelet.class);
	private QueueletContext context;

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		if(!(req instanceof QueueletCallInfo)){
			logger.info(req.toString());
			context.enque(req);
			return true;
		}
		QueueletCallInfo qci=(QueueletCallInfo)req;
		int methodNumber=qci.getMagic();
		MethodHooker hooker=MethodHooker.getMethodHooker(methodNumber);

		if(qci.isSync()){
			Throwable callStack=qci.getCallStack();
			if( callStack!=null){
				logger.info("callStack",callStack);
			}
		}

		StringBuffer sb=new StringBuffer();
		sb.append(qci.getCallThreadName());
		sb.append(",");
		
		Object thiz=qci.getThiz();
		if(thiz!=null){
			sb.append(thiz.getClass().getName());
		}else{
			sb.append("null");
		}
		sb.append(",");
		sb.append(hooker.getName());
		sb.append(",");
		sb.append(hooker.getSignature());
		sb.append(",");
		
		long callTime=qci.getCallTime();
		sb.append(callTime);
		sb.append(",");
		sb.append(qci.getBeginTime()-callTime);
		sb.append(",");
		sb.append(qci.getEndTime()-callTime);
		sb.append(",");
		
		sb.append(qci.getReturnValue());
		sb.append(",");
		Throwable retThrowable=qci.getReturnThrowable();
		sb.append(retThrowable);
		sb.append(",");
		Object[] args=qci.getArgs();
		if( args!=null){
			for(int i=0;i<args.length;i++){
				sb.append(args[i]);
				sb.append(",");
			}
		}
		logger.info(sb.toString());
		if(retThrowable!=null){
			logger.info("ReturnThrowable",retThrowable);
		}
		context.enque(req);
		return true;
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
		this.context=command;
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
