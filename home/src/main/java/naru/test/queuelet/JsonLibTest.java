package naru.test.queuelet;

import java.io.PrintWriter;

import naru.web.AccessLog;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;

public class JsonLibTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		AccessLog accesslog=AccessLog.getById(5865);
		JSON json=JSONSerializer.toJSON("TEST");
		json.write(new PrintWriter(System.out));

	}

}
