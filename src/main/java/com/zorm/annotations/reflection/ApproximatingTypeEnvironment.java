package com.zorm.annotations.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class ApproximatingTypeEnvironment implements TypeEnvironment {

	public Type bind(final Type type) {
		Type result = fineApproximation( type );
		assert TypeUtils.isResolved( result );
		return result;
	}

	private Type fineApproximation(final Type type) {
		return new TypeSwitch<Type>() {
			public Type caseWildcardType(WildcardType wildcardType) {
				return wildcardType;
			}

			@Override
			public Type caseClass(Class classType) {
				return classType;
			}

			@Override
			public Type caseGenericArrayType(GenericArrayType genericArrayType) {
				if ( TypeUtils.isResolved( genericArrayType ) ) {
					return genericArrayType;
				}
				Type componentType = genericArrayType.getGenericComponentType();
				Type boundComponentType = bind( componentType );
				if ( boundComponentType instanceof Class ) {
					return Array.newInstance( (Class) boundComponentType, 0 ).getClass();
				}
				// fall back to coarse approximation, because I found no standard way
				// to instance arrays of a generic type
				return Object[].class;
			}

			@Override
			public Type caseParameterizedType(ParameterizedType parameterizedType) {
				if ( TypeUtils.isResolved( parameterizedType ) ) {
					return parameterizedType;
				}

				if ( !TypeUtils.isCollection( parameterizedType ) ) {
					return Object.class; // fall back to coarse approximation
				}

				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				Type[] approximatedTypeArguments = new Type[typeArguments.length];
				for ( int i = 0; i < typeArguments.length ; i++ ) {
					approximatedTypeArguments[i] = coarseApproximation( typeArguments[i] );
				}

				return TypeFactory.createParameterizedType(
						bind( parameterizedType.getRawType() ),
						approximatedTypeArguments,
						parameterizedType.getOwnerType()
				);
			}

			@Override
			public Type defaultCase(Type t) {
				return coarseApproximation( t );
			}
		}.doSwitch( type );
	}

	private Type coarseApproximation(final Type type) {
		Type result = new TypeSwitch<Type>() {
			public Type caseWildcardType(WildcardType wildcardType) {
				return approximateTo( wildcardType.getUpperBounds() );
			}

			@Override
			public Type caseGenericArrayType(GenericArrayType genericArrayType) {
				if ( TypeUtils.isResolved( genericArrayType ) ) {
					return genericArrayType;
				}
				return Object[].class;
			}

			@Override
			public Type caseParameterizedType(ParameterizedType parameterizedType) {
				if ( TypeUtils.isResolved( parameterizedType ) ) {
					return parameterizedType;
				}
				return Object.class;
			}

			@Override
			public Type caseTypeVariable(TypeVariable typeVariable) {
				return approximateTo( typeVariable.getBounds() );
			}

			private Type approximateTo(Type[] bounds) {
				if ( bounds.length != 1 ) {
					return Object.class;
				}
				return coarseApproximation( bounds[0] );
			}

			@Override
			public Type defaultCase(Type t) {
				return t;
			}
		}.doSwitch( type );
		assert TypeUtils.isResolved( result );
		return result;
	}
    
    @Override
    public String toString() {
        return "approximated_types";
    }

}
