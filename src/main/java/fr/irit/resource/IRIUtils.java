package fr.irit.resource;

import fr.irit.complex.subgraphs.binary.Path;
import fr.irit.complex.subgraphs.unary.*;
import fr.irit.complex.utils.Utils;
import fr.irit.input.CQAManager;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;
import java.util.regex.Matcher;

public class IRIUtils {

    public static IRI findMostSimilarType(IRI iri, String endpointUrl, Set<String> targetLabels, double threshold) {
        double scoreTypeMax = -1;
        IRI finalType = null;

        try {
            if (iri.getTypes().isEmpty()) {
                IRIUtils.retrieveTypes(iri, endpointUrl);
            }

            for (IRI type : iri.getTypes()) {
                double scoreType;
                IRIUtils.retrieveLabels(type, endpointUrl);
                scoreType = Utils.similarity(type.getLabels(), targetLabels, threshold);
                if (scoreTypeMax < scoreType) {
                    scoreTypeMax = scoreType;
                    finalType = type;
                }
            }
        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }

        return finalType;
    }


    public static void retrieveTypes(IRI iri, String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        String query = SparqlSelect.buildTypesSelect(iri.getValue());
        SparqlSelect sq = new SparqlSelect(query);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(sq.getMainQueryWithPrefixes());

        for (Map<String, SelectResponse.Results.Binding> jsonNode : ret) {
            String s = jsonNode.get("type").getValue();
            iri.getTypes().add(new IRI("<" + s + ">"));
        }

        for (IRI type : iri.getTypes()) {
            IRIUtils.retrieveLabels(type, endpointUrl);
        }
    }


