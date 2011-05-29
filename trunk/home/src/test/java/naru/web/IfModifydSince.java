package naru.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class IfModifydSince {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleDateFormat sdf=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
//		sdf.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("GMT"),Locale.US ));
		try {
			System.out.println(sdf.format(new Date()));
			
			Date date=sdf.parse("Sat, 23 May 2008 07:12:49 GMT");
			System.out.println(date);
			System.out.println(sdf.format(date));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
