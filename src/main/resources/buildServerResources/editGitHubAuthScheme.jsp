<%@ page import="jetbrains.buildServer.serverSide.oauth.github.GitHubOAuthProvider" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="prop" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--@elvariable id="connection" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor"--%>
<c:set var="connectionType" value="<%=GitHubOAuthProvider.TYPE%>"/>
<c:url var="oauthConnectionsUrl" value="/admin/editProject.html?projectId=_Root&tab=oauthConnections"/>
<div>
    <c:choose>
        <c:when test="${connection != null}">
            GitHub authentication module uses <a href="${oauthConnectionsUrl}">GitHub
            Connection</a> from the Root Project
        </c:when>
        <c:otherwise>
            Please add <a href="${oauthConnectionsUrl}#addDialog=${connectionType}">GitHub
            Connection</a> to the Root Project to activate GitHub Authentication.
        </c:otherwise>
    </c:choose>
</div>
