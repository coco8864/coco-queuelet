package naru.queuelet.typed;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

public class ASyncReadQueuelet implements Queuelet {

	public void init(QueueletContext context, Map param) {

	}

	public boolean service(Object req) {
		ByteBuffer[] buffers=null;
		ASyncIOIf ioif=(ASyncIOIf)req;
		SocketChannel channel=(SocketChannel)ioif.getChannel();
		try {
			long length=channel.read(buffers);
		} catch (IOException e) {
			ioif.failer(e);
		}
		ioif.read(buffers);
		return true;
	}

	public void term() {
	}

}
