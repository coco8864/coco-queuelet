package naru.web;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * peekInputStream,peekOutputStream�Ŏw�肵��Stream�̓ǂݏ�����peek����B
 * 
 * @author naru
 *
 */
public class PeekStream{
	private boolean inUse=false;
	private InputStream peekInputStream;//���K�[�����f�[�^��ǂݍ���
	private OutputStream peekOutputStream;//peek�����f�[�^����������
	private String traceName;
	private boolean zip;
	
	public PeekStream(){
	}
	
	private class PeekFilterInputStream extends FilterInputStream{
		private InputStream is;
		protected PeekFilterInputStream(InputStream is) {
			super(is);
			this.is=is;
		}
		public int available() throws IOException {
			return 0;
		}
		public void close() throws IOException {
//			is.close();Socket��close�́A�ʂɎ��{����
			peekOutputStream.close();
//			setInUse(false);
		}
		public synchronized void mark(int readlimit) {
			throw new UnsupportedOperationException("PeekFilterInputStream unsupport mark");
		}
		public boolean markSupported() {
			return false;
		}
		public synchronized void reset() throws IOException {
			throw new UnsupportedOperationException("PeekFilterInputStream unsupport reset");
		}
		public long skip(long n) throws IOException {
			throw new UnsupportedOperationException("PeekFilterInputStream unsupport skip");
		}
		public int read() throws IOException {
			int data=is.read();
			if(data>0){
				peekOutputStream.write(data);
			}else{
				peekOutputStream.close();
			}
			return data;
		}

		public int read(byte[] b, int off, int len) throws IOException {
			int readLength=is.read(b, off, len);
			if(readLength>0){
				peekOutputStream.write(b,off,readLength);
			}else{
				peekOutputStream.close();
			}
			return readLength;
		}

		public int read(byte[] b) throws IOException {
			return read(b,0,b.length);
		}
	}
	
	private class PeekFilterOutputStream extends FilterOutputStream{
		private OutputStream os;
		protected PeekFilterOutputStream(OutputStream os) {
			super(os);
			this.os=os;
		}

		public void close() throws IOException {
//			os.close();socket��close�͕ʂɎ��{����
			peekOutputStream.close();
//			setInUse(false);
		}

		public void flush() throws IOException {
			os.flush();
			peekOutputStream.flush();
		}

		public void write(byte[] b, int off, int len) throws IOException {
			os.write(b, off, len);
			peekOutputStream.write(b,off,len);
		}

		public void write(byte[] b) throws IOException {
			os.write(b);
			peekOutputStream.write(b);
		}

		public void write(int data) throws IOException {
			os.write(data);
			peekOutputStream.write(data);
		}
	}
	
	public InputStream peekInputStream(InputStream is) throws IOException{
		setup();
		return new PeekFilterInputStream(is);
	}
	
	public OutputStream peekOutputStream(OutputStream os) throws IOException{
		setup();
		return new PeekFilterOutputStream(os);
	}
	
	public void setup() throws IOException{
		setInUse(true);
		peekInputStream=new PipedInputStream();
		peekOutputStream=new PipedOutputStream((PipedInputStream)peekInputStream);
	}
	public void recycle(){
		setInUse(false);
		//���̎��_��peek�̏o�͐�́A�܂��o�͂𑱂��Ă���\��������B
		//������recycle���\�b�h���Ă΂ꂽ���A�{���ɍė��p����̂́A���̃I�u�W�F�N�g��ێ�����
		//AccessLog�N���X���ė��p����鎞�ł��邽�ߖ��͂Ȃ�
//		peekInputStream=null;
//		peekOutputStream=null;
	}
	
	public String getTraceName() {
		return traceName;
	}

	public void setTraceName(String traceName) {
		this.traceName = traceName;
	}
	
	public boolean isZip() {
		return zip;
	}

	public void setZip(boolean zip) {
		this.zip = zip;
	}
	
	//���̃��\�b�h�́Apeek�����g�p�B
	//PeekStream�������O�̃f�[�^���L�^�������ꍇ�Ɏg�p
	public OutputStream getPeekOutputStream(){
		return peekOutputStream;
	}
	
	//���̃��\�b�h�́A���O�����g�p����B
	public InputStream getPeekInputStream(){
		return peekInputStream;
	}
	
	private synchronized void setInUse(boolean inUse){
		this.inUse=inUse;
		notify();
	}
	
	public synchronized void waitForPeek(){
		while(true){
			if(inUse==false){
				return;
			}
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
