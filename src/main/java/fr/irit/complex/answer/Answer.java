package fr.irit.complex.answer;

import fr.irit.main.ExecutionConfig;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.HashSet;
import java.util.Set;

public abstract class Answer {

    public void getSimilarIRIs(String targetEndpoint) {
    }

    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
    }

    public void retrieveIRILabels(String endpointURL) {
    }

    public abstract Set<SubgraphResult> findCorrespondingSubGraph(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig);

    public boolean hasMatch() {
        return false;
    }

    public String printMatchedEquivalents() {
        return "";
    }

}
