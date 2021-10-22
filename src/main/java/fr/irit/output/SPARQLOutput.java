package fr.irit.output;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.Triple;
import fr.irit.complex.subgraphs.TripleSubgraph;
import fr.irit.complex.utils.Parameters;
import fr.irit.resource.IRI;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.exceptions.IncompleteSubstitutionException;
import fr.irit.sparql.files.QueryTemplate;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;

public class SPARQLOutput extends Output {
    private final String outputEndpoint;
    private final Map<String, QueryTemplate> templates;
    private String outputAlignmentIRI;

    public SPARQLOutput(Parameters params) {
        super(params);
        outputEndpoint = params.getOutputEndpoint();
        templates = params.getQueryTemplates().get(outputEndpoint);

    }

    @Override
    public void init() {
        SparqlProxy spOutput = SparqlProxy.getSparqlProxy(outputEndpoint);
        String outputIRI = "http://alignment-output#";
        String alignmentHashCode = sourceEndpoint.hashCode() + "-" + targetEndpoint.hashCode();

        Map<String, String> substitution = new HashMap<>();
        outputAlignmentIRI = "<" + outputIRI + "alignment_" + alignmentHashCode + ">";
        substitution.put("alignment", outputAlignmentIRI);
        substitution.put("sourceOntology", "<" + sourceEndpoint + ">");
        substitution.put("targetOntology", "<" + targetEndpoint + ">");
        substitution.put("date", "\"" + new Date() + "\"^^xsd:dateTime");
        String createAlignment = createAlignment(substitution);
        try {
            spOutput.postSparqlUpdateQuery(createAlignment);
        } catch (SparqlQueryMalFormedException
                | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }

    }


    /**
     * Creates an alignment with datetime, source and target ontologies
     */
    public String createAlignment(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_alignment").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * Adds a Cell to the alignment
     */
    public String createCell(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_cell").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * Create source subgraphs with instance
     */
    public String createSourceSubgraph(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_sourcesubgraph").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    public String createTargetSubgraph(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_targetsubgraph").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    /**
     * create Triple
     */
    public String createTriple(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_triple").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    public void pushLabels(HashSet<String> labels, String uri, SparqlProxy spOutput) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Map<String, String> substitution = new HashMap<>();
        for (String l : labels) {
            if (!l.equals("")) {
                substitution.put("uri", uri);
                substitution.put("label", "\"" + l + "\"");
                spOutput.postSparqlUpdateQuery(createLabel(substitution));
            }
        }
    }

    public String createLabel(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_iri_info").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    @Override
    public void addToOutput(List<SubgraphForOutput> output, SparqlSelect query) {
        SparqlProxy spOutput = SparqlProxy.getSparqlProxy(outputEndpoint);
        String outputIRI = "http://alignment-output#";
        Map<String, String> substitution = new HashMap<>();

        String alignmentHashCode = sourceEndpoint.hashCode() + "-" + targetEndpoint.hashCode();

        substitution.put("alignment", outputAlignmentIRI);
        substitution.put("sourceSubgraph", "<" + outputIRI + "subgraph-source-" + query.hashCode() + alignmentHashCode + ">");
        substitution.put("competencyQuestion", "<" + outputIRI + "competencyQuestion" + query.hashCode() + alignmentHashCode + ">");
        substitution.put("SPARQLCQA", "\"" + query.toString().replaceAll("\n", " ").replaceAll("\"", "\\\\\"\\\\\"") + "\"");
        //TODO: deal with NLCQA (+add NLCQA in insert_cell template)

        substitution.put("form", "\"" + query.toSubgraphForm() + "\"");
        String createSourceInstance = createSourceSubgraph(substitution);
        try {
            spOutput.postSparqlUpdateQuery(createSourceInstance);
        } catch (SparqlQueryMalFormedException
                | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }
        //TODO: only keep instances which

        int i = 0;
        for (SubgraphForOutput s : output) {
            String cellHashCode = query.hashCode() + "-" + i + alignmentHashCode + "-" + i;
            substitution.put("cell", "<" + outputIRI + "cell_" + cellHashCode + ">");
            substitution.put("score", "\"" + s.getAverageSimilarity() + "\"^^xsd:float");
            substitution.put("targetSubgraph", "<" + outputIRI + "subgraph_" + cellHashCode + ">");
            substitution.put("intensionForm", "\"" + s.toIntensionString() + "\"");
            substitution.put("extensionForm", "\"" + s.toExtensionString() + "\"");
            try {
                spOutput.postSparqlUpdateQuery(createCell(substitution));
            } catch (SparqlQueryMalFormedException
                    | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
            try {
                spOutput.postSparqlUpdateQuery(createTargetSubgraph(substitution));
            } catch (SparqlQueryMalFormedException
                    | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }

            if (s instanceof TripleSubgraph) {
                int j = 0;
                for (Triple t : ((TripleSubgraph) s).getTriples()) {
                    substitution.put("triple", "<" + outputIRI + "triple_" + cellHashCode + j + ">");
                    substitution.put("predicate", t.getPredicate().toString());
                    substitution.put("similarity", "\"" + t.getSimilarity() + "\"^^xsd:float");
                    substitution.put("instance", t.getAnswer().toString());
                    if (t.keepObjectType) {
                        substitution.put("object", t.getObjectType().toString());
                    } else {
                        substitution.put("object", t.getObject().toValueString());
                    }
                    if (t.keepSubjectType) {
                        substitution.put("subject", t.getSubjectType().toString());
                    } else {
                        substitution.put("subject", t.getSubject().toValueString());
                    }

                    substitution.put("objectSimilarity", "\"" + t.getObjectSimilarity() + "\"^^xsd:float");
                    substitution.put("subjectSimilarity", "\"" + t.getSubjectSimilarity() + "\"^^xsd:float");
                    substitution.put("predicateSimilarity", "\"" + t.getPredicateSimilarity() + "\"^^xsd:float");
                    substitution.put("keepObjectType", "\"" + t.keepObjectType + "\"^^xsd:boolean");
                    substitution.put("keepSubjectType", "\"" + t.keepSubjectType + "\"^^xsd:boolean");
                    try {
                        spOutput.postSparqlUpdateQuery(createTriple(substitution));


                        if (!t.isSubjectTriple() || !t.keepSubjectType) {
                            pushLabels(t.getSubject().getLabels(), t.getSubject().toString(), spOutput);
                        }
                        if (!t.isObjectTriple() || !t.keepObjectType) {
                            if (t.getObject() instanceof IRI) {
                                pushLabels(((IRI) t.getObject()).getLabels(), t.getObject().toString(), spOutput);
                            }
                        }
                        pushLabels(t.getPredicate().getLabels(), t.getPredicate().toString(), spOutput);

                        if (t.keepSubjectType) {
                            pushLabels(t.getSubjectType().getLabels(), t.getSubjectType().toString(), spOutput);
                        }
                        if (t.keepObjectType) {
                            pushLabels(t.getObjectType().getLabels(), t.getObjectType().toString(), spOutput);
                        }
                    } catch (SparqlQueryMalFormedException
                            | SparqlEndpointUnreachableException e) {
                        e.printStackTrace();
                    }
                    j++;
                }
                i++;
            }
        }

    }

    @Override
    public void end() {

    }

}
