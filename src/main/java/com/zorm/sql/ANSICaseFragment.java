package com.zorm.sql;

import java.util.*;

public class ANSICaseFragment extends CaseFragment {

	public String toFragmentString() {
		
		StringBuilder buf = new StringBuilder( cases.size() * 15 + 10 )
			.append("case");

		Iterator iter = cases.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			buf.append(" when ")
				.append( me.getKey() )
				.append(" is not null then ")
				.append( me.getValue() );
		}
		
		buf.append(" end");

		if (returnColumnName!=null) {
			buf.append(" as ")
				.append(returnColumnName);
		}

		return buf.toString();
	}
	
}