package fr.irit.main;

import fr.irit.complex.ComplexAlignmentGeneration;
import fr.irit.complex.QueryArityException;
import fr.irit.input.ParameterException;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
//import io.netty.util.internal.StringUtil;
//import org.neo4j.dbms.api.DatabaseManagementService;
//import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
//import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;


public class Main {


    public static void main(String[] args) {

//        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(Paths.get("n4j")).build();
//        GraphDatabaseService graphDb = managementService.database("neo4j");
//        Runtime.getRuntime().addShutdownHook(new Thread(managementService::shutdown));

        try {
            System.out.println("===============================================================================");
            ExecutionConfig executionConfig = new ExecutionConfig(args);
            ComplexAlignmentGeneration complexAlignmentGeneration = new ComplexAlignmentGeneration(executionConfig);
            System.out.println("Running with " + executionConfig.getMaxMatches() + " support instances - " + executionConfig.getSimilarityThreshold() + " similarity.");

            complexAlignmentGeneration.run();

            executionConfig.end();
            System.out.println("Matching process ended");


        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException | IOException ex) {
            ex.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("At least 1 argument is expected, you provided: " + args.length +
                    "\nPlease refer to Readme.md file");
        } catch (ParameterException | QueryArityException e) {
            System.err.println(e.getMessage());
        }


    }
}
