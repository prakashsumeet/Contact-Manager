package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {

	Random random = new Random(100000);

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	// email id form open handler

	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		System.out.println(email);

		int otp = random.nextInt(999999);
		System.out.println(otp);

		// code for send otp to email

		String subject = "OTP from SCM";
		String message = "OTP is :" + otp;
		String to = email;

		boolean flag = emailService.sendEmail(subject, message, to);

		if (flag) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		} else {
			session.setAttribute("message", "Check your email id !!");
			return "forgot_email_form";
		}

	}

	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		int myotp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");
		
		

		if (myotp == otp) {

			User user = userRepository.getUserByUserName(email);

			if (user == null) {

				// send error message

				session.setAttribute("message", "User does'nt exists !!");
				return "forgot_email_form";

			} else {
				// send change passwowd form
			}

			return "password_change_form";
		} else {
			session.setAttribute("message", "Invalid OTP !!");
			return "verify_otp";
		}

	}
	
	//change password
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session)
	{
		String email = (String) session.getAttribute("email");
		User user = userRepository.getUserByUserName(email);
		
		user.setPassword(bCryptPasswordEncoder.encode(newpassword));
		userRepository.save(user);
		
		return "redirect:/signin?change=Password changed successfully...";
	}
	

}
