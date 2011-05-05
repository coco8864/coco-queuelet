/*
 * �쐬��: 2004/09/10
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.loader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import naru.queuelet.core.Container;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class HookerControler {
	static private Logger logger=Logger.getLogger(HookerControler.class.getName());
	private Map classHookers=new HashMap();
	
	public MethodHooker getMethodHooker(int id){
		return MethodHooker.getMethodHooker(id);
	}

	public ClassHooker getClassHooker(String className){
		ClassHooker ch=(ClassHooker)classHookers.get(className);
		return ch;
	}
	
	public MethodHooker[] getMethodHookers(String className){
		ClassHooker ch=getClassHooker(className);
		return ch.getMethodHookers();
	}
	
	public ClassHooker addClassHooker(ClassHooker hooker){
		classHookers.put(hooker.getName(),hooker);
		return hooker;
	}

	public MethodHooker addMethodHooker(String className,MethodHooker methodHooker){
		ClassHooker ch=getClassHooker(className);
		if( ch==null ){
			ch=new ClassHooker();
			ch.setName(className);
			addClassHooker(ch);
		}
		
		ch.addMethodHooker(methodHooker);
		return methodHooker;
	}
	
	public void setup(Container container){
		Map setupClassHookers=new HashMap();
		Iterator itr=classHookers.keySet().iterator();
		while(itr.hasNext()){
			String name=(String)itr.next();
			ClassHooker hooker=(ClassHooker)classHookers.get(name);
			String resolveName=container.resolveProperty(name);
			hooker.setName(resolveName);
			setupClassHookers.put(resolveName, hooker);
		}
		classHookers.clear();
		classHookers=setupClassHookers;
	}
}
