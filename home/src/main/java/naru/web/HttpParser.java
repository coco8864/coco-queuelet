package naru.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

public class HttpParser {
	private static Logger logger = Logger.getLogger(HttpParser.class);
	private static Configuration resonPhraseConfig = Config.getInstance().getConfiguration("ReasonPhrase");
	private static String CONTENT_TYPE_HEADER = "Content-type";
	private static String CONTENT_TYPE_Length = "Content-length";
	private static final String ENCODE = "ISO8859_1";
	private static byte[] HEADER_SEP = ": ".getBytes();
	private static byte[] CRLF = "\r\n".getBytes();
	private static byte[] SPACE = " ".getBytes();

	// �P�w�b�_�̍ő�T�C�Y
	private static int MAX_HEADER_BYTES = 8192;
	// �w�b�_�̍ő吔
	private static int MAX_HEADER_COUNT = 512;
	
	private String firstLine;
	private String firstLineToken1;
	private String firstLineToken2;
	private String firstLineToken3;

	//���̃N���X��parse�����ꍇ�̃w�b�_����ێ�����B
	private long headerLength;
	
	// private List rawHeaders;
	private Map headerNames;// �w�b�_�Ƃ��ẮA�啶���������̋�ʂ��Ȃ��A�Ή��\������
	private Map headers;
	private String contentType;
	private long contentLength;
	
	private InputStream bodyStream;
	private byte[] buffer;
	// private int buffPointer;
	private LengthInputStream lengthInputStream = new LengthInputStream();

	private boolean requestData;

	private class LengthInputStream extends InputStream {
		private long contentLength;
		private long length;
		private InputStream is;

		public void setInputStream(InputStream is,long contentLength) {
			this.length = 0;
			this.contentLength=contentLength;
			this.is = is;
		}

		public int read() throws IOException {
			if (length >= contentLength) {
				return -1;
			}
			length++;
			return is.read();
		}

		public int read(byte[] b, int off, int len) throws IOException {
			if (length >= contentLength) {
				return -1;
			}
			if ((length + len) >= contentLength) {
				len = (int)(contentLength - length);
			}
			int readLength = is.read(b, off, len);
			length += readLength;
			return readLength;
		}

		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}
		
