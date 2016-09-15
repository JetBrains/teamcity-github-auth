package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.log.Loggable;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GitHubToken implements Loggable {
    public String access_token;
    public String scope;

    public GitHubToken() {
    }

    public GitHubToken(String access_token, String scope) {
        this.access_token = access_token;
        this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubToken that = (GitHubToken) o;
        return Objects.equals(access_token, that.access_token) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(access_token, scope);
    }

    @NotNull
    @Override
    public String describe(boolean verbose) {
        return StringUtil.truncateStringValueWithDotsAtEnd(access_token, 10);
    }
}
