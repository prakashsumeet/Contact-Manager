package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@RequestMapping("/user")
@Controller
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private MyOrderRepository myOrderRepository;

	@ModelAttribute // ye aapne app call hota h
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println(userName);
		// get the user using database

		User user = userRepository.getUserByUserName(userName);
		System.out.println(user);
		model.addAttribute("title", "User DashBoard");
		model.addAttribute("user", user);
	}

	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {

		return "normal/user_dashboard";
	}

	// Open add form handler
	@GetMapping("/add_contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contacts");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// processing add contact form

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute("contact") Contact contact,
			@RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) {
		try {
			String userName = principal.getName();

			User user = this.userRepository.getUserByUserName(userName);
			contact.setUser(user);

			if (file.isEmpty()) {
				System.out.println("file is empty");
				contact.setImage("default.png");
			} else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("file uploaded");
			}

			user.getContacts().add(contact);
			this.userRepository.save(user);
			System.out.println("added");
			System.out.println(contact);

			session.setAttribute("message", new Message("Your Contact is added successfully !! Add more..", "success"));

		} catch (Exception e) {
			System.out.println("Error" + e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong !! Try Again..", "danger"));

		}
		return "normal/add_contact_form";
	}

	// show contacts handler

	@GetMapping("/show-contacts")
	public String showContacts(Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		List<Contact> contacts = contactRepository.findContactsByUser(user.getId());
		model.addAttribute("contacts", contacts);
		return "normal/show_contacts";
	}

	// showing particular contact details

	@RequestMapping("/contact/{cId}")
	public String showContactsDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println(cId);
		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		return "normal/contact_detail";
	}

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, Principal principal,
			HttpSession session) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		// contact.setUser(null);
		user.getContacts().remove(contact);
		userRepository.save(user);
		contactRepository.delete(contact);
		session.setAttribute("message", new Message("Contact deleted Successfully...", "success"));

		return "redirect:/user/show-contacts";
	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model model) {
		model.addAttribute("title", "Update Contact");
		Contact contact = contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);

		return "normal/update_form";
	}

	// update contact handler

	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute("contact") Contact contact,
			@RequestParam("profileImage") MultipartFile file, Model model, HttpSession session, Principal principal) {

		try {

			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();

			if (!file.isEmpty()) {

				// deleting the old photo

				File deleteFile = new ClassPathResource("static/img").getFile();

				File file1 = new File(deleteFile, oldcontactDetail.getImage());

				file1.delete();

				// upload new photo

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldcontactDetail.getImage());
			}
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);
			contact.setUser(user);

			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your Contact is updated...", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(contact.getcId());
		System.out.println(contact.getName());
		return "redirect:/user/contact/" + contact.getcId();
	}

	// your profile handler

	@GetMapping("/profile")
	public String yourProfile(Model model, Principal principal) {

		model.addAttribute("title", "Profile Page");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		model.addAttribute("user", user);
		return "normal/profile";
	}

	// open setting handler

	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}

	// change password handler

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal ,HttpSession session) {
		
		System.out.println(oldPassword);
		System.out.println(newPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		
		if(bCryptPasswordEncoder.matches(oldPassword , currentUser.getPassword()))
		{
			//change the password
			
			currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			userRepository.save(currentUser);
			
			session.setAttribute("message", new Message("Your Password Changes Successfully...","success"));
		}
		else 
		{
			session.setAttribute("message", new Message("You Entered Wrong Password !!","danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}
	
	
	//creating order for payment
	
	
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String,Object> data, Principal principal) throws Exception
	{
		System.out.println("Created");
		System.out.println(data);
		
		int amt = Integer.parseInt(data.get("amount").toString());
		
		RazorpayClient client = new RazorpayClient("rzp_test_pHJMo2dA8t3dbJ", "vlg1sGp8POGgI3pT8Jy33BmH");
		
		JSONObject ob = new JSONObject();
		ob.put("amount", amt*100);
		ob.put("currency", "INR");
		ob.put("receipt", "SCM_USER95766");
		
		//creating order
		
		Order order = client.orders.create(ob);
		System.out.println(order);
		//we can save this data to our database
		
		MyOrder myOrder = new MyOrder();
		
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(order.get(null));
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		
		myOrderRepository.save(myOrder);
		
	
		return order.toString();
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?>updateOrder(@RequestBody Map<String , Object> data)
	{
		
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myOrder);
		
		System.out.println(data);
		return ResponseEntity.ok("");
	}
	
	
	
	
	
	
	
	
	
	
	

}
