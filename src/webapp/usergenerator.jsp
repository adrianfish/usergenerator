<%@ page contentType="text/html" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="/library/skin/tool_base.css" />
<link rel="stylesheet" type="text/css" href="/library/skin/default/tool.css" />
<script type="text/javascript" src="/library/js/headscripts.js"></script>
</head>
<body onload="setMainFrameHeight(window.frameElement.id);">
<h2>Step 1: Upload your Excel spreadsheet</h2>
<form method="POST" action="/portal/tool/<%= session.getAttribute("toolId") %>" enctype="multipart/form-data">
<h3>What to do:</h3>
<span class="instruction">
	1. Select an Excel file containing your users details in three columns,
	first name, last name and then email. <b>DO NOT INCLUDE A ROW OF COLUMN HEADERS!</b>
</span>
<br />
<span class="instruction">
	2. Click 'Upload and Check'. The spreadsheet will be uploaded and the data
	checked for suitability.
</span>
<br />
<span class="instruction">
	3. View the results of the spreadsheet validation on the next screen.
</span>
<br />
<br />
<span style="font-weight: bold;">Need more help? <a href="mailto:support@opensourceforenterprise.org">Email us</a></span>
<br />
<br />
<input type="file" name="file"/>
<br />
<br />
<input type="submit" value="Upload and Check"/>
</form>
</body>
</html>