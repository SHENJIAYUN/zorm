package com.zorm.mapping;

import com.zorm.FetchMode;

public interface Fetchable {
	public FetchMode getFetchMode();
	public void setFetchMode(FetchMode joinedFetch);
	public boolean isLazy();
	public void setLazy(boolean lazy);
}
