package fr.irit.input;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class RunConfig {

    private Map<String, String> sourceOntology;
    private Map<String, String> targetOntology;
    private String cqaFolder;
    private List<Map<String, String>> output;


    private RunConfig(){

    }

    public static RunConfig load(String path) throws IOException, ParameterException {
        Gson gson = new Gson();

        RunConfig runConfig = gson.fromJson(Files.readString(Path.of(path)), RunConfig.class);
        checkParams(runConfig);

        return runConfig;
    }

    private static void checkParams(RunConfig runConfig) throws ParameterException {
        if (runConfig.sourceOntology == null){
            throw new ParameterException("No source ontology in parameter file");
        }

        if (!runConfig.sourceOntology.containsKey("file") && !runConfig.sourceOntology.containsKey("sparqlEndpoint")){
            throw new ParameterException("The source ontology field has no file or sparqlEndpoint parameter");
        }

        if (runConfig.output == null){
            throw new ParameterException("No output in parameter file");
        }

        if (runConfig.output.isEmpty()){
            throw new ParameterException("The output parameter is empty");
        }

        for (Map<String, String> out : runConfig.output){
            if (!out.containsKey("type")){
                throw new ParameterException("An output has no type field");
            }
            if (out.get("type").equals("sparql") && !out.containsKey("endpoint")){
                throw new ParameterException("The SPARQL output has no specified endpoint");
            }
        }
    }


    public Map<String, String> getSourceOntology() {
        return sourceOntology;
    }

    public Map<String, String> getTargetOntology() {
        return targetOntology;
    }

    public String getCqaFolder() {
        return cqaFolder;
    }

    public List<Map<String, String>> getOutput() {
        return output;
    }

    public String getSourceEndpoint(){
        if (sourceOntology.containsKey("sparqlEndpoint")) {
           return sourceOntology.get("sparqlEndpoint");
        } else if (sourceOntology.containsKey("file")) {
            return "http://localhost:3031/sourceOntology/";
        }
        return null;
    }

    public String getTargetEndpoint(){
        if (targetOntology.containsKey("sparqlEndpoint")) {
            return targetOntology.get("sparqlEndpoint");
        } else if (targetOntology.containsKey("file")) {
            return "http://localhost:3031/targetOntology/";
        }
        return null;
    }


    public String getSourceQueryTemplate(){
        return sourceOntology.getOrDefault("queryTemplates", "/queryTemplates/generic");
    }

    public String getTargetQueryTemplate(){
        return targetOntology.getOrDefault("queryTemplates", "/queryTemplates/generic");
    }
}
