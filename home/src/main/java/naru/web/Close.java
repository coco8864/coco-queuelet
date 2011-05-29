/*
 * Created on 2004/10/21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru hayashi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Close implements Queuelet {
	private static Logger logger=Logger.getLogger(Close.class);	
	private QueueletContext context;
	
	public void waitForClose(HttpContext httpContext){
		Socket socket=httpContext.getSocket();
		if(socket==null){
			return;
		}
		//�u���E�U�������close��҂�
		try{
			byte[] buffer=httpContext.getBuffer();
			socket.setSoTimeout(100);//�ʐM�Ƃ��Ẵ^�C���A�E�g��Z���ݒ肵�Ă���
			//HTTP�L���f�[�^�����ǂݍ��݂̏ꍇ����ǂ݂���B�i�g���[�X���̎悵�Ă���ꍇ�L���j
			InputStream is=httpContext.getRequestBodyStream();
			if(is==null){//�K��BodyStream������킯�ł͂Ȃ�
				return;
			}
			while(true){
				int readLength=is.read(buffer);//�ǂݎ̂�
				if(readLength<=0){
					break;
				}
			}
			//HTTP�I�ȗL���f�[�^�ł͂Ȃ����A�]���ȃf�[�^���������ꍇ
			is=socket.getInputStream();
			while(true){
				//POST�̏ꍇIE�́AContent-Length���v���X0D0A�𑗐M���Ă���???�����̂�???
				int readLength=is.read(buffer);//�ǂݎ̂�
				if(readLength<=0){
					break;
				}
			}
		}catch(IOException ignore){
			logger.debug("wait for browser close error",ignore);
		}
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_CLOSE);
		
		//�����Ȃ�N���[�Y����ƃ��X�|���X���Ō�܂œ��B���Ȃ��ꍇ������B
		waitForClose(httpContext);
		httpContext.closeSocket(true);
		httpContext.recycle();
		AccessLog accessLog=httpContext.getAccessLog();
		context.enque(accessLog,Config.QUEUE_ACCESSLOG);
		context.enque(httpContext);
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
