<%@page contentType="text/html" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.sakaiproject.authz.api.Role" %>
<%@ page import="org.sakaiproject.usergenerator.tool.UGUser" %>
<% boolean badData = false; %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javascript" src="/library/js/headscripts.js"></script>
<link rel="stylesheet" type="text/css" href="/library/skin/tool_base.css" />
<link rel="stylesheet" type="text/css" href="/library/skin/default/tool.css" />
</head>
<body onload="setMainFrameHeight(window.frameElement.id);">
<h2>Step 2: Check that your spreadsheet is ok</h2>
<br />
<%
if (badData)
{
%>
<span>
	The data you supplied contained errors (highlighted in red). The error is
	likely to be in the highlighted email address. You need to correct them in
	your spreadsheet and start again.
</span>
<%
}
else
{
%>
<span class="instruction">
	The data you supplied seems OK. Please check it over and, if you are happy
	with it, select the role you want the members to hold using the 'Role' drop
	down box and click the 'Create Users' button.
</span>
<br />
<br />
<span style="font-weight: bold;">
	Instruction to Lecturers: Please select user type as 'Student'.<br /> 
	Instruction to LEAD Provider: please select user type as 'access'.
</span>
<br />
<br />
<span style="font-weight: bold;">Need more help? <a href="mailto:support@opensourceforenterprise.org">Email us</a></span>
<%
}
%>
<br />
<br />
<table cols="4" border="1">
	<thead>
		<tr><th>First Name</th><th>Last Name</th><th>email</th></tr>
	</thead>
	<tbody>
<%
	List users = (List) session.getAttribute("users");

	for(Iterator i = users.iterator();i.hasNext();)
	{
		UGUser user = (UGUser) i.next();
		if(user.bad) badData = true;
%>
		<tr <% if(user.bad) {%>style="background-color: red;" <% } else {%>style="background-color: green;"<% } %>>
			<td><%= user.firstName %></td>
			<td><%= user.lastName %></td>
			<td><%= user.email %></td>
		</tr>
<%
	}
%>
	</tbody>
</table>

<%
if (!badData)
{
%>
<br />
<br />
<form action="/portal/tool/<%= session.getAttribute("toolId") %>" method="POST">
<span>User Type: </span>
<select name="role">

<%
	Set<Role> roles = (Set<Role>) request.getAttribute("roles");

	for(Role role : roles)
	{
%>
	<option><%= role.getId() %>
<%
	}
%>
</select>
<br />
<br />
<input type="hidden" name="action" value="create"/>
<input type="submit" value="Create Users"/>
</form>
<%
}
%>

</body>
</html>