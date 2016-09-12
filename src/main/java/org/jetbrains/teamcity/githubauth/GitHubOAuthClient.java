package org.jetbrains.teamcity.githubauth;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static java.util.Collections.singletonList;

public class GitHubOAuthClient {

    @NotNull
    private final RestTemplate restTemplate;

    public GitHubOAuthClient(@NotNull RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @NotNull
    public String getUserRedirect(@NotNull String clientId, @NotNull String scope, @NotNull String redirectUrl) {
        return String.format("https://github.com/login/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s",
                clientId, scope, redirectUrl);
    }

    @NotNull
    public String exchangeCodeToToken(@NotNull String code, @NotNull String clientId, @NotNull String clientSecret,
                                      @NotNull String redirectUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.put("client_id", singletonList(clientId));
        body.put("client_secret", singletonList(clientSecret));
        body.put("code", singletonList(code));
        body.put("redirect_uri", singletonList(redirectUrl));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            TokenResponse response = restTemplate.postForObject("https://github.com/login/oauth/access_token", request, TokenResponse.class);
            return response.access_token;
        } catch (RestClientException e) {
            throw new GitHubLoginException("Error obtaining GitHub OAuth token", e);
        }
    }

    @NotNull
    public GitHubUser getUser(@NotNull String token) {
        try {
            return restTemplate.getForObject("https://api.github.com/user?access_token=" + token, GitHubUser.class);
        } catch (RestClientException e) {
            throw new GitHubLoginException("Error obtaining GitHub user", e);
        }
    }

    public static final class TokenResponse {
        public String access_token;
    }

}
