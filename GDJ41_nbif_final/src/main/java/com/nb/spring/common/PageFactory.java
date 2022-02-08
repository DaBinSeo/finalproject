package com.nb.spring.common;

public class PageFactory {
	public static String getPageBar(int totalData, int cPage, int numPerPage,int pageBarSize,String url) {
		String pageBar = "";
      int totalPage = (int)Math.ceil((double)totalData/numPerPage);
      int pageNo = ((cPage-1)/pageBarSize)*pageBarSize+1;
      int pageEnd = pageNo + pageBarSize - 1;
      pageBar = "<ul class='pagination justify-content-center pagination-sm'>";
      if(pageNo==1) {
         pageBar += "<li class='page-item'>";
         pageBar += "<a class='page-link' href='#' tabindex='-1'>이전</a>";
         pageBar += "</li>";
      }else {
         pageBar += "<li class='page-item'>";
         pageBar += "<a class='page-link' href='javascript:fn_paging(" + (pageNo-1) + ","+numPerPage+")'>이전</a>";
         pageBar += "</li>";
      }
      
      while(!(pageNo>pageEnd||pageNo>totalPage)) {
         if(pageNo==cPage) {
            pageBar += "<li class='page-item active'>";
            pageBar += "<a class='page-link' href='#'>" + pageNo + "</a>";
            pageBar += "</li>";
         }else {
            pageBar += "<li class='page-item'>";
            pageBar += "<a class='page-link' href='javascript:fn_paging(" + (pageNo) + ","+numPerPage+")'>" + pageNo + "</a>";
            pageBar += "</li>";
         }
         pageNo++;
      }
      if(pageNo>totalPage) {
         pageBar += "<li class='page-item'>";
         pageBar += "<a class='page-link' href='#'>다음</a>";
         pageBar += "</li>";
      }else {
         pageBar += "<li class='page-item'>";
         pageBar += "<a class='page-link' href='javascript:fn_paging(" + (pageNo) + ","+numPerPage+")'>다음</a>";
         pageBar += "</li>";
      }
      pageBar += "</ul>";
      pageBar += "<script>";
      pageBar += "function fn_paging(cPage,numPerPage) {";    
      pageBar += "location.assign('" + url + "?cPage='+cPage+'&numPerPage='+ numPerPage);";
      pageBar += "}";
      pageBar += "$('#selectNum').change(e=>{";
      pageBar += "const selectVal = $('#selectNum').val();";
      pageBar += "fn_paging(1,selectVal);";
      pageBar += "});";
      pageBar += "</script>";
      
      return pageBar;

}
}