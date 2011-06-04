package naru.channel;

import java.nio.channels.SocketChannel;

public interface ASyncIOEvent {
	//acceptが完了し回線が開設された事を通知、続けてreadもしくはwriteを待つ
	//次にReadを待つか(SelectionKey.OP_READ)、Writeを待つか(SelectionKey.OP_WRITE)を返却
	public void accepted(SocketChannel channel);
	
	//connectが完了回線が開設された事を通知
	//次にReadを待つか(SelectionKey.OP_READ)、Writeを待つか(SelectionKey.OP_WRITE)を返却
//	public void connected(SocketChannel channel);
	
	//read可能になった事を通知
	public void readable(SocketChannel channel);
	
	//write可能になった事を通知
	public void writable(SocketChannel channel);
	
	//readTimeoutした事を通知
	public void readTimeout(SocketChannel channel);
	
	//writeTimeoutした事を通知
	public void writeTimeout(SocketChannel channel);
	
	//connectTimeoutした事を通知
	public void connectTimeout(SocketChannel channel);

	//connectに失敗した事を通知
	public void connectFailier(SocketChannel channel,Throwable t);

	/*
	//readに失敗した事を通知
	public void readFailier(SocketChannel channel,Throwable t);
	
	//writeに失敗した事を通知
	public void writeFailier(SocketChannel channel,Throwable t);
	
	//connectに失敗した事を通知
	public void connectFailier(InetAddress address, int port,Throwable t);
	
	//acceptに失敗した事を通知
	public void acceptFailier(int port,Throwable t);
	*/
}
