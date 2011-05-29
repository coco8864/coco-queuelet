/*
 * Created on 2004/10/21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.util.Map;

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
public class AccesslogQueuelet implements Queuelet {
	private static Logger logger=Logger.getLogger(AccesslogQueuelet.class);
	private QueueletContext context;
	
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		AccessLog accessLog=(AccessLog)req;
		accessLog.update();//DBíËã`Ç™Ç»Ç¢Ç∆Ç´ÇÕãÛêUÇËÇ∑ÇÈÇÊÇ§Ç…Ç»Ç¡ÇƒÇ¢ÇÈ
		accessLog.log();
		accessLog.recycle();
		context.enque(accessLog);
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
