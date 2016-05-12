package com.zorm.persister.entity;

import java.util.Comparator;

public interface OptimisticCacheSource {
	/*
	 * 乐观锁机制
	 */
	public boolean isVersioned();
	
	/*
	 * 获取版本控制的比较器 
	 */
	public Comparator getVersionComparator();
}
