package naru.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regxp {
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("aaaa.vm".matches(".*\\.vm$"));
//		String ck="Fujitsu Website_common_01=k7.fujitsu.co.jp.63941208497096656; NIN=1; RIYOU=4; MYPROXY_AUTHENTICATION=\"UgxIaMwUtKwIN3hwh8OiYYcrBNVyVpK3HlrKr15VacKsBFa_bDTDO3x40ronDNDJrMuu1v/uYVZ4n5eAg1in7tJJQhicQHsdDZzU3FB0gWDMyVd0cyW7pQ==\"; POTO=0";
		String ck="MYPROXY_AUTHENTICATION=aXM6aXM=";
		String[] cks=ck.split(";");
		for(int i=0;i<cks.length;i++){
			System.out.println("cks["+i+ "]:"+cks[i]);
		}
		Pattern p = Pattern.compile(" *MYPROXY_AUTHENTICATION *= *(\\S*)(;|\\z)");
		Matcher m = p.matcher(ck);
		if(m.find()){
			System.out.println(m.group());
			for(int i=0;i<=m.groupCount();i++){
				System.out.println("i:"+ i +"["+m.group(i)+"]");
			}
		}
		System.out.println("["+m.replaceFirst("")+"]");
		System.out.println(ck);
		/*
		Pattern p2 = Pattern.compile("\\b([A-Za-z_]\\w*)\\.(java|class)\\b");
		Matcher m2 = p2.matcher("Regex.java,2Regex.java, Regex.class");
		if(m2.find()){
			System.out.println(m2.group());
			for(int i=0;i<m2.groupCount();i++){
				System.out.println("i:"+ i +" "+m2.group(i));
			}
		}
		*/
		
	}

}
