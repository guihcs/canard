package fr.irit.complex.answer.binary;

import fr.irit.complex.answer.Answer;
import fr.irit.complex.answer.SubgraphResult;
import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.binary.Path;
import fr.irit.complex.subgraphs.unary.SimilarityValues;
import fr.irit.complex.utils.Utils;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;

public class PairAnswer extends Answer {
    final Resource r1;
    final Resource r2;
    boolean similarlooked;

    public PairAnswer(Resource r1, Resource r2) {
        if (r1.isIRI()) {
            this.r1 = new IRI("<" + r1 + ">");
        } else {
            this.r1 = r1;
        }
        if (r2.isIRI()) {
            this.r2 = new IRI("<" + r2 + ">");
        } else {
            this.r2 = r2;
        }
        similarlooked = false;
    }

    @Override
    public void retrieveIRILabels(String endpointURL) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (r1 instanceof IRI ri) {
            ri.retrieveLabels(endpointURL);
        }
        if (r2 instanceof IRI ri) {
            ri.retrieveLabels(endpointURL);
        }

    }
    @Override
    public void getSimilarIRIs(String targetEndpoint) {
        if (!similarlooked) {
            if (r1 instanceof IRI ri) {
                ri.findSimilarResource(targetEndpoint);
            }
            if (r2 instanceof IRI ri) {
                ri.findSimilarResource(targetEndpoint);
            }
            similarlooked = true;
        }
    }
    @Override
    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        if (r1 instanceof IRI ri) {
            ri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }
        if (r2 instanceof IRI ri) {
            ri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }

    }

    @Override
    public Set<SubgraphResult> findCorrespondingSubGraph(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig) {

        Set<SubgraphResult> paths = new HashSet<>();

        for (InstantiatedSubgraph p : getPaths(executionConfig, queryLabels)) {
            if (p instanceof Path pa) {
                double similarity = compareLabel(pa, queryLabels, executionConfig.getSimilarityThreshold(), executionConfig.getTargetEndpoint(), 0.5);
                paths.add(new SubgraphResult(p, new SimilarityValues(similarity, 0, 0, 0)));
            } else {
                System.err.println("problem in Pair answer: instantiated subgraph is not a path...");
            }
        }
        if (getPaths(executionConfig, queryLabels).isEmpty() && !similarlooked) {
            getSimilarIRIs(executionConfig.getTargetEndpoint());
            System.out.println("No path found, similar answers : " + printMatchedEquivalents());
            paths = findCorrespondingSubGraph(queryLabels, query, executionConfig);
        }

        return paths;
    }


    private double compareLabel(Path p, Set<String> targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        double similarity = 0;
        for (IRI prop : p.getProperties()) {
            try {
                prop.retrieveLabels(targetEndpoint);
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
                    if (scoreType > typeThreshold) {
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


    private Set<InstantiatedSubgraph> getPaths(ExecutionConfig executionConfig, Set<String> queryLabels){
        Set<InstantiatedSubgraph> paths = new HashSet<>();
        if (hasTotalMatch()) {
            for (Resource x : r1.getSimilarIRIs()) {
                for (Resource y : r2.getSimilarIRIs()) {
                    paths.addAll(findMostSimilarPaths(executionConfig, queryLabels, x, y));
                }
            }
        }

        if (paths.isEmpty() && hasR1Match()) {
            if (!r2.isIRI()) {
                for (IRI x : r1.getSimilarIRIs()) {
                    paths.addAll(findMostSimilarPaths(executionConfig, queryLabels, x, r2));
                }

            }
        }

        if (paths.isEmpty() && hasR2Match()) {
            if (!r1.isIRI()) {
                for (IRI y : r2.getSimilarIRIs()) {
                    paths.addAll(findMostSimilarPaths(executionConfig, queryLabels, r1, y));
                }

            }

        }
        return paths;
    }

    private Set<InstantiatedSubgraph> findMostSimilarPaths(ExecutionConfig executionConfig, Set<String> queryLabels, Resource x, Resource y) {
        Set<InstantiatedSubgraph> paths = new HashSet<>();
        int length = 1;
        boolean found = false;
        while (length < 4 && !found) {
            List<List<Boolean>> allInv = allInversePossibilities(length);
            for (List<Boolean> invArray : allInv) {
                Path p = new Path(x, y, executionConfig.getTargetEndpoint(), length, invArray);
                if (p.pathFound()) {
                    p.getMostSimilarTypes(executionConfig.getTargetEndpoint(), queryLabels, 0.0);
                    paths.add(p);
                    found = true;
                }
            }
            length++;
        }
        return paths;
    }

    @Override
    public boolean hasMatch() {
        boolean match = !r1.isIRI() || hasR1Match();
        if (r2.isIRI() && !hasR2Match()) {
            match = false;
        }
        return match;
    }

    private boolean hasR1Match() {
        return !r1.getSimilarIRIs().isEmpty();
    }

    private boolean hasR2Match() {
        return !r2.getSimilarIRIs().isEmpty();
    }

    private boolean hasTotalMatch() {
        return hasR1Match() && hasR2Match();
    }
    @Override
    public String toString() {
        return r1.toString() + " " + r2.toString();
    }

    private List<List<Boolean>> allInversePossibilities(int length) {
        List<List<Boolean>> result = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, length); i++) {
            List<Boolean> invArray = new ArrayList<>();
            StringBuilder invStr = new StringBuilder(Integer.toBinaryString(i));
            while (invStr.length() < length) {
                invStr.insert(0, "0");
            }
            for (char invCh : invStr.toString().toCharArray()) {
                if (invCh == '0') {
                    invArray.add(false);
                } else if (invCh == '1') {
                    invArray.add(true);
                }
            }
            result.add(invArray);
        }


        return result;

    }
    @Override
    public String printMatchedEquivalents() {
        return r1.getSimilarIRIs().toString() + " <--> " + r2.getSimilarIRIs().toString();
    }

}
