package aggregator.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@EnableCaching
@EnableAsync
public class AggregatorController {
    private final RestTemplate restTemplate;
    private static final int MAX_RETRIES = 5;

    @Autowired
    public AggregatorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/aggregate")
    @Cacheable(value = "aggregatedTransactionsCache", key = "#account")
    public List<Transaction> aggregateTransactions(@RequestParam String account) throws Exception {
        String url1 = "http://localhost:8888/transactions?account=" + account;
        String url2 = "http://localhost:8889/transactions?account=" + account;

        CompletableFuture<List<Transaction>> future1 = fetchTransactionsWithRetryAsync(url1);
        CompletableFuture<List<Transaction>> future2 = fetchTransactionsWithRetryAsync(url2);

        List<Transaction> combinedTransactions = new ArrayList<>();
        combinedTransactions.addAll(future1.get());
        combinedTransactions.addAll(future2.get());

        return combinedTransactions.stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Async
    @Cacheable(value = "transactionsCache", key = "#url")
    public CompletableFuture<List<Transaction>> fetchTransactionsWithRetryAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    ResponseEntity<List<Transaction>> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<Transaction>>() {}
                    );
                    return response.getBody();
                } catch (HttpServerErrorException e) {
                    if (e.getRawStatusCode() == 529 || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                        if (attempt == MAX_RETRIES - 1) {
                            return new ArrayList<>();
                        }
                        try {
                            Thread.sleep(100); // Reduced sleep time
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        throw e;
                    }
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
            return new ArrayList<>();
        });
    }
}