package org.hobbit.benchmark.faceted_browsing.v2;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.aksw.facete.v3.api.FacetValueCount;
import org.aksw.facete.v3.api.FacetedQuery;
import org.aksw.facete.v3.bgp.api.XFacetedQuery;
import org.aksw.facete.v3.impl.FacetedQueryImpl;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Before;
import org.junit.Test;

public class TestFacetedQuery {
	
	protected FacetedQuery fq;
	
	@Before
	public void beforeTest() {
		Model model = RDFDataMgr.loadModel("path-data.ttl");
		RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(model));		

		Model dataModel = ModelFactory.createDefaultModel();
		XFacetedQuery facetedQuery = dataModel.createResource().as(XFacetedQuery.class);
		FacetedQueryImpl.initResource(facetedQuery);
		
		fq = FacetedQueryImpl.create(facetedQuery, conn);

		//FacetedQueryResource fq = FacetedQueryImpl.create(model, conn);
	}

	/**
	 * Test to ensure that constraints on the same path are combined using OR (rather than AND)
	 * 
	 */
	@Test
	public void testConstraintDisjunction() {
		fq.root().fwd(RDF.type).one().constraints().eq(OWL.Class);
		fq.root().fwd(RDF.type).one().constraints().eq(RDFS.Class);

		System.out.println("Available values: " + fq.root().availableValues().exec().toList().blockingGet());
		
	}
	
	@Test
	public void testSimpleQuery2() {
		Query q = fq.root().fwd(RDF.type).one().availableValues().toConstructQuery().getValue();

		System.out.println("Query: " + q);
		
	}

	
	@Test
	public void testHeteroDimensionalConstraints() {
		fq.root().fwd(RDF.type).one().constraints().eq(OWL.Class);
		System.out.println("Available values: " + fq.root().availableValues().exec().toList().blockingGet());

		fq.root().fwd(RDFS.label).one().constraints().eqStr("ThingA");
		System.out.println("Label Available values: " + fq.root().fwd(RDFS.label).one().availableValues().exec().toList().blockingGet());
	}
	
	@Test
	public void testComplexQuery() {
		System.out.println("Complex Query");
		FacetValueCount fc =
				// -- Faceted Browsing API
				fq.root()
				.fwd(RDF.type).one()
					.constraints()
						.eq(OWL.Class).activate()
					.end()
				.parent()
				.fwd()
				.facetValueCounts()	
				// --- DataQuery API
				.sample()
				.randomOrder()
				.limit(1)
				.exec()
				// --- RxJava API
				.firstElement()
				.timeout(10, TimeUnit.SECONDS)
				.blockingGet();
		System.out.println("FacetValueCount: " + fc);
		
	}

	@Test
	public void testSimpleQuery() {
		System.out.println("Simple Query");
		RDFNode fc =
				// -- Faceted Browsing API
				fq.focus().availableValues()
				// --- DataQuery API
				.sample().randomOrder()
				.limit(1).exec()
				// --- RxJava API
				.firstElement()
				.timeout(10, TimeUnit.SECONDS)
				.blockingGet();
		System.out.println("FacetValueCount: " + fc);
		
	}

	@Test
	public void testNegatedFacetValues() {
		fq.root().fwd(RDF.type).one().constraints().eq(OWL.Class);
		System.out.println("Available values: " + fq.root().availableValues().exec().toList().blockingGet());

		fq.root().fwd(RDFS.label).one().constraints().eqStr("ThingA");
		System.out.println("Label Available values: " + fq.root().fwd(RDFS.label).one().availableValues().exec().toList().blockingGet());
		
		
		// Listing the nonConstrainedValues must not include
		// 'rdf:type owl:Class' and
		// 'rdfs:label
		
		List<FacetValueCount> fvcs = fq.root().fwd().nonConstrainedFacetValueCounts().exec().toList().blockingGet();
		System.out.println("Non constrained values: " + fvcs);
	
		
	}
	
}


// Maybe we can have an intermediate object with domain specific filtering options
// in a fashion like this:
//class FacetValueCountQuery
//	extends DataQuery<FacetValueCount>
//{
//	constraintPredicates()
//}
//

