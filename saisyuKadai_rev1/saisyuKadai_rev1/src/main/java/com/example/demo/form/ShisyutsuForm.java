package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ShisyutsuForm {
	@NotBlank private String date; // 日付 
	@NotBlank private String classification; // 分類 
	@NotNull private Integer amount; // 金額 
	private String shop; // 店舗 
	private String payment; // 支払方法 
	private String memo; // メモ
}