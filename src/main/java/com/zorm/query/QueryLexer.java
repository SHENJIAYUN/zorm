package com.zorm.query;

import java.io.InputStream;
import java.io.Reader;

@SuppressWarnings("unused")
public class QueryLexer extends QueryBaseLexer{
	
	private boolean possibleID = false;
	
	public QueryLexer(InputStream in) {
		super( in );
	}
	
    public  QueryLexer(Reader in) {
        super(in);
    }
    
    protected void setPossibleID(boolean possibleID) {
		this.possibleID = possibleID;
	}
}
