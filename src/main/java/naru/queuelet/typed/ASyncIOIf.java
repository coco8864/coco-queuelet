package naru.queuelet.typed;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

public interface ASyncIOIf {
	public int getOperation();
	public SelectableChannel getChannel();
	public void setChannel(SelectableChannel channel);
	public InetSocketAddress getSocketAdress();
	public int getTimeout();
	
	public ByteBuffer[] getWriteBuffers();
	public void read(ByteBuffer[] buffers);
	public void written();
	
	public void timeout();
	public void failer(Throwable t);
}
