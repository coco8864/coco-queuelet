/*
 * Created on 2004/11/19
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import naru.channel.AcceptChannel;
import naru.channel.ChannelEvent;

/**
 * @author naru
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
class QueueletAcceptChannel {
	/* ソケット受信端点 */
	private AcceptChannel acceptChannel=null;
	
	private class TerminalChannelEvent implements ChannelEvent{
		private Terminal terminal;
		public TerminalChannelEvent(Terminal terminal){
			this.terminal=terminal;
		}
		/* (non-Javadoc)
		 * @see naru.channel.ChannelEvent#lynnLynn(java.nio.channels.SocketChannel)
		 */
		public void lynnLynn(SocketChannel socketChannel) {
			terminal.enque(socketChannel.socket());
			
		}
		/* (non-Javadoc)
		 * @see naru.channel.ChannelEvent#timeout(java.nio.channels.SocketChannel)
		 */
		public void timeout(SocketChannel req) {
			
			return;
//			throw new IllegalStateException("AcceptQueuelet timeout!:" + req.toString());
		}
	}
	
	public void register(int port,Terminal terminal) throws IOException{
		if(acceptChannel==null){
			acceptChannel=new AcceptChannel(AcceptChannel.MODE_READ,0);
			acceptChannel.start();
		}
		acceptChannel.entry(port,new TerminalChannelEvent(terminal));
	}
	
	public void end(){
		if( acceptChannel!=null ){
			acceptChannel.stop();
		}
	}
	
}
