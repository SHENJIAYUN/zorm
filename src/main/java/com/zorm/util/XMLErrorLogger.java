package com.zorm.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLErrorLogger implements ErrorHandler,Serializable{

	private static final long serialVersionUID = -7519302919808312025L;
    private static final Log log = LogFactory.getLog(XMLErrorLogger.class);
	private List<SAXParseException> errors;
	private String file;
	
	public XMLErrorLogger() {}
	
	public XMLErrorLogger(String file){
		this.file = file;
	}
    
    public void error(SAXParseException error) throws SAXException {
       if(this.errors == null){
    	   errors = new ArrayList<SAXParseException>();
       }
       errors.add(error);
	}

	public void fatalError(SAXParseException error) throws SAXException {
		error(error);
	}

	public void warning(SAXParseException warn) throws SAXException {
		log.warn("Warning parsing XML ("+warn.getLineNumber()+"):"+warn.getMessage());
	}

	public List<SAXParseException> getErrors() {
		return errors;
	}

	public void reset(){
		errors = null;
	}
	
	public boolean hasErrors(){
		return errors != null && errors.size() > 0;
	}
	
	public void logErrors(){
		if(errors != null){
			for(SAXParseException e : errors){
				if(file == null){
					log.error("Error parsing XML ("+e.getLineNumber()+"):"+e.getMessage());
				}
				else{
					log.error("Error parsing XML:"+file+"("+e.getLineNumber()+") "+e.getMessage());
				}
			}
		}
	}
}
