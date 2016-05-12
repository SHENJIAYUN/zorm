package com.zorm.config;

import com.zorm.service.Service;

public interface RegionFactory extends Service{
	public long nextTimestamp();
}
