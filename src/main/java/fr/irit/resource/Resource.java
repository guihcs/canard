package fr.irit.resource;

import fr.irit.input.CQAManager;
import fr.irit.sparql.proxy.SparqlProxy;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resource {

    protected final String value;
    protected final Set<IRI> similarIRIs;
    private final Pattern pattern = Pattern.compile("[a-z][/:#]");

    public Resource(String val) {
        value = val.replaceAll("\\\\", "");
        similarIRIs = new HashSet<>();
    }

    public boolean isIRI() {

        Matcher matcher = pattern.matcher(value);
        return !value.contains(" ") && matcher.find();
    }

    public void findSimilarResource(String targetEndpoint) {

        Map<String, String> substitution = new HashMap<>();

        Set<String> values = Set.of(
                value,
                "\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"@en",
                "\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\""
        );

        substitution.put("labelValue", "(" + String.join("|", values) + ")");

        substitution.put("LabelValue", value.length() > 1 ?
                value.substring(0, 1).toUpperCase() + value.substring(1) :
                "\"" + value.toUpperCase() + "\"");

        String query = CQAManager.getInstance().getSimilarQuery(targetEndpoint, substitution);

        List<Map<String, String>> ret = SparqlProxy.getResponse(targetEndpoint, query);

        for(Map<String, String> node : ret) {
            String s = node.get("x");
            similarIRIs.add(new IRI("<" + s + ">"));
        }

    }

    public String getValue() {
        return value;
    }

    public String toValueString() {
        if (!(isIRI())) {
            return "\"" + value + "\"";
        } else {
            return toString();
        }
    }

    public String toString() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Resource r) {
            return value.equals(r.value);
        } else {
            return false;
        }
    }

    public Set<IRI> getSimilarIRIs() {
        return similarIRIs;
    }


    public boolean isType() {
        return toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
    }
}
