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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AcceptChannel implements Runnable {
	static private Logger logger=Logger.getLogger(AcceptChannel.class);
	private Selector selector;

	private Set serverSocketChannels=new HashSet();
	private long timeout=0;
	private long selectInterval=1000;
	
	/* accept�㎟�̏�Ԃ�҂��ۂ� */
	private int acceptMode;
	public static int MODE_OPEN=1;/* accept�̂� */
	public static int MODE_READ=2;/* read�\�ɂȂ�܂ő҂� */

	private Object lock=new Object();
	private boolean run=true;
	
	public AcceptChannel(int acceptMode,long timeout) throws IOException {
		this.acceptMode=acceptMode;
		//selector = SelectorProvider.provider().openSelector();
		this.selector = Selector.open(); // �ǂ���ł�OK
	}
	
	public void entry(int port,ChannelEvent event) throws IOException{
		//		ServerSocketChannel serverSocketChannel =
		//						SelectorProvider.provider().openServerSocketChannel();
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

		// Non-Blocking ���[�h�ɂ���
		serverSocketChannel.configureBlocking(false);
		InetAddress inetAdder=null;/* localhost �ł� ip�w��ł�OK */
		InetSocketAddress address =new InetSocketAddress(inetAdder, port);
		serverSocketChannel.socket().bind(address,256);

//		SelectionKey newKey=serverSocketChannel.keyFor(selector);
//		newKey.attach(event);
		serverSocketChannels.add(serverSocketChannel);
		waitForAccept(serverSocketChannel,event);
	}
	

	private void checkTimeout()throws IOException {
		// �SSelectionKey �I�u�W�F�N�g���擾����
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
			Object serverSocketChannel=key.attachment();
			key.cancel(); //�o�^����
			SocketChannel socketChannel = (SocketChannel) key.channel();
			socketChannel.configureBlocking(true);
			doTimeout(serverSocketChannel,socketChannel);
//			System.out.println("socketChannel.isRegistered():" + socketChannel.isRegistered());
		}
		}
	}

	void doTimeout(Object attachment,SocketChannel socketChannel){
		ChannelEvent event=(ChannelEvent)attachment;
		try {
			event.timeout(socketChannel);
		} catch (Throwable e) {
			logger.warn("timeout evnet throw exception.",e);
		}
	}
	
	void doLynnLynn(Object attachment,SocketChannel socketChannel){
		ChannelEvent event=(ChannelEvent)attachment;
		try {
			event.lynnLynn(socketChannel);
		} catch (Throwable e) {
			logger.warn("lynnLynn evnet throw exception.",e);
		}
	}
	
	private void dispatch() throws IOException {
		// �Z���N�g���ꂽ SelectionKey �I�u�W�F�N�g���܂Ƃ߂Ď擾����
		Iterator keyIterator = selector.selectedKeys().iterator();
		while (keyIterator.hasNext()) {
			SelectionKey key = (SelectionKey) keyIterator.next();
			keyIterator.remove();

			// �Z���N�g���ꂽ SelectionKey �̏�Ԃɉ����ď��������߂�
			if (key.isAcceptable()) {
//				System.out.println("Acceptable");
				// accept �̏ꍇ
				ServerSocketChannel serverSocketChannel =
					(ServerSocketChannel) key.channel();
				SocketChannel socketChannel = serverSocketChannel.accept();

				if(acceptMode==MODE_OPEN){
					/* �A�N�Z�v�g�ς݂�ԋp����@*/
					doLynnLynn(key.attachment(),socketChannel);
				}else if(acceptMode==MODE_READ){
					/* ���[�h�\�ɂȂ���Socket��ԋp����ꍇ */
					socketChannel.configureBlocking(false);
					socketChannel.register(selector, SelectionKey.OP_READ);
					
					/* serverSocket�Ɗ֘A�Â��āA�C�x���g�ʒm��ɗ��p */
					SelectionKey newKey=socketChannel.keyFor(selector);
					newKey.attach(key.attachment());
				}else{
					throw new IllegalArgumentException("acceptMode:"+ acceptMode);
				}
			} else if (key.isReadable()) {
				// �f�[�^�������Ă����ꍇ
				SocketChannel socketChannel = (SocketChannel) key.channel();
				key.cancel(); //�o�^����
				socketChannel.configureBlocking(true);
				doLynnLynn(key.attachment(),socketChannel);
			}
		}
	}
	
	/**
	 * ���N�G�X�g�҂��A���̃��\�b�h�̓u���b�N���܂��B
	 * ���N�G�X�g�����́AChannelEvent�ɒʒm���܂��B
	 * 
	 * @throws IOException
	 */
	public void waitForRequest() throws IOException {
		while (selector.select(selectInterval) >= 0) {
			/* register����������܂ł����ő҂� */
			synchronized(lock){
				/* ��~�v������Ă���ꍇ�́A��~ */
				if( run==false ){
					selector.close();
					lock.notifyAll();
					break;
				}
			}
			/* �I�����ꂽ�`���l������������ */
			dispatch(); 
			
			/* �^�C���A�E�g���������� */
			if( timeout>0){
				checkTimeout();
			}
		}
	}

	/**
	 * ���X�|���X�҂��A���̃��\�b�h�̓u���b�N���܂���B
	 * ���X�|���X�����́AChannelEvent�ɒʒm���܂��B
	 * 
	 * @throws IOException
	 */
	private void waitForAccept(SelectableChannel socketChannel,Object attachment) throws IOException {
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
			}
			selector.wakeup();
			SelectionKey key=socketChannel.register(selector,SelectionKey.OP_ACCEPT);
			key.attach(attachment);
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
			Iterator itr=serverSocketChannels.iterator();
			while(itr.hasNext()){
				ServerSocketChannel serverSocketChannel=
					(ServerSocketChannel)itr.next();
				try {
					serverSocketChannel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
			try {
				selector.close();
			} catch (IOException e1) {
				e1.printStackTrace();
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
			logger.error("AcceptChannel listener end.",e);
		}
	}
}
