/*
 * 作成日: 2004/07/21
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;

import naru.channel.ChannelEvent;
import naru.channel.ChannelListener;
import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class ReceiveQueuelet implements Queuelet, ChannelEvent {
	static private Logger logger=Logger.getLogger(ReceiveQueuelet.class);
	private QueueletContext context;
	private ChannelListener cl;
	private Map requestMap = Collections.synchronizedMap(new HashMap());
	private String getSocketMethod=null;
	private long timeout=0;
	private String timeoutTerminal=null;

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object, naru.quelet.Terminal)
	 */
	public boolean service(Object req) {
		Socket socket=null;//req.getSocket();
		SocketChannel socketChannel=null;
		Object mybeSocket=null;
		
		try {
			if(getSocketMethod!=null &&
				!(req instanceof Socket) && 
				!(req instanceof SocketChannel)){
				mybeSocket=MethodUtils.invokeMethod(req,getSocketMethod,new Object[0]);
			}else{
				mybeSocket=req;
			}
				
			if(mybeSocket instanceof Socket){
				socket=(Socket)mybeSocket;
				socketChannel=socket.getChannel();
			}else if(mybeSocket instanceof SocketChannel){
				socketChannel=(SocketChannel)mybeSocket;
				socket=socketChannel.socket();
			}else{
				throw new IllegalArgumentException("ReadQuelet service error:reqest=" + req);
			}
			requestMap.put(socket,req);
			
			try {
				cl.waitForResponse(socketChannel);
			} catch (IOException e) {
				try {
					socketChannel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
			return true;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return true;
	}

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#init()
	 */
	public void init(QueueletContext command,Map param) {
		this.context=command;
		this.getSocketMethod=(String)param.get("getSocketMethod");
		String timeoutString=(String)param.get("timeout");
		if(timeoutString!=null){
			this.timeout=Long.parseLong(timeoutString);
			this.timeoutTerminal=(String)param.get("timeoutTerminal");
		}
		try {
			this.cl=new ChannelListener(this,this.timeout);
		} catch (IOException e) {
			e.printStackTrace();
			command.finish();
			return;
		}
		cl.start();
	}

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#term()
	 */
	public void term() {
		if( cl==null ){
			return;
		}
		cl.stop();
		cl=null;
		context=null;
	}

	/* (非 Javadoc)
	 * @see naru.channel.ChannelEvent#lynnLynn(java.nio.channels.SocketChannel)
	 */
	public void lynnLynn(SocketChannel socketChannel) {
		Socket socket=socketChannel.socket();
		Object request=requestMap.remove(socket);
		if( request==null){
			logger.warn("lynnLynn null.socket:"+ socket.toString());
			return;
		}
		context.enque(request);
	}

	/* (non-Javadoc)
	 * @see naru.channel.ChannelEvent#timeout(java.nio.channels.SocketChannel)
	 */
	public void timeout(SocketChannel socketChannel) {
		Socket socket=socketChannel.socket();
		Object request=requestMap.remove(socket);
		if( request==null){
			logger.warn("timeout null.socket:"+ socket.toString());
			return;
		}
		context.enque(request,timeoutTerminal);
	}
}
