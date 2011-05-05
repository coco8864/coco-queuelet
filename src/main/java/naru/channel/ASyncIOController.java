/*
 * Created on 2004/06/12
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package naru.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ASyncIOController implements Runnable {
	static private Logger logger=Logger.getLogger(ASyncIOController.class);
	private Selector selector;

	private long selectInterval=10000;

	private Object lock=new Object();
	private boolean run=true;
	private boolean wakeuped=true;
	private ASyncIOEvent event;

	public ASyncIOController(ASyncIOEvent event) throws IOException {
		this.event=event;
		this.selector = Selector.open(); //
	}
	
	//TODO プールした方がよい
	static class ASyncAttachment{
		public ASyncAttachment(long timeout){
			this(timeout,0);
		}
		public ASyncAttachment(long timeout,int operation){
			this(timeout,operation,System.currentTimeMillis()+timeout);
		}
		public ASyncAttachment(long timeout,int operation,long timeoutDate){
			this.timeout=timeout;
			this.operation=operation;
			this.timeoutDate=timeoutDate;
		}
		long timeout;
		int operation;
		long timeoutDate;
	}
	
	private void waitFor(SelectableChannel channel,int op,ASyncAttachment attachment) throws IOException{
		channel.configureBlocking(false);
		synchronized(lock){
			if(wakeuped==false){
				logger.debug("wakeup before");
				selector.wakeup();
				logger.debug("wakeup after");
			}
			SelectionKey key=channel.register(selector, op,attachment);
		}
	}
	
	public void waitForRead(SelectableChannel channel,long timeout) throws IOException{
		waitFor(channel,SelectionKey.OP_READ,new ASyncAttachment(timeout));
	}
	
	public void waitForWrite(SelectableChannel channel,long timeout) throws IOException{
		waitFor(channel,SelectionKey.OP_WRITE,new ASyncAttachment(timeout));
	}
	
	//timeout:accept後のタイムアウト値を指定
	/**
	 * @param port リクエストを待ち受けるport番号を指定
	 * @param backlog acceptキューのバックログ長を指定
	 * @param timeout accept後のタイムアウト値を指定
	 */
	public void waitForAccept(ServerSocketChannel serverSocketChannel,int port,int backlog,long timeout) throws IOException{
//		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		InetAddress inetAdder=null;/* localhost でも ip指定でもOK */
		InetSocketAddress address =new InetSocketAddress(inetAdder, port);
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.socket().bind(address,backlog);
		waitForAccept(serverSocketChannel,timeout,true);
	}
	
	public void waitForAccept(ServerSocketChannel serverSocketChannel,long timeout) throws IOException{
		waitForAccept(serverSocketChannel,timeout,true);
	}
	
	/**
	 * @param timeout accept後のタイムアウト値を指定
	 * @param operation accept後に待ち受けるオペレーションを指定
	 */
	public void waitForAccept(ServerSocketChannel serverSocketChannel,long timeout,boolean isWaitForRead) throws IOException{
		// Non-Blocking モードにする
		serverSocketChannel.configureBlocking(false);
		while( serverSocketChannel.isRegistered()){
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		int operation;
		if(isWaitForRead){
			operation=SelectionKey.OP_READ;
		}else{
			operation=SelectionKey.OP_WRITE;
		}
		waitFor(serverSocketChannel,SelectionKey.OP_ACCEPT,new ASyncAttachment(timeout,operation,-1));
	}
	
	public void waitForConnect(SocketChannel channel,SocketAddress remote,long timeout) throws IOException{
		waitForConnect(channel,remote,timeout,false);
	}
	
	/**
	 * @param timeout connect処理にかかるタイムアウト値を指定
	 * @param operation connect後に待ち受けるオペレーションを指定
	 */
	public void waitForConnect(SocketChannel channel,SocketAddress remote,long timeout,boolean isWaitForRead) throws IOException{
		int operation;
		if(isWaitForRead){
			operation=SelectionKey.OP_READ;
		}else{
			operation=SelectionKey.OP_WRITE;
		}
		channel.configureBlocking(false);
		if( channel.connect(remote) ){
			waitFor(channel,operation,new ASyncAttachment(timeout,operation));
			return;
		}
		waitFor(channel,SelectionKey.OP_CONNECT/*|operation*/,new ASyncAttachment(timeout,operation));
	}

	/**
	 * 目覚めるたびにすべてのエントリーを舐めるのはよくない
	 * 改善の余地あり
	 * @throws IOException
	 */
	private void checkTimeout()throws IOException {
		// 全SelectionKey オブジェクトを取得する
		long now=System.currentTimeMillis();
		Iterator keyIterator = selector.keys().iterator();
		while (keyIterator.hasNext()) {
			SelectionKey key = (SelectionKey) keyIterator.next();
			if( key.isValid()==false ){
				continue;
			}
			ASyncAttachment atachment=(ASyncAttachment)key.attachment();
			if(atachment.timeoutDate<0){
				continue;
			}
			if( atachment.timeoutDate>now ){
				continue;
			}
			Object o=key.channel();
			if(!(o instanceof SocketChannel)){
				continue;
			}
			int operation=key.interestOps();
			key.cancel(); //登録解除
			
			SocketChannel socketChannel = (SocketChannel)o; 
			socketChannel.configureBlocking(true);
			switch(operation){
			case SelectionKey.OP_CONNECT:
				event.connectTimeout(socketChannel);
				break;
			case SelectionKey.OP_READ:
				event.readTimeout(socketChannel);
				break;
			case SelectionKey.OP_WRITE:
				event.writeTimeout(socketChannel);
				break;
			default:
				throw new IllegalArgumentException("key.interestOps():"+ key.interestOps());
			}
		}
	}
	
	private void dispatch() throws IOException {
		// セレクトされた SelectionKey オブジェクトをまとめて取得する
		Iterator keyIterator = selector.selectedKeys().iterator();
		while (keyIterator.hasNext()) {
			SelectionKey key = (SelectionKey) keyIterator.next();
			keyIterator.remove();//ずっと選択されたままになる事を防ぐ

			// セレクトされた SelectionKey の状態に応じて処理を決める
			if (key.isAcceptable()) {
				System.out.println("isAcceptable");
				ASyncAttachment atachment=(ASyncAttachment)key.attachment();
				ServerSocketChannel serverSocketChannel =
					(ServerSocketChannel) key.channel();
				SocketChannel socketChannel = serverSocketChannel.accept();
				event.accepted(socketChannel);
				System.out.println("isAcceptable socketChannel:"+socketChannel);
				switch(atachment.operation){
				case SelectionKey.OP_READ:
					waitForRead(socketChannel,atachment.timeout);
					break;
				case SelectionKey.OP_WRITE:
					waitForWrite(socketChannel,atachment.timeout);
					break;
				default:
					throw new RuntimeException("atachment.operation error."+atachment.operation);
				}
			} else if (key.isReadable()) {
				// 受信可能になった場合
				SocketChannel socketChannel = (SocketChannel) key.channel();
				key.cancel(); //登録解除
				socketChannel.configureBlocking(true);
				event.readable(socketChannel);
			}else if(key.isWritable()){
				// 送信可能になった場合
				SocketChannel socketChannel = (SocketChannel) key.channel();
				key.cancel(); //登録解除
				socketChannel.configureBlocking(true);
				event.writable(socketChannel);
			}else if(key.isConnectable()){
				// 通信可能になった場合
//				key.cancel(); //登録解除
				SocketChannel socketChannel = (SocketChannel) key.channel();
				try {
					socketChannel.finishConnect();
				} catch (IOException e) {
					event.connectFailier(socketChannel,e);
					continue;
				}
				ASyncAttachment atachment=(ASyncAttachment)key.attachment();
				key.interestOps(atachment.operation);
			}
		}
	}
	
	/**
	 * リクエスト待ち、このメソッドはブロックします。
	 * リクエスト到着は、ChannelEventに通知します。
	 * 
	 * @throws IOException
	 */
	private void waitForRequest() throws IOException {
		wakeuped=false;//若干余分にwakeupが呼び出されるのは許容する
		while (selector.select(selectInterval) >= 0) {
			System.out.println("select out");
			wakeuped=true;//若干余分にwakeupが呼び出されるのは許容する
			/* registerが完了するまでここで待つ */
			synchronized(lock){
				/* 停止要求されている場合は、停止 */
				if( run==false ){
					selector.close();
					lock.notifyAll();
					break;
				}
			}
			/* 選択されたチャネルを処理する */
			dispatch(); 
			
			/* タイムアウトを処理する */
			checkTimeout();
			wakeuped=false;//若干余分にwakeupが呼び出されるのは許容する
			System.out.println("select in");
		}
	}

	public synchronized void start() {
		synchronized(lock){
			this.run=true;
			Thread t = new Thread(this);
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
		}
	}

	public synchronized void stop() {
		if( selector==null){
			return;
		}
		synchronized(lock){
			this.run=false;
			selector.wakeup();
			try {
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Iterator keyIterator=selector.keys().iterator();
			while(keyIterator.hasNext()){
				SelectionKey key = (SelectionKey) keyIterator.next();
				if( !key.isValid() ){
					continue;
				}
				SelectableChannel channel=key.channel();
				if( !channel.isOpen() ){
					continue;
				}
				try {
					channel.close();
				} catch (IOException e) {
					logger.warn("stop fail to channel close.",e);
				}
			}
			try {
				selector.close();
			} catch (IOException e) {
				logger.warn("stop fail to selector close.",e);
			}
			selector=null;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			waitForRequest();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("AcceptChannel listener end.",e);
		}
	}
}
