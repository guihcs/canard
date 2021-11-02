package fr.irit.complex.answer.unary;

import fr.irit.complex.answer.Answer;
import fr.irit.complex.answer.SubgraphResult;
import fr.irit.complex.subgraphs.unary.SimilarityValues;
import fr.irit.complex.subgraphs.unary.Triple;
import fr.irit.complex.subgraphs.unary.TripleType;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
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
    public Set<SubgraphResult> findCorrespondingSubGraph(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {


        return new HashSet<>();


    }





    public Set<Triple> getAllTriples(String targetEndpoint) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        Set<Triple> triples = new HashSet<>();
        int count = 0;
        for (IRI iri : res.getSimilarIRIs()) {
            if (count >= numberMaxOfExploredAnswers) {
                continue;
            }
            count++;
            triples.addAll(getSubjectTriples(targetEndpoint, iri.getValue()));
            triples.addAll(getObjectTriples(targetEndpoint, iri.getValue()));
            triples.addAll(getPredicateTriples(targetEndpoint, iri.getValue()));
        }
        return triples;
    }


    private Set<Triple> getPredicateTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();

        String query = SparqlSelect.buildPredicateTriplesSelect(value);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            String sub = response.get("subject").getValue().replaceAll("\"", "");
            String obj = response.get("object").getValue().replaceAll("\"", "");
            triples.add(new Triple("<" + sub + ">", value, obj, TripleType.PREDICATE));
        }
        return triples;
    }

    private Set<Triple> getObjectTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildObjectTriplesSelect(value);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            String sub = response.get("subject").getValue().replaceAll("\"", "");
            String pred = response.get("predicate").getValue().replaceAll("\"", "");
            triples.add(new Triple("<" + sub + ">", "<" + pred + ">", value, TripleType.OBJECT));
        }
        return triples;
    }

    private Set<Triple> getSubjectTriples(String targetEndpoint, String value) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildSubjectTriplesSelect(value);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            if (!response.get("object").getValue().matches("\"b[0-9]+\"")) {
                String pred = response.get("predicate").getValue().replaceAll("\"", "");
                String obj = response.get("object").getValue().replaceAll("\"", "");
                triples.add(new Triple(value, "<" + pred + ">", obj, TripleType.SUBJECT));
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
