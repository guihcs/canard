package fr.irit.complex.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class CQAGenerator {

    private final String endpoint;
    private final String CQAFolder;
    private final double ratio;
    private final int maxCAV;
    private int count;

    public CQAGenerator(String endpoint, String CQAFolder) {
        this.endpoint = endpoint;
        this.CQAFolder = CQAFolder;
        count = 0;
        ratio = 30;
        maxCAV = 20;
    }

    public void createCQAs() {
        createClasses();
        createCAV();
        createProperties();
    }

    public void cleanCQARepository() {
        Path cqaPath = Paths.get(CQAFolder);
        try {
            if (Files.notExists(cqaPath)) {
                Files.createDirectory(cqaPath);
            }
            else {
                File dir = new File(CQAFolder);
                for (File file : dir.listFiles()) {
                    file.delete();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createClasses() {
        try {
            String query = """
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> \s
                    SELECT distinct ?x WHERE{ \s
                    ?x a owl:Class.\s
                    ?y a ?x. filter(isIRI(?x))}""";
            SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpoint);
            ArrayList<JsonNode> ret;
            ret = spIn.getResponse(query);
            for (JsonNode jsonNode : ret) {
                String owlClass = jsonNode.get("x").get("value").toString().replaceAll("\"", "");
                if (interestingIRI(owlClass)) {
                    PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
                    String CQA = "SELECT DISTINCT ?x WHERE {  \n" +
                            "?x a <" + owlClass + ">.} ";
                    writer.append(CQA);
                    writer.flush();
                    writer.close();
                    count++;
                }
            }
        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException | IOException e) {
            e.printStackTrace();
        }
    }

    public void createProperties() {
        try {
            String query = """
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> \s
                    SELECT distinct ?x WHERE{ \s
                    ?y ?x ?z. {?x a owl:ObjectProperty.}
                      union{
                        ?x a owl:DatatypeProperty.}
                      }""";
            SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpoint);
            ArrayList<JsonNode> ret;
            ret = spIn.getResponse(query);
            for (JsonNode jsonNode : ret) {
                String owlProp = jsonNode.get("x").get("value").toString().replaceAll("\"", "");
                if (interestingIRI(owlProp)) {
                    PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
                    String CQA = "SELECT DISTINCT ?x ?y WHERE {  \n" +
                            "?x <" + owlProp + "> ?y.} ";
                    writer.append(CQA);
                    writer.flush();
                    writer.close();
                    count++;
                }
            }
        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException | IOException e) {
            e.printStackTrace();
        }
    }

    public void createCAV() {
        try {
            String query = """
                    PREFIX owl: <http://www.w3.org/2002/07/owl#>  \s
                    SELECT distinct ?x WHERE {  \s
                    ?x a owl:ObjectProperty.}""";
            SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpoint);
            ArrayList<JsonNode> ret = spIn.getResponse(query);
            for (JsonNode node : ret) {
                String property = node.get("x").get("value").toString().replaceAll("\"", "");
                if (interestingIRI(property)) {
                    String queryNb = "SELECT (count(distinct ?x) as ?sub) (count(distinct ?y) as ?ob) where {\n" +
                            "?x <" + property + "> ?y.}";
                    ArrayList<JsonNode> retNb = spIn.getResponse(queryNb);
                    for (JsonNode nodeNb : retNb) {
                        int nbSub = Integer.parseInt(nodeNb.get("sub").get("value").toString().replaceAll("\"", ""));
                        int nbOb = Integer.parseInt(nodeNb.get("ob").get("value").toString().replaceAll("\"", ""));

                        if (nbSub != 0 && nbOb != 0) {
                            String queryOb = "SELECT distinct ?y where {\n" +
                                    "?x <" + property + "> ?y.}";
                            ArrayList<JsonNode> retOb = spIn.getResponse(queryOb);
                            if ((double) nbSub / (double) nbOb > ratio && nbOb < maxCAV) {


                                for (JsonNode jsonNode : retOb) {
                                    String object = jsonNode.get("y").get("value").toString().replaceAll("\"", "");
                                    PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
                                    String CQA = "SELECT DISTINCT ?x WHERE {\n" +
                                            "?x <" + property + "> <" + object + ">.} ";
                                    writer.append(CQA);
                                    writer.flush();
                                    writer.close();
                                    count++;
                                }

                            } else if ((double) nbSub / (double) nbOb > ratio && nbOb < maxCAV) {

                                for (JsonNode jsonNode : retOb) {
                                    String subject = jsonNode.get("x").get("value").toString().replaceAll("\"", "");
                                    PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
                                    String CQA = "SELECT DISTINCT ?x WHERE {\n" +
                                            "<" + subject + "> <" + property + "> ?x.} ";
                                    writer.append(CQA);
                                    writer.flush();
                                    writer.close();
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException | IOException e) {
            e.printStackTrace();
        }

    }

    public boolean interestingIRI(String iri) {
        return !(iri.contains("http://www.w3.org/2000/01/rdf-schema#") ||
                iri.contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#") ||
                iri.contains("http://www.w3.org/2001/XMLSchema#") ||
                iri.contains("http://www.w3.org/2004/02/skos/core#") ||
                iri.contains("http://www.w3.org/2008/05/skos-xl#") ||
                iri.contains("http://www.w3.org/2002/07/owl#") ||
                iri.contains("http://xmlns.com/foaf/") ||
                iri.contains("http://purl.org/dc/terms/") ||
                iri.contains("http://purl.org/dc/elements/1.1/"));
    }
}