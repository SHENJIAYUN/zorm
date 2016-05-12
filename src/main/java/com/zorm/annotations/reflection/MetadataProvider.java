package com.zorm.annotations.reflection;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

public interface MetadataProvider {

	/**
	 * provide default metadata
	 */
	Map<Object, Object> getDefaults();

	/**
	 * provide metadata for a gien annotated element
	 */
	AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement);
}
