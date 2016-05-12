package com.zorm.annotations.reflection;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;

public class JPAMetadataProvider implements MetadataProvider ,Serializable{
	private transient MetadataProvider delegate = new JavaMetadataProvider();
	private transient Map<Object, Object> defaults;
	private transient Map<AnnotatedElement, AnnotationReader> cache = new HashMap<AnnotatedElement, AnnotationReader>(100);
	@Override
	public Map<Object, Object> getDefaults() {
		return null;
	}

	@Override
	public AnnotationReader getAnnotationReader(
			AnnotatedElement annotatedElement) {
		AnnotationReader reader = cache.get( annotatedElement );
		if (reader == null) {
		    reader = delegate.getAnnotationReader( annotatedElement );
			
			cache.put(annotatedElement, reader);
		}
		return reader;
	}

}
