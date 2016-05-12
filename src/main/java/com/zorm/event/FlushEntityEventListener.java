package com.zorm.event;

import java.io.Serializable;

import com.zorm.exception.ZormException;

public interface FlushEntityEventListener extends Serializable{
	public void onFlushEntity(FlushEntityEvent event) throws ZormException;
}
