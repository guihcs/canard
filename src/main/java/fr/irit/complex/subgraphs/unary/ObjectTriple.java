package fr.irit.complex.subgraphs.unary;

import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.Set;

public class ObjectTriple extends Triple{

    public ObjectTriple(IRI sub, IRI pred, Resource obj) {
        super(sub, pred, obj, TripleType.OBJECT);
    }


    @Override
    public void retrieveIRILabels(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        getSubject().retrieveLabels(targetEndpoint);

        getPredicate().retrieveLabels(targetEndpoint);
    }


    @Override
    public void retrieveTypes(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        getSubject().retrieveTypes(targetEndpoint);

        getPredicate().retrieveTypes(targetEndpoint);
    }

    @Override
    public int commonPartValue(Triple t) {
        int res = -1;
        if (!(t instanceof ObjectTriple)) return res;
        if (getPredicate().equals(t.getPredicate()) && !isPredicateTriple()) {
            res = 2;
        }

        if (getSubject().equals(t.getSubject()) && !isSubjectTriple() && !keepSubjectType) {
            res = 1;
        }
        return res;
    }

    @Override
    public SimilarityValues compareLabel(Set<String> targetLabels, double threshold) {
        double subjectSimilarity = compareSubjectSimilarity(targetLabels, threshold);
        double predicateSimilarity = comparePredicateSimilarity(targetLabels, threshold);

        double similarity = subjectSimilarity + predicateSimilarity;


        return new SimilarityValues(similarity, subjectSimilarity, predicateSimilarity, 0);
    }


    @Override
    public Resource getAnswer() {
        return getObject();
    }
}
