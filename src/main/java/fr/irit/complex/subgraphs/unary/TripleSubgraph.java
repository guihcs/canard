package fr.irit.complex.subgraphs.unary;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.similarity.SimilarityValues;
import fr.irit.complex.subgraphs.similarity.TripleSimilarity;

import java.util.*;
import java.util.stream.Collectors;

public class TripleSubgraph extends SubgraphForOutput<Triple, TripleSimilarity> {

    private static class TripleSimilarityPair {
        Triple triple;
        TripleSimilarity tripleSimilarity;

        public TripleSimilarityPair(Triple triple, TripleSimilarity tripleSimilarity) {
            this.triple = triple;
            this.tripleSimilarity = tripleSimilarity;
        }
    }

    private final List<TripleSimilarityPair> triples;
    private final TripleType partWithMaxSim;
    private TripleType commonPart;
    private double maxSimilarity;
    private boolean formsCalculated;
    private String intension;
    private String extension;
    private final Map<Triple, SimilarityValues> similarityMap = new HashMap<>();

    public TripleSubgraph(Triple t, SimilarityValues sim) {
        triples = new ArrayList<>();
        triples.add(new TripleSimilarityPair(t, (TripleSimilarity) sim));
        commonPart = TripleType.NONE;
        maxSimilarity = sim.getSimilarity();
        similarity = maxSimilarity;
        formsCalculated = false;
        partWithMaxSim = ((TripleSimilarity) sim).getPartGivingMaxSimilarity();
        similarityMap.put(t, sim);
    }


    @Override
    public boolean addSubgraph(Triple t, TripleSimilarity s) {
        String t0String = buildTripleString(triples.get(0).triple, triples.get(0).tripleSimilarity);
        String tString = buildTripleString(t, s);

        if (t0String.equals(tString)) {
            triples.add(new TripleSimilarityPair(t, s));
            return true;

        }

        boolean hasCommonPart = hasCommonPart(triples.get(0).triple, triples.get(0).tripleSimilarity, t);
        TripleType commonPart = commonPartValue(triples.get(0).triple, triples.get(0).tripleSimilarity, t);
        boolean notHaveTypePredicate = !triples.get(0).triple.haveTypePredicate();

        if (hasCommonPart && this.commonPart == TripleType.NONE && (notHaveTypePredicate || commonPart != TripleType.PREDICATE)) {
            triples.add(new TripleSimilarityPair(t, s));

            addSimilarity(s.getSimilarity());
            similarityMap.put(t, s);

            this.commonPart = commonPart;
            return true;

        } else if (hasCommonPart && this.commonPart != TripleType.NONE && (notHaveTypePredicate || this.commonPart != TripleType.PREDICATE) && commonPart == this.commonPart) {
            triples.add(new TripleSimilarityPair(t, s));

            addSimilarity(s.getSimilarity());
            similarityMap.put(t, s);
            return true;
        }
        return false;
    }


    public String buildTripleString(Triple triple, TripleSimilarity tripleSimilarity) {

        String subjStr = triple.getSubject().toValueString();
        String predStr = triple.getPredicate().toValueString();
        String objStr = triple.getObject().toValueString();

        if (triple.getType() == TripleType.SUBJECT) {
            subjStr = "?answer";
        } else if (triple.getType() == TripleType.PREDICATE) {
            predStr = "?answer";
        } else {
            objStr = "?answer";
        }

        String result = subjStr + " " + predStr + " " + objStr + ". ";
        boolean keepSubjectType = tripleSimilarity.canKeepSubjectType(triple);
        boolean keepObjectType = tripleSimilarity.canKeepObjectType(triple);
        if (keepSubjectType && !keepObjectType) {

            result = "?x " + predStr + " " + objStr + ". " +
                    "?x a " + triple.getSubjectType() + ". ";
        } else if (keepObjectType && !keepSubjectType) {
            result = subjStr + " " + predStr + " ?y. " +
                    "?y a " + triple.getObjectType() + ". ";
        } else if (keepObjectType) {

            result = "?x " + predStr + " ?y. " +
                    "?y a " + triple.getObjectType() + ". " +
                    "?x a " + triple.getSubjectType() + ". ";
        }
        return result;
    }


    public boolean hasCommonPart(Triple a, TripleSimilarity as, Triple b) {
        return commonPartValue(a, as, b) != TripleType.NONE;
    }

