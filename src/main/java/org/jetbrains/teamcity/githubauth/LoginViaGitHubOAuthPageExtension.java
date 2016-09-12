package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

public class LoginViaGitHubOAuthPageExtension extends SimplePageExtension {
    @NotNull
    private final LoginConfiguration loginConfiguration;

    public LoginViaGitHubOAuthPageExtension(PagePlaces pagePlaces, PluginDescriptor pluginDescriptor, LoginConfiguration loginConfiguration) {
        super(pagePlaces, PlaceId.LOGIN_PAGE, "github-login", pluginDescriptor.getPluginResourcesPath("loginViaGitHub.jsp"));
        this.loginConfiguration = loginConfiguration;
        register();
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        return loginConfiguration.isAuthModuleConfigured(GitHubOAuth.class);
    }
}
