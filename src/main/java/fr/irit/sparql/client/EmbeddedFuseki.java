package fr.irit.sparql.client;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

import java.util.HashMap;
import java.util.Map;

public class EmbeddedFuseki {

    static private EmbeddedFuseki fusekiServer;
    private final HashMap<String, Dataset> datasets;
    private FusekiServer server;

    public EmbeddedFuseki() {
        datasets = new HashMap<>();
    }

    static public EmbeddedFuseki getFusekiServer() {
        if (EmbeddedFuseki.fusekiServer == null) {
            EmbeddedFuseki.fusekiServer = new EmbeddedFuseki();
        }
        return EmbeddedFuseki.fusekiServer;
    }

    public void addDataset(String name, String filePath) {
        System.out.println("Loading " + filePath + " into (" + name + ") KB...");
        Model m = ModelFactory.createDefaultModel();
        Model localKB = FileManager.get().loadModel(filePath);
        m.add(localKB);
        Dataset ds = DatasetFactory.create(m);
        datasets.put(name, ds);
    }

    public void startServer() {
        System.out.println("Trying to create the server");
        FusekiServer.Builder serverBuilder = FusekiServer.create().setPort(3031);
        System.out.println("addition of datasets");
        for (Map.Entry<String, Dataset> e : datasets.entrySet()) {
            serverBuilder.add("/" + e.getKey(), e.getValue());
        }
        server = serverBuilder.build();
        server.start();
        System.out.println("Fuseki server started as localhost:" + server.getPort());
    }

    public void closeConnection() {
        server.stop();
    }
}
