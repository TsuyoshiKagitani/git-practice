package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class Login {
	
	@NotBlank
	private String ID;
	
	@NotBlank
	private String Password;
}