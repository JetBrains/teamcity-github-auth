/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationScheme;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthTokensStorage;
import jetbrains.buildServer.serverSide.oauth.github.GitHubOAuthProvider;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.users.impl.UserEx;
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

    SUser createUser(String username, @Nullable String email, @Nullable String name, Map<PropertyKey, String> properties) {
        SUser created = myUserModel.createUserAccount(null, username);
        properties.forEach(created::setUserProperty);
        created.updateUserAccount(username, name, email);
        if (email != null && TeamCityProperties.getBooleanOrTrue("teamcity.gitHubAuth.setEmailIsVerified")) {
            ((UserEx) created).setEmailIsVerified(email); //looks like GitHub responds with verified emails only.
        }
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

    void rememberToken(OAuthConnectionDescriptor connection, SUser user, String githubLogin, String token, String scope) {
        oAuthTokensStorage.rememberPermanentToken(connection.getId(), user, githubLogin, token, scope);
    }
}
