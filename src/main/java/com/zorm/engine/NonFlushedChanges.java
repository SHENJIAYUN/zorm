package com.zorm.engine;

import java.io.Serializable;

public interface NonFlushedChanges extends Serializable{
  void clear();
}
