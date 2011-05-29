package naru.web;

import java.util.Date;

import net.sf.json.util.PropertyFilter;

public class DatePropertyFilter implements PropertyFilter {

	/* date�^��json�ɂ��鎞��time�����ȊO�𖳎�����ݒ� */
	public boolean apply(Object source, String name, Object value) {
		if((source instanceof Date) && !name.equals("time")){
			return true;
		}
		return false;
	}

}
