/*
 * 作成日: 2004/07/21
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;

import naru.channel.ASyncIOController;
import naru.channel.ASyncIOEvent;
import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru
 *
 * ASyncIOEventは、ASyncIOControllerのコントロールスレッドから呼び出されるため実IOを発生
 * させてはならない。
 */
public class ASyncIOQueuelet implements Queuelet,ASyncIOEvent {
	static private Logger logger=Logger.getLogger(ASyncIOQueuelet.class);
	private QueueletContext context;
	private ASyncIOController controller;
	private String poolQueue;
	private String readQueue;
	private String writeQueue;

	private Map channelsMap=Collections.synchronizedMap(new HashMap());
	
	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object, naru.quelet.Terminal)
	 */
	public boolean service(Object req) {
		ASyncIOIf ioif=(ASyncIOIf)req;
		int operation=ioif.getOperation();
		SelectableChannel channel=ioif.getChannel();
		channelsMap.put(channel, ioif);
		try {
			switch(operation){
			case SelectionKey.OP_READ:
				controller.waitForRead(channel, ioif.getTimeout());
				break;
			case SelectionKey.OP_WRITE:
				controller.waitForWrite(channel, ioif.getTimeout());
				break;
			case SelectionKey.OP_CONNECT:
				controller.waitForConnect((SocketChannel)channel,ioif.getSocketAdress(),ioif.getTimeout());
				break;
//			case SelectionKey.OP_ACCEPT:
//				controller.waitForAccept(serverSocketChannel, ioif.getTimeout());
//				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#init()
	 */
	public void init(QueueletContext context,Map param) {
		this.context=context;
		String portString;
		
		portString=(String)param.get("port");
		if(portString!=null){
			int port=Integer.parseInt(portString);
			try {
				int backlog=Integer.parseInt((String)param.get("backlog"));
				int timeout=Integer.parseInt((String)param.get("timeout"));
				this.controller=new ASyncIOController(this);
				
				ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
				controller.waitForAccept(serverSocketChannel,port,backlog,timeout);
			} catch (IOException e) {
				logger.error("failt to waitForAccept.",e);
				context.finish();
				return;
			}
		}
		this.poolQueue=(String)param.get("pool");
		this.readQueue=(String)param.get("read");
		this.writeQueue=(String)param.get("write");
		controller.start();
	}

	/* (非 Javadoc)
	 * @see naru.quelet.Quelet#term()
	 */
	public void term() {
		if( controller==null ){
			return;
		}
		controller.stop();
		controller=null;
		context=null;
	}

	public void accepted(SocketChannel channel) {
		ASyncIOIf ioif=(ASyncIOIf)context.deque(poolQueue);
		ioif.setChannel(channel);
		channelsMap.put(channel, ioif);
	}
	
	private ASyncIOIf popChannel(SocketChannel channel){
		ASyncIOIf ioif=(ASyncIOIf)channelsMap.remove(channel);
		return ioif;
	}

	public void connectTimeout(SocketChannel channel) {
		ASyncIOIf ioif=popChannel(channel);
		ioif.timeout();
	}
	
	public void connectFailier(SocketChannel channel, Throwable t) {
		ASyncIOIf ioif=popChannel(channel);
		ioif.failer(t);
	}

//	public void connected(SocketChannel channel) {
//		ASyncIOIf ioif=popChannel(channel);
//		ioif.connected();
//	}

	public void readTimeout(SocketChannel channel) {
		ASyncIOIf ioif=popChannel(channel);
		ioif.timeout();
	}

	public void readable(SocketChannel channel) {
		ASyncIOIf ioif=popChannel(channel);
		context.enque(ioif,readQueue);
	}
	
	public void writeTimeout(SocketChannel channel) {
		ASyncIOIf ioif=popChannel(channel);
		ioif.timeout();
	}

	public void writable(SocketChannel channel) {
		ASyncIOIf ioif=popChannel(channel);
		context.enque(ioif,writeQueue);
	}

}
