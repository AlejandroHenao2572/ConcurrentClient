import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentClient {
    private static final String URL = "http://localhost:8080/api/turn/ticket";

    public static void main(String[] args) throws Exception {
        int requests = 100;
        int concurrency = 20;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(10))
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            pool.submit(() -> {
                try {
                    HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                    success.incrementAndGet();
                    System.out.println(resp.statusCode() + " " + (resp.body() == null ? "" : resp.body()));
                } catch (Exception e) {
                    failed.incrementAndGet();
                    System.out.println("ERROR: " + e.toString());
                } finally {
                    latch.countDown();
                }
            });
        }

    
        latch.await();
        pool.shutdownNow();

        System.out.printf("Done: requests=%d, created_tickets=%d, failed_tickets=%d",
                requests, concurrency, success.get(), failed.get());
    }
}