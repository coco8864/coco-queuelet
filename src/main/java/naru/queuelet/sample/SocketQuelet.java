/*
 * �쐬��: 2004/07/23
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.sample;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;


/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class SocketQuelet implements Queuelet {
	private QueueletContext command;
	private static int MESSAGE_MAX_BYTE=1024;
	private static String ENCODING="SJIS";
	private static Charset charset = Charset.forName(ENCODING);
	private static CharsetDecoder decoder = charset.newDecoder();
	private static CharsetEncoder encoder = charset.newEncoder();

	public class Element{
		Socket socket;
		public Element(Socket s){
			setSocket(s);
		}
		public Socket getS(){
			return socket;
		}
		public void setSocket(Socket s){
			socket=s;
		}
	}

	/* ���b�Z�[�W�ʐM���\�b�h(���̕Ӊ��P�̗]�n����,nio�m���s��) */
	private String recive(SocketChannel socketChannel) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_MAX_BYTE);
		// �f�[�^�̓ǂݍ���
		if (socketChannel.read(buffer) < 0) {
			return null;
		}
		buffer.flip();
		String message = decoder.decode(buffer).toString();
		return message;
	}

	private void send(SocketChannel socketChannel, String message)
		throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(ENCODING));
		socketChannel.write(buffer);
	}
	

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		System.out.println("$$$$$$$$" + getClass().getClassLoader().toString());
		try {
			Socket socket=null;
			Element element=null;
			if( req instanceof Socket){
				socket=(Socket)req;
				element=new Element(socket);
			}else{
				element=(Element)req;
				socket=element.getS();
			}
			
			String msg=recive(socket.getChannel());
			System.out.println("receive:" + msg);
			if( msg==null){
				System.out.println("close client");
				return true;
			}

			try {
				Thread.sleep(1000);//�������ɂP�b������V���~���[�g
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			send(socket.getChannel(),"OK?");
			command.enque(element);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#init(naru.quelet.QueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
		this.command=command;
	}

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#term()
	 */
	public void term() {
	}

}
