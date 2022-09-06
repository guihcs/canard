package fr.irit.main;

import fr.irit.complex.ComplexAlignmentGeneration;
import fr.irit.complex.QueryArityException;
import fr.irit.input.ParameterException;
import fr.irit.similarity.ScibertSim;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
//import io.netty.util.internal.StringUtil;
//import org.neo4j.dbms.api.DatabaseManagementService;
//import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
//import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;


public class Main {


    public static void main(String[] args) {
//        Dataset d1 = DatasetFactory.create("/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/cmt_100.ttl");
//        Dataset d2 = DatasetFactory.create("/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/conference_100.ttl");
//        FusekiServer server = FusekiServer.create()
//                .add("/cmt_100", d1, true)
//                .add("/conference_100", d2, true)
//                .build();
//        server.start();
        ExecutionConfig executionConfig = null;
//
        try {
            System.out.println("===============================================================================");
            executionConfig = new ExecutionConfig(args);

            ComplexAlignmentGeneration complexAlignmentGeneration = new ComplexAlignmentGeneration(executionConfig);
            System.out.println("Running with " + executionConfig.getMaxMatches() + " support instances - " + executionConfig.getSimilarityThreshold() + " similarity.");

            complexAlignmentGeneration.run();

            executionConfig.end();

            System.out.println("Matching process ended");


        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException | IOException ex) {
            ex.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("At least 1 argument is expected, you provided: " + args.length +
                    "\nPlease refer to Readme.md file");
        } catch (ParameterException | QueryArityException e) {
            System.err.println(e.getMessage());
        } finally {
            if (executionConfig != null) executionConfig.end();
        }




    }
}
