package com.zorm.util;

import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public final class XMLHelper {

	private SAXReader saxReader;
	
	public SAXReader createSAXReader(ErrorHandler errorHandler,EntityResolver entityResolver) throws SAXException{
		SAXReader saxReader = resolveSAXReader();
		//saxReader.setEntityResolver(entityResolver);
		
		//不进行DTD验证
		saxReader.setValidation(false);
		saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		saxReader.setErrorHandler(errorHandler);
		return saxReader;
	}

	private SAXReader resolveSAXReader() {
        if(saxReader == null){
        	saxReader = new SAXReader();
        	saxReader.setMergeAdjacentText(true);
        	saxReader.setValidation(true);
        }
		return saxReader;
	}
}
