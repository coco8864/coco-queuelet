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
import java.nio.channels.SelectionKey;
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
public class CannelQueuelet implements Queuelet, ChannelEvent {
	static private Logger logger=Logger.getLogger(CannelQueuelet.class);
	private QueueletContext context;
	private ChannelListener cl;
	private Map requestMap = Collections.synchronizedMap(new HashMap());
	private String getSocketMethod=null;
	private long timeout=0;
	private String timeoutTerminal=null;
	private String ioqueue="io";
	

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object, naru.quelet.Terminal)
	 */
	public boolean service(Object req) {
		NonBlockIf state=(NonBlockIf)req;
		SocketChannel socketChannel;
		switch(state.getOperation()){
		case SelectionKey.OP_CONNECT:
			try {
					socketChannel=SocketChannel.open();
					socketChannel.configureBlocking(false);
					socketChannel.connect(state.getSocketAddress());
					requestMap.put(socketChannel,req);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//			cl.connectable(socketChannel);
			break;
		case SelectionKey.OP_READ:
			socketChannel=state.getSocketChannel();
			requestMap.put(socketChannel,req);
//			cl.readable(socketChannel);
			break;
		case SelectionKey.OP_WRITE:
			socketChannel=state.getSocketChannel();
			requestMap.put(socketChannel,req);
//			cl.writerble(socketChannel);
			break;
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
		NonBlockIf state=(NonBlockIf)requestMap.remove(socketChannel);
		if( context==null){
			logger.warn("lynnLynn null.socket:"+ socketChannel.toString());
			return;
		}
		context.enque(state,ioqueue);
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
