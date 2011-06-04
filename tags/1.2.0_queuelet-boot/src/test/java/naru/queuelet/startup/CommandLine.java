package naru.queuelet.startup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class CommandLine extends TestCase {
	/*
	 * �p�����^��"�Ŏn�܂�ꍇ,'�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * �p�����^��'�Ŏn�܂�ꍇ,"�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * �p�����^���ɃX�y�[�X���܂܂Ȃ��ꍇ�A�X�y�[�X�ŋ�؂�
	 * �p�����^����["�X�y�[�X]���܂ޏꍇ�A'�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * ��L�ȊO�̏ꍇ�A"�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * 
	 * �p�����^����'�ƃX�y�[�X���܂ޏꍇ�A"�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * �p�����^����"�ƃX�y�[�X���܂ޏꍇ,'�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * �p�����^����'��"�ƃX�y�[�X���܂ށA����["�X�y�[�X]���܂܂Ȃ��ꍇ�A"�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * �p�����^����'��"�ƃX�y�[�X���܂ށA����['�X�y�[�X]���܂܂Ȃ��ꍇ�A'�ň͂񂾏�ŃX�y�[�X�ŋ�؂�
	 * �p�����^����['�X�y�[�X]["�X�y�[�X]�̗������܂ނ��̂́A�\���ł��Ȃ��B
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
	 * [aaa]�@=>  aaa
	 * [aaa']�@=>  "aaa'"
	 * ['aaa]�@=>  "'aaa"
	 * [a' aa]�@=>  "a' aa"
	 * 
	 * ���ꂪ��̏ꍇ[aaa' bbb" ccc]�\���ł��Ȃ�
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
