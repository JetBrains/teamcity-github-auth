package org.jetbrains.teamcity.githubauth;

import jetbrains.buildServer.log.Loggable;
import org.jetbrains.annotations.NotNull;

public class GitHubUser implements Loggable {
    private String id;
    private String login;
    private String email;
    private String name;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String describe(boolean verbose) {
        return login + "(id = " + id + ")";
    }
}
