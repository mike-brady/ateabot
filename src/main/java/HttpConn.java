import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpConn {
    private HttpClient httpClient;

    private HttpRequest.Builder httpRequestBuilder;
    private HttpRequest httpRequest;
    private String requestUri;
    private String requestMethod;
    private HashMap requestParams;

    private HttpResponse httpResponse;

    public HttpConn() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public boolean startRequest(String uri, String method) {
        clearRequest();
        if(!method.equals("GET") && !method.equals("POST")) {
            return false;
        }
        requestUri = uri;
        requestMethod = method;
        return true;
    }

    public void setParam(String key, String value) {
        requestParams.put(key, value);
    }

    public void setHeader(String key, String value) {
        httpRequestBuilder.setHeader(key, value);
    }

//    public String getHeader(String key) {
//        return connection.getHeaderField(key);
//    }

    public void setAuthHeader(String user, String password) {
        String userCredentials = user + ":" + password;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
        httpRequestBuilder.setHeader ("Authorization", basicAuth);
    }

    public int sendRequest() throws IOException, InterruptedException, URISyntaxException {
        clearResponse();
        String queryString = buildQueryString(requestParams);
        if (requestMethod.equals("GET")) {
            requestUri += queryString;
            httpRequestBuilder.GET();
        } else if(requestMethod.equals("POST")) {
            httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(queryString));
        }
        httpRequestBuilder.uri(new URI(requestUri));
        httpRequest = httpRequestBuilder.build();

        httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return httpResponse.statusCode();
    }

    public Map<String, List<String>> getHeaders() {
        return httpResponse.headers().map();
    }

    public String getBody() {
        return httpResponse.body().toString();
    }

    public int getResponseCode() {
        return httpResponse.statusCode();
    }

    public String stringifyHeaders() {
        StringBuilder output = new StringBuilder();

        int i = 0;
        for(Map.Entry<String, List<String>> entry : httpResponse.headers().map().entrySet()) {
            if(i++ != 0) {
                output.append("\n");
            }
            String key = entry.getKey();
            if(key != null) {
                output.append(key);
                output.append(": ");
            }
            for(String item : entry.getValue()) {
                output.append(item);
            }
        }

        return output.toString();
    }

    public void disconnect() {
        clearResponse();
        httpClient = null;
    }

    public static String getContents(String url) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new URL(url).openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            output.append(inputLine);
        in.close();

        return output.toString();
    }

    private String buildQueryString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (query.length() != 0) {
                query.append('&');
            }
            // Encode the parameter based on the parameter map we've defined
            // and append the values from the map to form a single parameter
            query.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            query.append('=');
            query.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }

        return query.toString();
    }

    private void clearRequest() {
        httpRequestBuilder = HttpRequest.newBuilder();
        httpRequest = null;
        requestUri = null;
        requestParams = new HashMap<String, String>();
        requestMethod = null;
    }

    private void clearResponse() {
        httpResponse = null;
    }

    private String getContent(InputStream is) throws IOException {
        StringBuilder content = new StringBuilder();
        if(is == null) {
            return "";
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
            content.append("\n");
        }
        return content.toString();
    }

    public String getHeader(String key) {
        try {
            return httpResponse.headers().map().get(key).get(0);
        } catch(NullPointerException ex) {
            return null;
        }
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }
}
