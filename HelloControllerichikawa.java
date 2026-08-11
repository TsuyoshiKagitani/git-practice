package com.example.attendanceManagement;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HelloControllerichikawa {
	@RequestMapping("/helloichikawa")
	public String hello(@RequestParam("name") String name, @RequestParam("age") int age) {
		return name + "さん" + age + "才、こんにちは";
	}
}