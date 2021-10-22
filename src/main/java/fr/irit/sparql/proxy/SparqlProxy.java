package fr.irit.sparql.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;


public class SparqlProxy {

    private static final HashMap<String, SparqlProxy> insts = new HashMap<>();
    private final String urlServer;
    private HttpClient httpClient;

    private SparqlProxy(String urlServer) {
        this.urlServer = urlServer;
    }

    public static SparqlProxy getSparqlProxy(String url) {

        SparqlProxy inst = insts.get(url);

        if (inst == null) {
            inst = new SparqlProxy(url);
            inst.httpClient = HttpClient.newHttpClient();
            insts.put(url, inst);
        }

        return inst;
    }

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

    public ArrayList<JsonNode> getResponse(String query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        ArrayList<JsonNode> arr = new ArrayList<>();
        query = SparqlProxy.cleanString(query);

        URI uri = URI.create(urlServer + "sparql?output=json&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpRequest httpRequest = HttpRequest
                .newBuilder(uri)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            for (JsonNode jsonNode : root.get("results").get("bindings")) {
                arr.add(jsonNode);
            }
        } catch (MalformedURLException ex) {
            throw new SparqlQueryMalFormedException("Query malformed : " + query);
        } catch (UnsupportedEncodingException ex) {
            throw new SparqlQueryMalFormedException("Encoding unsupported");
        } catch (IOException ex) {
            throw new SparqlEndpointUnreachableException(ex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return arr;
    }

    public ArrayList<JsonNode> getResponse(SparqlSelect query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        return getResponse(query.toString());
    }

    public void postSparqlUpdateQuery(String query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
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


    public boolean sendAskQuery(String query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
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
