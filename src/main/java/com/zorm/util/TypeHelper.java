package com.zorm.util;

import com.zorm.LazyPropertyInitializer;
import com.zorm.entity.StandardProperty;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class TypeHelper {
	private TypeHelper() {
	}
	
	public static int[] findDirty(
			final StandardProperty[] properties,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {
			final boolean dirty = currentState[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& properties[i].isDirtyCheckable( anyUninitializedProperties )
					&& properties[i].getType().isDirty( previousState[i], currentState[i], includeColumns[i], session );
			if ( dirty ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}
	
	public static void deepCopy(
			final Object[] values,
			final Type[] types,
			final boolean[] copy,
			final Object[] target,
			final SessionImplementor session) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( copy[i] ) {
				if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					) {
					target[i] = values[i];
				}
				else {
					target[i] = types[i].deepCopy( values[i], session.getFactory() );
				}
			}
		}
	}

	public static int[] findModified(
			final StandardProperty[] properties,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {
			final boolean modified = currentState[i]!=LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& properties[i].isDirtyCheckable(anyUninitializedProperties)
					&& properties[i].getType().isModified( previousState[i], currentState[i], includeColumns[i], session );

			if ( modified ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}
}
