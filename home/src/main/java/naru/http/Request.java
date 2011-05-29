/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.http;

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
	private String pool;
	private String complete;
	private String incomplete;
	private String error;

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//ê≥èÌånÇÕÅAtrue,àŸèÌånÇÕÅAfalse
		HttpData httpData=null;
		if(req instanceof Socket){
			httpData=(HttpData)context.deque(pool);
			httpData.setSocket((Socket)req);
		}else{
			httpData=(HttpData)req;
		}
		httpData.recycle();
		
		try {
			if(httpData.parseRequestHeader()){
				context.enque(httpData,complete);
				rc=true;
			}else{
				if(httpData.getSocket()==null){
					/* keep-aliveê⁄ë±Ç™êÿífÇ≥ÇÍÇΩ */
					context.enque(httpData,pool);
				}else{
					/* ÉäÉNÉGÉXÉgÇ™ìríÜÇ≈ìrêÿÇÍÇΩ */
					context.enque(httpData,incomplete);
				}
			}
		} catch (IOException e) {
			logger.error("Request error.",e);
			context.enque(httpData,error);
		}
		return rc;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.pool=(String)param.get("pool");
		this.complete=(String)param.get("complete");
		this.incomplete=(String)param.get("incomplete");
		this.error=(String)param.get("error");
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
