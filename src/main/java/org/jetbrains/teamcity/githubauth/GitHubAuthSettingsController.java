package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GitHubAuthSettingsController extends BaseController {
    public static final String PATH = "/admin/gitHubOAuthSettings.html";

    private final PluginDescriptor pluginDescriptor;
    private final GitHubOAuth gitHubOAuth;

    public GitHubAuthSettingsController(PluginDescriptor pluginDescriptor,
                                        WebControllerManager webControllerManager, GitHubOAuth gitHubOAuth) {
        this.pluginDescriptor = pluginDescriptor;
        this.gitHubOAuth = gitHubOAuth;
        webControllerManager.registerController(PATH, this);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        ModelAndView modelAndView = new ModelAndView(pluginDescriptor.getPluginResourcesPath("editGitHubAuthScheme.jsp"));
        OAuthConnectionDescriptor connection = gitHubOAuth.tryFindSuitableConnection();
        modelAndView.addObject("connection", connection);
        return modelAndView;
    }
}
