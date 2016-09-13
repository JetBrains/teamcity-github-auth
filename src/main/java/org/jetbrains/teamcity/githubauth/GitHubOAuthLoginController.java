package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GitHubOAuthLoginController extends BaseController {
    public static final String PATH = "/githubLogin.html";

    @NotNull
    private final GitHubOAuth gitHubOAuth;

    public GitHubOAuthLoginController(@NotNull WebControllerManager webControllerManager,
                                      @NotNull AuthorizationInterceptor authInterceptor,
                                      @NotNull GitHubOAuth gitHubOAuth) {
        this.gitHubOAuth = gitHubOAuth;
        webControllerManager.registerController(PATH, this);
        authInterceptor.addPathNotRequiringAuth(PATH);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        return redirectTo(gitHubOAuth.getUserRedirect(request), response);
    }
}
