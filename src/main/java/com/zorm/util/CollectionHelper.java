package com.zorm.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zorm.config.UniqueConstraintHolder;
import com.zorm.service.ServiceBinding;

public final class CollectionHelper {

	public static final int MINIMUM_INITIAL_CAPACITY = 16;
	public static final float LOAD_FACTOR = 0.75f;
	public static int determineProperSizing(
			int numberOfElements) {
		int actual = ( (int) (numberOfElements / LOAD_FACTOR) ) + 1;
		return Math.max( actual, MINIMUM_INITIAL_CAPACITY );
	}
	
	public static ConcurrentHashMap<Class, ServiceBinding> concurrentMap(int expectedNumberOfElements) {
		return concurrentMap( expectedNumberOfElements, LOAD_FACTOR );
	}

	public static <K,V> ConcurrentHashMap<K,V> concurrentMap(int expectedNumberOfElements, float loadFactor) {
		final int size = expectedNumberOfElements + 1 + (int) ( expectedNumberOfElements * loadFactor );
		return new ConcurrentHashMap<K, V>( size, loadFactor );
	}
	
	public static <T> List<T> arrayList(int anticipatedSize) {
		return new ArrayList<T>( anticipatedSize );
	}
	public static boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}
	public static int determineProperSizing(Map original) {
		return determineProperSizing( original.size() );
	}
}
