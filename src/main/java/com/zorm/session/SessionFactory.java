package com.zorm.session;

import java.io.Serializable;

import javax.naming.Referenceable;

import com.zorm.Interceptor;
import com.zorm.exception.ZormException;
import com.zorm.proxy.EntityNotFoundDelegate;
import com.zorm.stat.Statistics;


public interface SessionFactory extends Referenceable, Serializable {

	public interface SessionFactoryOptions{
		Interceptor getInterceptor();
		EntityNotFoundDelegate getEntityNotFoundDelegate();
	}
	
	public Session openSession() throws ZormException;
	public SessionBuilder withOptions();
	public Statistics getStatistics();
	public SessionFactoryOptions getSessionFactoryOptions();
}
