package fr.irit.complex.answer;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.Triple;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SingleAnswer extends Answer {
    final Resource res;
    final int numberMaxOfExploredAnswers;


    public SingleAnswer(Resource r) {
        super();
        if (r.isIRI()) {
            res = new IRI("<" + r + ">");
        } else {
            res = r;
        }
        numberMaxOfExploredAnswers = 20;
    }
    @Override
    public void retrieveIRILabels(String endpointURL) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (res instanceof IRI) {
            ((IRI) res).retrieveLabels(endpointURL);
        }
    }
    @Override
    public void getSimilarIRIs(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (res.getSimilarIRIs().isEmpty()) {
            res.findSimilarResource(targetEndpoint);
        }
    }
    @Override
    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (res instanceof IRI) {
            ((IRI) res).findExistingMatches(sourceEndpoint, targetEndpoint);
        }
    }

    @Override
    public Set<InstantiatedSubgraph> findCorrespondingSubGraph(SparqlSelect query, ExecutionConfig executionConfig) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        Set<String> queryLabels = new HashSet<>();

        for (Map.Entry<String, IRI> iri : query.getIRIList().entrySet()) {
            queryLabels.addAll(iri.getValue().getLabels());
        }

        double maxSim = -1;
        Triple bestTriple = new Triple();
        Set<InstantiatedSubgraph> goodTriples = new HashSet<>();


        int count = 0;
        for (IRI iri : res.getSimilarIRIs()) {
            if (count < numberMaxOfExploredAnswers) {
                count++;
                double localMaxSim = -1;
                Set<Triple> triples = retrieveAllTriples(executionConfig.getTargetEndpoint(), iri.getValue());
                for (Triple t : triples) {
                    double similarity = 0;
                    t.retrieveIRILabels(executionConfig.getTargetEndpoint());
                    similarity += t.compareLabel(queryLabels, executionConfig.getSimilarityThreshold(), executionConfig.getTargetEndpoint());

                    if (similarity > maxSim) {
                        maxSim = similarity;
                        bestTriple = t;
                    }

                    if (similarity > localMaxSim) {
                        localMaxSim = similarity;
                    }

                    if (similarity >= 0.6) {
                        goodTriples.add(t);
                    }
                }
            }
        }
        if (goodTriples.isEmpty() && !bestTriple.isNullTriple()) {
            goodTriples.add(bestTriple);
        }
        return goodTriples;
    }

    private Set<Triple> retrieveAllTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        triples.addAll(getSubjectTriples(targetEndpoint, value));
        triples.addAll(getObjectTriples(targetEndpoint, value));
        triples.addAll(getPredicateTriples(targetEndpoint, value));
        return triples;
    }

    private Set<Triple> getPredicateTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();

        String query = "SELECT ?subject ?object WHERE {" +
                "?subject " + value + " ?object."
                + "} LIMIT 500";

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        ArrayList<JsonNode> ret = spIn.getResponse(query);

        for (JsonNode response : ret) {
            String sub = response.get("subject").get("value").toString().replaceAll("\"", "");
            String obj = response.get("object").get("value").toString().replaceAll("\"", "");
            triples.add(new Triple("<" + sub + ">", value, obj, 2));
        }
        return triples;
    }

    private Set<Triple> getObjectTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = "SELECT ?subject ?predicate WHERE {" +
                "?subject ?predicate " + value + "."
                + "MINUS{ ?subject <http://www.w3.org/2002/07/owl#sameAs> " + value + ".}"
                + "MINUS{ ?subject <http://www.w3.org/2004/02/skos/core#closeMatch> " + value + ".}"
                + "MINUS{ ?subject <http://www.w3.org/2004/02/skos/core#exactMatch> " + value + ".}"
                + "}LIMIT 500";

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        ArrayList<JsonNode> ret = spIn.getResponse(query);

        for (JsonNode response : ret) {
            String sub = response.get("subject").get("value").toString().replaceAll("\"", "");
            String pred = response.get("predicate").get("value").toString().replaceAll("\"", "");
            triples.add(new Triple("<" + sub + ">", "<" + pred + ">", value, 3));
        }
        return triples;
    }

    private Set<Triple> getSubjectTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = "SELECT ?predicate ?object WHERE {" +
                value + " ?predicate ?object."
                + "MINUS{ " + value + " <http://www.w3.org/2002/07/owl#sameAs> ?object.}"

                + "}LIMIT 500";

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        ArrayList<JsonNode> ret = spIn.getResponse(query);

        for (JsonNode response : ret) {
            if (!response.get("object").get("value").toString().matches("\"b[0-9]+\"")) {
                String pred = response.get("predicate").get("value").toString().replaceAll("\"", "");
                String obj = response.get("object").get("value").toString().replaceAll("\"", "");
                triples.add(new Triple(value, "<" + pred + ">", obj, 1));
            }

        }
        return triples;
    }


    @Override
    public String toString() {
        return res.toValueString();
    }
    @Override
    public int hashCode() {
        return res.toValueString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SingleAnswer) {
            return res.toValueString().equals(((SingleAnswer) obj).res.toValueString());
        } else {
            return false;
        }
    }
    @Override
    public boolean hasMatch() {
        return !res.getSimilarIRIs().isEmpty();
    }
    @Override
    public String printMatchedEquivalents() {
        return res.getSimilarIRIs().toString();
    }

}
