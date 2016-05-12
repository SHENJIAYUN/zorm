package com.zorm.event;

import java.io.Serializable;

public interface PreLoadEventListener extends Serializable {
	public void onPreLoad(PreLoadEvent event);
}
