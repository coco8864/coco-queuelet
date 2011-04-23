/*
 * Created on 2004/10/16
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.typed;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IOQueuelet implements Queuelet {
	private QueueletContext context;
	private String getInputStreamMethod;
	private String getOutputStreamMethod;
	private int bufSize=1024*8;
	private boolean selectable=false;

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		InputStream is=null;
		OutputStream os=null;

		try {
			is=(InputStream)MethodUtils.invokeMethod(req,getInputStreamMethod,new Object[0]);
			os=(OutputStream)MethodUtils.invokeMethod(req,getOutputStreamMethod,new Object[0]);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(is==null){
			context.enque(req);
			return true;
		}

		try {
			byte buf[]=new byte[bufSize];
			while(true){
				int len=is.read(buf);
				if(len<=0){
					break;
				}
				os.write(buf,0,len);
				if(selectable){
					break;
				}
			}
		} catch (IOException e) {
		}
		context.enque(req);
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.getInputStreamMethod=(String)param.get("getInputStreamMethod");
		this.getOutputStreamMethod=(String)param.get("getOutputStreamMethod");
		String bufSizeString=(String)param.get("bufferSize");
		if( bufSizeString!=null){
			this.bufSize=Integer.parseInt(bufSizeString);
		}
		String selectableString=(String)param.get("selectable");
		if( selectableString!=null){
			this.selectable="true".equalsIgnoreCase(selectableString);
		}
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
