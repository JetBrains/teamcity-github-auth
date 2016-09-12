package org.jetbrains.teamcity.githubauth;

import org.jetbrains.annotations.NonNls;

public class GitHubLoginException extends RuntimeException {
    public GitHubLoginException(@NonNls String message) {
        super(message);
    }

    public GitHubLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
