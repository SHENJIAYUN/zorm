package com.zorm.annotations.reflection;

import java.lang.annotation.Annotation;

public interface XAnnotatedElement {

	<T extends Annotation> T getAnnotation(Class<T> annotationType);

	<T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

	Annotation[] getAnnotations();

	/**
	 * Returns true if the underlying artefact
	 * is the same
	 */
	boolean equals(Object x);
}
