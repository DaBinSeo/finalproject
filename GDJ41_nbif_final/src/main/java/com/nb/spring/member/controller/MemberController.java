package com.nb.spring.member.controller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nb.spring.common.DealType;
import com.nb.spring.common.MsgModelView;
import com.nb.spring.common.PageFactory;
import com.nb.spring.common.StringHandler;
import com.nb.spring.common.WalletType;
import com.nb.spring.member.model.service.MemberService;
import com.nb.spring.member.model.service.SendEmailService;
import com.nb.spring.member.model.vo.Member;
import com.nb.spring.member.model.vo.Wallet;
import com.nb.spring.member.model.vo.WishList;
import com.nb.spring.product.model.vo.Product;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/member")
@SessionAttributes({"loginMember","admin","accessToken" ,"salesCnt","buyCnt","msgCount"})
public class MemberController {
	
	
	
	@Autowired
	private MemberService service;      
	
	@Autowired
	private SendEmailService mailService;
	
	@Autowired
	private PasswordEncoder encoder;
	
	@PostMapping("/kakaoEnroll")
	public ModelAndView kakaoEnroll(@RequestParam Map param,HttpSession session, ModelAndView mv) {
		log.debug("{}",param);
		Member m = service.loginMemberKakao(param);
		
		if(m!=null) {
			return MsgModelView.msgBuild(mv, "/member/login", "?????? ????????? ??????????????????.");
		}
		
		session.setAttribute("userEmail", param.get("email"));
		mv.setViewName("login/enrollMember");
		return mv;
	}
	
	
	@PostMapping("/kakaoLogin")
	public ModelAndView kakaoLogin(@RequestParam Map param, ModelAndView mv) {
		
		log.debug("{}",param);
		
		Member m = service.loginMemberKakao(param);
		
		if(m!=null) {
			mv.addObject("loginMember", m);
			mv.addObject("accessToken", param.get("accessToken"));
			return MsgModelView.msgBuild(mv, "/", "????????? ??????!");
		}else {
		    return MsgModelView.msgBuild(mv, "/member/login", "????????? ??????!");
		}

		
	}

