/*
 * Created on 2004/06/09
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package naru.queuelet;

import java.util.Map;

/**
 * @author naru
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface Queuelet {
	public static final String PARAM_KEY_DEAMON="QueueletDaemon";
	public static final String PARAM_KEY_LOADER="QueueletLoader";
	public static final String PARAM_KEY_ARGS="QueueletArgs";
	public static final String PARAM_KEY_THIS_TERMINAL="thisTerminal";
	public static final String PARAM_KEY_STARTUPINFO="QueueletStartupInfo";
	
	/**
	 * éÂèàóù
	 * 
	 * @param req
	 */
	public boolean service(Object req);
	
	public void init(QueueletContext context,Map param);
	public void term();
}
