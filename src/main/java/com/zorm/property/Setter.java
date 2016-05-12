package com.zorm.property;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;

public interface Setter extends Serializable{
	public void set(Object target, Object value, SessionFactoryImplementor factory) throws ZormException;

	public String getMethodName();
	
	public Method getMethod();
}
