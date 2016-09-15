package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationScheme;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthTokensStorage;
import jetbrains.buildServer.serverSide.oauth.github.GitHubOAuthProvider;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.users.UserSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class TeamCityCoreFacade {

    @NotNull
    private final UserModel myUserModel;
    @NotNull
    private final LoginConfiguration loginConfiguration;
    @NotNull
    private final ServerSettings serverSettings;
    @NotNull
    private final ProjectManager projectManager;
    @NotNull
    private final OAuthConnectionsManager oAuthConnectionsManager;
    @NotNull
    private final OAuthTokensStorage oAuthTokensStorage;

    public TeamCityCoreFacade(@NotNull UserModel myUserModel, @NotNull LoginConfiguration loginConfiguration,
                              @NotNull ServerSettings serverSettings, @NotNull ProjectManager projectManager,
                              @NotNull OAuthConnectionsManager oAuthConnectionsManager,
                              @NotNull OAuthTokensStorage oAuthTokensStorage) {
        this.myUserModel = myUserModel;
        this.loginConfiguration = loginConfiguration;
        this.serverSettings = serverSettings;
        this.projectManager = projectManager;
        this.oAuthConnectionsManager = oAuthConnectionsManager;
        this.oAuthTokensStorage = oAuthTokensStorage;
    }

    UserSet<SUser> findUserByPropertyValue(PropertyKey propertyKey, String propertyValue) {
        return myUserModel.findUsersByPropertyValue(propertyKey, propertyValue, true);
    }

    SUser createUser(String username, Map<PropertyKey, String> properties) {
        SUser created = myUserModel.createUserAccount(null, username);
        properties.forEach(created::setUserProperty);
        return created;
    }

    void registerAuthModule(HttpAuthenticationScheme scheme) {
        loginConfiguration.registerAuthModuleType(scheme);
    }

    boolean isAuthModuleConfigured(Class<? extends HttpAuthenticationScheme> schemeType) {
        return loginConfiguration.getConfiguredAuthModules(schemeType).size() == 1;
    }

    String getRootUrl() {
        return serverSettings.getRootUrl();
    }

    @Nullable
    OAuthConnectionDescriptor getRootProjectGitHubConnection() {
        List<OAuthConnectionDescriptor> found = oAuthConnectionsManager.getAvailableConnectionsOfType(projectManager.getRootProject(), GitHubOAuthProvider.TYPE);
        return found.isEmpty() ? null : found.get(0);
    }

    void rememberToken(OAuthConnectionDescriptor connection, SUser user, String githubLogin, GitHubToken token) {
        oAuthTokensStorage.rememberPermanentToken(connection.getId(), user, githubLogin, token.access_token, token.scope);
    }
}