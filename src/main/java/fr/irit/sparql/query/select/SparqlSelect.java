package fr.irit.sparql.query.select;

import fr.irit.sparql.query.SparqlQuery;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparqlSelect extends SparqlQuery {
    private String select;
    private final List<String> selectFocus;

    public SparqlSelect(String query) {
        super(query);
        mainQuery = mainQuery.trim().replaceAll("SELECT", "select").replaceAll("WHERE", "where").replaceAll("\n", " ");
        selectFocus = new ArrayList<>();

        final Pattern pattern = Pattern.compile("""
                select[ \t
                distncDISTNC]+(\\?[A-Za-z0-9_-]+)[ \t
                ]+(\\?*[A-Za-z0-9_-]*[ \t
                ]*)where[ \t
                ]*\\{(.+)}[ \t
                ]*$""");
        Matcher matcher = pattern.matcher(mainQuery);
        while (matcher.find()) {
            selectFocus.add(matcher.group(1).trim());
            if (!matcher.group(2).trim().isEmpty()) {
                selectFocus.add(matcher.group(2).trim());
            }
            where = matcher.group(3).trim();
        }

        final Pattern pattern2 = Pattern.compile("""
                select([ \t
                distncDISTNC]+\\?[A-Za-z0-9_-]+[ \t
                ]+\\?*[A-Za-z0-9_-]*[ \t
                ]*)where""");
        Matcher matcher2 = pattern2.matcher(mainQuery);
        if (matcher2.find()) {
            select = matcher2.group(1);
        }


    }


    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }


    @Override
    public String toString() {
        return mainQuery;
    }

    public String toSubgraphForm() {

        String ret = where;
        if (selectFocus.size() > 1) {
            int i = 0;
            for (String sf : selectFocus) {
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + " ", "\\?answer" + i + " ");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "\\.", "\\?answer" + i + ".");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "}", "\\?answer" + i + "}");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "\\)", "\\?answer" + i + ")");
                i++;
            }
        } else {
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + " ", "\\?answer ");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "\\.", "\\?answer.");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "}", "\\?answer}");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "\\)", "\\?answer)");
        }
        return ret.replaceAll("\n", " ").replaceAll("\"", "\\\"");
    }

    public List<String> getSelectFocus() {
        return selectFocus;
    }



    public int getFocusLength() {
        return selectFocus.size();
    }


    public static String buildTypesSelect(String value){
        return "SELECT DISTINCT ?type WHERE {" + value
                 + " a ?type."
                + "filter(isIRI(?type))}";
    }


    public static String buildPredicateTriplesSelect(String value) {
        return "SELECT ?subject ?object WHERE {" +
                "?subject " + value + " ?object."
                + "} LIMIT 500";
    }

    public static String buildObjectTriplesSelect(String value) {
        return "SELECT ?subject ?predicate WHERE {" +
                "?subject ?predicate " + value + "."
                + "MINUS{ ?subject <http://www.w3.org/2002/07/owl#sameAs> " + value + ".}"
                + "MINUS{ ?subject <http://www.w3.org/2004/02/skos/core#closeMatch> " + value + ".}"
                + "MINUS{ ?subject <http://www.w3.org/2004/02/skos/core#exactMatch> " + value + ".}"
                + "}LIMIT 500";
    }

    public static String buildSubjectTriplesSelect(String value) {
        return "SELECT ?predicate ?object WHERE {" +
                value + " ?predicate ?object."
                + "MINUS{ " + value + " <http://www.w3.org/2002/07/owl#sameAs> ?object.}"

                + "}LIMIT 500";
    }

    public static String buildExactMatchExistsAsk(String match) {
        return "ASK {" +
                "{" + match + " ?z ?x. "
                + "MINUS {" + match + " <http://www.w3.org/2002/07/owl#sameAs> ?x.}"
                + "MINUS{ " + match + " <http://www.w3.org/2004/02/skos/core#closeMatch> ?x.}"
                + "MINUS{ " + match + "  <http://www.w3.org/2004/02/skos/core#exactMatch> ?x.}"
                + "}" +
                "UNION{?a ?b " + match + ". "
                + "MINUS { ?a <http://www.w3.org/2002/07/owl#sameAs> " + match + "}"
                + "MINUS { ?a <http://www.w3.org/2004/02/skos/core#exactMatch> " + match + "}"
                + "MINUS { ?a <http://www.w3.org/2004/02/skos/core#closeMatch> " + match + "}"
                + "}" +
                "}";
    }

    public static String buildSelectDistinctClasses(){
        return """
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> \s
                    SELECT distinct ?x WHERE{ \s
                    ?x a owl:Class.\s
                    ?y a ?x. filter(isIRI(?x))}""";
    }

    public static String buildSelectDistinctByClassType(String owlClass){
        return "SELECT DISTINCT ?x WHERE {  \n" +
                "?x a <" + owlClass + ">.} ";
    }

    public static String buildSelectDistinctProperties(){
        return """
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> \s
                    SELECT distinct ?x WHERE{ \s
                    ?y ?x ?z. {?x a owl:ObjectProperty.}
                      union{
                        ?x a owl:DatatypeProperty.}
                      }""";

    }

    public static String buildBinarySelectDistinct(String owlProp){
        return "SELECT DISTINCT ?x ?y WHERE {  \n" +
                "?x <" + owlProp + "> ?y.} ";
    }
}
