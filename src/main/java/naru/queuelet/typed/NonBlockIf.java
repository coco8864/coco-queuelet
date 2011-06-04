package naru.queuelet.typed;

import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface NonBlockIf {
	//SelectionKey.OP_CONNECT ,OP_READ ,OP_WRITE を返却
	public int getOperation();
	
	//SelectionKey.OP_CONNECTの場合
	public SocketAddress getSocketAddress();
	
	//nonBlock対象となるSocketを返却
	public SocketChannel getSocketChannel();
	
	//nonBlock対象となるSocketを設定
	public void setSocketChannel(Socket SocketChannel);
	
	//バッファの取り出し
	public ByteBuffer getByteBuffer();
}
