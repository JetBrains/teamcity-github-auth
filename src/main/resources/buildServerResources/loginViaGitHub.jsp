<%@ page import="org.jetbrains.teamcity.githubauth.GitHubOAuthLoginController" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<c:url var="path" value="<%=GitHubOAuthLoginController.PATH%>"/>
<div><a href="${path}">Log in using GitHub account</a></div>
