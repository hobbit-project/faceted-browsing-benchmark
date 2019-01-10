package org.hobbit.benchmark.faceted_browsing.v2.main;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.connection.QueryExecutionFactorySparqlQueryConnection;
import org.aksw.jena_sparql_api.core.connection.SparqlQueryConnectionJsa;
import org.aksw.jena_sparql_api.core.utils.RDFDataMgrRx;
import org.aksw.jena_sparql_api.core.utils.UpdateRequestUtils;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.utils.DatasetDescriptionUtils;
import org.aksw.jena_sparql_api.utils.model.ResourceUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionModular;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.update.UpdateRequest;
import org.hobbit.benchmark.faceted_browsing.component.FacetedBrowsingVocab;
import org.hobbit.benchmark.faceted_browsing.v2.task_generator.TaskGenerator;
import org.hobbit.core.service.docker.api.DockerService;
import org.hobbit.core.service.docker.api.DockerServiceSystem;
import org.hobbit.core.service.docker.impl.docker_client.DockerServiceSystemDockerClient;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.exceptions.DockerCertificateException;

import io.reactivex.Flowable;

public class MainFacetedBrowsingBenchmarkV2Run {
	public static void main(String[] args) throws DockerCertificateException, Exception {

//		Dataset raw = DatasetFactory.create();
		
		//.forEach(d -> Streams.stream(d.asDatasetGraph().find()).forEach(raw.asDatasetGraph()::add));

		
//		
//		Model model = RDFDataMgr.loadModel(uri);
//		RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(model));
//
//		taskGenerator = TaskGenerator.autoConfigure(conn);
//		changeTracker = taskGenerator.getChangeTracker();
//		fq = taskGenerator.getCurrentQuery();
//		changeTracker.commitChangesWithoutTracking();
//		
		
		
		try (DockerServiceSystem<?> dss = DockerServiceSystemDockerClient.create(true, Collections.emptyMap(), Collections.emptySet())) {

			DockerService ds = dss.create("docker-service-example", "tenforce/virtuoso", ImmutableMap.<String, String>builder()
					.put("SPARQL_UPDATE", "true")
					.put("DEFAULT_GRAPH", "http://www.example.org/")
					.build());
			
			try {
				ds.startAsync().awaitRunning();

				// Give the sparql endpoint 5 seconds to come up
				// TODO Add another example for wrapping with health checks
				Thread.sleep(10000);
				
				// Load a subset of the data
				
				
				String sparqlApiBase = "http://" + ds.getContainerId() + ":8890/";
				String sparqlEndpoint = sparqlApiBase + "sparql";

				dss.findServiceByName("docker-service-example");
				
				
				try(RDFConnection rawConn = RDFConnectionFactory.connect(sparqlEndpoint)) {
					
					// Wrap the connection to use a different content type for queries...
					// Jena rejects some of Virtuoso's json output
					@SuppressWarnings("resource")
					RDFConnection conn =
							new RDFConnectionModular(new SparqlQueryConnectionJsa(
									FluentQueryExecutionFactory
										.from(new QueryExecutionFactorySparqlQueryConnection(rawConn))
										.config()
										.withDatasetDescription(DatasetDescriptionUtils.createDefaultGraph("http://example.org/"))
										.withPostProcessor(qe -> {
											if(qe instanceof QueryEngineHTTP) {
												((QueryEngineHTTP)qe).setSelectContentType(WebContent.contentTypeResultsXML);
											}
										})
										.end()
										.create()
										), rawConn, rawConn);

					
					
					Flowable<Dataset> flow = RDFDataMgrRx.createFlowableDatasets(
							() -> new FileInputStream("/home/raven/Projects/Data/Hobbit/hobbit-sensor-stream-150k.trig"),
							Lang.TRIG,
							"http://www.example.org/");
					
					Iterator<Dataset> it = flow.blockingNext().iterator();
					for(int i = 0; i < 1000 && it.hasNext(); ++i) {
						Dataset batch = it.next();
						Model m = batch.getUnionModel();
						
						UpdateRequest ur = UpdateRequestUtils.createUpdateRequest(m, null);
						UpdateRequestUtils.applyWithIri(ur, "http://example.org/");
						System.out.println("Update request: " + ur);
						conn.update(ur);
					}
					
					
					// One time auto config based on available data
					TaskGenerator taskGenerator = TaskGenerator.autoConfigure(conn);
					
					// Now wrap the scenario supplier with the injection of sparql update statements
					
					Callable<SparqlTaskResource> querySupplier = taskGenerator.createScenarioQuerySupplier();
					Callable<SparqlTaskResource> updateSupplier = () -> null;
					
					
					Callable<SparqlTaskResource> taskSupplier = SupplierUtils.interleave(querySupplier, updateSupplier);

					
					
					SparqlTaskResource tmp = null;
					
					//List<String> 
					for(int i = 0; (tmp = taskSupplier.call()) != null; ++i) {			
						int scenarioId = ResourceUtils.tryGetLiteralPropertyValue(tmp, FacetedBrowsingVocab.scenarioId, Integer.class)
							.orElseThrow(() -> new RuntimeException("no scenario id"));
						
						System.out.println("GENERATED TASK: " + tmp.getURI());
						RDFDataMgr.write(System.out, tmp.getModel(), RDFFormat.TURTLE_PRETTY);
						if(scenarioId >= 10) {
							break;
						}
						
						SparqlStmt stmt = SparqlTaskResource.parse(tmp);
						//System.out.println(i + ": " + SparqlTaskResource.parse(tmp));
						
						System.out.println("Query: " + stmt);
						SparqlStmtUtils.execAny(conn, stmt);
					}
					
					System.out.println("DONE");
					
					
//					conn.update("PREFIX eg: <http://www.example.org/> INSERT DATA { GRAPH <http://www.example.org/> { eg:s eg:p eg:o } }");
//
//					Model model = conn.queryConstruct("CONSTRUCT FROM <http://www.example.org/> WHERE { ?s ?p ?o }");
//					RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
				}
				
			} finally {
				ds.stopAsync().awaitTerminated(30, TimeUnit.SECONDS);
			}
			
			
		}
		
	}
}
