package org.jetbrains.teamcity.githubauth;

import com.intellij.openapi.diagnostic.Log4jLogger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.TestInternalProperties;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.controllers.interceptors.auth.util.HttpAuthUtil;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.github.GitHubConstants;
import jetbrains.buildServer.users.DuplicateUserAccountException;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.util.StringUtil;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.BDDAssertions.then;
import static org.jetbrains.teamcity.githubauth.GitHubOAuth.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Test
public class GitHubOAuthTest {

    private static final String CLIENT_ID = "321";
    private static final String CLIENT_SECRET = "123";
    private static final String TC_URL = "http://teamcity.com";

    private GitHubOAuth gitHubOAuth;
    private TeamCityCoreFacade teamCityCoreMock;
    private OAuthConnectionDescriptor rootProjectConnection;
    private SUser tcUser;
    private MockHttpSession session;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockRestServiceServer server;

    @BeforeMethod
    public void setUp() throws Exception {
        TestInternalProperties.init();
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        GitHubOAuthClient gitHubClient = new GitHubOAuthClient(restTemplate);

        teamCityCoreMock = mock(TeamCityCoreFacade.class);
        gitHubOAuth = new GitHubOAuth(gitHubClient, teamCityCoreMock);
        Logger logger = Logger.getLogger("oauth");
        logger.setLevel(Level.DEBUG);
        logger.addAppender(new ConsoleAppender(new SimpleLayout()));
        gitHubOAuth.setLogger(new Log4jLogger(logger));

        tcUser = mock(SUser.class);
        when(tcUser.describe(anyBoolean())).thenReturn("TeamCity User");

        rootProjectConnection = new OAuthConnectionDescriptor(mock(SProject.class), mock(SProjectFeatureDescriptor.class), mock(ExtensionHolder.class));
        Map<String, String> connectionParams = new HashMap<>();
        connectionParams.put(GitHubConstants.CLIENT_ID_PARAM, CLIENT_ID);
        connectionParams.put(GitHubConstants.CLIENT_SECRET_PARAM, CLIENT_SECRET);
        when(rootProjectConnection.getParameters()).thenReturn(connectionParams);
        when(teamCityCoreMock.getRootProjectGitHubConnection()).thenReturn(rootProjectConnection);
        when(teamCityCoreMock.getRootUrl()).thenReturn(TC_URL);
        when(teamCityCoreMock.isAuthModuleConfigured(GitHubOAuth.class)).thenReturn(true);

        session = new MockHttpSession();
        newRequest();
    }

    private void setupGitHubApiAnswers(String code, String token, String tokenScope, String userJson) {
        server.reset();

        MultiValueMap<String, String> expectedTokenBody = createTokenRequestBody(CLIENT_ID, CLIENT_SECRET, code);

        server.expect(requestTo("https://github.com/login/oauth/access_token")).andExpect(method(POST))
                .andExpect(content().formData(expectedTokenBody))
                .andRespond(withSuccess(createTokenJson(token, tokenScope), APPLICATION_JSON));

        server.expect(requestTo("https://api.github.com/user?access_token=" + token)).andExpect(method(GET))
                .andRespond(withSuccess(userJson, APPLICATION_JSON));
    }

    private void setupGitHubApiErrorTokenAnswers(String code, String clientId, String clientSecret) {
        server.reset();

        MultiValueMap<String, String> expectedTokenBody = createTokenRequestBody(clientId, clientSecret, code);

        server.expect(requestTo("https://github.com/login/oauth/access_token")).andExpect(method(POST))
                .andExpect(content().formData(expectedTokenBody))
                .andRespond(withSuccess(createErrorTokenJson(), APPLICATION_JSON));
    }

    @NotNull
    private MultiValueMap<String, String> createTokenRequestBody(String clientId, String clientSecret, String code) {
        MultiValueMap<String, String> expectedTokenBody = new LinkedMultiValueMap<>();
        expectedTokenBody.put("client_id", singletonList(clientId));
        expectedTokenBody.put("client_secret", singletonList(clientSecret));
        expectedTokenBody.put("code", singletonList(code));
        expectedTokenBody.put("redirect_uri", singletonList(TC_URL + TOKEN_REDIRECT_URL));
        return expectedTokenBody;
    }

    private void newRequest() {
        request = new MockHttpServletRequest();
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    @Test
    public void should_not_accept__foreign_requests() throws IOException {
        emulateFirstOAuthStep();

        newRequest();
        request.setRequestURI("/foreignPath.html");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.NOT_APPLICABLE);
    }

