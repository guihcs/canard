package fr.irit.n4j;

import org.apache.jena.graph.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Neo4jBuilder {

    public static String buildCreate(List<Triple> triples){
        Map<String, String> idMap = new HashMap<>();
        int id = 0;
        List<String> relations = new ArrayList<>();
        List<String> nodes = new ArrayList<>();
        for (Triple triple : triples) {
            if (!idMap.containsKey(triple.getSubject().toString())) idMap.put(triple.getSubject().toString(), "id" + id++);
            if (!idMap.containsKey(triple.getObject().toString())) idMap.put(triple.getObject().toString(), "id" + id++);
            String predicate = removePrefix(triple.getPredicate().toString());

            String relation = String.format("(%s)-[:%s]->(%s)", idMap.get(triple.getSubject().toString()), predicate.equals("a") ? "type" : predicate, idMap.get(triple.getObject().toString()));
            relations.add(relation);
        }

        for (String idKey : idMap.keySet()) {
            String key = removePrefix(idKey);
            String type = Character.isLowerCase(key.charAt(0)) ? "Instance" : "Class";
            nodes.add(String.format("(%s:%s {label: '%s'})", idMap.get(idKey), type, key));
        }
        nodes.addAll(relations);
        return "create " + String.join(",", nodes);
    }


    public static String removePrefix(String string){
        String[] split = string.split(":");
        return split.length == 1 ? split[0] : split[1];
    }


}
