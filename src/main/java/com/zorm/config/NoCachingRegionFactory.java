package com.zorm.config;

public class NoCachingRegionFactory implements RegionFactory {

	@Override
	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

}
