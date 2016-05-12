package com.zorm.event;

import java.io.Serializable;

import com.zorm.exception.ZormException;

public interface AutoFlushEventListener extends Serializable{
	public void onAutoFlush(AutoFlushEvent event) throws ZormException;
}
