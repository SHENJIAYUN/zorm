package com.zorm.event;

import java.io.Serializable;

public interface PreInsertEventListener extends Serializable{
  public boolean onPreInsert(PreInsertEvent event);
}
