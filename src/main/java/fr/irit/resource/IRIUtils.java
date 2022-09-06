package fr.irit.resource;

import fr.irit.complex.subgraphs.binary.Path;
import fr.irit.complex.subgraphs.unary.Triple;
import fr.irit.complex.subgraphs.unary.TripleType;
import fr.irit.complex.utils.Utils;
import fr.irit.input.CQAManager;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;
import java.util.regex.Matcher;

public class IRIUtils {

    public static IRI findMostSimilarType(IRI iri, String endpointUrl, Set<String> targetLabels, double threshold) {
        double scoreTypeMax = -1;
        IRI finalType = null;

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

        return finalType;
    }


    public static void retrieveTypes(IRI iri, String endpointUrl) {
        String query = SparqlSelect.buildTypesSelect(iri.getValue());
        SparqlSelect sq = new SparqlSelect(query);

        List<Map<String, String>> ret = SparqlProxy.getResponse(endpointUrl, sq.getMainQueryWithPrefixes());

        for (Map<String, String> jsonNode : ret) {
            String s = jsonNode.get("type");
            iri.getTypes().add(new IRI("<" + s + ">"));
        }

        for (IRI type : iri.getTypes()) {
            IRIUtils.retrieveLabels(type, endpointUrl);
        }
    }


    public static Set<Triple> getAllTriples(Resource resource, Set<String> queryLabels, String targetEndpoint, float threshold) {
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


    private static Set<Triple> getPredicateTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildPredicateTriplesSelect(value.getValue());

        List<Map<String, String>> ret = SparqlProxy.getResponse(targetEndpoint, query);
        for (Map<String, String> response : ret) {
            IRI sub = new IRI("<" + response.get("subject") + ">");
            String obj = response.get("object");

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

    private static Set<Triple> getObjectTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildObjectTriplesSelect(value.getValue());

        List<Map<String, String>> ret = SparqlProxy.getResponse(targetEndpoint, query);

        for (Map<String, String> response : ret) {
            IRI sub = new IRI("<" + response.get("subject") + ">");
            IRI pred = new IRI("<" + response.get("predicate") + ">");

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

    private static Set<Triple> getSubjectTriples(Set<String> queryLabels, String targetEndpoint, IRI value, float threshold) {
        Set<Triple> triples = new HashSet<>();
        String query = SparqlSelect.buildSubjectTriplesSelect(value.getValue());

        List<Map<String, String>> ret = SparqlProxy.getResponse(targetEndpoint, query);

        for (Map<String, String> response : ret) {
            if (response.get("object").matches("\"b[0-9]+\"")) continue;

            IRI pred = new IRI("<" + response.get("predicate") + ">");
            String obj = response.get("object");
            Resource object = new Resource(obj);
            if (object.isIRI()) {
                object = new IRI("<" + obj.replaceAll("[<>]", "") + ">");
            }

            Triple triple = new Triple(value, pred, object, TripleType.SUBJECT);

            if (object instanceof IRI to) {
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


    public static void retrieveLabels(IRI iri, String endpointUrl) {
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

        List<Map<String, String>> ret = SparqlProxy.getResponse(endpointUrl, literalQuery);

        for (Map<String, String> jsonNode : ret) {
            String s = jsonNode.get("x");
            Resource res = new Resource(s);
            if (!res.isIRI()) {
                iri.addLabel(s);
            }

        }
        iri.setLabelsGot(true);
    }


    public static double comparePathLabel(Path path, Set<String> targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        double similarity = 0;
        double typeSimilarity = 0;
        for (IRI prop : path.getProperties()) {
            IRIUtils.retrieveLabels(prop, targetEndpoint);
            similarity += Utils.similarity(prop.getLabels(), targetLabels, threshold);
        }


        for (int i = 0; i < path.getEntities().size(); i++) {
            Resource ent = path.getEntities().get(i);
            if (ent instanceof IRI) {
                IRI type = path.getTypes().get(i);
                if (type == null) continue;
                double scoreType = Utils.similarity(type.getLabels(), targetLabels, threshold);
                if (scoreType > typeThreshold) {
                    typeSimilarity += scoreType;
                } else {
                    path.getTypes().set(i, null);
                }
            }
        }
        if (path.pathFound()) {
            similarity += 0.5;
        }

        return similarity + typeSimilarity;
    }

}
