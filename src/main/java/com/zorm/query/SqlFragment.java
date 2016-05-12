package com.zorm.query;

import java.util.ArrayList;
import java.util.List;

public class SqlFragment extends Node{

	private List embeddedParameters;

	public void addEmbeddedParameter(ParameterSpecification specification) {
		if ( embeddedParameters == null ) {
			embeddedParameters = new ArrayList();
		}
		embeddedParameters.add( specification );
	}

}
