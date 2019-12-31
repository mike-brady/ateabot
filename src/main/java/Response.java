public class Response {
    private int responseCode;
    private String headers;
    private String body;

    public Response(int responseCode, String headers, String body) {
        this.responseCode = responseCode;
        this.headers = headers;
        this.body = body;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
