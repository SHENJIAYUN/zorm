package com.zorm.annotations.reflection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;

public interface ReflectionManager {

	public <T> XClass toXClass(Class<T> clazz);

	public Class toClass(XClass xClazz);

	public Method toMethod(XMethod method);

	public <T> XClass classForName(String name, Class<T> caller) throws ClassNotFoundException;

	public XPackage packageForName(String packageName) throws ClassNotFoundException;

	public <T> boolean equals(XClass class1, Class<T> class2);

    public AnnotationReader buildAnnotationReader(AnnotatedElement annotatedElement);

    public Map getDefaults();
}
