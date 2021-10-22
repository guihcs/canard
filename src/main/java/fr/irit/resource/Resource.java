package fr.irit.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.complex.utils.Utils;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resource {

    protected final String value;
    protected final HashSet<IRI> similarIRIs;

    public Resource(String val) {
        value = val.replaceAll("\\\\", "");
        similarIRIs = new HashSet<>();
    }

    public boolean isIRI() {
        Pattern pattern = Pattern.compile("[a-z][/:#]");
        Matcher matcher = pattern.matcher(value);
        return !value.contains(" ") && matcher.find();
    }

    public void findSimilarResource(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        Map<String, String> substitution = new HashMap<>();
        substitution.put("labelValue", value);
        if (value.length() > 1) {
            substitution.put("LabelValue", value.substring(0, 1).toUpperCase() + value.substring(1));
        } else {
            substitution.put("LabelValue", "\"" + value.toUpperCase() + "\"");
        }
        String query = Utils.getInstance().getSimilarQuery(targetEndpoint, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);


        ArrayList<JsonNode> ret = spIn.getResponse(query);

        Iterator<JsonNode> retIterator = ret.iterator();
        while (retIterator.hasNext()) {
            String s = retIterator.next().get("x").get("value").toString().replaceAll("\"", "");
            similarIRIs.add(new IRI("<" + s + ">"));
        }

        substitution.put("labelValue", "\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"@en");
        query = Utils.getInstance().getSimilarQuery(targetEndpoint, substitution);
        ret = spIn.getResponse(query);

        retIterator = ret.iterator();
        while (retIterator.hasNext()) {
            String s = retIterator.next().get("x").get("value").toString().replaceAll("\"", "");
            similarIRIs.add(new IRI("<" + s + ">"));
        }

        substitution.put("labelValue", "\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"");
        ret = spIn.getResponse(query);

        retIterator = ret.iterator();
        while (retIterator.hasNext()) {
            String s = retIterator.next().get("x").get("value").toString().replaceAll("\"", "");
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
        if (obj instanceof Resource) {
            return value.equals(((Resource) obj).value);
        } else {
            return false;
        }
    }

    public HashSet<IRI> getSimilarIRIs() {
        return similarIRIs;
    }


}
