# Transaction Aggregator

Here you can view the code snipplets from my Class files for this project.

## src/aggregator/service/AggregatorController.java

```java
// This class is a RESTful controller that aggregates transactions from multiple sources
@RestController
@EnableCaching // Enables Spring's caching capabilities
@EnableAsync // Enables asynchronous method execution
public class AggregatorController {

    private final RestTemplate restTemplate;
    private static final int MAX_RETRIES = 5; // Maximum number of retry attempts for failed requests

    // Constructor injection of RestTemplate
    @Autowired
    public AggregatorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Endpoint to aggregate transactions for a given account
    @GetMapping("/aggregate")
    @Cacheable(value = "aggregatedTransactionsCache", key = "#account") // Caches the result based on the account
    public List<Transaction> aggregateTransactions(@RequestParam String account) throws Exception {
        // URLs for the two transaction sources
        String url1 = "http://localhost:8888/transactions?account=" + account;
        String url2 = "http://localhost:8889/transactions?account=" + account;

        // Asynchronously fetch transactions from both sources
        CompletableFuture<List<Transaction>> future1 = fetchTransactionsWithRetryAsync(url1);
        CompletableFuture<List<Transaction>> future2 = fetchTransactionsWithRetryAsync(url2);

        // Combine transactions from both sources
        List<Transaction> combinedTransactions = new ArrayList<>();
        combinedTransactions.addAll(future1.get());
        combinedTransactions.addAll(future2.get());

        // Sort combined transactions by timestamp in descending order
        return combinedTransactions.stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .collect(Collectors.toList());
    }

    // Asynchronously fetch transactions with retry logic
    @Async
    @Cacheable(value = "transactionsCache", key = "#url") // Caches the result based on the URL
    public CompletableFuture<List<Transaction>> fetchTransactionsWithRetryAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    // Attempt to fetch transactions
                    ResponseEntity<List<Transaction>> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<Transaction>>() {}
                    );
                    return response.getBody();
                } catch (HttpServerErrorException e) {
                    // Handle specific server errors (529 or 503)
                    if (e.getRawStatusCode() == 529 || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                        if (attempt == MAX_RETRIES - 1) {
                            return new ArrayList<>(); // Return empty list if all retries fail
                        }
                        try {
                            Thread.sleep(100); // Short delay before retrying
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        throw e; // Rethrow other HTTP server errors
                    }
                } catch (Exception e) {
                    return new ArrayList<>(); // Return empty list for other exceptions
                }
            }
            return new ArrayList<>(); // Return empty list if all retries fail
        });
    }
}
```
## src/aggregator/service/Transaction.java

```java

public class Transaction implements Comparable<Transaction> {
    private String id;
    private String serverId;
    private String account;
    private String amount;
    private String timestamp;

    // Default constructor
    public Transaction() {}

    // JSON Creator constructor
    @JsonCreator
    public Transaction(
            @JsonProperty("id") String id,
            @JsonProperty("serverId") String serverId,
            @JsonProperty("account") String account,
            @JsonProperty("amount") String amount,
            @JsonProperty("timestamp") String timestamp) {
        this.id = id;
        this.serverId = serverId;
        this.account = account;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Getters and setters

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("serverId")
    public String getServerId() {
        return serverId;
    }

    @JsonProperty("account")
    public String getAccount() {
        return account;
    }

    @JsonProperty("amount")
    public String getAmount() {
        return amount;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Transaction otherTransaction) {
        return this.timestamp.compareTo(otherTransaction.timestamp);
    }
}
```

## src/aggregator/Application.java

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```
