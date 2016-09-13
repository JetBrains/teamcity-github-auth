package org.jetbrains.teamcity.githubauth;

import com.google.common.base.Strings;
import jetbrains.buildServer.PluginTypes;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationScheme;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.serverSide.auth.ServerPrincipal;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthTokensStorage;
import jetbrains.buildServer.serverSide.oauth.github.GitHubConstants;
import jetbrains.buildServer.serverSide.oauth.github.GitHubOAuthProvider;
import jetbrains.buildServer.users.PluginPropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.users.UserSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class GitHubOAuth implements HttpAuthenticationScheme {
    private static final PluginPropertyKey GITHUB_USER_ID_PROPERTY_KEY = new PluginPropertyKey(PluginTypes.AUTH_PLUGIN_TYPE, "github-oauth", "userId");
    private static final String DEFAULT_SCOPE = "user,public_repo,repo,repo:status,write:repo_hook";

    @NotNull
    private final GitHubOAuthClient gitHubOAuthClient;
    @NotNull
    private final UserModel myUserModel;
    @NotNull
    private final LoginConfiguration loginConfiguration;
    @NotNull
    private final ServerSettings myServerSettings;
    @NotNull
    private final OAuthConnectionsManager oAuthConnectionsManager;
    @NotNull
    private final OAuthTokensStorage oAuthTokensStorage;
    @NotNull
    private final ProjectManager projectManager;

    public GitHubOAuth(@NotNull GitHubOAuthClient gitHubOAuthClient,
                       @NotNull UserModel myUserModel,
                       @NotNull LoginConfiguration loginConfiguration,
                       @NotNull ServerSettings myServerSettings,
                       @NotNull OAuthConnectionsManager oAuthConnectionsManager,
                       @NotNull OAuthTokensStorage oAuthTokensStorage,
                       @NotNull ProjectManager projectManager) {
        this.gitHubOAuthClient = gitHubOAuthClient;
        this.myUserModel = myUserModel;
        this.loginConfiguration = loginConfiguration;
        this.myServerSettings = myServerSettings;
        this.oAuthConnectionsManager = oAuthConnectionsManager;
        this.oAuthTokensStorage = oAuthTokensStorage;
        this.projectManager = projectManager;
        loginConfiguration.registerAuthModuleType(this);
    }

    @NotNull
    public String getUserRedirect() {
        OAuthConnectionDescriptor connection = getSuitableConnection();
        return gitHubOAuthClient.getUserRedirect(connection.getParameters().get(GitHubConstants.CLIENT_ID_PARAM), DEFAULT_SCOPE, myServerSettings.getRootUrl());
    }

    @NotNull
    @Override
    public HttpAuthenticationResult processAuthenticationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Map<String, String> properties) throws IOException {
        String code = request.getParameter("code");

        if (Strings.isNullOrEmpty(code))
            return HttpAuthenticationResult.notApplicable();

        OAuthConnectionDescriptor connection = getSuitableConnection();
        GitHubOAuthClient.TokenResponse token = gitHubOAuthClient.exchangeCodeToToken(code, connection.getParameters().get(GitHubConstants.CLIENT_ID_PARAM),
                connection.getParameters().get(GitHubConstants.CLIENT_SECRET_PARAM),
                myServerSettings.getRootUrl());
        GitHubUser user = gitHubOAuthClient.getUser(token.access_token);

        UserSet<SUser> users = myUserModel.findUsersByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, user.getId(), true);
        Iterator<SUser> iterator = users.getUsers().iterator();
        if (iterator.hasNext()) {
            final SUser found = iterator.next();
            oAuthTokensStorage.rememberPermanentToken(connection.getId(), found, user.getLogin(), token.access_token, token.scope);
            return HttpAuthenticationResult.authenticated(new ServerPrincipal(null, found.getUsername()), true);
        }

        ServerPrincipal principal = new ServerPrincipal(null, user.getLogin(), null, true,
                Collections.singletonMap(GITHUB_USER_ID_PROPERTY_KEY, user.getId()));
        return HttpAuthenticationResult.authenticated(principal, true);
    }

    @NotNull
    @Override
    public String getName() {
        return "github-oauth";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "GitHub OAuth";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Allows authentication using GitHub account";
    }

    @Override
    public boolean isMultipleInstancesAllowed() {
        return false;
    }

    @NotNull
    @Override
    public Map<String, String> getDefaultProperties() {
        return Collections.emptyMap();
    }

    @Nullable
    @Override
    public String getEditPropertiesJspFilePath() {
        return GitHubAuthSettingsController.PATH;
    }

    @NotNull
    @Override
    public String describeProperties(@NotNull Map<String, String> properties) {
        return "";
    }

    @Nullable
    @Override
    public Collection<String> validate(@NotNull Map<String, String> properties) {
        if (tryFindSuitableConnection() == null) {
            return Collections.singleton("GitHub Authentication is inactive as GitHub.com Connection in the Root Project is not specified");
        }
        return null;
    }

    @Nullable
    public OAuthConnectionDescriptor tryFindSuitableConnection() {
        List<OAuthConnectionDescriptor> foundConnections = oAuthConnectionsManager.getAvailableConnectionsOfType(projectManager.getRootProject(), GitHubOAuthProvider.TYPE);
        return foundConnections.isEmpty() ? null : foundConnections.get(0);
    }

    @NotNull
    public OAuthConnectionDescriptor getSuitableConnection() {
        if (!isAuthModuleConfigured()) {
            throw new GitHubLoginException("Attempt to login via GitHub OAuth while corresponding auth module is not configured");
        }
        OAuthConnectionDescriptor found = tryFindSuitableConnection();
        if (found == null) {
            throw new GitHubLoginException("Attempt to login via GitHub OAuth while GitHub.com Connection in the Root Project is not configured");
        }
        return found;
    }

    public boolean isAuthModuleConfigured() {
        return loginConfiguration.getConfiguredAuthModules(GitHubOAuth.class).size() == 1;
    }
}
