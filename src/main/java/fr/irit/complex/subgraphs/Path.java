package fr.irit.complex.subgraphs;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.complex.utils.Utils;
import fr.irit.resource.IRI;
import fr.irit.resource.IRITypeUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Path extends InstantiatedSubgraph {
    final List<Resource> entities;
    final List<IRI> types;
    final List<Boolean> inverse;
    List<IRI> properties;
    double similarity;
    double typeSimilarity;

    public Path(Resource x, Resource y, String sparqlEndpoint, int length, List<Boolean> inverse) {
        properties = new ArrayList<>();
        entities = new ArrayList<>();
        types = new ArrayList<>();
        similarity = 0;
        this.inverse = inverse;
        findPathWithLength(x, y, sparqlEndpoint, length);

    }

    private void findPathWithLength(Resource x, Resource y, String sparqlEndpoint, int length) {
        String query;
        StringBuilder queryBody = new StringBuilder();
        ArrayList<String> variables = new ArrayList<>();

        if (!x.isIRI()) {
            variables.add("?x");
        } else {
            variables.add(x.toString());
        }


        for (int i = 1; i <= length - 1; i++) {
            variables.add("?v" + i);
        }
        if (!y.isIRI()) {
            variables.add("?y");
        } else {
            variables.add(y.toString());
        }

        for (int i = 1; i <= length; i++) {
            if (inverse.get(i - 1)) {
                queryBody.append(variables.get(i)).append(" ?p").append(i).append(" ").append(variables.get(i - 1)).append(". \n");
            } else {
                queryBody.append(variables.get(i - 1)).append(" ?p").append(i).append(" ").append(variables.get(i)).append(". \n");
            }

        }

        if (!x.isIRI()) {
            queryBody.append("   filter (regex(?x, \"^").append(x).append("$\",\"i\"))\n");
            //	queryBody += "FILTER(str(?x)="+x.toValueString()+") \n";
        }
        if (!y.isIRI()) {
            queryBody.append("   filter (regex(?y, \"^").append(y).append("$\",\"i\"))\n");
            //	queryBody += "FILTER(str(?y)="+y.toValueString()+") \n";
        }

        query = "SELECT DISTINCT * WHERE { " + queryBody + " }  LIMIT 20";

        //System.out.println(query);
        //run query
        SparqlProxy spProx = SparqlProxy.getSparqlProxy(sparqlEndpoint);
        ArrayList<JsonNode> ret;
        try {
            ret = spProx.getResponse(query);
            Iterator<JsonNode> retIteratorTarg = ret.iterator();
            //	System.out.println(query);
            // if more than one result, only take the first one (if instead of while)
            if (retIteratorTarg.hasNext()) {
                JsonNode next = retIteratorTarg.next();
//				System.out.println(next);
                if (next.has("x")) {
                    entities.add(new Resource(next.get("x").get("value").toString().replaceAll("\"", "")));
                } else {
                    entities.add(x);
                }
                int i = 1;
                boolean stop = false;
                while (i <= length && !stop) {
                    String p = next.get("p" + i).get("value").toString().replaceAll("\"", "");
                    Resource res = new Resource(p);
                    if (p.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        stop = true;
                    }
                    if (p.equals("http://www.w3.org/2002/07/owl#sameAs")) {
                        stop = true;
                    }
                    if (p.equals("http://www.w3.org/2004/02/skos/core#exactMatch")) {
                        stop = true;
                    }
                    if (p.equals("http://www.w3.org/2004/02/skos/core#closeMatch")) {
                        stop = true;
                    }
                    if (p.equals("http://dbpedia.org/ontology/wikiPageWikiLink")) {
                        stop = true;
                    }
                    if (res.isIRI()) {
                        properties.add(new IRI("<" + p + ">"));
                    }
                    i++;
                }
                //If a property is rdf:type, remove all properties from list
                if (stop) {
                    properties = new ArrayList<>();
                }
                if (length >= 2 && !stop) {
                    for (int j = 1; j <= length - 1; j++) {
                        String v = next.get("v" + j).get("value").toString().replaceAll("\"", "");
                        Resource res = new Resource(v);
                        if (res.isIRI()) {
                            entities.add(new IRI("<" + v + ">"));
                        } else {
                            entities.add(res);
                        }
                    }
                }
                if (next.has("y")) {
                    entities.add(new Resource(next.get("y").get("value").toString().replaceAll("\"", "")));
                } else {
                    entities.add(y);
                }

            }


        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }
    }

    public void compareLabel(Set<String> targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        similarity = 0;
        for (IRI prop : properties) {
            try {
                prop.retrieveLabels(targetEndpoint);
                similarity += Utils.similarity(prop.getLabels(), targetLabels, threshold);
            } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < entities.size(); i++) {
            Resource ent = entities.get(i);
            if (ent instanceof IRI) {
                IRI type = types.get(i);
                if (type != null) {
                    double scoreType = Utils.similarity(type.getLabels(), targetLabels, threshold);
                    if (scoreType > typeThreshold) {
                        typeSimilarity += scoreType;
                    } else {
                        types.set(i, null);
                    }
                }
            }
        }
        if (pathFound()) {
            similarity += 0.5;
        }

        getSimilarity();
    }

    public boolean pathFound() {
        return !properties.isEmpty();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            ret.append(entities.get(i)).append(" ").append(properties.get(i)).append(" ").append(entities.get(i + 1)).append(".  ");
        }
        return getSimilarity() + " <-> " + ret;
    }

    public String toSubGraphString() {
        StringBuilder ret = new StringBuilder();
        ArrayList<String> variables = new ArrayList<>();
        variables.add("?answer0");
        for (int i = 1; i <= properties.size() - 1; i++) {
            variables.add("?v" + i);
        }
        variables.add("?answer1");

        for (int i = 0; i < properties.size(); i++) {
            String xStr = variables.get(i);
            String yStr = variables.get(i + 1);

            if (types.get(i) != null) {
                ret.append(xStr).append(" a ").append(types.get(i)).append(".  ");
            }
            if (inverse.get(i)) {
                ret.append(yStr).append(" ").append(properties.get(i)).append(" ").append(xStr).append(".  ");
            } else {
                ret.append(xStr).append(" ").append(properties.get(i)).append(" ").append(yStr).append(".  ");
            }
        }
        if (types.get(properties.size()) != null) {
            ret.append(variables.get(properties.size())).append(" a ").append(types.get(properties.size())).append(".  ");
        }
        return ret.toString();
    }

    public double getSimilarity() {
        return similarity + typeSimilarity;
    }

    public void getMostSimilarTypes(String endpointUrl, Set<String> targetLabels, double threshold) {
        for (Resource r : entities) {
            if (r instanceof IRI) {
                IRI type = IRITypeUtils.findMostSimilarType((IRI) r, endpointUrl, targetLabels, threshold);
                types.add(type);
            } else {
                types.add(null);
            }
        }
    }

    public List<IRI> getProperties() {
        return properties;
    }

    public List<IRI> getTypes() {
        return types;
    }

    public List<Boolean> getInverse() {
        return inverse;
    }
}
