<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
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
            float: left;
        }

        #canvas {
            float: left;
            height: 400px;
            width: 400px;
            border: 1px solid;
            margin-bottom: 10px;
            margin-left: 10px;
            background-color: white;
        }

        #webRtc {
            float: left;
            width: 400px;
            height: 400px;
            border: 1px solid;
            margin-left: 10px;
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
    登录状态： <span id="denglu" style="color: red;">正在登录</span> <br> 昵称：
    <span id="userName"></span> <br> <br> To： <select
        id='userlist'>
</select> <span style="color: red;">*</span>请选择聊天对象 <br> 发送内容： <input
        type="text" id="writeMsg" value="嗨~"/> <br> 聊天框：<br>
    <Br/>
</div>

<div id='canvas'>
    <canvas id="c1" width="400" height="400">
        <span>不支持canvas浏览器</span>
    </canvas>
</div>

<div id="webRtc">
    <video id="webcam" width=100% height=100%></video>
</div>

<div style="clear: both;"></div>


<div id="message"></div>
<br>
<input type="button" value="send" onclick="sendMsg()"/>
<br>


<script type="text/javascript">
    /** 判断浏览器是否支持视频聊天 **/
    navigator.getUserMedia || (navigator.getUserMedia = navigator.mozGetUserMedia || navigator.webkitGetUserMedia || navigator.msGetUserMedia);
    if (!navigator.getUserMedia) {
        alert('not support');
    }


    var self = "<%=name%>";
    var ws = null;

    var oC = document.getElementById('c1');
    var oGC = oC.getContext('2d');

    function startWebSocket() {
        if ('WebSocket' in window)
            ws = new WebSocket("ws://localhost:8080/websocket");
        else if ('MozWebSocket' in window)
            ws = new MozWebSocket("ws://localhost:8080/websocket");
        else
            alert("not support");

        ws.onmessage = function (evt) {
            var data = evt.data;
            var msg = eval('(' + data + ')');//将字符串转换成JSON
            console.log(msg)
            // msg = {createTime: 1548750571403, msgType: "user", userList: Array(1)}
            if (msg.msgType === 'text') {
                setMessageInnerHTML(msg.content);
            } else if (msg.msgType === 'user') {
                console.log("msg: " + msg)
                var userArry = msg.userList;
                $("#userlist")
                    .empty()
                    .append("<option value ='all'>所有人</option>");
                $.each(userArry, function (n, value) {
                    if (value !== self && value !== 'admin') {
                        $("#userlist").append(
                            '<option value = ' + value + '>' + value + '</option>');
                    }
                });
            } else if (msg.msgType === 'coord') {
                console.log("接收画图坐标")
                var coordArry = msg.content.split(",");
                var x = coordArry[0];
                var y = coordArry[1];
                console.log("x: " + x + " y: " + y)
                oGC.lineWidth = 2;
                oGC.lineTo(x, y);
                // oGC.closePath();
                oGC.stroke();
            }
        };
        ws.onclose = function (evt) {
            $('#denglu').html("离线");
        };

        ws.onopen = function (evt) {
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
        var message = {
            fromUserName: self,
            toUserName: $("#userlist").val(),
            content: $("#writeMsg").val(),
            msgType: 'text',
        };
        ws.send(JSON.stringify(message));
    }

    initCanvas();

    function drawPanel(x, y) {

    }

    function initCanvas() {
        var oC = document.getElementById('c1');
        var oGC = oC.getContext('2d');
        oC.onmousedown = function (ev) {
            var ev = ev || window.event;
            oGC.moveTo(ev.clientX - oC.offsetLeft, ev.clientY - oC.offsetTop);
            document.onmousemove = function (ev) {
                var ev = ev || window.event;
                var x = ev.clientX - oC.offsetLeft;
                var y = ev.clientY - oC.offsetTop;
                if (x == y == 0) {
                    oGC.moveTo(0, 0);
                }else{
                    oGC.lineTo(x, y);
                    var message = {
                        fromUserName: self,
                        toUserName: $("#userlist").val(),
                        content: x + ',' + y,
                        msgType: "coord",
                    };
                    ws.send(JSON.stringify(message));
                    oGC.stroke();
                }

            };
            document.onmouseup = function () {
                document.onmousemove = null;
                document.onmouseup = null;
                var message = {
                    fromUserName: self,
                    toUserName: $("#userlist").val(),
                    content: 0 + ',' + 0,
                    msgType: "coord",
                };
                ws.send(JSON.stringify(message));
            };
        };
    }
</script>

</body>
</html>