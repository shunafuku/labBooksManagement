import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenLibraryRDFFetcher {
    private static final String BASE_URL = "https://openlibrary.org/isbn/";

    public static void main(String[] args) {
        String isbn = "9780140328721"; // 例としてのISBN
        fetchBookRDF(isbn);
    }

    public static void fetchBookRDF(String isbn) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + isbn + ".rdf"))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("RDF Data for ISBN " + isbn + ":");
                System.out.println(response.body());
            } else {
                System.out.println("Error: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}