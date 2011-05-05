/*
 * Created on 2004/06/12
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package naru.channel;

import java.nio.channels.SocketChannel;

/**
 * @author naru
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface ChannelEvent {
	/* リクエスト受付通知.callじゃわかり難いのでリンリン */
	public void lynnLynn(SocketChannel req);
	public void timeout(SocketChannel req);
}
