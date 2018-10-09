package org.aksw.facete.v3.impl;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;

interface PathTraitString<T extends PathTrait<T>>
	extends PathTrait<T>
{
	// The following commented-out method must be implemented:
	// T step(String p, boolean isFwd);
	
	default T fwd(Resource p) { return fwd(p.asNode()); }
	default T fwd(Node p) { return fwd(p.getURI()); }
	default T fwd(String p) { return step(p, true); }
	
	default T bwd(Resource p) { return bwd(p.asNode()); }
	default T bwd(Node p) { return bwd(p.getURI()); }
	default T bwd(String p) { return step(p, false); }
	
	default T step(Resource p, boolean isFwd) {
		return step(p.asNode(), isFwd);
	}
	
	default T step(Node p, boolean isFwd) {
		return step(p.getURI(), isFwd);
	}
}