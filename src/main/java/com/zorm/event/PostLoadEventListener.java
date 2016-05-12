package com.zorm.event;

import java.io.Serializable;

public interface PostLoadEventListener extends Serializable {
	public void onPostLoad(PostLoadEvent event);
}
