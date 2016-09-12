package org.jetbrains.teamcity.githubauth;

import com.google.common.base.Strings;
import jetbrains.buildServer.PluginTypes;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationScheme;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.auth.AuthModule;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.serverSide.auth.ServerPrincipal;
import jetbrains.buildServer.users.PluginPropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class GitHubOAuth implements HttpAuthenticationScheme {
    private static final PluginPropertyKey GITHUB_USER_ID_PROPERTY_KEY = new PluginPropertyKey(PluginTypes.AUTH_PLUGIN_TYPE, "github-oauth", "userId");

    @NotNull
    private final PluginDescriptor pluginDescriptor;
    @NotNull
    private final GitHubOAuthClient gitHubOAuthClient;
    @NotNull
    private final UserModel myUserModel;
    @NotNull
    private final LoginConfiguration loginConfiguration;
    @NotNull
    private final ServerSettings myServerSettings;

    public GitHubOAuth(@NotNull PluginDescriptor pluginDescriptor,
                       @NotNull GitHubOAuthClient gitHubOAuthClient,
                       @NotNull UserModel myUserModel,
                       @NotNull LoginConfiguration loginConfiguration,
                       @NotNull ServerSettings myServerSettings) {
        this.pluginDescriptor = pluginDescriptor;
        this.gitHubOAuthClient = gitHubOAuthClient;
        this.myUserModel = myUserModel;
        this.loginConfiguration = loginConfiguration;
        this.myServerSettings = myServerSettings;
        loginConfiguration.registerAuthModuleType(this);
    }

    @NotNull
    public String getUserRedirect() {
        Map<String, String> appSettings = getAuthModule().getProperties();
        return gitHubOAuthClient.getUserRedirect(appSettings.get("clientId"), appSettings.get("scope"), myServerSettings.getRootUrl());
    }

    @NotNull
    @Override
    public HttpAuthenticationResult processAuthenticationRequest(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Map<String, String> properties) throws IOException {
        String code = request.getParameter("code");

        if (Strings.isNullOrEmpty(code))
            return HttpAuthenticationResult.notApplicable();

        Map<String, String> appSettings = getAuthModule().getProperties();
        String token = gitHubOAuthClient.exchangeCodeToToken(code, appSettings.get("clientId"), appSettings.get("clientSecret"),
                myServerSettings.getRootUrl());
        GitHubUser user = gitHubOAuthClient.getUser(token);

        UserSet<SUser> users = myUserModel.findUsersByPropertyValue(GITHUB_USER_ID_PROPERTY_KEY, user.getId(), true);
        Iterator<SUser> iterator = users.getUsers().iterator();
        if (iterator.hasNext()) {
            final SUser found = iterator.next();
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
        return "Allows authentication via GitHub account";
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
        return pluginDescriptor.getPluginResourcesPath("editGitHubAuthScheme.jsp");
    }

    @NotNull
    @Override
    public String describeProperties(@NotNull Map<String, String> properties) {
        return "";
    }

    @Nullable
    @Override
    public Collection<String> validate(@NotNull Map<String, String> properties) {
        return null;
    }

    @NotNull
    private AuthModule<GitHubOAuth> getAuthModule() {
        List<AuthModule<GitHubOAuth>> authModules = loginConfiguration.getConfiguredAuthModules(GitHubOAuth.class);
        if (authModules.size() != 1) {
            throw new GitHubLoginException("Attempt to login via GitHub OAuth while corresponding auth module is not configured");
        }
        return authModules.get(0);
    }
}
