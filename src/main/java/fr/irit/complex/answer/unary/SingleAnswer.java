package fr.irit.complex.answer.unary;

import fr.irit.complex.answer.Answer;
import fr.irit.complex.answer.SubgraphResult;
import fr.irit.complex.subgraphs.unary.*;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.IRITypeUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.HashSet;
import java.util.List;
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
        if (res instanceof IRI riri) {
            riri.retrieveLabels(endpointURL);
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
        if (res instanceof IRI riri) {
            riri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }
    }



    @Override
    public Set<SubgraphResult> findCorrespondingSubGraph(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig) {


        return new HashSet<>();


    }





    public Set<Triple> getAllTriples(Set<String> queryLabels, String targetEndpoint, float threshold) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        Set<Triple> triples = new HashSet<>();
        int count = 0;
        for (IRI iri : res.getSimilarIRIs()) {
            if (count >= numberMaxOfExploredAnswers) {
                continue;
            }
            count++;
            triples.addAll(getSubjectTriples(queryLabels, targetEndpoint, iri, threshold));
            triples.addAll(getObjectTriples(queryLabels, targetEndpoint, iri, threshold));
            triples.addAll(getPredicateTriples(queryLabels, targetEndpoint, iri, threshold));
        }
        return triples;
    }


    private Set<Triple> getPredicateTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildPredicateTriplesSelect(value.getValue());

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);
        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            IRI sub =  new IRI("<" + response.get("subject").getValue() + ">");
            String obj = response.get("object").getValue();

            Resource object = new Resource(obj);
            if (object.isIRI()) {
                object = new IRI("<" + obj.replaceAll("[<>]", "") + ">");
            }

            Triple triple = new PredicateTriple(sub, value, object);
            IRI subjectType = IRITypeUtils.findMostSimilarType(sub, targetEndpoint, queryLabels, threshold);
            triple.setSubjectType(subjectType);

            triple.retrieveIRILabels(targetEndpoint);
            triple.retrieveTypes(targetEndpoint);
            triples.add(triple);

        }
        return triples;
    }

    private Set<Triple> getObjectTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildObjectTriplesSelect(value.getValue());

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);
        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            IRI sub =  new IRI("<" + response.get("subject").getValue() + ">");
            IRI pred = new IRI("<" + response.get("predicate").getValue() + ">");

            Triple triple = new ObjectTriple(sub, pred, value);

            IRI subjectType = IRITypeUtils.findMostSimilarType(sub, targetEndpoint, queryLabels, threshold);
            triple.setSubjectType(subjectType);

            triple.retrieveIRILabels(targetEndpoint);
            triple.retrieveTypes(targetEndpoint);
            triples.add(triple);
        }
        return triples;
    }

    private Set<Triple> getSubjectTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildSubjectTriplesSelect(value.getValue());

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);
        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            if (response.get("object").getValue().matches("\"b[0-9]+\"")) continue;

            IRI pred = new IRI("<" + response.get("predicate").getValue() + ">");
            String obj = response.get("object").getValue();
            Resource object = new Resource(obj);
            if (object.isIRI()) {
                object = new IRI("<" + obj.replaceAll("[<>]", "") + ">");
            }

            Triple triple = new SubjectTriple(value, pred, object);

            if(object instanceof IRI to) {
                IRI objectType = IRITypeUtils.findMostSimilarType(to, targetEndpoint, queryLabels, threshold);
                triple.setObjectType(objectType);
            }

            triple.retrieveIRILabels(targetEndpoint);
            triple.retrieveTypes(targetEndpoint);
            triples.add(triple);
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

}
