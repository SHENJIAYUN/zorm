package com.zorm.annotations.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.GenerationType;

public class JavaXClass extends JavaXAnnotatedElement implements XClass{
  private final TypeEnvironment context;
  private final Class clazz;
  
  public JavaXClass(Class clazz, TypeEnvironment env, JavaReflectionManager factory) {
		super( clazz, factory );
      this.clazz = clazz; 
      this.context = env;
	}

public String getName() {
	return toClass().getName();
}

public Class<?> toClass() {
	return clazz;
}

public XClass getSuperclass() {
	return getFactory().toXClass(toClass().getSuperclass(),
			CompoundTypeEnvironment.create(getTypeEnvironment(),
					getFactory().getTypeEnvironment(toClass())));
}

TypeEnvironment getTypeEnvironment() {
	return context;
}

public XClass[] getInterfaces() {
	// TODO Auto-generated method stub
	return null;
}

public boolean isInterface() {
	// TODO Auto-generated method stub
	return false;
}

public boolean isAbstract() {
	return Modifier.isAbstract( toClass().getModifiers() );
}

public boolean isPrimitive() {
	// TODO Auto-generated method stub
	return false;
}

public boolean isEnum() {
	// TODO Auto-generated method stub
	return false;
}

public boolean isAssignableFrom(XClass c) {
	// TODO Auto-generated method stub
	return false;
}

public List<XProperty> getDeclaredProperties(String accessType) {
	return getDeclaredProperties(accessType, XClass.DEFAULT_FILTER);
}

public List<XProperty> getDeclaredProperties(String accessType, Filter filter) {
    if(accessType.equals(ACCESS_FIELD)){
    	return getDeclaredFieldProperties(filter);
    }
    //Property是指注解在方法上
    if(accessType.equals(ACCESS_PROPERTY)){
    	return getDeclaredMethodProperties(filter); 
    }
	throw new IllegalArgumentException("Unknown access type " + accessType);
}

private List<XProperty> getDeclaredMethodProperties(Filter filter) {

	List<XProperty> result = new LinkedList<XProperty>();
	for(Method m : toClass().getDeclaredMethods()){
		if(ReflectionUtil.isProperty(m, getTypeEnvironment().bind(m.getGenericReturnType()), filter)){
			//通过相关的setter、getter方法获取属性名，并存储在result当中
			result.add( getFactory().getXProperty( m, getTypeEnvironment() ) );
		}
	}
	return result;
}

private List<XProperty> getDeclaredFieldProperties(Filter filter) {
    List<XProperty> result = new LinkedList<XProperty>();
    for(Field f : toClass().getDeclaredFields()){
    	if(ReflectionUtil.isProperty(f,getTypeEnvironment().bind(f.getGenericType()),filter)){
    		result.add(getFactory().getXProperty(f,getTypeEnvironment()));
    	}
    }
	return result;
}

public List<XMethod> getDeclaredMethods() {
	// TODO Auto-generated method stub
	return null;
}
}
