package org.jetbrains.teamcity.githubauth;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Test
public class GitHubOAuthTest {

    private GitHubOAuthClient gitHubClient;
    private MockRestServiceServer server;

    @BeforeMethod
    public void setUp() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        gitHubClient = new GitHubOAuthClient(restTemplate);
    }

    @Test
    public void successful_token_request() throws Exception {
        MultiValueMap<String, String> expectedBody = new LinkedMultiValueMap<>();
        expectedBody.put("client_id", singletonList("id"));
        expectedBody.put("client_secret", singletonList("secret"));
        expectedBody.put("code", singletonList("code"));
        expectedBody.put("redirect_uri", singletonList("redirect"));

        server.expect(requestTo("https://github.com/login/oauth/access_token")).andExpect(method(POST))
                .andExpect(content().formData(expectedBody))
                .andRespond(withSuccess("{\"access_token\":\"e72e16c7e42f292c6912e7710c838347ae178b4a\", \"scope\":\"repo,gist\", \"token_type\":\"bearer\"}", APPLICATION_JSON));

        String token = gitHubClient.exchangeCodeToToken("code", "id", "secret", "redirect");

        then(token).isEqualTo("e72e16c7e42f292c6912e7710c838347ae178b4a");
    }

    @Test
    public void successful_user_request() throws Exception {
        server.expect(requestTo("https://api.github.com/user?access_token=token")).andExpect(method(GET))
                .andRespond(withSuccess("{\n" +
                        "  \"login\": \"octocat\",\n" +
                        "  \"id\": 1,\n" +
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
                        "}", APPLICATION_JSON));

        GitHubUser user = gitHubClient.getUser("token");

        then(user.getLogin()).isEqualTo("octocat");
        then(user.getId()).isEqualTo("1");
        then(user.getEmail()).isEqualTo("octocat@github.com");
    }
}
