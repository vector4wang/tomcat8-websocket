<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<style>
body {
	padding: 20px;
}

#message {
	height: 300px;
	border: 1px solid;
	overflow: auto;
}
</style>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>WebIM</title>
<script type="text/javascript" src="js/jquery-1.7.2.min.js"></script>
<%
	String name = request.getParameter("username");
	session.setAttribute("user", name);
%>
<script type="text/javascript">
var self = "<%=name%>";
	var ws = null;
	function startWebSocket() {
		if ('WebSocket' in window)
			ws = new WebSocket("ws://localhost/WebSocket8/websocket");
		else if ('MozWebSocket' in window)
			ws = new MozWebSocket("ws://localhost/WebSocket8/websocket");
		else
			alert("not support");

		ws.onmessage = function(evt) {
			var data = evt.data;
			var o = eval('(' + data + ')');//将字符串转换成JSON
			if (o.type == 'message') {
				setMessageInnerHTML(o.data);
			} else if (o.type == 'user') {
				var userArry = o.data.split(',');
				$("#userlist").empty();
				$("#userlist").append("<option value ='all'>所有人</option>");
				$.each(userArry, function(n, value) {
					if (value != self && value != 'admin') {
						$("#userlist").append(
								'<option value = '+value+'>' + value
										+ '</option>');
					}
				});
			}
		};
		ws.onclose = function(evt) {
			$('#denglu').html("离线");
		};

		ws.onopen = function(evt) {
			$('#denglu').html("在线");
			$('#userName').html(self);
		};
	}

	function setMessageInnerHTML(innerHTML) {
		var temp = $('#message').html();
		temp += innerHTML + '<br/>';
		$('#message').html(temp);
	}

	function sendMsg() {
		var fromName = self;
		var toName = $("#userlist").val(); //发给谁
		var content = $("#writeMsg").val(); //发送内容
		var msg = fromName + "," + toName + "," + content;
		ws.send(msg);
	}
</script>
</head>
<body onload="startWebSocket();" oncontextmenu="return false;" onselectstart="return false;">
	<h1>WebIM</h1>
	登录状态：
	<span id="denglu" style="color: red;">正在登录</span>
	<br> 昵称：
	<span id="userName"></span>
	<br>
	<br> To：
	<select id='userlist'>
	</select>
	<span style="color: red;">*</span>请选择聊天对象
	<br> 发送内容：
	<input type="text" id="writeMsg" value="嗨~" />
	<br> 聊天框：
	<div id="message"></div>
	<br>
	<input type="button" value="send" onclick="sendMsg()" />
	<br>
</body>
</html>