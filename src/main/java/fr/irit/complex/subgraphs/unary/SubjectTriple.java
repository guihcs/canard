package fr.irit.complex.subgraphs.unary;

import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.Set;

public class SubjectTriple extends Triple {


    public SubjectTriple(IRI sub, IRI pred, Resource obj) {
        super(sub, pred, obj, TripleType.SUBJECT);
    }


    @Override
    public void retrieveIRILabels(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        getPredicate().retrieveLabels(targetEndpoint);

        if (getObject() instanceof IRI ob) {
            ob.retrieveLabels(targetEndpoint);
        }

    }


    @Override
    public void retrieveTypes(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        getPredicate().retrieveTypes(targetEndpoint);
        if (getObject() instanceof IRI ob) {
            ob.retrieveTypes(targetEndpoint);
        }

    }

    @Override
    public int commonPartValue(Triple t) {
        int res = -1;
        if (!(t instanceof SubjectTriple)) return res;

        if (getPredicate().equals(t.getPredicate())) {
            res = 2;
        }
        if (getObject().equals(t.getObject()) && !keepObjectType) {
            res = 3;
        }

        return res;
    }

    @Override
    public SimilarityValues compareLabel(Set<String> targetLabels, double threshold) {
        double predicateSimilarity = comparePredicateSimilarity(targetLabels, threshold);
        double objectSimilarity = compareObjectSimilarity( targetLabels, threshold);

        double similarity = predicateSimilarity + objectSimilarity;

        return new SimilarityValues(similarity, 0, predicateSimilarity, objectSimilarity);

    }


    @Override
    public Resource getAnswer() {
        return getSubject();
    }
}
