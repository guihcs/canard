package fr.irit.complex.answer.unary;

import fr.irit.complex.answer.Answer;
import fr.irit.complex.answer.SubgraphResult;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.IRIUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.Set;

public class SingleAnswer extends Answer {
    final Resource res;



    public SingleAnswer(Resource r) {
        super();
        res = r;
    }
    @Override
    public void retrieveIRILabels(String endpointURL) {
        if (res instanceof IRI riri) {
            IRIUtils.retrieveLabels(riri, endpointURL);
        }
    }

    @Override
    public Set<SubgraphResult> findCorrespondingSubGraph(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig) {
        return null;
    }

    @Override
    public void getSimilarIRIs(String targetEndpoint) {
        if (res.getSimilarIRIs().isEmpty()) {
            res.findSimilarResource(targetEndpoint);
        }
    }
    @Override
    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (res instanceof IRI riri) {
            riri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }
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
        if (obj instanceof SingleAnswer sa) {
            return res.toValueString().equals(sa.res.toValueString());
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


    public Resource getRes() {
        return res;
    }
}