    @Test
    public void successful_login__new_user_created() throws Exception {
        String code = emulateFirstOAuthStep();
        setupGitHubApiAnswers(code, "token1", DEFAULT_SCOPE, createUserJson("1", "octocat", "monalisa octocat", "octocat@github.com"));

        when(teamCityCoreMock.findUserByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, "1")).thenReturn(UserSet.EMPTY);
        when(teamCityCoreMock.createUser("octocat", "octocat@github.com", "monalisa octocat", singletonMap(GITHUB_USER_ID_PROPERTY_KEY, "1"))).thenReturn(tcUser);
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.AUTHENTICATED);
        then(result.getPrincipal().getName()).isEqualTo("octocat");
        verify(teamCityCoreMock).rememberToken(rootProjectConnection, tcUser, "octocat", "token1", DEFAULT_SCOPE);
    }

    @Test
    public void successful_login__new_user_created_with_empty_email() throws Exception {
        String code = emulateFirstOAuthStep();
        setupGitHubApiAnswers(code, "token1", DEFAULT_SCOPE, createUserJson("1", "octocat", "monalisa octocat", null));

        when(teamCityCoreMock.findUserByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, "1")).thenReturn(UserSet.EMPTY);
        when(teamCityCoreMock.createUser("octocat", null, "monalisa octocat", singletonMap(GITHUB_USER_ID_PROPERTY_KEY, "1"))).thenReturn(tcUser);
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.AUTHENTICATED);
    }

    @Test
    public void successful_login__user_exists() throws Exception {
        String code = emulateFirstOAuthStep();
        setupGitHubApiAnswers(code, "token1", DEFAULT_SCOPE, createUserJson("1", "octocat", "monalisa octocat", "octocat@github.com"));

        when(teamCityCoreMock.findUserByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, "1")).thenReturn(() -> singleton(tcUser));
        when(tcUser.getUsername()).thenReturn("octocat");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.AUTHENTICATED);
        then(result.getPrincipal().getName()).isEqualTo("octocat");
        verify(teamCityCoreMock).rememberToken(rootProjectConnection, tcUser, "octocat", "token1", DEFAULT_SCOPE);
        verify(teamCityCoreMock, never()).createUser(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    public void failed_login__username_exists_and_correspond_to_different_user() throws Exception {
        String code = emulateFirstOAuthStep();
        setupGitHubApiAnswers(code, "token1", DEFAULT_SCOPE, createUserJson("1", "octocat", "monalisa octocat", "octocat@github.com"));

        when(teamCityCoreMock.findUserByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, "1")).thenReturn(UserSet.EMPTY);
        when(teamCityCoreMock.createUser(eq("octocat"), anyString(), anyString(), anyMap())).thenThrow(new DuplicateUserAccountException("octocat"));
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.UNAUTHENTICATED);
        then(HttpAuthUtil.getUnauthenticatedReason(request)).isEqualTo("User with username 'octocat' already exist");
        verify(teamCityCoreMock, never()).rememberToken(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    public void should_not_accept_incorrect_state_param() throws IOException {
        emulateFirstOAuthStep();

        request.setParameter("state", "incorrect_state");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.UNAUTHENTICATED);
        then(HttpAuthUtil.getUnauthenticatedReason(request)).isEqualTo("GitHub login error: 'state' parameter is invalid");
        then(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void should_not_accept_missing_state_param() throws IOException {
        emulateFirstOAuthStep();

        request.removeParameter("state");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.UNAUTHENTICATED);
        then(HttpAuthUtil.getUnauthenticatedReason(request)).isEqualTo("GitHub login error: 'state' parameter is empty");
        then(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void should_not_accept_missing_code_param() throws IOException {
        emulateFirstOAuthStep();

        request.removeParameter("code");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.UNAUTHENTICATED);
        then(HttpAuthUtil.getUnauthenticatedReason(request)).isEqualTo("GitHub login error: 'code' parameter is empty");
        then(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void should_handle_error_when_user_redirected_back() throws IOException {
        emulateFirstOAuthStep();

        request.removeParameter("code");
        request.addParameter("error", "application_suspended");
        request.addParameter("error_description", "Your+application+has+been+suspended.+Contact+support@github.com.");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.UNAUTHENTICATED);
        then(HttpAuthUtil.getUnauthenticatedReason(request)).isEqualTo("GitHub login error: user was redirected with 'error' param.");
        then(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void should_handle_error_when_token_request_failed___invalid_credentials() throws IOException {
        String code = emulateFirstOAuthStep();

        rootProjectConnection.getParameters().put(GitHubConstants.CLIENT_SECRET_PARAM, "incorrectSecret");
        setupGitHubApiErrorTokenAnswers(code, CLIENT_ID, "incorrectSecret");
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.UNAUTHENTICATED);
        then(HttpAuthUtil.getUnauthenticatedReason(request)).isEqualTo("Unexpected GitHub login error (see teamcity-auth.log for the details).");
    }

    private String emulateFirstOAuthStep() {
        String redirect = gitHubOAuth.getUserRedirect(request);
        Map<String, String> params = verifyRedirectUrlAndFetchQueryParams(redirect);

        String code = StringUtil.generateUniqueHash();
        newRequest();
        request.addParameter("code", code);
        request.addParameter("state", params.get("state"));
        request.setRequestURI(params.get("redirect_uri"));
        return code;
    }

    @NotNull
    private Map<String, String> verifyRedirectUrlAndFetchQueryParams(String redirect) {
        then(redirect).isNotNull();
        String queryString = redirect.substring(redirect.indexOf("?") + 1);
        Map<String, String> parsedQueryString = Stream.of(queryString.split("&")).collect(toMap(param -> param.substring(0, param.indexOf("=")),
                param -> param.substring(param.indexOf("=") + 1)));
        then(parsedQueryString).containsEntry("client_id", CLIENT_ID).containsEntry("redirect_uri", TC_URL + TOKEN_REDIRECT_URL).containsKey("state");
        return parsedQueryString;
    }

    private String createTokenJson(String token, String scope) {
        return "{\"access_token\":\"" + token + "\", \"scope\":\"" + scope + "\", \"token_type\":\"bearer\"}";
    }

    private String createErrorTokenJson() {
        return "{" +
                "  \"error\": \"incorrect_client_credentials\",\n" +
                "  \"error_description\": \"The client_id and/or client_secret passed are incorrect.\",\n" +
                "  \"error_uri\": \"https://developer.github.com/v3/oauth/#incorrect-client-credentials\"" +
                "}";
    }

    private String createUserJson(String id, String login, String name, String email) {
        return
                "{\n" +
                        "  \"login\": \"" + login + "\",\n" +
                        "  \"id\": " + id + ",\n" +
                        "  \"avatar_url\": \"https://github.com/images/error/octocat_happy.gif\",\n" +
                        "  \"gravatar_id\": \"\",\n" +
                        "  \"url\": \"https://api.github.com/users/octocat\",\n" +
                        "  \"html_url\": \"https://github.com/octocat\",\n" +
                        "  \"followers_url\": \"https://api.github.com/users/octocat/followers\",\n" +
                        "  \"following_url\": \"https://api.github.com/users/octocat/following{/other_user}\",\n" +
                        "  \"gists_url\": \"https://api.github.com/users/octocat/gists{/gist_id}\",\n" +
                        "  \"starred_url\": \"https://api.github.com/users/octocat/starred{/owner}{/repo}\",\n" +
                        "  \"subscriptions_url\": \"https://api.github.com/users/octocat/subscriptions\",\n" +
                        "  \"organizations_url\": \"https://api.github.com/users/octocat/orgs\",\n" +
                        "  \"repos_url\": \"https://api.github.com/users/octocat/repos\",\n" +
                        "  \"events_url\": \"https://api.github.com/users/octocat/events{/privacy}\",\n" +
                        "  \"received_events_url\": \"https://api.github.com/users/octocat/received_events\",\n" +
                        "  \"type\": \"User\",\n" +
                        "  \"site_admin\": false,\n" +
                        "  \"name\": \"" + name + "\",\n" +
                        "  \"company\": \"GitHub\",\n" +
                        "  \"blog\": \"https://github.com/blog\",\n" +
                        "  \"location\": \"San Francisco\",\n" +
                        "  \"email\": " + (email != null ? "\"" + email + "\"" : "null") + ",\n" +
                        "  \"hireable\": false,\n" +
                        "  \"bio\": \"There once was...\",\n" +
                        "  \"public_repos\": 2,\n" +
                        "  \"public_gists\": 1,\n" +
                        "  \"followers\": 20,\n" +
                        "  \"following\": 0,\n" +
                        "  \"created_at\": \"2008-01-14T04:33:35Z\",\n" +
                        "  \"updated_at\": \"2008-01-14T04:33:35Z\",\n" +
                        "  \"total_private_repos\": 100,\n" +
                        "  \"owned_private_repos\": 100,\n" +
                        "  \"private_gists\": 81,\n" +
                        "  \"disk_usage\": 10000,\n" +
                        "  \"collaborators\": 8,\n" +
                        "  \"plan\": {\n" +
                        "    \"name\": \"Medium\",\n" +
                        "    \"space\": 400,\n" +
                        "    \"private_repos\": 20,\n" +
                        "    \"collaborators\": 0\n" +
                        "  }\n" +
                        "}";
    }
}
