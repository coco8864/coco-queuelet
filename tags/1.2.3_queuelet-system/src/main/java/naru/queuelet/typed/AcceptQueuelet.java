/*
 * �쐬��: 2004/07/21
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.typed;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;

import naru.channel.AcceptChannel;
import naru.channel.ChannelEvent;
import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class AcceptQueuelet implements Queuelet,ChannelEvent {
	static private Logger logger=Logger.getLogger(AcceptQueuelet.class);
	private QueueletContext context;
//	private ChannelListener cl;
	private AcceptChannel cl;
	private String pool;
	private String setSocketMethod;

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object, naru.quelet.Terminal)
	 */
	public boolean service(Object req) {
		return false;
	}

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#init()
	 */
	public void init(QueueletContext context,Map param) {
		this.context=context;
		int port;
		int backLog;
		int acceptMode;
		
		port=Integer.parseInt((String)param.get("port"));
		String acceptModeString=(String)param.get("mode");
		if("read".equalsIgnoreCase(acceptModeString)){
			acceptMode=AcceptChannel.MODE_READ;
		}else{
			acceptMode=AcceptChannel.MODE_OPEN;
		}

		try {
			this.cl=new AcceptChannel(acceptMode,0);
//			this.cl=new ChannelListener(port,this,acceptMode);
			cl.entry(port,this);
		} catch (IOException e) {
			e.printStackTrace();
			context.finish();
			return;
		}
		this.pool=(String)param.get("pool");
		this.setSocketMethod=(String)param.get("setSocketMethod");
		cl.start();
	}

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#term()
	 */
	public void term() {
		if( cl==null ){
			return;
		}
		cl.stop();
		cl=null;
		context=null;
	}

	/* (�� Javadoc)
	 * @see naru.channel.ChannelEvent#lynnLynn(java.nio.channels.SocketChannel)
	 */
	public void lynnLynn(SocketChannel req) {
		Object queueObject=null;
		Socket socket=req.socket();
		queueObject=socket;
		if(pool!=null){
			queueObject=context.deque(pool);
			Object[] param=new Object[1];
			param[0]=socket;
			try {
				MethodUtils.invokeMethod(queueObject,setSocketMethod,param);
			} catch (Exception e) {
				logger.error("setSocketMethod invoke error.setSocketMethod=" + setSocketMethod,e);
			}
		}
		context.enque(queueObject);
	}

	/* (non-Javadoc)
	 * @see naru.channel.ChannelEvent#timeout(java.nio.channels.SocketChannel)
	 */
	public void timeout(SocketChannel req) {
		throw new IllegalStateException("AcceptQueuelet timeout!:" + req.toString());
	}
}
