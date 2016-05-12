package com.zorm.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AssertionFailure extends RuntimeException{

	private static final long serialVersionUID = -29787333314984441L;
    private static final Log log = LogFactory.getLog(AssertionFailure.class); 
    
    public AssertionFailure(String s){
    	super(s);
    	log.fatal(this);
    }
    
    public AssertionFailure(String s,Throwable t){
    	super(s, t);
    	log.fatal(t);
    }
}
