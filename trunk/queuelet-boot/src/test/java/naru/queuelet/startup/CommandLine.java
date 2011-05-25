package naru.queuelet.startup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class CommandLine extends TestCase {
	
	
	private String[] parse(String commandLine){
		Pattern pattern=Pattern.compile("((\".*\")|([^\\s]*))*");
		Matcher matcher=pattern.matcher(commandLine);
		if(matcher.matches()){
			int count=matcher.groupCount();
			for(int i=0;i<=count;i++){
				System.out.println(i + ":" +matcher.group(i));
			}
		}
		
		return commandLine.split(" ");
	}
	
	
	public void test1(){
		String commandLine="aa bb cc";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb", ret[1]);
		assertEquals("cc", ret[2]);
	}

	public void test2(){
		String commandLine="aa \"bb\" cc";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb", ret[1]);
		assertEquals("cc", ret[2]);
	}
	
	public void test3(){
		String commandLine="aa \"bb bb\" cc";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb bb", ret[1]);
		assertEquals("cc", ret[2]);
	}
}
