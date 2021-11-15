package fr.irit.main;

import fr.irit.complex.utils.Parameters;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;

public class A1 {


    public static void main(String[] args) {
        Dataset d1 = DatasetFactory.create("/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/cmt_100.ttl");
        Dataset d2 = DatasetFactory.create("/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/conference_100.ttl");
        FusekiServer server = FusekiServer.create()
                .add("/cmt_100", d1, true)
                .add("/conference_100", d2, true)
                .build();
        server.start();
    }
}
