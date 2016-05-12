package com.zorm.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.exception.PropertyNotFoundException;
import com.zorm.property.BasicPropertyAccessor;
import com.zorm.property.DirectPropertyAccessor;
import com.zorm.property.Getter;
import com.zorm.property.PropertyAccessor;
import com.zorm.tuple.EntityTuplizer;

public final class ReflectHelper {

	private static final Method OBJECT_EQUALS;
	private static final Method OBJECT_HASHCODE;
	
	public static final Class[] SINGLE_OBJECT_PARAM_SIGNATURE = new Class[] { Object.class };
	public static final Class[] NO_PARAM_SIGNATURE = new Class[0];
	private static final PropertyAccessor BASIC_PROPERTY_ACCESSOR = new BasicPropertyAccessor();
	private static final PropertyAccessor DIRECT_PROPERTY_ACCESSOR = new DirectPropertyAccessor();
	
	static {
		Method eq;
		Method hash;
		try {
			eq = extractEqualsMethod( Object.class );
			hash = extractHashCodeMethod( Object.class );
		}
		catch ( Exception e ) {
			throw new AssertionFailure( "Could not find Object.equals() or Object.hashCode()", e );
		}
		OBJECT_EQUALS = eq;
		OBJECT_HASHCODE = hash;
	}
	
	private ReflectHelper(){}
	
	private static Method extractEqualsMethod(Class<Object> class1) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Method extractHashCodeMethod(Class<Object> class1) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Class classForName(String className) throws ClassNotFoundException{
        try{
        	ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        	if(contextClassLoader != null){
        		return contextClassLoader.loadClass(className);
        	}
        }catch(Throwable ignore){}
		return Class.forName(className);
	}

	public static Class classForName(String name, Class caller) throws ClassNotFoundException {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass( name );
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name, true, caller.getClassLoader() );
	}
	
	public static boolean isAbstractClass(Class clazz) {
		int modifier = clazz.getModifiers();
		return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
	}
	
	public static boolean isPublic(Class clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Constructor getDefaultConstructor(Class clazz) throws PropertyNotFoundException {
		if ( isAbstractClass( clazz ) ) {
			return null;
		}

		try {
			Constructor constructor = clazz.getDeclaredConstructor( NO_PARAM_SIGNATURE );
			if ( !isPublic( clazz, constructor ) ) {
				constructor.setAccessible( true );
			}
			return constructor;
		}
		catch ( NoSuchMethodException nme ) {
			throw new PropertyNotFoundException(
					"Object class [" + clazz.getName() + "] must declare a default (no-argument) constructor"
			);
		}
	}

	public static boolean isFinalClass(Class clazz) {
		return Modifier.isFinal( clazz.getModifiers() );
	}

	public static boolean isPublic(Member member) {
		return isPublic( member.getDeclaringClass(), member );
	}
	
	public static Object getConstantValue(String name) {
		Class clazz;
		try {
			clazz = classForName( StringHelper.qualifier( name ) );
		}
		catch ( Throwable t ) {
			return null;
		}
		try {
			return clazz.getField( StringHelper.unqualify( name ) ).get( null );
		}
		catch ( Throwable t ) {
			return null;
		}
	}
	
	public static Class reflectedPropertyClass(String className, String name) throws MappingException {
		try {
			Class clazz = ReflectHelper.classForName( className );
			return getter( clazz, name ).getReturnType();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new MappingException( "class " + className + " not found while looking for property: " + name, cnfe );
		}
	}
	
	private static Getter getter(Class clazz, String name) throws MappingException {
		try {
			return BASIC_PROPERTY_ACCESSOR.getGetter( clazz, name );
		}
		catch ( PropertyNotFoundException pnfe ) {
			return DIRECT_PROPERTY_ACCESSOR.getGetter( clazz, name );
		}
	}

}
