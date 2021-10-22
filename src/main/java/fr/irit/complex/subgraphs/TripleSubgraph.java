package fr.irit.complex.subgraphs;

import java.util.ArrayList;
import java.util.HashSet;

public class TripleSubgraph extends SubgraphForOutput {

    final ArrayList<Triple> triples;
    final int partWithMaxSim;
    int commonPart;
    /**
     * commonPart : 1-subject 2-predicate 3-object
     */
    double maxSimilarity;
    boolean formsCalculated;
    String intension;
    String extension;


    public TripleSubgraph(Triple t) {
        triples = new ArrayList<>();
        triples.add(t);
        commonPart = -1;
        maxSimilarity = t.getSimilarity();
        similarity = t.getSimilarity();
        formsCalculated = false;
        partWithMaxSim = t.getPartGivingMaxSimilarity();
    }

    public boolean addSubgraph(Triple t) {
        boolean added = false;
        if (triples.get(0).toString().equals(t.toString())) {
            triples.add(t);
            added = true;
        } else if (triples.get(0).hasCommonPart(t) && commonPart == -1) {
            if (!triples.get(0).getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") || triples.get(0).commonPartValue(t) != 2) {
                addSimilarity(t);
                triples.add(t);
                commonPart = triples.get(0).commonPartValue(t);
                added = true;
            }

        } else if (triples.get(0).hasCommonPart(t) && triples.get(0).commonPartValue(t) == commonPart) {
            if (!triples.get(0).getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") || commonPart != 2) {
                addSimilarity(t);
                triples.add(t);
                added = true;
            }
        }
        return added;
    }

    public void addSimilarity(Triple t) {
        maxSimilarity = Math.max(maxSimilarity, t.getSimilarity());
        similarity = ((similarity * triples.size()) + t.getSimilarity()) / (triples.size() + 1);
    }

    public void calculateIntensionString() {
        String res = triples.get(0).toString();
        Triple t = triples.get(0);
        HashSet<String> concatSub = new HashSet<>();
        HashSet<String> concatPred = new HashSet<>();
        HashSet<String> concatObj = new HashSet<>();
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
        HashSet<String> concatSub = new HashSet<>();
        HashSet<String> concatPred = new HashSet<>();
        HashSet<String> concatObj = new HashSet<>();
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
        HashSet<String> concatTriple = new HashSet<>();
        for (Triple t1 : triples) {
            concatTriple.add(t1.toString());
        }
        ArrayList<String> unionMembers = new ArrayList<>(concatTriple);
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

    public ArrayList<Triple> getTriples() {
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


}