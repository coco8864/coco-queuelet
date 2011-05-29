/*
 * Created on 2004/10/21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru hayashi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PeekQueuelet implements Queuelet {
	private static Logger logger=Logger.getLogger(PeekQueuelet.class);
	
	private static ThreadLocal buffPool=new ThreadLocal();	
	private QueueletContext context;
	private File traceBaseDir;
	private int bufferLength;
	
	private byte[] getBuffer(){
		byte[] buff=(byte [])buffPool.get();
		if( buff==null){
			buff=new byte[bufferLength];
			buffPool.set(buff);
		}
		return buff;
	}
	
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		PeekStream peek=(PeekStream)req;
		String traceName=peek.getTraceName();
		OutputStream os=null;
		InputStream peekIs=null;
		byte[] buf=getBuffer();
		try {
//			File outputFile=File.createTempFile(peek.getTraceName(),suffix,traceBaseDir);
			File outputFile=new File(traceBaseDir,traceName);
			os=new FileOutputStream(outputFile);
			if(peek.isZip()){
				ZipOutputStream traceStream=new ZipOutputStream(os);
				ZipEntry ze=new ZipEntry(traceName);
				traceStream.putNextEntry(ze);
				os=traceStream;
			}
			peekIs=peek.getPeekInputStream();
			int readTotal=0;
			while(true){
				int readLen=peekIs.read(buf);
				if(readLen<=0){
					break;
				}
				readTotal+=readLen;
				os.write(buf,0,readLen);
			}
			if(readTotal==0){
				logger.info("read 0:traceName:"+traceName);
			}
		} catch (FileNotFoundException e) {
			logger.error("peekQueue FileNotFound error.",e);
		} catch (IOException e) {
			logger.error("peekQueue IO error.",e);
		}finally{
			if( os!=null ){
				try {
					os.close();
				} catch (IOException ignore) {}
			}
			if( peekIs!=null ){
				try {
					peekIs.close();
				} catch (IOException ignore) {}
			}
			//この時点でpeekの出力先は、まだ出力を続けている可能性がある。
			//ここでrecycleメソッドを呼ぶが、本当に再利用するのは、このオブジェクトを保持する
			//AccessLogクラスが再利用される時である。
			peek.recycle();
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		Configuration config=Config.getInstance().getConfiguration();
		this.traceBaseDir=new File(config.getString("traceBaseDir"));
		String bufferLength=(String)param.get("bufferLength");
		this.bufferLength=Integer.parseInt(bufferLength);
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}
}
