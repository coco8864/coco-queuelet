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
	
	/* accept�㎟�̏�Ԃ�҂��ۂ� */
	private int acceptMode;
	public static int MODE_OPEN=1;/* accept�̂� */
	public static int MODE_READ=2;/* read�\�ɂȂ�܂ő҂� */

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
		selector = Selector.open(); // �ǂ���ł�OK
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

		// Non-Blocking ���[�h�ɂ���
		serverSocketChannel.configureBlocking(false);

		InetAddress inetAdder=null;/* localhost �ł� ip�w��ł�OK */
		InetSocketAddress address =new InetSocketAddress(inetAdder, port);
		serverSocketChannel.socket().bind(address,256);
		if( false ){
			serverSocketChannel.configureBlocking(true);
			return;
		}

		// Selector �ւ̓o�^
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
			key.cancel(); //�o�^����
			SocketChannel socketChannel = (SocketChannel) key.channel();
			socketChannel.configureBlocking(true);
			doTimeout(socketChannel); /* �C�x���g�ʒm */
//			System.out.println("socketChannel.isRegistered():" + socketChannel.isRegistered());
		}
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
					doLynnLynn(socketChannel); /* �C�x���g�ʒm */
				}else if(acceptMode==MODE_READ){
					/* ���[�h�\�ɂȂ���Socket��ԋp����ꍇ */
					socketChannel.configureBlocking(false);
					socketChannel.register(selector, SelectionKey.OP_READ);
				}else{
					throw new IllegalArgumentException("acceptMode:"+ acceptMode);
				}
			} else if (key.isReadable()) {
//				System.out.println("Readable");
				// �f�[�^�������Ă����ꍇ
				SocketChannel socketChannel = (SocketChannel) key.channel();
				key.cancel(); //�o�^����
				socketChannel.configureBlocking(true);
				doLynnLynn(socketChannel); /* �C�x���g�ʒm */
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
				/* ��~�v������Ă���ꍇ�́A��~ */
				if( run==false ){
					selector.close();
					lock.notifyAll();
					break;
				}
			}
			SocketChannel socketChannel = serverSocketChannel.accept();
			event.lynnLynn(socketChannel); /* �C�x���g�ʒm */
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
			System.out.println("���X�i�ُ�I��");
			e.printStackTrace();
		}
	}
}