		public void close() throws IOException{
			is.close();
		}
	}

	private void setFirstLine(String firstLine) {
		this.firstLine = firstLine;
		if (firstLine.length() == 0) {
			return;
		}
		if (firstLine.startsWith("HTTP")) {
			// ���X�|���X�̃X�e�[�^�X���C��"HTTP/1.1 200 OK"
			requestData = false;
		} else {
			// ���N�G�X�g�̃��N�G�X�g���C��"GET / HTTP/1.0"
			requestData = true;
		}
		StringTokenizer st = new StringTokenizer(firstLine, " ");
		firstLineToken1 = st.nextToken();
		if (!st.hasMoreTokens()) {
			firstLineToken2="";
			firstLineToken3="";
			return;
		}
		firstLineToken2 = st.nextToken();
		if (!st.hasMoreTokens()) {
			/* 508�ȏ�̃��X�|���X�̏ꍇ�Atomcat�́Areason���ȗ����ė��� */
			firstLineToken3="";
			return;
		}
		firstLineToken3 = st.nextToken("").trim();
	}

	public void addRawHeader(String header) {
		int pos = header.indexOf(":");
		String headerName;
		String headerValue;
		if (pos > 0) {
			headerName = header.substring(0, pos).trim();
			headerValue = header.substring(pos + 1).trim();
		} else {
			headerName = header;
			headerValue = "";
		}
		addHeader(headerName, headerValue);
	}

	//�w�b�_���m�肵�Ă���Ăяo����
	public void setBodyStream(InputStream is) {
		if(is==null){
			this.bodyStream = null;
			return;
		}
		contentType=getHeader(CONTENT_TYPE_HEADER);
		contentLength = getIntHeader(CONTENT_TYPE_Length);
		if (contentLength<0) {//GET���N�G�X�g�Ƃ�Keep-Alive��O��ɂ��Ȃ����X�|���X�Ƃ�
			String statusCode=getStatusCode();
			if(isRequest()){
				lengthInputStream.setInputStream(is,0);
				this.bodyStream = lengthInputStream;
			}else if("304".equals(statusCode) || "204".equals(statusCode)){
				lengthInputStream.setInputStream(is,0);
				this.bodyStream = lengthInputStream;
			}else{
				this.bodyStream = is;
			}
			return;
		}
		lengthInputStream.setInputStream(is,contentLength);
		this.bodyStream = lengthInputStream;
	}

	private static final int PHASE_FIRST_LINE = 1;
	private static final int PHASE_HEADER = 2;
	private static final int PHASE_R = 3;
	private static final int PHASE_N = 4;
	private static final int PHASE_RN = 5;
	private static final int PHASE_RNR = 6;

	public HttpParser() {
		headers = new HashMap();
		headerNames = new HashMap();
		buffer = new byte[MAX_HEADER_BYTES];
	}

	public void recycle() {
		headers.clear();
		headerNames.clear();
		firstLine = firstLineToken1 = firstLineToken2 = firstLineToken3 = null;
		bodyStream = null;
		headerLength=0;
	}

	//�w�b�_�̏I�[������܂Ńu���b�N����A���N�G�X�g�͂���ł悢�����X�|���X�͂�����Ɩ�肩��?
	public void parse(InputStream is) throws IOException {
		int buffIndex = 0;
		headerLength=0;

		int phase = PHASE_FIRST_LINE;
		while (true) {
			int b = is.read();
			if (b < 0) {
				// �w�b�_�I�[���Ȃ�����
				throw new IOException("No header end");
			}
			headerLength++;
			switch (b) {
			case '\r':
				switch (phase) {
				case PHASE_FIRST_LINE:
					setFirstLine(new String(buffer, 0, buffIndex));
					phase = PHASE_R;
					break;
				case PHASE_HEADER:
					addRawHeader(new String(buffer, 0, buffIndex));
					phase = PHASE_R;
					break;
				case PHASE_R:// RR�́A�w�b�_�I�[�Ƃ��ĔF�߂�
					setBodyStream(is);//***trace���鎞�͂��������***
					return;
				case PHASE_RN:
					phase = PHASE_RNR;
					break;
				case PHASE_N:// NR�́A�F�߂Ȃ�
				case PHASE_RNR:// RNRR�́A�F�߂Ȃ�
					throw new IOException("sec error.phase:" + phase + " \\\\r");
				}
				buffIndex = 0;
				break;
			case '\n':
				switch (phase) {
				case PHASE_FIRST_LINE:
					setFirstLine(new String(buffer, 0, buffIndex));
					phase = PHASE_N;
					break;
				case PHASE_HEADER:
					addRawHeader(new String(buffer, 0, buffIndex));
					phase = PHASE_N;
					break;
				case PHASE_N:// NN�́A�w�b�_�I�[�Ƃ��ĔF�߂�
				case PHASE_RNR:// RNRN�́A�w�b�_�I�[
					setBodyStream(is);//***trace���鎞�͂��������***
					return;
				case PHASE_R:
					phase = PHASE_RN;
					break;
				case PHASE_RN:// RNN�́A�F�߂Ȃ�
					throw new IOException("sec error.phase:" + phase + " \\\\n");
				}
				buffIndex = 0;
				break;
			default:
				switch (phase) {
				case PHASE_FIRST_LINE:
					break;
				case PHASE_HEADER:
					break;
				case PHASE_N:// N�́A�w�b�_�I�[�Ƃ��ĔF�߂�
				case PHASE_R:// R�́A�w�b�_�I�[�Ƃ��ĔF�߂�
				case PHASE_RN:// RN�́A�w�b�_�I�[
					phase = PHASE_HEADER;
					break;
				case PHASE_RNR:// RNRX�́A�ُ�
					phase = PHASE_RN;
					throw new IOException("sec error.phase:" + phase + " "
							+ Character.toString((char) b));
				}
				buffer[buffIndex] = (byte) b;
				buffIndex++;
				if (buffIndex >= MAX_HEADER_BYTES) {
					throw new IOException("too long header:" + buffIndex);
				}
				break;
			}
		}
	}

	// ���N�G�X�g�n�̃��\�b�h
	public String getRequestLine() {
		return firstLine;
	}

	public String getMethod() {
		return firstLineToken1;
	}

	public void setMethod(String method) {
		firstLineToken1 = method;
	}

	public String getUri() {
		return firstLineToken2;
	}

	public void setUri(String uri) {
		firstLineToken2 = uri;
	}

	public String getReqHttpVersion() {
		return firstLineToken3;
	}

	public void setReqHttpVersion(String httpVersion) {
		firstLineToken3 = httpVersion;
	}

	// ���X�|���X�n�̃��\�b�h
	public String getStatusLine() {
		return firstLine;
	}

	public String getResHttpVersion() {
		return firstLineToken1;
	}

	public void setResHttpVersion(String httpVersion) {
		firstLineToken1 = httpVersion;
	}

	public String getStatusCode() {
		return firstLineToken2;
	}

	public void setStatusCode(String statusCode) {
		firstLineToken2 = statusCode;
		String resonPhrase = resonPhraseConfig.getString(statusCode);
		if (resonPhrase == null) {
			resonPhrase = "Unkown reason Phrase:" + statusCode;
		}
		firstLineToken3 = resonPhrase;
	}

	public String getReasonPhrase() {
		return firstLineToken3;
	}

	// ���N�G�X�g���X�|���X���p
	// �����̓����w�b�_��z��
	private void putHeaderValues(String name, List values) {
		headerNames.put(name.toUpperCase(), name);
		headers.put(name, values);
	}

	public List getHeaders(String name) {
		Object realName = headerNames.get(name.toUpperCase());
		return (List) headers.get(realName);
	}

	public String getHeader(String name) {
		List values = getHeaders(name);
		if (values == null || values.size() == 0) {
			return null;
		}
		return (String) values.get(0);
	}

	public void replaceHeader(String name, String value) {
		List values = getHeaders(name);
		if (values == null) {
			values = new ArrayList();
			putHeaderValues(name, values);
		} else {
			values.clear();
		}
		values.add(value);
		if (this.headers.size() > MAX_HEADER_COUNT) {
			throw new RuntimeException("too many headers:"
					+ this.headers.size());
		}
	}

	public void removeHeader(String name) {
		List values = getHeaders(name);
		if (values == null) {
			return;
		}
		Object realName = headerNames.remove(name.toUpperCase());
		headers.remove(realName);
	}
	
	public void setHeader(String name, String value) {
		List values = getHeaders(name);
		if (values == null) {
			addHeader(name,value);
			return;
		}
		values.clear();
		values.add(value);
	}

	public void addHeader(String name, String value) {
		List values = getHeaders(name);
		if (values == null) {
			values = new ArrayList();
			putHeaderValues(name, values);
		}
		values.add(value);
		if (this.headers.size() > MAX_HEADER_COUNT) {
			throw new RuntimeException("too many headers:"
					+ this.headers.size());
		}
	}

	private int writeBuffer(byte[] data, int buffPointer) {
		System.arraycopy(data, 0, buffer, buffPointer, data.length);
		return buffPointer + data.length;
	}

	/* �w�b�_����os�ɓ]������ */
	public int writeSeriarizeHeader(OutputStream os) throws IOException {
		if(firstLineToken1==null){
			return 0;//statusCode���m�肵�Ă��Ȃ��ꍇ
		}
		int buffPointer = 0;
		try {
			buffPointer = writeBuffer(firstLineToken1.getBytes(ENCODE),
					buffPointer);
			buffPointer = writeBuffer(SPACE, buffPointer);
			buffPointer = writeBuffer(firstLineToken2.getBytes(ENCODE),
					buffPointer);
			buffPointer = writeBuffer(SPACE, buffPointer);
			buffPointer = writeBuffer(firstLineToken3.getBytes(ENCODE),
					buffPointer);
			buffPointer = writeBuffer(CRLF, buffPointer);
			Iterator itr = headers.keySet().iterator();
			while (itr.hasNext()) {
				String name = (String) itr.next();
				List values = (List) headers.get(name);
				int n = values.size();
				for (int i = 0; i < n; i++) {
					buffPointer = writeBuffer(name.getBytes(ENCODE), buffPointer);
					buffPointer = writeBuffer(HEADER_SEP, buffPointer);
					String value = (String) values.get(i);
					buffPointer = writeBuffer(value.getBytes(ENCODE),
							buffPointer);
					buffPointer = writeBuffer(CRLF, buffPointer);
				}
			}
			//�w�b�_�I�[
			buffPointer = writeBuffer(CRLF, buffPointer);
		} catch (UnsupportedEncodingException e) {// ���蓾�Ȃ�
			logger.error("ENCODE error." + ENCODE, e);
			throw new RuntimeException("fail to headerToStream", e);
		}
		os.write(buffer, 0, buffPointer);
		return buffPointer;
	}

	/* �{�f�B����os�ɓ]������ */
	public int writeBody(OutputStream os) throws IOException {
		return writeBody(os,null);
	}
	public int writeBody(OutputStream os,StringBuffer peek) throws IOException {
		int length = 0;
		while (true) {
			int readLen = bodyStream.read(buffer);
			if (readLen <= 0) {
				break;
			}
			if(os!=null){
				os.write(buffer, 0, readLen);
			}
			if(peek!=null){
				peek.append(new String(buffer,0,readLen,"ISO8859_1"));
			}
			length += readLen;
		}
		return length;
	}
	
	public boolean isRequest(){
		return requestData;
	}

	public int getIntHeader(String name) {
		String value = getHeader(name);
		if(value==null){
			return -1;
		}
		return Integer.parseInt(value);
	}

	public Iterator getHeaderNames() {
		return headers.keySet().iterator();
	}

	public InputStream getBodyStream() {
		return bodyStream;
	}
	
	public String getContentType(){
		return contentType;
	}
	
	public long getContentLength(){
		return contentLength;
	}
	
	public long getHeaderLength(){
		return headerLength;
	}
}
