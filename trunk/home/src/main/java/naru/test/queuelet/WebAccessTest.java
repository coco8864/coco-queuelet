package naru.test.queuelet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import naru.web.AccessLog;

public class WebAccessTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String source="/xxx";
		String dest="http://aaa:bbb/ccc/ddd";
		String reqLine="/xxx/yyy/zzz?www";
		
		String exceptProxy="\\S*.fujitsu.com|\\S*.fujitsu.co.jp|\\S*.pfu.co.jp|10.\\S*|localhost";
		Pattern pattern=Pattern.compile(exceptProxy);
		Matcher m = pattern.matcher("judus.soft.fujitsu.com");
		System.out.println(m.matches());
		

		System.out.println("judus.soft.fujitsu.com".matches(exceptProxy));
		
		
		
		String result=reqLine.replaceFirst("^"+source, dest);
		System.out.println(result);

	}

}
