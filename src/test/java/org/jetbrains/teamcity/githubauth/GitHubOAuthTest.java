package org.jetbrains.teamcity.githubauth;

import com.intellij.openapi.diagnostic.Log4jLogger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.TestInternalProperties;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.github.GitHubConstants;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserSet;
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

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.jetbrains.teamcity.githubauth.GitHubOAuth.DEFAULT_SCOPE;
import static org.jetbrains.teamcity.githubauth.GitHubOAuth.GITHUB_USER_ID_PROPERTY_KEY;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Test
public class GitHubOAuthTest {

    private static final String OAUTH_CODE = "code123";
    private static final String OAUTH_TOKEN = "e72e16c7e42f292c6912e7710c838347ae178b4a";
    private static final String CLIENT_ID = "321";
    private static final String CLIENT_SECRET = "123";
    private static final String TC_URL = "http://teamcity.com";
    private static final String GITHUB_USER_LOGIN = "octocat";
    private static final String GITHUB_USER_ID = "1";

    private GitHubOAuth gitHubOAuth;
    private TeamCityCoreFacade teamCityCoreMock;
    private OAuthConnectionDescriptor rootProjectConnection;
    private SUser tcUser;
    private MockHttpSession session;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeMethod
    public void setUp() throws Exception {
        TestInternalProperties.init();
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GitHubOAuthClient gitHubClient = new GitHubOAuthClient(restTemplate);

        teamCityCoreMock = mock(TeamCityCoreFacade.class);
        gitHubOAuth = new GitHubOAuth(gitHubClient, teamCityCoreMock);
        Logger logger = Logger.getLogger("oauth");
        logger.setLevel(Level.DEBUG);
        logger.addAppender(new ConsoleAppender(new SimpleLayout()));
        gitHubOAuth.setLogger(new Log4jLogger(logger));

        tcUser = mock(SUser.class);
        when(tcUser.describe(anyBoolean())).thenReturn("TeamCity user " + GITHUB_USER_LOGIN);
        when(tcUser.getUsername()).thenReturn(GITHUB_USER_LOGIN);

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

        //Setup GitHub API responses
        MultiValueMap<String, String> expectedTokenBody = new LinkedMultiValueMap<>();
        expectedTokenBody.put("client_id", singletonList(CLIENT_ID));
        expectedTokenBody.put("client_secret", singletonList(CLIENT_SECRET));
        expectedTokenBody.put("code", singletonList(OAUTH_CODE));
        expectedTokenBody.put("redirect_uri", singletonList(TC_URL));

        server.expect(requestTo("https://github.com/login/oauth/access_token")).andExpect(method(POST)).andExpect(content().formData(expectedTokenBody))
                .andRespond(withSuccess(TOKEN_JSON, APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user?access_token=" + OAUTH_TOKEN)).andExpect(method(GET))
                .andRespond(withSuccess(USER_JSON, APPLICATION_JSON));
    }

    private void newRequest() {
        request = new MockHttpServletRequest();
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    @Test
    public void successful_login__new_user_created() throws Exception {
        emulateFirstOAuthStep();

        when(teamCityCoreMock.findUserByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, "1")).thenReturn(UserSet.EMPTY);
        when(teamCityCoreMock.createUser(GITHUB_USER_LOGIN, singletonMap(GITHUB_USER_ID_PROPERTY_KEY, "1"))).thenReturn(tcUser);
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.AUTHENTICATED);
        then(result.getPrincipal().getName()).isEqualTo(GITHUB_USER_LOGIN);
        verify(teamCityCoreMock).rememberToken(rootProjectConnection, tcUser, GITHUB_USER_LOGIN, new GitHubToken(OAUTH_TOKEN, DEFAULT_SCOPE));
    }

    @Test
    public void successful_login__user_exists() throws Exception {
        emulateFirstOAuthStep();

        when(teamCityCoreMock.findUserByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, "1")).thenReturn(() -> singleton(tcUser));
        HttpAuthenticationResult result = gitHubOAuth.processAuthenticationRequest(request, response, emptyMap());

        then(result.getType()).isEqualTo(HttpAuthenticationResult.Type.AUTHENTICATED);
        then(result.getPrincipal().getName()).isEqualTo(GITHUB_USER_LOGIN);
        verify(teamCityCoreMock).rememberToken(rootProjectConnection, tcUser, GITHUB_USER_LOGIN, new GitHubToken(OAUTH_TOKEN, DEFAULT_SCOPE));
        verify(teamCityCoreMock, never()).createUser(anyString(), anyMap());
    }

    private void emulateFirstOAuthStep() {
        String redirect = gitHubOAuth.getUserRedirect(request);
        String state = verifyRedirectUrlAndFetchStateParam(redirect);

        newRequest();
        request.addParameter("code", OAUTH_CODE);
        request.addParameter("state", state);
    }

    @NotNull
    private String verifyRedirectUrlAndFetchStateParam(String redirect) {
        then(redirect).isNotNull().contains("client_id=" + CLIENT_ID).contains("redirect_uri=" + TC_URL).contains("state=");
        return redirect.substring(redirect.indexOf("state=") + 6);
    }

    private static final String TOKEN_JSON = "{\"access_token\":\"" + OAUTH_TOKEN + "\", \"scope\":\"" + DEFAULT_SCOPE + "\", \"token_type\":\"bearer\"}";

    private static final String USER_JSON = "{\n" +
            "  \"login\": \"" + GITHUB_USER_LOGIN + "\",\n" +
            "  \"id\": " + GITHUB_USER_ID + ",\n" +
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
            "  \"name\": \"monalisa octocat\",\n" +
            "  \"company\": \"GitHub\",\n" +
            "  \"blog\": \"https://github.com/blog\",\n" +
            "  \"location\": \"San Francisco\",\n" +
            "  \"email\": \"octocat@github.com\",\n" +
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
