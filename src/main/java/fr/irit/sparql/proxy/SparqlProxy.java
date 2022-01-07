package fr.irit.sparql.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.query.ResultSet;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


public abstract class SparqlProxy {


    public static String cleanString(String s) {
        return s.replaceAll("\r", "").replaceAll("\n", "");
    }

    public static void replaceAll(StringBuilder builder, String from, String to) {
        int index = builder.indexOf(from);
        while (index != -1) {
            builder.replace(index, index + from.length(), to);
            index += to.length();
            index = builder.indexOf(from, index);
        }
    }

    public static List<Map<String, String>> getResponse(String urlServer, String query) {

        List<Map<String, String>> result = new ArrayList<>();

        try (RDFConnection connection = RDFConnectionFactory.connect(urlServer)) {
            ResultSet resultSet = connection.query(query).execSelect();

            resultSet.forEachRemaining(querySolution -> {
                Map<String, String> bind = new HashMap<>();
                querySolution.varNames().forEachRemaining(name ->
                        bind.put(name, querySolution.get(name).toString())
                );
                result.add(bind);
            });
        }

        return result;
    }

    public static void postSparqlUpdateQuery(String urlServer, String query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        HttpURLConnection connection = null;
        try {
            String urlParameters = "update="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8).replaceAll("%22%22", "%5C%22");
            URL url = new URL(urlServer + "update");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(
                    connection.getOutputStream());

            writer.write(urlParameters);
            writer.flush();


            writer.close();
        } catch (UnsupportedEncodingException ex) {
            throw new SparqlQueryMalFormedException("Encoding unsupported");
        } catch (MalformedURLException ex) {
            throw new SparqlQueryMalFormedException("Query malformed");
        } catch (IOException ex) {
            throw new SparqlEndpointUnreachableException(ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    public static boolean sendAskQuery(String urlServer, String query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        boolean ret;

        HttpURLConnection connection = null;
        query = SparqlProxy.cleanString(query);
        try {
            URL url = new URL(urlServer + "sparql?output=json&query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            String jsonRet = response.toString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonRet);
            ret = root.get("boolean").asBoolean();
        } catch (UnsupportedEncodingException ex) {
            throw new SparqlQueryMalFormedException("Encoding unsupported");
        } catch (MalformedURLException ex) {
            throw new SparqlQueryMalFormedException("Query malformed");
        } catch (IOException ex) {
            throw new SparqlEndpointUnreachableException(ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ret;
    }


}
