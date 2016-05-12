package com.zorm.annotations.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class TypeFactory {
	static ParameterizedType createParameterizedType(
			final Type rawType, final Type[] substTypeArgs,
			final Type ownerType
	) {
		return new ParameterizedType() {

			public Type[] getActualTypeArguments() {
				return substTypeArgs;
			}

			public Type getRawType() {
				return rawType;
			}

			public Type getOwnerType() {
				return ownerType;
			}

			@Override
			public boolean equals(Object obj) {
				if ( !( obj instanceof ParameterizedType ) ) {
					return false;
				}
				ParameterizedType other = (ParameterizedType) obj;
				return Arrays.equals( getActualTypeArguments(), other.getActualTypeArguments() )
						&& safeEquals( getRawType(), other.getRawType() ) && safeEquals(
						getOwnerType(), other.getOwnerType()
				);
			}

			@Override
			public int hashCode() {
				return safeHashCode( getActualTypeArguments() ) ^ safeHashCode( getRawType() ) ^ safeHashCode(
						getOwnerType()
				);
			}
		};
	}

	static Type createArrayType(Type componentType) {
		if ( componentType instanceof Class ) {
			return Array.newInstance( (Class) componentType, 0 ).getClass();
		}
		return TypeFactory.createGenericArrayType( componentType );
	}

	private static GenericArrayType createGenericArrayType(final Type componentType) {
		return new GenericArrayType() {

			public Type getGenericComponentType() {
				return componentType;
			}

			@Override
			public boolean equals(Object obj) {
				if ( !( obj instanceof GenericArrayType ) ) {
					return false;
				}
				GenericArrayType other = (GenericArrayType) obj;
				return safeEquals( getGenericComponentType(), other.getGenericComponentType() );
			}

			@Override
			public int hashCode() {
				return safeHashCode( getGenericComponentType() );
			}
		};
	}

	private static boolean safeEquals(Type t1, Type t2) {
		if ( t1 == null ) {
			return t2 == null;
		}
		return t1.equals( t2 );
	}

	private static int safeHashCode(Object o) {
		if ( o == null ) {
			return 1;
		}
		return o.hashCode();
	}
}
