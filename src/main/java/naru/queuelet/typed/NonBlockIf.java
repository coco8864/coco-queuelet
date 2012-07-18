package naru.queuelet.typed;

import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface NonBlockIf {
	//SelectionKey.OP_CONNECT ,OP_READ ,OP_WRITE ��ԋp
	public int getOperation();
	
	//SelectionKey.OP_CONNECT�̏ꍇ
	public SocketAddress getSocketAddress();
	
	//nonBlock�ΏۂƂȂ�Socket��ԋp
	public SocketChannel getSocketChannel();
	
	//nonBlock�ΏۂƂȂ�Socket��ݒ�
	public void setSocketChannel(Socket SocketChannel);
	
	//�o�b�t�@�̎��o��
	public ByteBuffer getByteBuffer();
}
