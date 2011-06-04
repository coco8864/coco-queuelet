package naru.queuelet.startup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class CommandLine extends TestCase {
	/*
	 * パラメタが"で始まる場合,'で囲んだ上でスペースで区切る
	 * パラメタが'で始まる場合,"で囲んだ上でスペースで区切る
	 * パラメタ中にスペースを含まない場合、スペースで区切る
	 * パラメタ中に["スペース]を含む場合、'で囲んだ上でスペースで区切る
	 * 上記以外の場合、"で囲んだ上でスペースで区切る
	 * 
	 * パラメタ中に'とスペースを含む場合、"で囲んだ上でスペースで区切る
	 * パラメタ中に"とスペースを含む場合,'で囲んだ上でスペースで区切る
	 * パラメタ中に'と"とスペースを含む、かつ["スペース]を含まない場合、"で囲んだ上でスペースで区切る
	 * パラメタ中に'と"とスペースを含む、かつ['スペース]を含まない場合、'で囲んだ上でスペースで区切る
	 * パラメタ中に['スペース]["スペース]の両方を含むものは、表現できない。
	 * 
	 * aaa
	 *  aa
	 * a a
	 * aa 
	 * a' a
	 * a" a
	 * 'aa
	 * "aa
	 * a'a
	 * a"a
	 * aa'
	 * aa"
	 * a" a' a
	 * 
	 * 
	 * [aaa]　=>  aaa
	 * [aaa']　=>  "aaa'"
	 * ['aaa]　=>  "'aaa"
	 * [a' aa]　=>  "a' aa"
	 * 
	 * これが一個の場合[aaa' bbb" ccc]表現できない
	 * 
	 */
	
	private String[] parse(String commandLine){
		Pattern pattern=Pattern.compile("'(.*)'[\\s|$]|\"(.*)\"[\\s|$]|([\\S]+)");
		Matcher matcher=pattern.matcher(commandLine);
		int index=0;
		System.out.println("===" +commandLine +"===");
		while(matcher.find()){
			int count=matcher.groupCount();
			for(int i=0;i<=count;i++){
				System.out.println(index+":" +i + ":" +matcher.group(i));
			}
			index++;
		}
		System.out.println("---" +commandLine +"---");
		return commandLine.split(" ");
	}
	
	public void test1(){
		String commandLine="aa bb cc";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb", ret[1]);
		assertEquals("cc", ret[2]);
	}

	public void test11(){
		String commandLine="aa  bb    cc";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb", ret[1]);
		assertEquals("cc", ret[2]);
	}

	public void test12(){
		String commandLine="aa  b\"b    cc";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb", ret[1]);
		assertEquals("cc", ret[2]);
	}

	public void test13(){
		String commandLine="aa  b\"b    c\"c";
		String[]ret=parse(commandLine);
		assertEquals("aa", ret[0]);
		assertEquals("bb", ret[1]);
		assertEquals("cc", ret[2]);
	}

	public void test14(){
		String commandLine="aa  b\"b    c\"c";
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
	
	public void test21(){
		String commandLine="  aa \"bb\" cc";
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
