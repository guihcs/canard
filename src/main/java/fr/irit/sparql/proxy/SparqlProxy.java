package fr.irit.sparql.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import fr.irit.input.CQAManager;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    public List<Map<String, SelectResponse.Results.Binding>> getResponse(String query) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        List<Map<String, SelectResponse.Results.Binding>> bindings = new ArrayList<>();
        query = SparqlProxy.cleanString(query);
        URI uri = URI.create(urlServer + "sparql?output=json&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpRequest httpRequest = HttpRequest
                .newBuilder(uri)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            SelectResponse selectResponse = gson.fromJson(response.body(), SelectResponse.class);
            bindings = selectResponse.getResults().getBindings();

        } catch (MalformedURLException ex) {
            throw new SparqlQueryMalFormedException("Query malformed : " + query);
        } catch (UnsupportedEncodingException ex) {
            throw new SparqlQueryMalFormedException("Encoding unsupported");
        } catch (IOException ex) {
            throw new SparqlEndpointUnreachableException(ex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return bindings;
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



    public static Set<String> retrieveIRILabels(String endpoint, IRI iri) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        Set<String> labels = new HashSet<>();
        Pattern pattern = Pattern.compile("<([^>]+)[#/]([A-Za-z0-9_-]+)>");

        Matcher matcher = pattern.matcher(iri.getValue());

        if (matcher.find()) {
            addLabel(labels, matcher.group(2));
        } else {
            addLabel(labels, iri.getValue());
        }


        Map<String, String> substitution = new HashMap<>();
        substitution.put("uri", iri.getValue());
        String literalQuery = CQAManager.getInstance().getLabelQuery(endpoint, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpoint);

        List<Map<String, SelectResponse.Results.Binding>> response = spIn.getResponse(literalQuery);

        for (Map<String, SelectResponse.Results.Binding> jsonNode : response) {
            String s = jsonNode.get("x").getValue();
            Resource res = new Resource(s);
            if (!res.isIRI()) {
                addLabel(labels, s);
            }

        }

        return labels;
    }


    private static void addLabel(Set<String> labels, String label) {
        labels.add(label.trim().replaceAll("\\\\", "").toLowerCase());
    }


    public static List<Map<String, SelectResponse.Results.Binding>> getAnswers(SparqlSelect sparqlSelect, String endpoint) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        SparqlProxy sparqlProxy = SparqlProxy.getSparqlProxy(endpoint);
        return sparqlProxy.getResponse(sparqlSelect.toUnchangedString());
    }
}
