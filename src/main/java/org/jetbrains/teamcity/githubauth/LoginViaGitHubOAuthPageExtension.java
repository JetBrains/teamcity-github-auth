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

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

public class LoginViaGitHubOAuthPageExtension extends SimplePageExtension {
    @NotNull
    private final GitHubOAuth gitHubOAuth;

    public LoginViaGitHubOAuthPageExtension(PagePlaces pagePlaces, PluginDescriptor pluginDescriptor, @NotNull GitHubOAuth gitHubOAuth) {
        super(pagePlaces, PlaceId.LOGIN_PAGE, "github-login", pluginDescriptor.getPluginResourcesPath("loginViaGitHub.jsp"));
        this.gitHubOAuth = gitHubOAuth;
        register();
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        return gitHubOAuth.isAuthModuleConfigured() && gitHubOAuth.tryFindSuitableConnection() != null
                && TeamCityProperties.getBooleanOrTrue("teamcity.gitHubAuth.showLinkOnLoginPage");
    }
}
