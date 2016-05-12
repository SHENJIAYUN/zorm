package com.zorm.event;

import java.io.Serializable;

public interface PostDeleteEventListener extends Serializable{
	public void onPostDelete(PostDeleteEvent event);
}
