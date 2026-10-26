package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.Data;

@Data
public class SyuunyuuForm {
	
	@NotBlank
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "「日時」は YYYY-MM-DD 形式（例: 2025-01-01）で入力してください") // ★ 追加
	private String date; // 日付 
	
	@NotBlank
	private String classification; // 分類 
	
	@NotNull
	@PositiveOrZero
	private Integer amount; // 金額 
	
	private String shop; // 店舗 
	private String payment; // 支払方法 
	private String memo; // メモ
}