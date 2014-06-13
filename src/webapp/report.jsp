<%@page contentType="text/html" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.sakaiproject.usergenerator.tool.UGUser" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javascript" src="/library/js/headscripts.js"></script>
<link rel="stylesheet" type="text/css" href="/library/skin/tool_base.css" />
<link rel="stylesheet" type="text/css" href="/library/skin/default/tool.css" />
</head>
<body onload="setMainFrameHeight(window.frameElement.id);">
<h2>Step 3: Report</h2>

<span class="instruction">
	The users were created and added to the site successfully. Their login
	details are contained in the table below. Click on 'Download Report' to
	download a spreadsheet version for your records.
</span>
<br />
<br />
<table cols="4" border="1">
	<thead>
		<tr><th>Login</th><th>First Name</th><th>Last Name</th><th>Password</th></tr>
	</thead>
	<tbody>
<%
	List users = (List) session.getAttribute("users");

	for(Iterator i = users.iterator();i.hasNext();)
	{
		UGUser user = (UGUser) i.next();
%>
		<tr>
			<td><%= user.email %></td>
			<td><%= user.firstName %></td>
			<td><%= user.lastName %></td>
			<td><%= user.password %></td>
		</tr>
<%
	}
%>
	</tbody>
</table>

<br />
<br />

<form action="/portal/tool/<%= session.getAttribute("toolId") %>" method="POST">
<input type="hidden" name="action" value="downloadReport"/>
<input type="submit" value="Download Report"/>
</form>

</body>
</html>