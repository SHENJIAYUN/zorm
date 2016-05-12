package com.zorm.property;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;

public interface Getter extends Serializable{
	public Object get(Object owner) throws ZormException;
	
	public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) 
			throws ZormException;
	
	public Member getMember();
	
	public Class getReturnType();
	
	public String getMethodName();
	/*
	 * 获取getter方法
	 */
	public Method getMethod();
}
