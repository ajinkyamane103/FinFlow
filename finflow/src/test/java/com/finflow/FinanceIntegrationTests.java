package com.finflow;

import com.finflow.repository.AccountTransactionRepository;
import com.finflow.repository.BankAccountRepository;
import com.finflow.repository.UserRepository;
import com.finflow.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private BankAccountRepository accountRepository;
    @Autowired private AccountTransactionRepository transactionRepository;
    @Autowired private IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void profileAccountsLedgerTransfersDashboardAndOwnershipAreEnforced() throws Exception {
        register("owner@example.com");
        String token = login("owner@example.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        long primaryId = createAccount(token, "Primary", "CHECKING", "INR", "100.00");
        long savingsId = createAccount(token, "Savings", "SAVINGS", "INR", "0.00");
        long reserveId = createAccount(token, "Reserve", "SAVINGS", "INR", "0.00");

        mockMvc.perform(post("/api/accounts/{id}/deposits", primaryId)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"25.00\",\"description\":\"Paycheck\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.balanceAfter").value(125.00));

        // Retrying with the same key returns the original ledger result and does not credit twice.
        mockMvc.perform(post("/api/accounts/{id}/deposits", primaryId)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"25.00\",\"description\":\"Paycheck\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(125.00));

        mockMvc.perform(post("/api/accounts/{id}/deposits", primaryId)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"26.00\",\"description\":\"Paycheck\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(post("/api/accounts/{id}/withdrawals", primaryId)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"25.00\",\"description\":\"Paycheck\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/accounts/{id}/withdrawals", primaryId)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "withdraw-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"30.00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(95.00));

        mockMvc.perform(post("/api/accounts/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "transfer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":" + primaryId + ",\"destinationAccountId\":" + savingsId
                                + ",\"amount\":\"10.00\",\"description\":\"Move savings\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.balanceAfter").value(85.00));

        mockMvc.perform(post("/api/accounts/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "transfer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":" + primaryId + ",\"destinationAccountId\":" + reserveId
                                + ",\"amount\":\"10.00\",\"description\":\"Move savings\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(get("/api/accounts/{id}", savingsId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10.00));
        mockMvc.perform(get("/api/accounts/{id}/transactions?size=5&sort=createdAt,desc", primaryId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalanceByCurrency.INR").value(95.00));

        mockMvc.perform(post("/api/accounts/{id}/withdrawals", primaryId)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "too-much")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"1000.00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        register("other@example.com");
        String otherToken = login("other@example.com");
        mockMvc.perform(get("/api/accounts/{id}", primaryId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void concurrentDuplicateDepositDoesNotApplyTwice() throws Exception {
        register("concurrent@example.com");
        String token = login("concurrent@example.com");
        long accountId = createAccount(token, "Concurrent", "CHECKING", "INR", "0.00");

        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var request = (java.util.concurrent.Callable<Integer>) () -> {
                start.await();
                return mockMvc.perform(post("/api/accounts/{id}/deposits", accountId)
                                .header("Authorization", bearer(token))
                                .header("Idempotency-Key", "concurrent-deposit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\":\"10.00\",\"description\":\"Concurrent test\"}"))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            assertThat(statuses).contains(201);
            assertThat(statuses).allMatch(status -> status == 201 || status == 409);
        }

        mockMvc.perform(get("/api/accounts/{id}", accountId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10.00));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    private long createAccount(String token, String name, String type, String currency, String balance) throws Exception {
        String body = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"accountType\":\"" + type
                                + "\",\"currency\":\"" + currency + "\",\"initialBalance\":\"" + balance + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\",\"email\":\"" + email
                                + "\",\"password\":\"Password123\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.path("accessToken").asText();
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
