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
.details {
	width: 300px;
	height: 400px;
	border: 1px solid;
	margin-bottom: 10px;
	float:left;
}
#canvas{
	float:left;
	height: 400px; 
	width: 400px; 
	border: 1px solid; 
	margin-bottom: 10px;
	margin-left: 10px;
	background-color: white;
}
</style>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>WebIM</title>
<script type="text/javascript" src="js/jquery-1.7.2.min.js"></script>
<%
	String name = request.getParameter("username");
	session.setAttribute("user", name);
%>

</head>
<body onload="startWebSocket();" oncontextmenu="return false;"
	onselectstart="return false;">
	<div class='details'>
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
	</div>
	
	<div id='canvas'>
		<canvas id="c1" width="400" height="400">  
		<span>不支持canvas浏览器</span>  
		</canvas>
	</div>
	
	<div style="clear:both;"></div>
	
	<div id="message"></div>
	<br>
	<input type="button" value="send" onclick="sendMsg()" />
	<br>

	
	<script type="text/javascript">
	var self = "<%=name%>";
	var ws = null;
	function startWebSocket() {
		if ('WebSocket' in window)
			ws = new WebSocket("ws://localhost:8080/WebSocket8/websocket");
		else if ('MozWebSocket' in window)
			ws = new MozWebSocket("ws://localhost:8080/WebSocket8/websocket");
		else
			alert("not support");

		var flag;
		
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
			}else if(o.type == 'coord'){
				var coordArry = o.data.split("_");
				var oC = document.getElementById('c1');
				var oGC = oC.getContext('2d');
				var x = coordArry[0];
				var y = coordArry[1];
				oGC.arc(x,y,1,0,360,false);
				oGC.stroke();

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
		var type = 'message';
		var msg = fromName + "," + toName + "," + content + "," + type;
		ws.send(msg);
	}

	initCanvas();
	
	function drawPanel(x,y){
		
	}
	
	function initCanvas() {
		var oC = document.getElementById('c1');
		var oGC = oC.getContext('2d');
		oC.onmousedown = function(ev) {
			var ev = ev || window.event;
			oGC.moveTo(ev.clientX - oC.offsetLeft, ev.clientY - oC.offsetTop);
			document.onmousemove = function(ev) {
				 var ev = ev || window.event;  
				 	var x = ev.clientX-oC.offsetLeft;
		            var y = ev.clientY-oC.offsetTop;
		            oGC.lineTo(x,y);
		           
		           
		            
		            var fromName = self;
		    		var toName = $("#userlist").val(); //发给谁
		    		var type = "coord";
		    		var content = x + '_' + y;
		    		var msg = fromName + "," + toName + "," + content + "," + type;
		    		ws.send(msg);
		    		
		    		oGC.stroke();
		    		
			};
			document.onmouseup = function() {
				document.onmousemove = null;
				document.onmouseup = null;
			};
		};
	}
</script>
	
</body>
</html>