/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.manager;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Request implements Queuelet {
	static private Logger logger=Logger.getLogger(Request.class);
	private QueueletContext context;
	String thisTerminal;

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//³íŒn‚ÍAtrue,ˆÙíŒn‚ÍAfalse
		HttpData httpData=null;
		if(req instanceof Socket){
			httpData=(HttpData)new HttpData("8192");
			httpData.setSocket((Socket)req);
			context.enque(httpData,thisTerminal);
			return false;
		}else{
			httpData=(HttpData)req;
		}
		
		try {
			if(httpData.readRequestHeader()){
				rc=true;
			}else{
				if(httpData.getSocket()==null){
					/* keep-aliveÚ‘±‚ªØ’f‚³‚ê‚½ */
					httpData.closeSocket(true);
				}else{
					/* ƒŠƒNƒGƒXƒg‚ª“r’†‚Å“rØ‚ê‚½ */
					httpData.closeSocket(true);
				}
			}
		} catch (IOException e) {
			logger.error("Request error.",e);
			httpData.closeSocket(true);
			rc=false;
		}
		return rc;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		thisTerminal=(String)param.get("thisTerminal");
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
