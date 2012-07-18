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
public class ChannelListener implements Runnable {
	static private Logger logger=logger=Logger.getLogger(ChannelListener.class);
	private Selector selector;
	private ServerSocketChannel serverSocketChannel=null;
	private ChannelEvent event;
	private int port=0;
	private long timeout=0;
	private long selectInterval=1000;
	
	/* accept後次の状態を待つか否か */
	private int acceptMode;
	public static int MODE_OPEN=1;/* acceptのみ */
	public static int MODE_READ=2;/* read可能になるまで待つ */

	private Object lock=new Object();
	private boolean run=true;

	public ChannelListener(ChannelEvent event) throws IOException {
		this(event,0);
	}
	
	public ChannelListener(ChannelEvent event,long timeout) throws IOException {
		this.event = event;
		this.timeout=timeout;
		if( timeout==0){
			selectInterval=0;
		}
		//			selector = SelectorProvider.provider().openSelector();
		selector = Selector.open(); // どちらでもOK
	}

	
	
	public ChannelListener(int port, ChannelEvent event) throws IOException {
		this(port,event,MODE_OPEN);
	}
	
	public ChannelListener(int port, ChannelEvent event,int acceptMode) throws IOException {
		this(event);
		
		this.port = port;
		this.acceptMode=acceptMode;

		//		ServerSocketChannel serverSocketChannel =
		//						SelectorProvider.provider().openServerSocketChannel();
		serverSocketChannel = ServerSocketChannel.open();

		// Non-Blocking モードにする
		serverSocketChannel.configureBlocking(false);

		InetAddress inetAdder=null;/* localhost でも ip指定でもOK */
		InetSocketAddress address =new InetSocketAddress(inetAdder, port);
		serverSocketChannel.socket().bind(address,256);
		if( false ){
			serverSocketChannel.configureBlocking(true);
			return;
		}

		// Selector への登録
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	void doTimeout(SocketChannel socketChannel){
		try {
			event.timeout(socketChannel);
		} catch (Throwable e) {
			logger.warn("timeout evnet throw exception.",e);
		}
	}
	
	void doLynnLynn(SocketChannel socketChannel){
		try {
			event.lynnLynn(socketChannel);
		} catch (Throwable e) {
			logger.warn("lynnLynn evnet throw exception.",e);
		}
	}
	
	private void checkTimeout()throws IOException {
		// 全SelectionKey オブジェクトを取得する
		synchronized(lock){
		Iterator keyIterator = selector.keys().iterator();
		long now=System.currentTimeMillis();
		while (keyIterator.hasNext()) {
			SelectionKey key = (SelectionKey) keyIterator.next();
			Long startTime=(Long)key.attachment();
			if( startTime==null){
				continue;
			}
			if( (now-startTime.longValue())<timeout ){
				continue;
			}
			key.cancel(); //登録解除
			SocketChannel socketChannel = (SocketChannel) key.channel();
			socketChannel.configureBlocking(true);
			doTimeout(socketChannel); /* イベント通知 */
//			System.out.println("socketChannel.isRegistered():" + socketChannel.isRegistered());
		}
		}
	}
	
	
	private void dispatch() throws IOException {
		// セレクトされた SelectionKey オブジェクトをまとめて取得する
		Iterator keyIterator = selector.selectedKeys().iterator();
		while (keyIterator.hasNext()) {
			SelectionKey key = (SelectionKey) keyIterator.next();
			keyIterator.remove();

			// セレクトされた SelectionKey の状態に応じて処理を決める
			if (key.isAcceptable()) {
//				System.out.println("Acceptable");
				// accept の場合
				ServerSocketChannel serverSocketChannel =
					(ServerSocketChannel) key.channel();
				SocketChannel socketChannel = serverSocketChannel.accept();

				if(acceptMode==MODE_OPEN){
					/* アクセプト済みを返却する　*/
					doLynnLynn(socketChannel); /* イベント通知 */
				}else if(acceptMode==MODE_READ){
					/* リード可能になったSocketを返却する場合 */
					socketChannel.configureBlocking(false);
					socketChannel.register(selector, SelectionKey.OP_READ);
				}else{
					throw new IllegalArgumentException("acceptMode:"+ acceptMode);
				}
			} else if (key.isReadable()) {
//				System.out.println("Readable");
				// データが送られてきた場合
				SocketChannel socketChannel = (SocketChannel) key.channel();
				key.cancel(); //登録解除
				socketChannel.configureBlocking(true);
				doLynnLynn(socketChannel); /* イベント通知 */
			}
		}
	}
	
	/**
	 * リクエスト待ち、このメソッドはブロックします。
	 * リクエスト到着は、ChannelEventに通知します。
	 * 
	 * @throws IOException
	 */
	public void waitForRequest() throws IOException {
		while (selector.select(selectInterval) >= 0) {
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
			if( timeout>0){
				checkTimeout();
			}
		}
	}

	/**
	 * レスポンス待ち、このメソッドはブロックしません。
	 * レスポンス到着は、ChannelEventに通知します。
	 * 
	 * @throws IOException
	 */
	public void waitForResponse(SocketChannel socketChannel) throws IOException {
		socketChannel.configureBlocking(false);
		synchronized(lock){
			if( run==false ){
				throw new IOException("ChannelLissener stopped");
			}
			while( socketChannel.isRegistered()){
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
//				System.out.println(socketChannel.toString() + " isRegistered=" + socketChannel.isRegistered());
//				socketChannel.close();
//				return;
			}
			selector.wakeup();
			socketChannel.register(selector, SelectionKey.OP_READ);
			SelectionKey key=socketChannel.keyFor(selector);
			key.attach(new Long(System.currentTimeMillis()));
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
		synchronized(lock){
			this.run=false;
			selector.wakeup();
			try {
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if( serverSocketChannel!=null){
				try {
					serverSocketChannel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	private void waitForAccept() throws IOException{
		while(true){
			synchronized(lock){
				/* 停止要求されている場合は、停止 */
				if( run==false ){
					selector.close();
					lock.notifyAll();
					break;
				}
			}
			SocketChannel socketChannel = serverSocketChannel.accept();
			event.lynnLynn(socketChannel); /* イベント通知 */
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
//			if( port==0){
				waitForRequest();
//			}else{
//				waitForAccept();
//			}
		} catch (IOException e) {
			System.out.println("リスナ異常終了");
			e.printStackTrace();
		}
	}
}
