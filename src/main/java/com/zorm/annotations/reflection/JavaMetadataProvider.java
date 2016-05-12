package com.zorm.annotations.reflection;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Map;

public class JavaMetadataProvider implements MetadataProvider{

	public Map<Object, Object> getDefaults() {
		return Collections.emptyMap();
	}

	public AnnotationReader getAnnotationReader(
			AnnotatedElement annotatedElement) {
		return new JavaAnnotationReader(annotatedElement);
	}

}
