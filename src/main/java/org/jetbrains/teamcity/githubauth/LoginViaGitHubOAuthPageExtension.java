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
