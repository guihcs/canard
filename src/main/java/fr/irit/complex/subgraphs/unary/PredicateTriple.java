package fr.irit.complex.subgraphs.unary;

import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.Set;

public class PredicateTriple extends Triple {


    public PredicateTriple(IRI sub, IRI pred, Resource obj) {
        super(sub, pred, obj, TripleType.PREDICATE);
    }

    @Override
    public void retrieveIRILabels(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        getSubject().retrieveLabels(targetEndpoint);

        if (getObject() instanceof IRI ob) {
            ob.retrieveLabels(targetEndpoint);
        }
    }

    @Override
    public void retrieveTypes(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        getSubject().retrieveTypes(targetEndpoint);

        if (getObject() instanceof IRI ob) {
            ob.retrieveTypes(targetEndpoint);
        }
    }


    @Override
    public int commonPartValue(Triple t) {
        int res = -1;
        if (!(t instanceof PredicateTriple)) return res;

        if (getObject().equals(t.getObject()) && !keepObjectType) {
            res = 3;
        }
        if (getSubject().equals(t.getSubject()) && !keepSubjectType) {
            res = 1;
        }
        return res;
    }

    @Override
    public SimilarityValues compareLabel(Set<String> targetLabels, double threshold) {
        double subjectSimilarity = compareSubjectSimilarity(targetLabels, threshold);
        double objectSimilarity = compareObjectSimilarity( targetLabels, threshold);

        double similarity = subjectSimilarity + objectSimilarity;

        return new SimilarityValues(similarity, subjectSimilarity, 0, objectSimilarity);
    }


    @Override
    public Resource getAnswer() {
        return getPredicate();
    }
}
