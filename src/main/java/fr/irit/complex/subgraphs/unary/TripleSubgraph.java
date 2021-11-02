package fr.irit.complex.subgraphs.unary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;

import java.util.*;

public class TripleSubgraph extends SubgraphForOutput {

    final ArrayList<Triple> triples;
    final int partWithMaxSim;
    int commonPart;
    double maxSimilarity;
    boolean formsCalculated;
    String intension;
    String extension;
    final Map<Triple, SimilarityValues> similarityMap = new HashMap<>();

    public TripleSubgraph(Triple t, SimilarityValues sim) {
        triples = new ArrayList<>();
        triples.add(t);
        commonPart = -1;
        maxSimilarity = sim.similarity();
        similarity = maxSimilarity;
        formsCalculated = false;
        partWithMaxSim = getPartGivingMaxSimilarity(sim);
        similarityMap.put(t, sim);
    }

    private int getPartGivingMaxSimilarity(SimilarityValues similarityValues) {
        int res = 0;
        if (similarityValues.subjectSimilarity() > similarityValues.objectSimilarity() && similarityValues.subjectSimilarity() > similarityValues.predicateSimilarity()) {
            res = 1;
        } else if (similarityValues.objectSimilarity() > similarityValues.subjectSimilarity() && similarityValues.objectSimilarity() > similarityValues.predicateSimilarity()) {
            res = 3;
        } else if (similarityValues.predicateSimilarity() > similarityValues.subjectSimilarity() && similarityValues.predicateSimilarity() > similarityValues.objectSimilarity()) {
            res = 2;
        }
        return res;
    }



    @Override
    public boolean addSubgraph(InstantiatedSubgraph t, SimilarityValues s) {
        boolean added = false;
        if (triples.get(0).toString().equals(t.toString())) {
            triples.add((Triple) t);
            added = true;
        } else if (triples.get(0).hasCommonPart((Triple) t) && commonPart == -1) {
            if (!triples.get(0).getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") || triples.get(0).commonPartValue((Triple) t) != 2) {
                addSimilarity(s.similarity());
                triples.add((Triple) t);
                similarityMap.put((Triple) t, s);

                commonPart = triples.get(0).commonPartValue((Triple) t);
                added = true;
            }

        } else if (triples.get(0).hasCommonPart((Triple) t) && triples.get(0).commonPartValue((Triple) t) == commonPart) {
            if (!triples.get(0).getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") || commonPart != 2) {
                addSimilarity(s.similarity());
                triples.add((Triple) t);
                similarityMap.put((Triple) t, s);
                added = true;
            }
        }
        return added;
    }

    public void addSimilarity(double s) {
        maxSimilarity = Math.max(maxSimilarity, s);
        similarity = ((similarity * triples.size()) + s) / (triples.size() + 1);
    }

    public void calculateIntensionString() {
        String res = triples.get(0).toString();
        Triple t = triples.get(0);
        Set<String> concatSub = new HashSet<>();
        Set<String> concatPred = new HashSet<>();
        Set<String> concatObj = new HashSet<>();
        for (Triple t1 : triples) {
            concatSub.add(t1.getSubject().toString());
            concatPred.add(t1.getPredicate().toString());
            concatObj.add(t1.getObject().toString());
        }
        if (t.isSubjectTriple() && !t.getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
            if (commonPart == 2 && concatObj.size() > 1) {
                res = res.replaceFirst(stringToRegex(t.getObject().toValueString()), "?someObject");
            } else if (commonPart == 3 && concatPred.size() > 1) {
                res = res.replaceFirst(t.getPredicate().toValueString(), "?somePredicate");
            } else if (commonPart == -1 && predicateHasMaxSim()) {
                res = res.replaceFirst(stringToRegex(t.getObject().toValueString()), "?someObject");
            }
        } else if (t.isPredicateTriple()) {
            if (commonPart == 1 && concatObj.size() > 1) {
                res = res.replaceFirst(stringToRegex(t.getObject().toValueString()), "?someObject");
            } else if (commonPart == 3 && concatSub.size() > 1) {
                res = res.replaceFirst(t.getSubject().toValueString(), "?someSubject");
            }
        } else if (t.isObjectTriple() && !t.getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
            if (commonPart == 1 && concatPred.size() > 1) {
                res = res.replaceFirst(t.getPredicate().toValueString(), "?somePredicate");
            } else if (commonPart == 2 && concatSub.size() > 1) {
                res = res.replaceFirst(t.getSubject().toValueString(), "?someSubject");
            } else if (commonPart == -1 && predicateHasMaxSim()) {
                res = res.replaceFirst(t.getSubject().toValueString(), "?someSubject");
            }
        }

        intension = res;
    }

