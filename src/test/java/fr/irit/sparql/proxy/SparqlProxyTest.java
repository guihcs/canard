package fr.irit.sparql.proxy;

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
        Dataset ds = DatasetFactory.create("/home/guilherme/Documents/kg/conference/cmt.owl");
        FusekiServer server = FusekiServer.create()
                .add("/ds", ds, true)
                .build();
        server.start();

        try (RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3330/ds/")) {
            conn.querySelect("prefix cmt: <http://cmt#>" +
                    "prefix rdf: <http://www.w3.org/2000/01/rdf-schema#>" +
                    " SELECT DISTINCT ?s ?p ?o { values ?s { cmt:hasDecision } values ?p {rdf:domain} ?s ?p ?o. ?s rdf:range ?z }", (qs) -> {
                Resource subject = qs.getResource("s");
                System.out.printf("Subject: %s, Predicate: %s, Object: %s\n", subject, qs.get("p"), qs.get("o"));

            });
        }
        server.stop();

    }

    @Test
    public void request() throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://jsonplaceholder.typicode.com/todos/1")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());

    }

}