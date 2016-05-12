package com.zorm.event;

import java.io.Serializable;

import com.zorm.exception.ZormException;

public interface FlushEventListener extends Serializable{
	public void onFlush(FlushEvent event) throws ZormException;
}
