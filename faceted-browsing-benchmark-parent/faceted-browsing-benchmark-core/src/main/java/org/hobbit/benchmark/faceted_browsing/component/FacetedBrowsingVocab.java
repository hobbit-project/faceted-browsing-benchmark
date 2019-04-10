package org.hobbit.benchmark.faceted_browsing.component;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * This is an ad-hoc vocab for exchanging task information;
 * in addition to the attributes below, we (ab)use the following properties for now:
 * rdfs:label is used for the query string
 * rdfs:comment is used for the result set string in json
 * 
 * 
 * @author raven Oct 2, 2017
 *
 */
public class FacetedBrowsingVocab {
    //public static final Property scenarioClassifier = ResourceFactory.createProperty("http://example.org/scenarioClassifier");
    public static final Property scenarioId = ResourceFactory.createProperty("http://www.example.org/scenarioId");
    public static final Property taskId = ResourceFactory.createProperty("http://www.example.org/taskId");
    
    public static final Property sequenceId = ResourceFactory.createProperty("http://www.example.org/sequenceId");
    
    public static final Property queryId = ResourceFactory.createProperty("http://example.org/queryId");
    public static final Property chokepointId = ResourceFactory.createProperty("http://www.example.org/chokepointId");

    
    public static final Property warmup = ResourceFactory.createProperty("http://www.example.org/warmup");

    
    
    public static final Property preconfData = ResourceFactory.createProperty("http://w3id.org/bench#paramPreconfData");
    public static final Property preconfTasks = ResourceFactory.createProperty("http://w3id.org/bench#paramPreconfTasks");

}
