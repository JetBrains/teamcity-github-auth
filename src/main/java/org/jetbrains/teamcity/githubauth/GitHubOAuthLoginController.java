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

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.jetbrains.teamcity.githubauth.GitHubOAuth.PLUGIN_PATH_PREFIX;

public class GitHubOAuthLoginController extends BaseController {
    public static final String PATH = PLUGIN_PATH_PREFIX + "/login.html";

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
