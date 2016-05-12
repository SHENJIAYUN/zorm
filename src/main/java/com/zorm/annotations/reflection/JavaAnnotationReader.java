package com.zorm.annotations.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class JavaAnnotationReader implements AnnotationReader {

	protected final AnnotatedElement element;
	
	public JavaAnnotationReader(AnnotatedElement element){
		this.element = element;
	}
	
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return element.getAnnotation(annotationType);
	}

	public <T extends Annotation> boolean isAnnotationPresent(
			Class<T> annotationType) {
		return element.isAnnotationPresent(annotationType);
	}

	public Annotation[] getAnnotations() {
		return element.getAnnotations();
	}

}
