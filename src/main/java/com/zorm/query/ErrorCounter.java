package com.zorm.query;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import antlr.RecognitionException;

import com.zorm.exception.QueryException;
import com.zorm.exception.QuerySyntaxException;

public class ErrorCounter implements ParseErrorHandler {

	private static final Log log = LogFactory.getLog(ErrorCounter.class);
	
	private List errorList = new ArrayList();
	private List warningList = new ArrayList();
	private List recognitionExceptions = new ArrayList();
	
	@Override
	public void reportError(RecognitionException e) {
		reportError( e.toString() );
		recognitionExceptions.add( e );
        log.error(e.toString(), e);
	}

	@Override
	public void reportError(String message) {
		log.error(message);
		errorList.add( message );
	}

	@Override
	public void reportWarning(String message) {
       log.debug(message);
       warningList.add(message);
	}

	@Override
	public int getErrorCount() {
		return errorList.size();
	}

	@Override
	public void throwQueryException() throws QueryException {
		if ( getErrorCount() > 0 ) {
            if (recognitionExceptions.size() > 0) throw QuerySyntaxException.convert((RecognitionException)recognitionExceptions.get(0));
            throw new QueryException(getErrorString());
        }
		log.debug("throwQueryException() : no errors");
	}
	
	private String getErrorString() {
		StringBuilder buf = new StringBuilder();
		for ( Iterator iterator = errorList.iterator(); iterator.hasNext(); ) {
			buf.append( ( String ) iterator.next() );
			if ( iterator.hasNext() ) buf.append( "\n" );

		}
		return buf.toString();
	}

}