    public static Set<Triple> getAllTriples(Resource resource, Set<String> queryLabels, String targetEndpoint, float threshold) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        Set<Triple> triples = new HashSet<>();
        int count = 0;
        final int numberMaxOfExploredAnswers = 20;
        for (IRI iri : resource.getSimilarIRIs()) {
            if (count >= numberMaxOfExploredAnswers) continue;
            count++;
            triples.addAll(getSubjectTriples(queryLabels, targetEndpoint, iri, threshold));
            triples.addAll(getObjectTriples(queryLabels, targetEndpoint, iri, threshold));
            triples.addAll(getPredicateTriples(queryLabels, targetEndpoint, iri, threshold));
        }
        return triples;
    }


    private static Set<Triple> getPredicateTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
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

            Triple triple = new Triple(sub, value, object, TripleType.PREDICATE);
            IRI subjectType = IRIUtils.findMostSimilarType(sub, targetEndpoint, queryLabels, threshold);
            triple.setSubjectType(subjectType);


            IRIUtils.retrieveLabels(triple.getSubject(), targetEndpoint);

            if (triple.getObject() instanceof IRI ob) {
                IRIUtils.retrieveLabels(ob, targetEndpoint);
            }
            IRIUtils.retrieveTypes(triple.getSubject(), targetEndpoint);

            if (triple.getObject() instanceof IRI ob) {
                IRIUtils.retrieveTypes(ob, targetEndpoint);
            }
            triples.add(triple);

        }
        return triples;
    }

    private static Set<Triple> getObjectTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildObjectTriplesSelect(value.getValue());

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);
        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            IRI sub =  new IRI("<" + response.get("subject").getValue() + ">");
            IRI pred = new IRI("<" + response.get("predicate").getValue() + ">");

            Triple triple = new Triple(sub, pred, value, TripleType.OBJECT);

            IRI subjectType = IRIUtils.findMostSimilarType(sub, targetEndpoint, queryLabels, threshold);
            triple.setSubjectType(subjectType);

            IRIUtils.retrieveLabels(triple.getSubject(), targetEndpoint);
            IRIUtils.retrieveLabels(triple.getPredicate(), targetEndpoint);

            IRIUtils.retrieveTypes(triple.getSubject(), targetEndpoint);
            IRIUtils.retrieveTypes(triple.getPredicate(), targetEndpoint);
            triples.add(triple);
        }
        return triples;
    }

    private static Set<Triple> getSubjectTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
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

            Triple triple = new Triple(value, pred, object, TripleType.SUBJECT);

            if(object instanceof IRI to) {
                IRI objectType = IRIUtils.findMostSimilarType(to, targetEndpoint, queryLabels, threshold);
                triple.setObjectType(objectType);
            }

            IRIUtils.retrieveLabels(triple.getPredicate(), targetEndpoint);

            if (triple.getObject() instanceof IRI ob) {
                IRIUtils.retrieveLabels(ob, targetEndpoint);
            }

            IRIUtils.retrieveTypes(triple.getPredicate(), targetEndpoint);
            if (triple.getObject() instanceof IRI ob) {
                IRIUtils.retrieveTypes(ob, targetEndpoint);
            }
            triples.add(triple);
        }

        return triples;
    }


    public static double compareLabel(Path p, Set<String> targetLabels, double threshold, String targetEndpoint) {
        double similarity = 0;
        for (IRI prop : p.getProperties()) {
            try {
                IRIUtils.retrieveLabels(prop, targetEndpoint);
                similarity += Utils.similarity(prop.getLabels(), targetLabels, threshold);
            } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < p.getEntities().size(); i++) {
            Resource ent = p.getEntities().get(i);
            if (ent instanceof IRI) {
                IRI type = p.getTypes().get(i);
                if (type != null) {
                    double scoreType = Utils.similarity(type.getLabels(), targetLabels, threshold);
                    if (scoreType > 0.5) {
                        p.setTypeSimilarity(p.getTypeSimilarity() + scoreType);
                    } else {
                        p.getTypes().set(i, null);
                    }
                }
            }
        }
        if (p.pathFound()) {
            similarity += 0.5;
        }

        p.setSimilarity(similarity);
        return similarity;
    }


    public static SimilarityValues compareLabel(Triple triple, Set<String> targetLabels, double threshold) {
        double subjectSimilarity = 0;
        double predicateSimilarity = 0;
        double objectSimilarity = 0;


        if (triple.getType() != TripleType.SUBJECT) {
            subjectSimilarity = IRIUtils.compareSubjectSimilarity(triple, targetLabels, threshold);
        }

        if (triple.getType() != TripleType.PREDICATE) {
            predicateSimilarity = IRIUtils.comparePredicateSimilarity(triple, targetLabels, threshold);
        }

        if (triple.getType() != TripleType.OBJECT) {
            objectSimilarity = IRIUtils.compareObjectSimilarity(triple, targetLabels, threshold);
        }

        double similarity = subjectSimilarity + predicateSimilarity + objectSimilarity;


        return new SimilarityValues(similarity, subjectSimilarity, predicateSimilarity, 0);
    }

    public static double compareSubjectSimilarity(Triple triple, Set<String> targetLabels, double threshold){
        double scoreTypeSubMax = 0;
        double subjectSimilarity;


        if (triple.getSubjectType() != null) {
            scoreTypeSubMax = Utils.similarity(triple.getSubjectType().getLabels(), targetLabels, threshold);
        }
        subjectSimilarity = Utils.similarity(triple.getSubject().getLabels(), targetLabels, threshold);

        if (scoreTypeSubMax > subjectSimilarity) {
            triple.setKeepSubjectType(true);
            subjectSimilarity = scoreTypeSubMax;
        }
        return subjectSimilarity;
    }

    public static double comparePredicateSimilarity(Triple triple, Set<String> targetLabels, double threshold){
        if (triple.getPredicate().isType()) return 0;
        return Utils.similarity(triple.getPredicate().getLabels(), targetLabels, threshold);
    }

    public static double compareObjectSimilarity(Triple triple, Set<String> targetLabels, double threshold){
        double objectSimilarity = 0;
        if(triple.getObject() instanceof IRI to) {

            if (triple.getObjectType() != null) {
                double scoreTypeObMax = Utils.similarity(triple.getObjectType().getLabels(), targetLabels, threshold);
                objectSimilarity = Utils.similarity(to.getLabels(), targetLabels, threshold);
                if (scoreTypeObMax > objectSimilarity) {
                    triple.setKeepObjectType(true);
                    objectSimilarity = scoreTypeObMax;
                }
            }
        } else {
            Set<String> hashObj = new HashSet<>();
            hashObj.add(triple.getObject().toString());
            objectSimilarity = Utils.similarity(hashObj, targetLabels, threshold);
        }
        return objectSimilarity;
    }



    public static void retrieveLabels(IRI iri, String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (iri.isLabelsGot()) return;

        Matcher matcher = iri.getPattern().matcher(iri.getValue());

        if (matcher.find()) {
            iri.addLabel(matcher.group(2));
        } else {
            iri.addLabel(iri.getValue());
        }


        Map<String, String> substitution = new HashMap<>();
        substitution.put("uri", iri.getValue());
        String literalQuery = CQAManager.getInstance().getLabelQuery(endpointUrl, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(literalQuery);

        for (Map<String, SelectResponse.Results.Binding> jsonNode : ret) {
            String s = jsonNode.get("x").getValue();
            Resource res = new Resource(s);
            if (!res.isIRI()) {
                iri.addLabel(s);
            }

        }
        iri.setLabelsGot(true);
    }

}
