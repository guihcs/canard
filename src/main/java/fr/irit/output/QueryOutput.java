package fr.irit.output;

import fr.irit.complex.subgraphs.binary.PathSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.unary.TripleSubgraph;
import fr.irit.complex.utils.Parameters;
import fr.irit.sparql.query.select.SparqlSelect;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class QueryOutput extends Output {

    private final String outputFolder;
    private final Map<String, String> CQANames;

    public QueryOutput(Parameters params) {
        super(params);
        outputFolder = params.getOutputQueryFolder();
        CQANames = params.getCqaNames();
    }

    @Override
    public void init() {

    }

    @Override
    public void addToOutput(List<SubgraphForOutput> output, SparqlSelect sq) {
        String cqaName = CQANames.get(sq.toUnchangedString()).replaceAll("\\..*", "");
        File theDir = new File(outputFolder + "/" + cqaName);

        if (!theDir.exists()) {
            System.out.println("creating directory: " + theDir.getName());
            try {
                theDir.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
        int i = 0;
        for (SubgraphForOutput s : output) {
            String outputQuery = "SELECT " + sq.getSelect() + " WHERE {";
            if (s instanceof TripleSubgraph ts) {
                if (s.toIntensionString().contains("somePredicate")) {
                    outputQuery += ts.toSPARQLExtension();
                }
                else if (s.toIntensionString().contains("someObject") || s.toIntensionString().contains("someSubject")) {
                    if (((TripleSubgraph) s).predicateHasMaxSim()) {
                        outputQuery += s.toIntensionString();
                    }
                    else {
                        outputQuery += ((TripleSubgraph) s).toSPARQLExtension();
                    }

                } else {
                    outputQuery += ((TripleSubgraph) s).toSPARQLExtension();
                }
            }

            else if (s instanceof PathSubgraph) {
                outputQuery += s.toIntensionString();
            }


            outputQuery += " } ";
            outputQuery = toSubgraphForm(outputQuery, sq.getSelectFocus());
            try {
                PrintWriter writer = new PrintWriter(outputFolder + "/" + cqaName + "/" + cqaName + i + ".sparql", StandardCharsets.UTF_8);
                writer.println(outputQuery);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            i++;

        }

    }

    @Override
    public void end() {

    }

    public String toSubgraphForm(String queryContent, List<String> selectFocus) {

        String ret = queryContent;
        if (selectFocus.size() > 1) {
            int i = 0;
            for (String sf : selectFocus) {
                ret = ret.replaceAll("\\?answer" + i + " ", sf + " ");
                ret = ret.replaceAll("\\?answer" + i + "\\.", sf + ".");
                ret = ret.replaceAll("\\?answer" + i + "}", sf + "}");
                i++;
            }
        } else {
            ret = ret.replaceAll("\\?answer ", selectFocus.get(0) + " ");
            ret = ret.replaceAll("\\?answer\\.", selectFocus.get(0) + ".");
            ret = ret.replaceAll("\\?answer}", selectFocus.get(0) + "}");
        }
        return ret;
    }

}