    public void calculateExtensionString() {
        String res = intension;
        Set<String> concatSub = new HashSet<>();
        Set<String> concatPred = new HashSet<>();
        Set<String> concatObj = new HashSet<>();
        for (Triple t : triples) {
            concatSub.add(t.getSubject().toString());
            concatPred.add(t.getPredicate().toString());
            concatObj.add(t.getObject().toString());
        }


        res = res.replaceAll("\\?someSubject", concatSub.toString());

        res = res.replaceAll("\\?somePredicate", concatPred.toString());

        res = res.replaceAll("\\?someObject", concatObj.toString());

        res = res.replaceAll("\\[", "\\{").replaceAll("]", "\\}");
        extension = res;

    }

    public String toIntensionString() {
        if (!formsCalculated) {
            calculateIntensionString();
            calculateExtensionString();
            formsCalculated = true;
        }
        return intension;
    }

    public String toExtensionString() {
        if (!formsCalculated) {
            calculateIntensionString();
            calculateExtensionString();
            formsCalculated = true;
        }
        return extension;
    }

    public String toSPARQLForm() {
        String res = "SELECT DISTINCT ?answer WHERE {";
        if (toIntensionString().contains("somePredicate")) {
            res += toSPARQLExtension();
        }
        else if (commonPart == 2 || commonPart == -1) {
            if (predicateHasMaxSim()) {
                res += intension;
            }
            else {
                res += toSPARQLExtension();
            }

        } else {
            res += toSPARQLExtension();
        }

        res += "}";
        return res;
    }

    public String toSPARQLExtension() {
        Set<String> concatTriple = new HashSet<>();
        for (Triple t1 : triples) {
            concatTriple.add(t1.toString());
        }
        List<String> unionMembers = new ArrayList<>(concatTriple);
        StringBuilder res = new StringBuilder();

        if (toIntensionString().equals(extension)) {
            res = new StringBuilder(extension);
        } else if (unionMembers.size() > 1) {
            res.append("{").append(unionMembers.get(0)).append("}\n");
            for (int i = 1; i < unionMembers.size(); i++) {
                res.append("UNION {").append(unionMembers.get(i)).append("}\n");
            }
        }
        return res.toString();
    }

    public List<Triple> getTriples() {
        return triples;
    }

    public double getAverageSimilarity() {
        return similarity;
    }

    public boolean predicateHasMaxSim() {
        return partWithMaxSim == 2;
    }

    private String stringToRegex(String s) {
        s = s.replaceAll("\\{", "\\\\{");
        s = s.replaceAll("}", "\\\\}");
        s = s.replaceAll("\\[", "\\\\[");
        s = s.replaceAll("]", "\\\\]");
        s = s.replaceAll("\\.", "\\\\.");
        s = s.replaceAll("\\?", "\\\\?");
        s = s.replaceAll("\\+", "\\\\+");
        s = s.replaceAll("\\*", "\\\\*");
        s = s.replaceAll("\\|", "\\\\|");
        s = s.replaceAll("\\^", "\\\\^");
        s = s.replaceAll("\\$", "\\\\$");
        return s;
    }

    public Map<Triple, SimilarityValues> getSimilarityMap() {
        return similarityMap;
    }
}