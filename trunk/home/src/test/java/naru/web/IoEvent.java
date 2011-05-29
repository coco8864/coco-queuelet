package naru.web;

import java.nio.channels.Channel;

public interface IoEvent {
	public Channel getChannel();
	public String readable();
	public String writeadable();

}
