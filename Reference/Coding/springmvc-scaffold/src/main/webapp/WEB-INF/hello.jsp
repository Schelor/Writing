<%--
  Created by IntelliJ IDEA.
  User: simon
  Date: 2018/3/27
  Time: 下午6:56
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;
charset=UTF-8">
    <title>你好,SpringMVC</title>
</head>
<body>

<h1>从这里开始</h1>

<h2>参数传递: </h2>
<h3>${message}</h3>
<h3>${requestScope.divMessage}</h3>

<p id="tip"></p>

</body>
<script></script>
<script>

    document.getElementById("tip").innerHTML = "Javascript Generated";

</script>

</html>
