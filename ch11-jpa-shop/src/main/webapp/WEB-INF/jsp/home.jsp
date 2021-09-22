<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>

<jsp:include page="fragments/head.jsp"/>

<body>

<div class="container">

    <jsp:include page="fragments/bodyHeader.jsp" />

    <div class="jumbotron">
        <h1>HELLO SHOP</h1>
        <p class="lead">회원 기능</p>
        <p>
            <a class="btn btn-lg btn-primary" href="<c:url value="/members/new"/>">회원 가입</a>
            <a class="btn btn-lg btn-primary" href="<c:url value="/members"/>">회원 목록</a>
        </p>
        <p class="lead">상품 기능</p>
        <p>
            <a class="btn btn-lg btn-primary" href="<c:url value="/items/new"/>">상품 등록</a>
            <a class="btn btn-lg btn-primary" href="<c:url value="/items"/>">상품 목록</a>
        </p>
        <p class="lead">주문 기능</p>
        <p>
            <a class="btn btn-lg btn-success" href="<c:url value="/order"/>">상품 주문</a>
            <a class="btn btn-lg btn-success" href="<c:url value="/orders"/>">주문 내역</a>
        </p>
    </div>

    <jsp:include page="fragments/footer.jsp" />

</div> <!-- /container -->

</body>
</html>