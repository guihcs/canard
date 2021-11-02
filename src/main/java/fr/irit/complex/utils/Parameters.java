package fr.irit.complex.utils;

import fr.irit.input.ParameterException;
import fr.irit.input.RunConfig;
import fr.irit.sparql.client.EmbeddedFuseki;
import fr.irit.sparql.files.QueryTemplate;
import fr.irit.sparql.query.select.SparqlSelect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Parameters {
    private final Map<String, Map<String, QueryTemplate>> queryTemplates;
    private boolean outputEDOAL;
    private boolean outputSPARQL;
    private boolean outputQUERY;
    private String outputEndpoint;
    private String outputEdoalFile;
    private String outputQueryFolder;
    private String sourceEndpoint;
    private String targetEndpoint;
    private final List<SparqlSelect> queries;
    private final Map<String, String> cqaNames;
    private String cqaFolder;
    private boolean startEmbeddedFuseki;
    private boolean cqaToBeGenerated;

    private Parameters() {
        cqaToBeGenerated = false;
        startEmbeddedFuseki = false;

        queryTemplates = new HashMap<>();
        queries = new ArrayList<>();
        cqaNames = new HashMap<>();
    }

    public static Parameters load(String path) throws IOException, ParameterException {
        Parameters parameters = new Parameters();
        RunConfig runConfig = RunConfig.load(path);
        String sourceEndpoint = runConfig.getSourceEndpoint();

        if (runConfig.getSourceOntology().containsKey("file")) {
            EmbeddedFuseki fusekiServer = EmbeddedFuseki.getFusekiServer();
            fusekiServer.addDataset("sourceOntology", runConfig.getSourceOntology().get("file"));
            parameters.startEmbeddedFuseki = true;
        }

        parameters.queryTemplates.put(sourceEndpoint, loadQueries(runConfig.getSourceQueryTemplate()));

        parameters.sourceEndpoint = sourceEndpoint;


        String targetEndpoint = runConfig.getTargetEndpoint();

        if (runConfig.getTargetOntology().containsKey("file")) {
            EmbeddedFuseki fusekiServer = EmbeddedFuseki.getFusekiServer();
            fusekiServer.addDataset("targetOntology", runConfig.getTargetOntology().get("file"));
            parameters.startEmbeddedFuseki = true;
        }

        parameters.queryTemplates.put(targetEndpoint, loadQueries(runConfig.getTargetQueryTemplate()));

        parameters.targetEndpoint = targetEndpoint;

        if (runConfig.getCqaFolder() == null) {
            parameters.cqaFolder = "generatedCQA";
            parameters.cqaToBeGenerated = true;

        } else {
            parameters.cqaFolder = runConfig.getCqaFolder();

        }


        for (Map<String, String> o : runConfig.getOutput()) {

            if (o.get("type").equals("edoal")) {
                parameters.outputEDOAL = true;
                parameters.outputEdoalFile = o.getOrDefault("file", "./output.edoal");

            } else if (o.get("type").equals("query")) {
                parameters.outputQUERY = true;
                parameters.outputQueryFolder = o.getOrDefault("folder", "./outputQueries/");

                if(Files.notExists(Paths.get(parameters.outputQueryFolder))){
                    System.out.println("creating directory: " + parameters.outputQueryFolder);
                    Files.createDirectory(Paths.get(parameters.outputQueryFolder));
                }


            } else if (o.get("type").equals("sparql")) {
                parameters.outputSPARQL = true;
                parameters.outputEndpoint = o.get("endpoint");
                parameters.queryTemplates.put(parameters.outputEndpoint, loadQueries("/queryTemplates/output/"));
            }
        }


        if (parameters.startEmbeddedFuseki) {
            EmbeddedFuseki fusekiServer = EmbeddedFuseki.getFusekiServer();
            fusekiServer.startServer();
        }

        if (parameters.cqaToBeGenerated) {
            System.out.println("cqa to be generated");
            CQAGenerator generator = new CQAGenerator(parameters.sourceEndpoint, parameters.cqaFolder);
            generator.cleanCQARepository();
            generator.createCQAs();
        }
        Path queryFolder = Paths.get(parameters.cqaFolder);
        if (Files.notExists(queryFolder)){
            throw new IOException("CQA path not exists");
        }
        if (!Files.isDirectory(queryFolder)){
            throw new IOException("CQA path is not a folder");
        }



        List<Path> pathList = Files.walk(queryFolder)
                .filter(path1 -> path1.toString().endsWith(".sparql"))
                .collect(Collectors.toList());

        if (pathList.isEmpty()){
            System.out.println("No queries in CQA folder.");
        }

        for(Path path1 : pathList){
            SparqlSelect sq = new SparqlSelect(Files.readString(path1));
            parameters.queries.add(sq);
            parameters.cqaNames.put(sq.toUnchangedString(), path1.getFileName().toString());
        }

        return parameters;
    }

    private static Map<String, QueryTemplate> loadQueries(String path) throws IOException {
        final Path folderPath = Paths.get(Objects.requireNonNull(Parameters.class.getResource(path)).getPath());

        if (!Files.isDirectory(folderPath)) {
            throw new IOException("The path is not a directory");
        }

        Map<String, QueryTemplate> queryTemplates = new HashMap<>();
        List<Path> pathList = Files.walk(folderPath)
                .filter(path1 -> path1.toString().endsWith(".sparql"))
                .collect(Collectors.toList());

        for(Path path1 : pathList){

            String query = Files.readString(path1);
            String fileName = path1.getFileName().toString().split("\\.")[0];
            if (query.replaceAll("\n", " ").matches("^.*\\{\\{ ?([A-Za-z0-9]+) ?}}.*$")) {
                queryTemplates.put(fileName, new QueryTemplate(query));
            }
        }

        return queryTemplates;

    }


    public Map<String, Map<String, QueryTemplate>> getQueryTemplates() {
        return queryTemplates;
    }

    public String getOutputEdoalFile() {
        return outputEdoalFile;
    }


    public String getSourceEndpoint() {
        return sourceEndpoint;
    }


    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public String getOutputQueryFolder() {
        return outputQueryFolder;
    }

    public Map<String, String> getCqaNames() {
        return cqaNames;
    }


    public String getOutputEndpoint() {
        return outputEndpoint;
    }

    public boolean isOutputEDOAL() {
        return outputEDOAL;
    }

    public boolean isOutputSPARQL() {
        return outputSPARQL;
    }

    public boolean isOutputQUERY() {
        return outputQUERY;
    }

    public boolean isStartEmbeddedFuseki() {
        return startEmbeddedFuseki;
    }

    public List<SparqlSelect> getQueries() {
        return queries;
    }
}
