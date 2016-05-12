package com.zorm.util;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.config.Environment;
import com.zorm.exception.ZormException;

public final class ConfigHelper {

	private static final Log log = LogFactory.getLog(ConfigHelper.class);
	
	public static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;
        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if(classLoader != null){
			stream = classLoader.getResourceAsStream(stripped);
		}
		if(stream == null){
			stream = Environment.class.getResourceAsStream(resource);
		}
		if(stream == null){
			stream = Environment.class.getClassLoader().getResourceAsStream(stripped);
		}
		if(stream == null){
			throw new ZormException(resource+" not found");
		}
        return stream;
	}

}
