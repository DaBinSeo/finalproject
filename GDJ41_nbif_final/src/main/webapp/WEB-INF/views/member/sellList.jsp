<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="path" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />

<style>
section {
	padding: 200px 10%;
	min-height: 800px;
}

#goods-div {
	display: flex;
	flex-wrap: wrap;
	justify-content: left;
}

.goods-box {
	width: 270px;
	height: 350px;
	border: lightsteelblue solid 1px;
	border-radius: 10px;
	padding: 10px;
	margin-bottom: 30px;
	/* margin-right: 5px; */
	margin-left: 0.5%;
}
</style>


<section>
	<h2><strong style="color:skyblue">${sellList.get(0).seller.nickName}</strong>님의 판매 상품</h2>
	<br>
	<c:if test="${not empty sellList }">
		<div id="goods-div">
			<c:forEach items="${sellList }" var="sl">
				<a href="${path }/product/productDetail?productNo=${sl.productNo}">
					<div class="goods-box">
						<c:if test="${not empty sl.images}">
							<img
								src="${path }/resources/upload/product/${sl.images.get(0).imageName}"
								width="100%" height="200"
								style="margin-bottom: 12px; border-radius: 20px;">
						</c:if>
						<c:if test="${empty sl.images}">
							<img src="${path }/resources/images/" width="100%" height="200"
								style="margin-bottom: 12px; border-radius: 20px;">
						</c:if>
						<p>
							<strong style="font-size: 15px">${sl.productName }</strong><br>
							<c:if test="${p.nowBidPrice != null }">
									현재입찰가&nbsp;<span class="price1"><fmt:formatNumber
										value="${sl.nowBidPrice }" />원</span>
								<br>
							</c:if>
							<c:if test="${p.nowBidPrice == null }">
									현재입찰가&nbsp;<span class="price1"><fmt:formatNumber
										value="${sl.minBidPrice }" />원</span>
								<br>
							</c:if>
							<c:if test="${p.buyNowPrice != null }">
									즉시구매가&nbsp;<span class="price2"><fmt:formatNumber
										value="${sl.buyNowPrice }" />원</span>
								<br>
							</c:if>
							<c:if test="${p.buyNowPrice == null }">
									즉시구매불가<br>
							</c:if>
							판매자&nbsp;<strong>${sl.seller.nickName }</strong>
						</p>
					</div>
				</a>
			</c:forEach>
		</div>
	</c:if>
</section>


<jsp:include page="/WEB-INF/views/common/footer.jsp" />