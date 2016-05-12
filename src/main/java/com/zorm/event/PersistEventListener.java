package com.zorm.event;

import java.io.Serializable;
import java.util.Map;

import com.zorm.exception.ZormException;

public interface PersistEventListener extends Serializable{
	public void onPersist(PersistEvent event) throws ZormException;
	public void onPersist(PersistEvent event, Map createdAlready) throws ZormException;
}
