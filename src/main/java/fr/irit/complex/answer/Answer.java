package fr.irit.complex.answer;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.main.ExecutionConfig;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.HashSet;
import java.util.Set;

public abstract class Answer {
    final HashSet<String> goodTriples;

    public Answer() {
        goodTriples = new HashSet<>();
    }

    public void getSimilarIRIs(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
    }

    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
    }

    public void retrieveIRILabels(String endpointURL) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
    }

    public abstract Set<InstantiatedSubgraph> findCorrespondingSubGraph(SparqlSelect query, ExecutionConfig executionConfig) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException;

    public boolean hasMatch() {
        return false;
    }

    public String printMatchedEquivalents() {
        return "";
    }

}
