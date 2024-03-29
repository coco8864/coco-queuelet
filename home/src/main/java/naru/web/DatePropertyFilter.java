package naru.web;

import java.util.Date;

import net.sf.json.util.PropertyFilter;

public class DatePropertyFilter implements PropertyFilter {

	/* date型をjsonにする時にtime属性以外を無視する設定 */
	public boolean apply(Object source, String name, Object value) {
		if((source instanceof Date) && !name.equals("time")){
			return true;
		}
		return false;
	}

}
