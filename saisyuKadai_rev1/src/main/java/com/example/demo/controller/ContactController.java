package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.example.demo.form.Login;
@Controller
public class ContactController {

	// データベース（SQL）と通信するための道具を準備
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	// ①ログイン画面の表示 (GET)
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(@ModelAttribute("login") Login log) { // ★ "login" という名前で空の箱を画面へ渡す
        return "login";
    }

	// ① IDとパスワードを入力し、「GO」ボタンを押下したときに動くメソッド
	@RequestMapping(value = "/top", method = RequestMethod.POST)
	public String top(@ModelAttribute("login") @Validated Login log, BindingResult result, Model model) {
		
	    if (result.hasErrors()) {
	        return "login"; 
	    }
	    
		// 【①】入力されたIDとパスワードをフォームから取得
		String inputId = log.getID();
		String inputPassword = log.getPassword();


		// 【②】〇〇テーブルの「ID」と「パスワード」を取得する
		// （※ここでは入力されたIDをキーにして該当のユーザーレコードをDBから取得します）
		List<Map<String, Object>> userList = jdbcTemplate.queryForList("SELECT id, password FROM login WHERE id = ?", inputId);

		// 【③】①で入力したIDと②で取得したIDを比較する
		// DBから検索結果が得られなかった場合（＝入力されたIDがDBに存在しない / ID不一致）
		if (userList.isEmpty()) {
			// ★画面全体のエラー（グローバルエラー）を手動で登録する
			result.reject("loginError", "IDまたはパスワードが間違っています。");
			return "login"; // IDが不一致のためログイン画面に戻る
		}

		// DBにIDが存在した場合、該当する1件目のデータからDB上のIDとパスワードを取り出す
		Map<String, Object> userData = userList.get(0);
		String dbId = (String) userData.get("id");
		String dbPassword = (String) userData.get("password");

		// 【④】①で入力したパスワードと②で取得したパスワードを比較する
		if (dbPassword != null && dbPassword.equals(inputPassword)) {
			
			// ★【追加】ログイン成功したユーザーのIDを画面に渡す
			model.addAttribute("username", dbId); 
			
			// 【⑤】パスワードも一致すればトップ画面に遷移する
			return "top";

		} else {
			// パスワードが不一致の場合
			// ★パスワードが不一致の場合も、同じエラーメッセージを登録する
			result.reject("loginError", "IDまたはパスワードが間違っています。");
			return "login"; // ログイン画面に戻る
		}
	}
	
    // 「支出入力」ボタンが押された時の遷移処理
    @RequestMapping(value = "/shisyutsu", method = RequestMethod.GET)
    public String shisyutsu() {
        // 表示したい支出入力画面のHTML名（例: shishutsu.html）を指定します
        return "shisyutsu"; 
    }
    
    // 「収入入力」ボタンが押された時の遷移処理
    @RequestMapping(value = "/syuunyuu", method = RequestMethod.GET)
    public String syuunyuu() {
        // 表示したい収入入力画面のHTML名（例: syuunyuu.html）を指定します
        return "syuunyuu"; 
    }
    
    // 「集計データ」ボタンが押された時の遷移処理
    @RequestMapping(value = "/syuukei", method = RequestMethod.GET)
    public String syuukei() {
        // 表示したい集計データ画面のHTML名（例: syuukei.html）を指定します
        return "syuukei"; 
    }
}