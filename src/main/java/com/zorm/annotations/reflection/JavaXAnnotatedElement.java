package com.zorm.annotations.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

abstract class JavaXAnnotatedElement implements XAnnotatedElement{
	private final JavaReflectionManager factory;
	private final AnnotatedElement annotatedElement;
	
	public JavaXAnnotatedElement(AnnotatedElement annotatedElement, JavaReflectionManager factory) {
        this.factory = factory;
		this.annotatedElement = annotatedElement;
	}

	protected JavaReflectionManager getFactory(){
		return factory;
	}
	
	private AnnotationReader getAnnotationReader() {
        return factory.buildAnnotationReader(annotatedElement);
	}
	
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return getAnnotationReader().getAnnotation( annotationType );
	}
	
	public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
		return getAnnotationReader().isAnnotationPresent( annotationType );
	}
	
	public Annotation[] getAnnotations() {
		return getAnnotationReader().getAnnotations();
	}
	
	AnnotatedElement toAnnotatedElement() {
		return annotatedElement;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( ! ( obj instanceof JavaXAnnotatedElement ) ) return false;
		JavaXAnnotatedElement other = (JavaXAnnotatedElement) obj;
		//FIXME yuk this defeat the type environment
		return annotatedElement.equals( other.toAnnotatedElement() );
	}
	
	@Override
	public int hashCode() {
		return annotatedElement.hashCode();
	}

	@Override
	public String toString() {
		return annotatedElement.toString();
	}
}
