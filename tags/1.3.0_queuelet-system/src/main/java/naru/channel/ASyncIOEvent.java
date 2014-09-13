package naru.channel;

import java.nio.channels.SocketChannel;

public interface ASyncIOEvent {
	//accept��������������J�݂��ꂽ����ʒm�A������read��������write��҂�
	//����Read��҂�(SelectionKey.OP_READ)�AWrite��҂�(SelectionKey.OP_WRITE)��ԋp
	public void accepted(SocketChannel channel);
	
	//connect������������J�݂��ꂽ����ʒm
	//����Read��҂�(SelectionKey.OP_READ)�AWrite��҂�(SelectionKey.OP_WRITE)��ԋp
//	public void connected(SocketChannel channel);
	
	//read�\�ɂȂ�������ʒm
	public void readable(SocketChannel channel);
	
	//write�\�ɂȂ�������ʒm
	public void writable(SocketChannel channel);
	
	//readTimeout��������ʒm
	public void readTimeout(SocketChannel channel);
	
	//writeTimeout��������ʒm
	public void writeTimeout(SocketChannel channel);
	
	//connectTimeout��������ʒm
	public void connectTimeout(SocketChannel channel);

	//connect�Ɏ��s��������ʒm
	public void connectFailier(SocketChannel channel,Throwable t);

	/*
	//read�Ɏ��s��������ʒm
	public void readFailier(SocketChannel channel,Throwable t);
	
	//write�Ɏ��s��������ʒm
	public void writeFailier(SocketChannel channel,Throwable t);
	
	//connect�Ɏ��s��������ʒm
	public void connectFailier(InetAddress address, int port,Throwable t);
	
	//accept�Ɏ��s��������ʒm
	public void acceptFailier(int port,Throwable t);
	*/
}
