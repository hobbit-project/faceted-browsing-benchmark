package org.aksw.jena_sparql_api.collection;

import org.apache.jena.sparql.core.DatasetGraph;

public class ObservableDatasetGraphImpl {
	protected DatasetGraph delegate;
	
	public ObservableDatasetGraphImpl(DatasetGraph delegate) {
		super();
		this.delegate = delegate;
	}
	
	// FIXME Implement
	
	public static ObservableDatasetGraph decorate(DatasetGraph delegate) {
		return null;
	}
}
