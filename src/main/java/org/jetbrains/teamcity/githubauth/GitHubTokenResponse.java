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

import jetbrains.buildServer.log.Loggable;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GitHubTokenResponse implements Loggable {
    public String access_token;
    public String scope;
    public String error;
    public String error_description;
    public String error_uri;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubTokenResponse that = (GitHubTokenResponse) o;
        return Objects.equals(access_token, that.access_token) &&
                Objects.equals(scope, that.scope) &&
                Objects.equals(error, that.error) &&
                Objects.equals(error_description, that.error_description) &&
                Objects.equals(error_uri, that.error_uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(access_token, scope, error, error_description, error_uri);
    }

    @NotNull
    @Override
    public String describe(boolean verbose) {
        return error != null ? error + " (description: " + error_description + ", url: " + error_uri + ")" :
                StringUtil.truncateStringValueWithDotsAtEnd(access_token, 10);
    }
}
