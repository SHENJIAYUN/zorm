package com.zorm.annotations.reflection;

import java.lang.annotation.Annotation;

public interface AnnotationReader {

	public <T extends Annotation> T getAnnotation(Class<T> annotationType);

    public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

    public Annotation[] getAnnotations();
}
