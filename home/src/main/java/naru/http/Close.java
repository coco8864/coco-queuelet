/*
 * Created on 2004/10/21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.http;

import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru hayashi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Close implements Queuelet {
	private QueueletContext context;

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		HttpData httpData=(HttpData)req;
		httpData.closeSocket(true);
		context.enque(httpData);
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
