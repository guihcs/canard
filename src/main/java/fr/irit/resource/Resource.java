package fr.irit.resource;

import fr.irit.input.CQAManager;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resource {

    protected final String value;
    protected final HashSet<IRI> similarIRIs;
    private final Pattern pattern = Pattern.compile("[a-z][/:#]");

    public Resource(String val) {
        value = val.replaceAll("\\\\", "");
        similarIRIs = new HashSet<>();
    }

    public boolean isIRI() {

        Matcher matcher = pattern.matcher(value);
        return !value.contains(" ") && matcher.find();
    }

    public void findSimilarResource(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        Map<String, String> substitution = new HashMap<>();
        substitution.put("labelValue", value);

        substitution.put("LabelValue", value.length() > 1 ?
                value.substring(0, 1).toUpperCase() + value.substring(1) :
                "\"" + value.toUpperCase() + "\"");

        String query = CQAManager.getInstance().getSimilarQuery(targetEndpoint, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);

        for(Map<String, SelectResponse.Results.Binding> node : ret) {
            String s = node.get("x").getValue();
            similarIRIs.add(new IRI("<" + s + ">"));
        }

        substitution.put("labelValue", "\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"@en");
        query = CQAManager.getInstance().getSimilarQuery(targetEndpoint, substitution);
        ret = spIn.getResponse(query);

        for(Map<String, SelectResponse.Results.Binding> node : ret) {
            String s = node.get("x").getValue();
            similarIRIs.add(new IRI("<" + s + ">"));
        }

        substitution.put("labelValue", "\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"");
        ret = spIn.getResponse(query);

        for(Map<String, SelectResponse.Results.Binding> node : ret) {
            String s = node.get("x").getValue();
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

    public HashSet<IRI> getSimilarIRIs() {
        return similarIRIs;
    }


    public boolean isType() {
        return toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
    }
}
