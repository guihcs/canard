package fr.irit.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.complex.utils.Utils;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRI extends Resource {
    private final HashSet<String> labels;
    private final HashSet<IRI> types;
    private boolean labelsGot;
    private final Pattern pattern = Pattern.compile("<([^>]+)[#/]([A-Za-z0-9_-]+)>");

    public IRI(String iri) {
        super(iri);
        labels = new HashSet<>();
        types = new HashSet<>();
        labelsGot = false;
    }

    public void retrieveLabels(String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (!labelsGot) {

            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                addLabel(matcher.group(2));
            } else {
                addLabel(value);
            }


            Map<String, String> substitution = new HashMap<>();
            substitution.put("uri", value);
            String literalQuery = Utils.getInstance().getLabelQuery(endpointUrl, substitution);
            SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

            List<JsonNode> ret = spIn.getResponse(literalQuery);

            for (JsonNode jsonNode : ret) {
                String s = jsonNode.get("x").get("value").toString().replaceAll("\"", "");
                Resource res = new Resource(s);
                if (!res.isIRI()) {
                    addLabel(s);
                }

            }
            labelsGot = true;
        }

    }


    public void addLabel(String label) {
        labels.add(label.trim().replaceAll("\\\\", "").toLowerCase());
    }

    public void findExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        List<IRI> allMatches = new ArrayList<>();
        allMatches.add(this);

        Map<String, String> substitution = new HashMap<>();
        substitution.put("uri", value.replaceAll("\\$", ""));

        allMatches.addAll(IRI.parseMatches(sourceEndpoint, substitution));
        allMatches.addAll(IRI.parseMatches(targetEndpoint, substitution));

        for (IRI match : allMatches) {
            if (existsInTarget(match, targetEndpoint)) {
                similarIRIs.add(match);
            }
        }


    }

    private static List<IRI> parseMatches(String endpoint, Map<String, String> substitution) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        List<IRI> allMatches = new ArrayList<>();
        String query = Utils.getInstance().getMatchedURIs(endpoint, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpoint);
        List<JsonNode> ret = spIn.getResponse(query);

        for (JsonNode node : ret) {
            String s = node.get("x").get("value").toString().replaceAll("\"", "");
            Resource res = new Resource(s);
            if (res.isIRI()) {
                allMatches.add(new IRI("<" + s + ">"));
            }
        }

        return allMatches;
    }


    public boolean existsInTarget(IRI match, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        String queryMatch = "ASK {" +
                "{" + match.toString() + " ?z ?x. "
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
        SparqlProxy spTarg = SparqlProxy.getSparqlProxy(targetEndpoint);
        return spTarg.sendAskQuery(queryMatch);

    }


    public void findSimilarResource(String targetEndpoint) {
        if (labels.isEmpty()) {
            try {
                retrieveLabels(targetEndpoint);
            } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
        }
        for (String rawLab : labels) {

            String label = rawLab.replaceAll("[\\^+{}.?]", "");
            Map<String, String> substitution = new HashMap<>();
            substitution.put("labelValue", label.toLowerCase());
            if (label.length() > 1) {
                substitution.put("LabelValue", label.substring(0, 1).toUpperCase() + label.substring(1));
            } else {
                substitution.put("LabelValue", label.toUpperCase());
            }
            String litteralQuery = Utils.getInstance().getSimilarQuery(targetEndpoint, substitution);

            if (litteralQuery.length() < 2000) {
                SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);

                List<JsonNode> ret;
                try {
                    ret = spIn.getResponse(litteralQuery);
                    for (JsonNode jsonNode : ret) {
                        String s = jsonNode.get("x").get("value").toString().replaceAll("\"", "");
                        similarIRIs.add(new IRI("<" + s + ">"));
                    }
                } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                    e.printStackTrace();
                }


            }
        }
    }


    public HashSet<String> getLabels() {
        return labels;
    }


    public Set<IRI> getTypes() {
        return types;
    }
}