    public TripleType commonPartValue(Triple a, TripleSimilarity as, Triple b) {
        TripleType res = TripleType.NONE;
        if (a.getType() != b.getType()) return res;

        boolean keepObjectType = as.canKeepObjectType(a);
        if (a.getType() == TripleType.SUBJECT) {

            if (a.getPredicate().equals(b.getPredicate())) {
                res = TripleType.PREDICATE;
            }
            if (a.getObject().equals(b.getObject()) && !keepObjectType) {
                res = TripleType.OBJECT;
            }

        } else {
            boolean keepSubjectType = as.canKeepSubjectType(a);
            if (a.getType() == TripleType.PREDICATE) {

                if (a.getObject().equals(b.getObject()) && !keepObjectType) {
                    res = TripleType.OBJECT;
                }
                if (a.getSubject().equals(b.getSubject()) && !keepSubjectType) {
                    res = TripleType.SUBJECT;
                }
            } else {
                if (a.getPredicate().equals(b.getPredicate()) && !a.isPredicateTriple()) {
                    res = TripleType.PREDICATE;
                }

                if (a.getSubject().equals(b.getSubject()) && !a.isSubjectTriple() && !keepSubjectType) {
                    res = TripleType.SUBJECT;
                }

            }
        }
        return res;
    }

    public void addSimilarity(double s) {
        maxSimilarity = Math.max(maxSimilarity, s);
        similarity = ((similarity * triples.size()) + s) / (triples.size() + 1);
    }

    public void calculateIntensionString() {
        String res = buildTripleString(triples.get(0).triple, triples.get(0).tripleSimilarity);
        Triple t = triples.get(0).triple;
        Set<String> concatSub = new HashSet<>();
        Set<String> concatPred = new HashSet<>();
        Set<String> concatObj = new HashSet<>();
        for (Triple t1 : triples.stream().map(tripleSimilarityPair -> tripleSimilarityPair.triple).toList()) {
            concatSub.add(t1.getSubject().toString());
            concatPred.add(t1.getPredicate().toString());
            concatObj.add(t1.getObject().toString());
        }
        if (t.isSubjectTriple() && !t.haveTypePredicate()) {
            if (commonPart == TripleType.PREDICATE && concatObj.size() > 1) {
                res = res.replaceFirst(stringToRegex(t.getObject().toValueString()), "?someObject");
            } else if (commonPart == TripleType.OBJECT && concatPred.size() > 1) {
                res = res.replaceFirst(t.getPredicate().toValueString(), "?somePredicate");
            } else if (commonPart == TripleType.NONE && predicateHasMaxSim()) {
                res = res.replaceFirst(stringToRegex(t.getObject().toValueString()), "?someObject");
            }
        } else if (t.isPredicateTriple()) {
            if (commonPart == TripleType.SUBJECT && concatObj.size() > 1) {
                res = res.replaceFirst(stringToRegex(t.getObject().toValueString()), "?someObject");
            } else if (commonPart == TripleType.OBJECT && concatSub.size() > 1) {
                res = res.replaceFirst(t.getSubject().toValueString(), "?someSubject");
            }
        } else if (t.isObjectTriple() && !t.haveTypePredicate()) {
            if (commonPart == TripleType.SUBJECT && concatPred.size() > 1) {
                res = res.replaceFirst(t.getPredicate().toValueString(), "?somePredicate");
            } else if (commonPart == TripleType.PREDICATE && concatSub.size() > 1) {
                res = res.replaceFirst(t.getSubject().toValueString(), "?someSubject");
            } else if (commonPart == TripleType.NONE && predicateHasMaxSim()) {
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

        for (Triple t : triples.stream().map(tripleSimilarityPair -> tripleSimilarityPair.triple).toList()) {
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
        } else if (commonPart == TripleType.PREDICATE || commonPart == TripleType.NONE) {
            if (predicateHasMaxSim()) {
                res += intension;
            } else {
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
        for (TripleSimilarityPair t1 : triples) {
            concatTriple.add(buildTripleString(t1.triple, t1.tripleSimilarity));
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
        return triples.stream().map(tripleSimilarityPair -> tripleSimilarityPair.triple).collect(Collectors.toList());
    }

    public double getAverageSimilarity() {
        return similarity;
    }

    public boolean predicateHasMaxSim() {
        return partWithMaxSim == TripleType.PREDICATE;
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