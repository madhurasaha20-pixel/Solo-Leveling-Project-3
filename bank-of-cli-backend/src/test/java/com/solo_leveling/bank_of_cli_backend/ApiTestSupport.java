package com.solo_leveling.bank_of_cli_backend;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for tests that call the API through MockMvc. Rolls back after each test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ApiTestSupport {
    static final String REGISTER = "/api/auth/register";
    static final String LOGIN = "/api/auth/login";
    static final String LOGOUT = "/api/auth/logout";

    @Autowired
    protected MockMvc mockMvc;

    protected static String registerBody(String name, String email, String password) {
        return """
                {"name": "%s", "email": "%s", "password": "%s"}
                """.formatted(name, email, password);
    }

    protected static String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    protected ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions register(String name, String email, String password) throws Exception {
        return postJson(REGISTER, registerBody(name, email, password));
    }

    /** Registers a user and returns the register response's JSON body. */
    protected String registerAndGetBody(String name, String email, String password) throws Exception {
        MvcResult result = register(name, email, password).andExpect(status().isCreated()).andReturn();
        return result.getResponse().getContentAsString();
    }

    protected static String field(String json, String jsonPath) {
        return JsonPath.read(json, jsonPath);
    }

    /** Builds an HS256 JWT by hand, so tests don't depend on the implementation's classes. */
    protected static String forgeToken(String subject, String secret) throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(
                "{\"sub\":\"%s\",\"iat\":%d}".formatted(subject, Instant.now().getEpochSecond())
                        .getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = b64.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }
}
