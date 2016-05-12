package com.zorm.transaction;

import com.zorm.engine.transaction.spi.LocalStatus;

public interface Transaction {
  public boolean isInitiator();
  public void begin();
  public void commit();
  public void rollback();
  public LocalStatus getLocalStatus();
  public boolean isActive();
  public boolean wasCommitted();
  public boolean wasRolledBack();
  public void setTimeout(int seconds);
  public int getTimeout();
  public boolean isParticipating();
}
