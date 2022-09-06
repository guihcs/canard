package fr.irit.sparql.proxy;

import fr.irit.complex.utils.Parameters;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class SparqlProxyTest {

    @Test
    public void serveFromFile() throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, IOException {
        Dataset d1 = DatasetFactory.create("/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/cmt_100.ttl");
        Dataset d2 = DatasetFactory.create("/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/conference_100.ttl");
        FusekiServer server = FusekiServer.create()
                .add("/cmt_100", d1, true)
                .add("/conference_100", d2, true)
                .build();
        server.start();
//
//        try (RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3330/ds/")) {
//            conn.querySelect("""
//                        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
//                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
//                        PREFIX owl: <http://www.w3.org/2002/07/owl#>
//
//                        SELECT DISTINCT ?x WHERE {
//                        {?x rdfs:seeAlso <http://o1#paper1>.}
//                        UNION{?x owl:sameAs <http://o1#paper1>.}
//                        UNION{?x skos:closeMatch <http://o1#paper1>.}
//                        UNION{?x skos:exactMacth <http://o1#paper1>.}
//                        UNION{<http://o1#paper1> rdfs:seeAlso ?x.}
//                        UNION{<http://o1#paper1> owl:sameAs ?x.}
//                        UNION{<http://o1#paper1> skos:closeMatch ?x.}
//                        UNION{<http://o1#paper1> skos:exactMatch ?x.}
//                        }
//                    """, (qs) -> {
//                Resource subject = qs.getResource("x");
//                System.out.printf("Subject: %s, Predicate: %s, Object: %s\n", subject, qs.get("y"), qs.get("z"));
//
//            });
//        }
//        server.stop();

    }

//    @Test
//    public void request() throws IOException, InterruptedException {
//        HttpClient httpClient = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder(URI.create("https://jsonplaceholder.typicode.com/todos/1")).GET().build();
//
//        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//        System.out.println(response.body());
//
//    }

}