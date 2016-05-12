package com.zorm.event;

import java.io.Serializable;

public interface PreDeleteEventListener extends Serializable{
	public void onPostDelete(PostDeleteEvent event);
}
