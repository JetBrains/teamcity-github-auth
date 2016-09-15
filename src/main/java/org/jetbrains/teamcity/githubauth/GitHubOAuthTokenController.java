package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.jetbrains.teamcity.githubauth.GitHubOAuth.PLUGIN_PATH_PREFIX;

public class GitHubOAuthTokenController extends BaseController {
    public static final String PATH = PLUGIN_PATH_PREFIX + "/token.html";

    public GitHubOAuthTokenController(@NotNull WebControllerManager webControllerManager) {
        webControllerManager.registerController(PATH, this);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        return redirectTo("/overview.html", response);
    }
}
