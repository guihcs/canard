package fr.irit.sparql.client;

//import org.apache.jena.fuseki.embedded.FusekiServer;
//import org.apache.jena.query.Dataset;
//import org.apache.jena.query.DatasetFactory;


import java.util.HashMap;
import java.util.Map;

public class EmbeddedFuseki {

    private static EmbeddedFuseki instance;
//    private final Map<String, Dataset> datasets;
//    private FusekiServer server;

    public EmbeddedFuseki() {
//        datasets = new HashMap<>();
    }


    public static EmbeddedFuseki getInstance() {
        if (instance == null) instance = new EmbeddedFuseki();
        return instance;
    }

    public void addDataset(String name, String filePath) {
//        Dataset dataset = DatasetFactory.create(filePath);
//        datasets.put(name, dataset);
    }

    public void startServer() {
//        FusekiServer.Builder serverBuilder = FusekiServer.create().setPort(3031);
//
//        for (Map.Entry<String, Dataset> e : datasets.entrySet()) {
//            serverBuilder.add("/" + e.getKey(), e.getValue());
//        }
//        server = serverBuilder.build();
//        server.start();
    }

    public void stop() {
//        server.stop();
    }


    public int getPort(){
//        return server.getPort();
        return 0;
    }
}
