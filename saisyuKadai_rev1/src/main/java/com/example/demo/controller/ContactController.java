package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.example.demo.form.Login;

@Controller
public class ContactController {
	
	@RequestMapping("/login")
	public String login(Login log, Model model) {
		return "login";
	}
	@RequestMapping(value = "/top", method = RequestMethod.POST)
	public String top(@ModelAttribute Login log, BindingResult result,
			Model model) {
		return "top";
	}
}