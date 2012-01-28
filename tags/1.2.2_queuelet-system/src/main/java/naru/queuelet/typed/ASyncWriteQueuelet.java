package naru.queuelet.typed;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

public class ASyncWriteQueuelet implements Queuelet {
	private QueueletContext context;
	private String asyncIo="asyncIo";
	
	public void init(QueueletContext context, Map param) {
		this.context=context;
	}

	public boolean service(Object req) {
		ASyncIOIf ioif=(ASyncIOIf)req;
		SocketChannel channel=(SocketChannel)ioif.getChannel();
		ByteBuffer[] buffers=ioif.getWriteBuffers();
		try {
			long length=channel.write(buffers);
		} catch (IOException e) {
			ioif.failer(e);
		}
		for(int i=0;i<buffers.length;i++){
			if(buffers[i].remaining()!=0){
				context.enque(req,asyncIo);
			}
		}
		ioif.written();
		return true;
	}

	public void term() {
	}

}
