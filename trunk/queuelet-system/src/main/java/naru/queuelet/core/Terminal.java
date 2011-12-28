/*
 * �쐬��: 2004/07/21
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.core;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import naru.queuelet.store.QueueletStore;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class Terminal {
	static private Logger logger=Logger.getLogger(Terminal.class);

	private Container container;
	private String name;
	private int threadCount;
	private int priority=Thread.NORM_PRIORITY;//thread�̗D��x
	private int maxQueueLength=-1;
	private boolean enqueBlock=true;
	private String port=null;
	private QueueletStore store=null;

//	private StoreTerminal storeTerminal=null;
	private int threadId=0;
	public synchronized int getThreadId(){
		threadId++;
		return threadId;
	}
	
	private List queuelets=new ArrayList();
	private int finishQuletCount=0;
	public synchronized void finishQulet(){
		finishQuletCount++;
		if( /*startup*/ false && finishQuletCount==queuelets.size() ){
			stop(false);//�S�X���b�h�̏I����҂����킹�Ȃ�
		}
	}

	private List queue = Collections.synchronizedList(new LinkedList());
	private Set threads = Collections.synchronizedSet(new HashSet());
	private int waitThreadCount;
	private int totalInCount=0;
	private int totalOutCount=0;
	private long maxDelay=0;
	
	private int queueEntryPoolMax;
	private List queueEntryPool=new LinkedList();
	private static class QueueEntry implements Serializable{
		private static final long serialVersionUID = 1L;
		public Object entry;
		public long enqueueTime;
	}

	//�I����worker�X���b�h�I�����Ď�����Ԋu
	private static final int STOP_WAIT_TIMEOUT = 1000;
	//�����I���܂ł̊Ď���
	private static final int STOP_FORCE_LOOP_COUNT=10;
	//queue��ꂪ���������ۂ́A��queue�Ď��Ԋu
	private static final int MAX_LENGTH_WAIT_TIMEOUT = 100;

	private static final int STATUS_INIT = 1;
	private static final int STATUS_START = 2;
	private static final int STATUS_STOPPING = 3;
	private static final int STATUS_MUST_STOP = 4;
	private static final int STATUS_STOP = 5;

	private int status=STATUS_INIT; /* this��synchronized�Ŏ�� */

	//	private Object decrementThreadLock=que;//new Object();
	private int decrementThreadCount = 0;

	/* �ȉ��Q�̃��\�b�h�́A�����ĕK�v�Ȃ����A�X���b�h�����I�����ɂ͕K�v */
	void registerServiceThread(ServiceThread st) {
		threads.add(st);
	}

	void deregisterServiceThread(ServiceThread st) {
		threads.remove(st);
		if( getThreadsCount()==0 ){
//			term();//�������̃X���b�h���S�����Ȃ��Ȃ��Ă���term�𗬂�?
			status = STATUS_STOP;
		}
	}

	/**
	 * ���N�G�X�g�擾�i�����j���\�b�h
	 * ���̃��\�b�h��null��Ԃ��΁A�����X���b�h�͏I������B
	 * �����X���b�h�����R���g���[���i����������j����ꍇ�͂���𗘗p
	 * @return
	 */
	Object deque() {
		while (true) {
			synchronized (this) {
				if (status == STATUS_MUST_STOP) {
					break;
				}
				if (status == STATUS_STOPPING && getQueLength()==0) {
					break;
				}
			}
			QueueEntry queueEntry=null;
			synchronized (queue) {
				/* �����X���b�h������������ */
				if (decrementThreadCount > 0) {
					decrementThreadCount--;
					if (getQueLength() > 0) {
						queue.notify();
					}
					return null;
				}
				
				int queLength=getQueLength();
				if (queLength > 0) {
					queueEntry=(QueueEntry)queue.remove(0);
					long time=System.currentTimeMillis()-queueEntry.enqueueTime;
					if(maxDelay<time){
						maxDelay=time;
					}
					return queueEntry.entry;
				}
				try {
					waitThreadCount++;
					queue.wait(STOP_WAIT_TIMEOUT); /* �I���R�}���h�̃��X�|���X�ɉe�� */
					waitThreadCount--;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			if(queueEntry!=null){
				synchronized(queueEntryPool){
					if(queueEntryPool.size()<queueEntryPoolMax){
						queueEntryPool.add(queueEntry);
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return Returns the waitThreadCount.
	 */
	public int getWaitThreadCount() {
		return waitThreadCount;
	}
	
	/* ���݂�que�� */
	public int getQueLength() {
		return queue.size();
	}

	/* ���݂̃T�[�r�X�X���b�h�� */
	public int getThreadsCount() {
		return threads.size();
	}

	public void addTotalOutCount(){
		totalOutCount++;
	}
	
	public int getTotalInCount(){
		return totalInCount;
	}

	public int getTotalOutCount(){
		return totalOutCount;
	}
	
	//�ő�queue�ؗ�����
	public long getMaxDelay(){
		return maxDelay;
	}

	/** 
	 *  ���N�G�X�g��t 
	 */
	public void enque(Object entry) {
		synchronized (this) {
			if (status != STATUS_INIT&&status != STATUS_START) {
				IllegalStateException e=new IllegalStateException("Terminal:"+getName() +" status error.status:" + status);
				logger.warn("fail to enque",e );
				throw e;
			}
		}
		//QueueEntry�̃v�[�����O
		QueueEntry queueEntry=null;
		synchronized(queueEntryPool){
			if(queueEntryPool.size()>0){
				queueEntry=(QueueEntry)queueEntryPool.remove(0);
			}
		}
		if(queueEntry==null){
			queueEntry=new QueueEntry();
		}
		queueEntry.entry=entry;
		queueEntry.enqueueTime=System.currentTimeMillis();
		
		if( store!=null ){
/*
 // TODO delete 
   			ClassLoader cl=container.getLoader("main");
			if( cl!=null ){
				Thread.currentThread().setContextClassLoader(cl);
			}
*/
			/* �̂т�����Queue�́ADB�Ɋi�[����ݒ� */
			store.enqueStore(queueEntry);
		}else{
			/* ������Queue��L�΂����HQueue���̂т������ꍇ�́A�G���[�ɂ���ݒ� */
			enqueMem(queueEntry);
		}
	}
	
	/** 
	 *   
	 */
	public synchronized void enqueMem(Object queEntry) {
		if(queEntry==null){
			throw new IllegalArgumentException("Terminal enque error.queElement is null");
		}
		
		while (true) {
			/*
			if (status != STATUS_START && threadCount!=0 ) {
				throw new IllegalStateException("fail to enque:status=" + status);
			}
			*/
			synchronized (queue) {
				if (maxQueueLength<0 || getQueLength() < maxQueueLength ) {
					totalInCount++;
					queue.add(queEntry);
					queue.notify();
					return;
				} else if (enqueBlock==false) {
					//Que�������Ȃ肷�����A�G���[�Ƃ���
					throw new IllegalStateException(
						"Que length too long:maxQueLength="	+ maxQueueLength
							+ ",curQueLength=" + getQueLength());
				}
			}
			logger.debug("Que length too long:maxQueLength="	+ maxQueueLength
							+ ",curQueLength=" + getQueLength());
			/* Que�����L�т������ꍇ�̑҂����� */
			try {
				wait(MAX_LENGTH_WAIT_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public Container getContainer(){
		return container;
	}
	
	/**
	 * @throws IOException
	 * @throws InvocationTargetException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * 
	 */
	public void init(Container container) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException {
		if (status != STATUS_INIT&&status != STATUS_STOP) {
			throw new IllegalStateException("fail to init Terminal:"+getName() +" status error.status:" + status);
		}
		status = STATUS_INIT;
		this.container=container;
		
		if( port!=null ){
			String  resolvePort=container.resolveProperty(port);
			container.registerTerminalPort(Integer.parseInt(resolvePort),this);
		}
		if( store!=null ){
			if( maxQueueLength==-1){
				logger.warn("store = true but maxQueueLength=-1.don't use store queue");
				store=null;
			}else{
				store.init(container.getQueueletProperties(),this);
			}
		}
		for(int i=0;i<queuelets.size();i++){
			QueueletWrapper qw=(QueueletWrapper)queuelets.get(i);
			qw.init(container);
		}
//		Iterator itr=queuelets.iterator();
//		while(itr.hasNext()){
//			QueueletWrapper qw=(QueueletWrapper)itr.next();
//			qw.init(container);
//		}
	}


	/** 
	 *  ���N�G�X�g��t�����J�n 
	 */
	public synchronized void start() {
		if (status != STATUS_INIT) {
			throw new IllegalStateException("fail to start:status=" + status);
		}
		status = STATUS_START;
		if( store!=null ){
			store.start();
		}
		for (int i = 0; i < threadCount; i++) {
			incrementServiceThread();
		}
	}

	/**
	 * �����X���b�h�𑝉�
	 */
	public synchronized void incrementServiceThread() {
		if (status != STATUS_START) {
			throw new IllegalStateException("fail to start:status=" + status);
		}
		ServiceThread t = new ServiceThread(this);
		t.start();
	}

	/**
	 * �����X���b�h������
	 */
	public synchronized void decrementServiceThread() {
		if (status != STATUS_START) {
			throw new IllegalStateException("fail to start:status=" + status);
		}
		synchronized (queue) { //�{�������J�E���^�p���b�N�͕ʂ�������������Ȃ�
			decrementThreadCount++;
		}
	}

	/**
	 * 
	 */
	public void term() {
		for(int i=queuelets.size()-1;i>=0;i--){
			QueueletWrapper qw=(QueueletWrapper)queuelets.get(i);
			qw.term();
		}
		/*
		Iterator itr=queuelets.iterator();
		while(itr.hasNext()){
			QueueletWrapper qw=(QueueletWrapper)itr.next();
			qw.term();
		}
		*/
		if( store!=null ){
			store.term();
			store=null;
		}
	}

	/** 
	 * ���N�G�X�g��t�����I��
	 */
	public synchronized void stop() {
		stop(true);
	}

	/** 
	 * ���N�G�X�g��t�����I��
	 */
	public synchronized void stop(boolean wait) {
		if (status != STATUS_START) {
			logger.error("fail to stop:status=" + status +":name:" +getName(),new Exception());
//			throw new IllegalStateException();
			return;
		}
		status = STATUS_STOPPING;
		if( wait==false){
			return;
		}
		
		/* �S�ẴX���b�h���I������̂�҂����킹�� */
		for(int i=0;i<STOP_FORCE_LOOP_COUNT;i++){
		//while (true) {
			if (getThreadsCount() == 0) {
				break;
			}
			logger.warn("stop terminal:"+getName()+":"+getThreadsCount());
			if(logger.isDebugEnabled()){
				Iterator itr=threads.iterator();
				while(itr.hasNext()){
					ServiceThread st=(ServiceThread)itr.next();
					st.dump();
				}
			}
			try {
				wait(STOP_WAIT_TIMEOUT);
				//Thread.sleep(STOP_WAIT_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(i==(STOP_FORCE_LOOP_COUNT-1)){
				logger.error("force stop terminal:"+getName()+":"+getThreadsCount());
				Iterator itr=threads.iterator();
				while(itr.hasNext()){
					ServiceThread st=(ServiceThread)itr.next();
					st.suspend();//���X���b�h��suspend���Ȃ�
					st.dump();
				}
				
			}
		}
		if( store!=null ){
			store.stop();
		}
	}
	
	public void addQuelet(QueueletWrapper queletWrapper){
		queuelets.add(queletWrapper);
	}

	/**
	 * @return
	 */
	public boolean isEnqueBlock() {
		return enqueBlock;
	}

	/**
	 * @return
	 */
	public int getMaxQueueLength() {
		return maxQueueLength;
	}

	/**
	 * @return
	 */
	public int getThreadCount() {
		return threadCount;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param b
	 */
	public void setEnqueBlock(boolean b) {
		enqueBlock = b;
	}

	/**
	 * @param i
	 */
	public void setMaxQueueLength(int i) {
		maxQueueLength = i;
	}

	/**
	 * @param i
	 */
	public void setThreadCount(int i) {
		threadCount = i;
		queueEntryPoolMax=threadCount*4;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

	/**
	 * @return Returns the store.
	 */
	public QueueletStore getStore() {
		return store;
	}
	
	/**
	 * @param store The store to set.
	 */
	public void setStore(QueueletStore store) {
		this.store = store;
	}

	/**
	 * @return Returns the port.
	 */
	public String getPort() {
		return port;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(String port) {
		this.port = port;
	}
	
	/**
	 * @param priority
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * @param string
	 */
	public int getPriority() {
		return priority;
	}
	
	
	/**
	 * @param req
	 */
	public void service(Object req) {
		Iterator itr=queuelets.iterator();
		while(itr.hasNext()){
			QueueletWrapper qw=(QueueletWrapper)itr.next();
			if( qw.service(req)==false ){
				return;
			}
		}
	}
}