	@PostMapping("/loginMember")
	public ModelAndView loginMember(ModelAndView mv, String email, String password, String flexCheckDefault, 
			HttpServletResponse res) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("email", email);
		//param.put("password", password);
		Member m = service.loginMember(param);
		if(flexCheckDefault!=null) {
			Cookie c = new Cookie("flexCheckDefault",email);
			c.setPath("/");
			c.setMaxAge(24*60*60*7);
			res.addCookie(c);
		}else {
			Cookie c = new Cookie("flexCheckDefault",email);
			c.setPath("/");
			c.setMaxAge(0);
			res.addCookie(c);
		}
		if(m!=null&&encoder.matches(password, m.getPassword())) {

			if(m.getNickName().equals("admin")) {
				mv.addObject("admin",true);
			}else {
				mv.addObject("admin",false);
			}
			mv.addObject("loginMember", m);
			mv.addObject("msg","????????? ??????");
			mv.addObject("loc","/");
		}else {
			mv.addObject("msg","????????? ??????, ?????? ???????????????.");
			mv.addObject("loc","/member/login");
		}
		mv.setViewName("common/msg");
		return mv;
	}
	
	@RequestMapping("/logout")
	public ModelAndView logout(HttpSession session, SessionStatus stauts, ModelAndView mv) {
		if(!stauts.isComplete()) {
			stauts.setComplete();
		}
		session.invalidate();
		String msg = "???????????? ??????";
		String loc = "/";
		mv.addObject("msg", msg);
		mv.addObject("loc", loc);
		mv.setViewName("common/msg");
		return mv;
	}
	
	@RequestMapping("/myPage")
	public ModelAndView myPage(String memberNo, ModelAndView mv) {
		Member m = service.selectMember(memberNo);
		
//		??????
		List<Product> list = service.salesList(memberNo);
		int total = list.size();
		int status1=0;
		int status2=0;
		int status3=0;
		int status4=0;
		if(list.isEmpty()) {
			List<Integer> zeroList = List.of(0,0,0,0,0);
			mv.addObject("salesCnt", zeroList);
		} else {
			for(Product p : list) {
				if(p.getProductNo() != null) {
					if(p.getPermissionYn().equals("0") || p.getPermissionYn().equals("2")) { //????????????
						status1++;
					}
					if(p.getPermissionYn().equals("1") && p.getProductStatus().equals("0")) { //?????????
						status2++;
					}
					if(p.getProductStatus().equals("1")|| p.getProductStatus().equals("2")||p.getProductStatus().equals("3")) { //????????????
						status3++;
					}
					if(p.getProductStatus().equals("4") || p.getProductStatus().equals("5")) { //??????
						status4++;
					}
				}
			}
			List<Integer> salesCnt = List.of(total,status1,status2,status3,status4);
			mv.addObject("salesCnt", salesCnt);
		}
		
		mv.addObject("productList",list);
		
//		??????
		List<Wallet> buyList = service.buyList(memberNo);
		int buyTotal = buyList.size();
		int buying=0;
		int waiting=0;
		int end=0;
		
		if(buyList.isEmpty()) {
			List<Integer> zeroList = List.of(0,0,0,0,0);
			mv.addObject("buyCnt", zeroList);
		} else {
			for(Wallet w : buyList) {
				if(!(w.getCategoryDetail().equals("0"))) {
					if(w.getProductNo().getProductStatus() != null) {
						if(w.getProductNo().getProductStatus().equals("0")) {
							buying++;
						}
					}
					if(w.getProductNo().getProductStatus() != null && w.getProductNo().getFinalPrice() != null) {
						if((w.getProductNo().getProductStatus().equals("1")
								||w.getProductNo().getProductStatus().equals("2"))
								&& w.getProductNo().getFinalPrice().equals(w.getAmount())) { //????????????
							waiting++;
						}
						if((w.getProductNo().getProductStatus().equals("3")
								||w.getProductNo().getProductStatus().equals("4")
								||w.getProductNo().getProductStatus().equals("5"))
								&& w.getProductNo().getFinalPrice().equals(w.getAmount()) ) { //??????
							end++;
						}
						if(!(w.getProductNo().getProductStatus().equals("0"))
								&& !(w.getProductNo().getFinalPrice().equals(w.getAmount()))) { //??????
							end++;
						}
					}
				}
			}
			List<Integer> buyCnt = List.of(buyTotal,buying,waiting,end);
			mv.addObject("buyCnt", buyCnt);
		}
		mv.addObject("productList",buyList);
		
		mv.addObject("myPageMember",m);	
		mv.setViewName("login/myPage");
		return mv;
	}
	
	@RequestMapping("/enrollMember")
	public String enrollmemberView() {
		return "login/enrollEmail";
	}
	
	@PostMapping("/email")
	@ResponseBody
	public Map sendEmail(HttpSession session, String userEmail) {
		log.debug(userEmail);
		String result="";
		String code="";
		Map<String,String> param = Map.of("email", userEmail);
		Member m = service.selectMemberPhoneEmail(param);
		
		if(m!=null) {
			return Map.of("result","?????? ????????? ???????????????.");
		}
		
		
		
		session.removeAttribute("userEmail");
		try {
			code = mailService.mailSend(userEmail);			
		}catch(Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException();
		}
			
		if(code!=null&&code.length()>0) {
			result ="????????????";
			session.setAttribute("emailCode", code);
			session.setAttribute("userEmail", userEmail);
		}else {
			result ="????????????";
		}
		
		return Map.of("result",result);
	}
	
	@PostMapping("/certification")
	@ResponseBody
	public Map certification(String inputCode, HttpSession session) {
		
		String codeInSession = (String)session.getAttribute("emailCode");
		boolean result =false;
		if(codeInSession.equals(inputCode)) {
			// ?????? ?????? 
			result =true;
			session.removeAttribute("emailCode");
		}else {
			//?????? 
			result =false;
		}
		
		
		return Map.of("result",result);
	}
	
	@RequestMapping("/enrollMemberMainView")
	public String enrollMemberMainView() {
		return "login/enrollMember";
	}
	
	@RequestMapping("/duplicationCheck")
	@ResponseBody
	public Map duplicationCheck(String nickName) {
		
		Member m  = service.selectMemberNickName(nickName);
		boolean result = false;
		if(m!=null) {
			result = false;
		}else {
			result =true;
		}
		return Map.of("result",result);
	}
	
	@PostMapping("/enrollMemberMain")
	public ModelAndView enrollMemberMain(@RequestParam Map<String, String> param,HttpSession session, ModelAndView mv) {
		log.debug("{}",param);
		String totalAddress = param.get("address")+" "+ param.get("detailAddress")+" "+param.get("plusAddress");
		String email = (String)session.getAttribute("userEmail");
		
		String encodingPw = encoder.encode(param.get("password"));
		log.debug(encodingPw);
		Member m = Member.builder()
				.memberName(param.get("name"))
				.password(encodingPw)
				.phone(param.get("phone"))
				.email(email)
				.nickName(param.get("nickName"))
				.address(totalAddress)
				.deliveryAddress(totalAddress)
				.build();
		
		int result = service.insertMember(m);
		session.removeAttribute("userEmail");
		if(result > 0) {
			
			mv.addObject("msg","???????????? ??????");
			mv.addObject("loc","/");
		}else {
			mv.addObject("msg","???????????? ??????. ?????? ??????????????????.");
			mv.addObject("loc","/member/enrollMember");
		}
		mv.setViewName("common/msg");
		return mv;
	
	}
	
	@RequestMapping("/findId")
	public String findId() {
		return "login/findId";
	}
	
	@PostMapping("/findIdEnd")
	public ModelAndView findIdEnd(String name, String phone, ModelAndView mv) {
		log.debug(name,phone);
		Member m = service.selectMemberNamePhone(Map.of("name",name,"phone",phone));
		log.debug("{}",m);
		
		if(m==null) {
			String msg = "?????? ???????????????.";
			String loc = "/login";
			mv.addObject("msg", msg);
			mv.addObject("loc", loc);
			mv.setViewName("common/msg");
			return mv;
		}
		
		
		
		
		String email = m.getEmail();
		String id = email.substring(0, email.indexOf("@"));
		String address = email.substring(email.indexOf("@"));
		String idFront = id.substring(0,id.length()-3);
		String idEnd = id.substring(id.length()-3);
		String temp="";
		
		for(int i=0; i<idEnd.length();i++) {
			temp+="*";
		}
		
		String modifyEmail = idFront+temp+address;
		
		mv.addObject("userId", modifyEmail);
		mv.addObject("userName",m.getMemberName());
		mv.setViewName("login/findIdConfirm");
		
		
		return mv;
	}
	
	@RequestMapping("/findPassword")
	public String findPassword() {
		return "login/findPassword";
	}
	
	@PostMapping("/findPasswordEnd")
	public ModelAndView findPasswordEnd(String phone, String email, ModelAndView mv) throws Exception {
		
		Map<String, String > param = Map.of("phone",phone,"email",email);
		Member m = service.selectMemberPhoneEmail(param);
		String msg = "";
		String loc = "";
		if(m==null) {
			msg = "?????? ???????????????.";
			loc = "/member/findPassword";
		}else {
			
			String newEncodingPw = encoder.encode(mailService.mailSendNewPassword(m.getEmail()));
			log.debug(newEncodingPw);
			Map<String, String> param2 = Map.of("memberNo",m.getMemberNo(),"newPw",newEncodingPw);
			int result = service.updatePassword(param2);
			
			log.debug("{}",result);
			if(result>0) {
				msg = "?????? ???????????? ????????????";
				loc = "/member/login";
			}else {
				msg = "?????? ???????????? ?????? ??????";
				loc = "/member/findPassword";
			}
			
		}
		mv.addObject("msg", msg);
		mv.addObject("loc", loc);
		mv.setViewName("common/msg");
		return mv;
	}
	
	@RequestMapping("/login")
	public String loginView() {
		return "login/loginView";
	}
	
	@RequestMapping("/salesStates")
	public ModelAndView salesStates(String memberNo, ModelAndView mv) {
		List<Product> list = service.salesList(memberNo);
		mv.addObject("productList",list);
		mv.setViewName("product/salesStates");
		return mv;
	}

	@RequestMapping(value = "/salesSearch", method=RequestMethod.POST)
	public String salesSearch ( @RequestParam(value = "status", required=false ) 
	String status, String startDate, String endDate, String memberNo, Model m) { //@RequestParam(value = "count") List<Integer> count
		Map param = new HashMap<>();
			param.put("startDate", startDate);
			param.put("endDate", endDate);
			param.put("status", status);
			param.put("memberNo", memberNo);
		List<Product> list = service.salesSearch(param);

//		m.addAttribute("salesCnt", count);
		m.addAttribute("productList",list);
		return "product/salesStates";
	}
	
	@RequestMapping("/buyStates")
	public ModelAndView buyStates(String memberNo, ModelAndView mv) {
		List<Wallet> buyList = service.buyList(memberNo);
		mv.addObject("productList",buyList);
		mv.setViewName("product/buyStates");
		return mv;
	}
	
	@RequestMapping(value = "/buySearch", method=RequestMethod.POST)
	public String buySearch ( @RequestParam(value = "status", required=false ) 
	String status, String startDate, String endDate, String memberNo, Model m) {
		Map param = new HashMap<>();
			param.put("startDate", startDate);
			param.put("endDate", endDate);
			param.put("status", status);
			param.put("memberNo", memberNo);
		List<Wallet> list = service.buySearch(param);

//		m.addAttribute("buyCnt", count);
//		m.addAttribute("productList",list.get(0).getWalletList());
		m.addAttribute("productList",list);
		return "product/buyStates";
	}
	
	@RequestMapping("/emoneyDetail")
	public ModelAndView emoneyDetail(@RequestParam(name = "cPage",defaultValue = "1") int cPage,
			@RequestParam(value="numPerPage",defaultValue="15") int numPerPage, String memberNo, ModelAndView mv) {	
		
		int pageBarSize = 5;
		int totalData = service.emoneyCount(memberNo);
		List<Wallet> list = service.emoneyDetail(cPage, numPerPage, memberNo);
		Member m = service.selectMember(memberNo);
		System.out.println(list);
		
		mv.addObject("m",m);
		mv.addObject("list",list);
		mv.addObject("numPerPage",numPerPage);
		mv.addObject("pageBar", PageFactory.getPageBar(totalData, cPage, numPerPage, pageBarSize, "emoneyDetail"));
		mv.setViewName("login/emoneyDetail");
		return mv;
	}

	@RequestMapping("/charge")
	public ModelAndView charge(HttpSession session ,ModelAndView mv) {
		
		Member m = (Member)session.getAttribute("loginMember");
		if(m==null) {
			return MsgModelView.msgBuild(mv, "/", "????????? ??? ??????????????????.");
		}
		
		//???????????? ?????????
		m = service.selectMember(m.getMemberNo());
		
		mv.addObject("member", m);
		
		mv.setViewName("login/chargeMoney");
		
		return mv;
	}
	

	@RequestMapping("/kakaoPay")
	@ResponseBody
	public String kakaoPay(String amount) {
		log.debug(amount);
		String numAmount = StringHandler.removeComma(amount);
		try {
			URL address = new URL("https://kapi.kakao.com/v1/payment/ready");
			HttpURLConnection connection = (HttpURLConnection)address.openConnection();//????????????
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "KakaoAK 6bbfc35e54bdc78656dc5040bd19b498"); // ????????? ???
			connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
			connection.setDoOutput(true); // ???????????? ???????????? ????????? ?????????
			String parameter = "cid=TC0ONETIME" // ????????? ??????
					+ "&partner_order_id=partner_order_id" // ????????? ????????????
					+ "&partner_user_id=partner_user_id" // ????????? ?????? id
					+ "&item_name=????????????" // ?????????
					+ "&quantity=1" // ?????? ??????
					+ "&total_amount="+numAmount // ??? ??????
					+ "&vat_amount=200" // ?????????
					+ "&tax_free_amount=0" // ?????? ????????? ??????
					+ "&approval_url=http://localhost:9090" // ?????? ?????? ???
					+ "&fail_url=http://localhost:9090" // ?????? ?????? ???
					+ "&cancel_url=http://localhost:9090"; // ?????? ?????? ???
			log.debug(parameter);
			OutputStream send = connection.getOutputStream(); // ?????? ????????? ??? ??? ??? ??????.
			DataOutputStream dataSend = new DataOutputStream(send); // ?????? ???????????? ??? ??? ??????.
			dataSend.writeBytes(parameter); // OutputStream??? ???????????? ????????? ???????????? ?????? ????????? ???????????? ??????. (?????????)
			dataSend.close(); // flush??? ???????????? ????????? ?????? ?????????. (????????? ????????? ??????)
			
			int result = connection.getResponseCode(); // ?????? ??? ?????? ????????? ????????? ?????????.
			InputStream receive; // ??????
			
			if(result == 200) {
				receive = connection.getInputStream();
			}else {
				receive = connection.getErrorStream(); 
			}
			// ?????? ??????
			InputStreamReader read = new InputStreamReader(receive); // ????????? ?????????.
			BufferedReader change = new BufferedReader(read); // ???????????? ?????? ?????? ????????? ??????????????? ????????? ???????????? ?????? ???????????? ???????????? ?????????.
			// ?????? ??????
			return change.readLine(); // ???????????? ???????????? ????????? ????????? ???????????? ????????? ????????? ????????????.




		}catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	@RequestMapping("/kakaoSuccess")
	@ResponseBody
	public Boolean kakaoSuccess(String amount, HttpSession session) {
		String numAmount = StringHandler.removeComma(amount);
		
		Member m = (Member)session.getAttribute("loginMember");
		
		Map<String, Object> param = Map.of(
										"memberNo",m.getMemberNo(),
										"dealType",DealType.INPUT,
										"walletType",WalletType.CHARGE,
										"bidPrice",numAmount,
										"productNo",""
									);
		int result = service.updateBalance(DealType.INPUT, param);
		
		if(result>0) {
			return true;
		}else {
			return false;
		}
		
		
	}
	
	@RequestMapping("/emoneySelectList")
	public String emoneySelectList(HttpSession session, @RequestParam(value = "btnCategory", required=false ) String category,
			@RequestParam(name = "cPage",defaultValue = "1") int cPage,
			@RequestParam(value="numPerPage",defaultValue="15") int numPerPage, Model m) {
		Member sessionMem = (Member) session.getAttribute("loginMember");
		Map param = new HashMap<>();
		param.put("category", category);
		param.put("memberNo", sessionMem.getMemberNo());
		param.put("cPage", cPage);
		param.put("numPerPage", numPerPage);
		
		int pageBarSize = 5;
		int totalData = service.emoneySelectCount(param);

		List<Wallet> list = service.emoneySelectList(param);
		Member mem = service.selectMember(sessionMem.getMemberNo());
		System.out.println("select : "+category+ mem+ list);
		m.addAttribute("m",mem);
		m.addAttribute("list",list);
		m.addAttribute("numPerPage",numPerPage);
		m.addAttribute("pageBar", PageFactory.emoneySearch(totalData, cPage, numPerPage, pageBarSize, "emoneySelectList", category));
		return "login/emoneyDetail";
	}
	
	@RequestMapping("/myWishList")
	public ModelAndView myWishList(String memberNo, ModelAndView mv) {
		List<WishList> list = service.myWishList(memberNo);
//		List<Member> list = service.myWishList(memberNo);
		mv.addObject("list",list);
		mv.setViewName("/login/wishList");
		return mv;
	}
	
	@RequestMapping("/deleteWish")
	public ModelAndView deleteWish(@RequestParam Map<String,String> param, ModelAndView mv) {
		int result = service.deleteWish(param);
		System.out.println(param.get("memberNo"));
		
		String msg = "";
		String loc = "/member/myWishList?memberNo="+param.get("memberNo");
		
		if(result>0) {
			msg = "????????? ?????????????????????.";
		} else {
			msg = "?????????????????????.";
		}
		
		mv.addObject("msg",msg);
		mv.addObject("loc",loc);
		mv.setViewName("/common/msg");
		return mv;
	}

	
	//????????????
	@RequestMapping("/sellerrank")
	public ModelAndView sellerrank(ModelAndView mv) {
		List<Map<String,Object>> sellerList = service.sellerrank();
		
		mv.addObject("sellerList",sellerList);
		mv.setViewName("member/sellerrank");
		return mv;
	}
	//????????????
	@RequestMapping("/sellList")
	public ModelAndView sellList(ModelAndView mv, String memberNo) {
		List<Product> sellList=service.sellList(memberNo);
		
		mv.addObject("sellList",sellList);
		mv.addObject("seller",memberNo);
		mv.setViewName("member/sellList");
		return mv;
	}
	
	//??????????????????
	@RequestMapping("/deleteMemberView")
	public String deleteMemberView() {
		return "member/deleteMemberView";
	}
	
	//???????????? ??????
	@RequestMapping("/beforeDelete")
	@ResponseBody
	public void beforeDelete(HttpServletResponse response, String memberNo) throws IOException{
		List<Map<String,Object>> list=service.beforeDelete(memberNo);
		int result;
		if(list.isEmpty()) {
			result=0;
		}else {
			result=1;
		}
		System.out.println("result="+result);
		PrintWriter out=response.getWriter();
		out.write(result+"");
	}
	
	@RequestMapping(value="/checkPw", method=RequestMethod.POST)
	@ResponseBody
	public void pwCheck(HttpServletResponse response, String memberNo, String password) throws Exception{
		String pw = service.pwCheck(memberNo);
		
		int result;
		if(pw!=null&&encoder.matches(password, pw)){
			result= 1;
		}else {
			result=0;
		}
		PrintWriter out=response.getWriter();
		out.write(result+"");
	}
	
	@RequestMapping("/deleteMember")
	public String deleteMember(String memberNo, SessionStatus status, HttpServletResponse response,
			HttpSession session) throws Exception{
		int result=service.deleteMember(memberNo);
		if(result>0) {
			status.setComplete();
			session.invalidate();
		}
		response.setContentType("text/html; charset=UTF-8"); 
		PrintWriter out = response.getWriter(); 
		out.println("<script>alert('??????????????? ??????????????????.'); location.href='/';</script>"); 
		out.flush();
		return "redirect:/";
	}
	
	
	@RequestMapping("/updateMyPage")
	public String updateMyPage(String memberNo, Model m) {
		Member member = service.selectMember(memberNo);
		m.addAttribute("m",member);
		return "login/updateMyPage";
	}
	
	@RequestMapping("/updateMyPageEnd") 
	public ModelAndView updateMyPageEnd(HttpSession session, @RequestParam Map<String,String> param, ModelAndView mv) {
		Member m = (Member) session.getAttribute("loginMember");
		System.out.println(param);
		
		if(param.get("shipAddress").equals("")) {
			param.put("address", param.get("memberAddress"));
		} else {
			String totalAddress = param.get("shipAddress")+" "+param.get("detailAddress");
			param.put("address", totalAddress);
		}
		System.out.println(param);
		int result = service.updateMember(param); //??????????????? ????????? ?????? 
		
		String msg = "";
		String loc = "/member/myPage?memberNo="+m.getMemberNo();
		
		if(result>0) {
			msg = "????????? ?????????????????????.";
		} else {
			msg = "?????????????????????. ??????????????? ???????????????.";
		}
		
		mv.addObject("msg",msg);
		mv.addObject("loc",loc);
		mv.setViewName("/common/msg");
		return mv;
	}
	
	@RequestMapping("/updatePassword")
	public String updatePassword() {
		return "/login/updatePassword";
	}
	
	@RequestMapping("/updatePasswordEnd")
	@ResponseBody
	public Map updatePasswordEnd(HttpSession session, String pw, String newPw) {
		Member m = (Member) session.getAttribute("loginMember");

		String msg = "";
		if(encoder.matches(pw,m.getPassword())) { //????????????
			newPw = encoder.encode(newPw);
			Map<String, String> param = new HashMap<>();
			param.put("newPw", newPw);
			param.put("memberNo", m.getMemberNo());
			int result = service.updatePassword(param);
			if(result>0) {
				msg = "???????????? ????????? ?????????????????????.";
			} else {
				msg = "???????????? ????????? ?????????????????????. ???????????? ???????????????.";
			}
		} else {
			msg = "????????????????????? ???????????? ????????????.";
		}
		return Map.of("result",msg);
	}
	
}
