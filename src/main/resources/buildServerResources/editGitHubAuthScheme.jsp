<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="prop" tagdir="/WEB-INF/tags/props" %>
<div>
    <label width="100%" for="clientId">Client ID:</label><br/>
    <prop:textProperty style="width: 100%;" name="clientId"/><br/>
    <span class="grayNote">Client identifier.</span>
</div>
<div>
    <label width="100%" for="clientSecret">Client Secret:</label><br/>
    <prop:textProperty style="width: 100%;" name="clientSecret"/><br/>
    <span class="grayNote">Client secret.</span>
</div>
<div>
    <label width="100%" for="scope">Scope:</label><br/>
    <prop:textProperty style="width: 100%;" name="scope"/><br/>
    <span class="grayNote">Scope.</span>
</div>