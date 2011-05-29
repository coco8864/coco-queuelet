/*
 * Created on 2004/10/21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru hayashi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Close implements Queuelet {
	private static Logger logger=Logger.getLogger(Close.class);	
	private QueueletContext context;
	
	public void waitForClose(HttpContext httpContext){
		Socket socket=httpContext.getSocket();
		if(socket==null){
			return;
		}
		//ブラウザ側からのcloseを待つ
		try{
			byte[] buffer=httpContext.getBuffer();
			socket.setSoTimeout(100);//通信としてのタイムアウトを短く設定しておく
			//HTTP有効データが未読み込みの場合から読みする。（トレースを採取している場合有効）
			InputStream is=httpContext.getRequestBodyStream();
			if(is==null){//必ずBodyStreamがあるわけではない
				return;
			}
			while(true){
				int readLength=is.read(buffer);//読み捨て
				if(readLength<=0){
					break;
				}
			}
			//HTTP的な有効データではないが、余分なデータがあった場合
			is=socket.getInputStream();
			while(true){
				//POSTの場合IEは、Content-Length長プラス0D0Aを送信してくる???いいのか???
				int readLength=is.read(buffer);//読み捨て
				if(readLength<=0){
					break;
				}
			}
		}catch(IOException ignore){
			logger.debug("wait for browser close error",ignore);
		}
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_CLOSE);
		
		//いきなりクローズするとレスポンスが最後まで到達しない場合がある。
		waitForClose(httpContext);
		httpContext.closeSocket(true);
		httpContext.recycle();
		AccessLog accessLog=httpContext.getAccessLog();
		context.enque(accessLog,Config.QUEUE_ACCESSLOG);
		context.enque(httpContext);
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
