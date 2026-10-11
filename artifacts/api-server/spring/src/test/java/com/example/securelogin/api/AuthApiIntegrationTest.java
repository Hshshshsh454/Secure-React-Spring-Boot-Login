package com.example.securelogin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.securelogin.api.dto.CsrfResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationLoginProtectedProfileAndLogoutWorkAsOneSession() {
        String email = newTestEmail();
        CsrfState csrf = csrfState();

        ResponseEntity<String> registration = post(
                "/auth/register",
                Map.of("email", email, "password", "correct-horse-battery"),
                csrf,
                null);
        assertEquals(HttpStatus.ACCEPTED, registration.getStatusCode());

        ResponseEntity<String> login = post(
                "/auth/login",
                Map.of("email", email, "password", "correct-horse-battery"),
                csrf,
                null);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        String sessionCookie = login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("JSESSIONID="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Session cookie was not set."));
        assertTrue(sessionCookie.contains("HttpOnly"));
        assertTrue(sessionCookie.contains("Secure"));
        assertTrue(sessionCookie.contains("SameSite=Lax"));

        String cookies = combineCookies(csrf.cookies(), login.getHeaders());
        ResponseEntity<String> profile = get("/auth/me", csrf, cookies);
        assertEquals(HttpStatus.OK, profile.getStatusCode());
        assertEquals(email, readJson(profile.getBody()).path("email").asText());

        ResponseEntity<String> logout = post("/auth/logout", null, csrf, cookies);
        assertEquals(HttpStatus.NO_CONTENT, logout.getStatusCode());

        ResponseEntity<String> afterLogout = get("/auth/me", csrf, cookies);
        assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatusCode());
    }

    @Test
    void duplicateRegistrationAndInvalidCredentialsUseGenericResponses() {
        String email = newTestEmail();
        CsrfState csrf = csrfState();

        ResponseEntity<String> first = post(
                "/auth/register",
                Map.of("email", email, "password", "correct-horse-battery"),
                csrf,
                null);
        ResponseEntity<String> duplicate = post(
                "/auth/register",
                Map.of("email", email, "password", "another-valid-password"),
                csrf,
                null);
        assertEquals(HttpStatus.ACCEPTED, first.getStatusCode());
        assertEquals(HttpStatus.ACCEPTED, duplicate.getStatusCode());
        assertEquals(
                readJson(first.getBody()).path("message").asText(),
                readJson(duplicate.getBody()).path("message").asText());

        ResponseEntity<String> knownUserWrongPassword = post(
                "/auth/login",
                Map.of("email", email, "password", "incorrect-password"),
                csrf,
                null);
        ResponseEntity<String> unknownUser = post(
                "/auth/login",
                Map.of("email", newTestEmail(), "password", "incorrect-password"),
                csrf,
                null);
        assertEquals(HttpStatus.UNAUTHORIZED, knownUserWrongPassword.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, unknownUser.getStatusCode());
        assertEquals(
                readJson(knownUserWrongPassword.getBody()).path("message").asText(),
                readJson(unknownUser.getBody()).path("message").asText());
    }

    @Test
    void csrfAndAuthenticationBoundariesRejectRequestsWithoutRequiredState() {
        CsrfState csrf = csrfState();
        ResponseEntity<String> anonymousProfile = get("/auth/me", csrf, null);
        assertEquals(HttpStatus.UNAUTHORIZED, anonymousProfile.getStatusCode());

        ResponseEntity<String> missingCsrf = postWithoutCsrf(
                "/auth/login",
                Map.of("email", newTestEmail(), "password", "incorrect-password"),
                csrf);
        assertEquals(HttpStatus.FORBIDDEN, missingCsrf.getStatusCode());

        ResponseEntity<String> actuator = restTemplate.getForEntity(
                baseUrl() + "/actuator/health",
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, actuator.getStatusCode());
    }

    private CsrfState csrfState() {
        ResponseEntity<CsrfResponse> response =
                restTemplate.getForEntity(baseUrl() + "/auth/csrf", CsrfResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.SET_COOKIE));
        String csrfCookie = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CSRF cookie was not set."));
        assertTrue(csrfCookie.contains("Secure"));
        assertTrue(csrfCookie.contains("SameSite=Lax"));
        assertTrue(!csrfCookie.contains("HttpOnly"));
        return new CsrfState(
                response.getBody().token(),
                cookiePairs(response.getHeaders()));
    }

    private ResponseEntity<String> post(
            String path, Object body, CsrfState csrf, String cookies) {
        return restTemplate.exchange(
                baseUrl() + path,
                HttpMethod.POST,
                new HttpEntity<>(body, headers(csrf, cookies)),
                String.class);
    }

    private ResponseEntity<String> get(String path, CsrfState csrf, String cookies) {
        return restTemplate.exchange(
                baseUrl() + path,
                HttpMethod.GET,
                new HttpEntity<>(headers(csrf, cookies)),
                String.class);
    }

    private ResponseEntity<String> postWithoutCsrf(
            String path, Object body, CsrfState csrf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE, String.join("; ", csrf.cookies()));
        return restTemplate.exchange(
                baseUrl() + path,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private HttpHeaders headers(CsrfState csrf, String cookies) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-XSRF-TOKEN", csrf.token());
        if (cookies != null && !cookies.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookies);
        } else {
            headers.set(HttpHeaders.COOKIE, String.join("; ", csrf.cookies()));
        }
        return headers;
    }

    private String combineCookies(List<String> initialCookies, HttpHeaders responseHeaders) {
        Map<String, String> byName = new LinkedHashMap<>();
        for (String cookie : initialCookies) {
            byName.put(cookie.substring(0, cookie.indexOf('=')), cookie);
        }
        for (String cookie : cookiePairs(responseHeaders)) {
            byName.put(cookie.substring(0, cookie.indexOf('=')), cookie);
        }
        return String.join("; ", byName.values());
    }

    private List<String> cookiePairs(HttpHeaders headers) {
        List<String> pairs = new ArrayList<>();
        for (String cookie : headers.getOrEmpty(HttpHeaders.SET_COOKIE)) {
            int attributesStart = cookie.indexOf(';');
            pairs.add(attributesStart >= 0 ? cookie.substring(0, attributesStart) : cookie);
        }
        return pairs;
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception exception) {
            throw new AssertionError("Expected a JSON response.");
        }
    }

    private String newTestEmail() {
        return "test-" + UUID.randomUUID() + "@integration.invalid";
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    private record CsrfState(String token, List<String> cookies) {}
}
