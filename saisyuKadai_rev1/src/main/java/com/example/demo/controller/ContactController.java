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
import org.springframework.web.bind.annotation.RequestParam;

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
	
	  // 支出入力画面の表示と年月での絞り込み (GET)
    @RequestMapping(value = "/shisyutsu", method = RequestMethod.GET)
    public String shisyutsu(
            @RequestParam(value = "yearMonth", required = false) String yearMonth,
            Model model
    ) {
        // 1. 初期表示時のデフォルト処理 (パラメータが空ならデフォルトで "2025-01" とする)
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = "2025-01";
        }

        // 2. 絞り込み用のSQLを作成（日付の昇順、分類の昇順、金額の昇順でソート）
        String sql = "SELECT id, date, classification, amount, shop, payment, memo FROM payout "
                   + "WHERE CAST(date AS VARCHAR) LIKE ? "
                   + "ORDER BY date ASC, classification ASC, amount ASC";

        List<Map<String, Object>> shisyutsuList = jdbcTemplate.queryForList(sql, yearMonth + "%");

        // 3. 画面側にデータと「現在選択されている年月」を渡す
        model.addAttribute("shisyutsuList", shisyutsuList);
        model.addAttribute("selectedYearMonth", yearMonth);

        return "shisyutsu";
    }

    // テーブル各行の一括更新処理 (POST)
    @RequestMapping(value = "/shisyutsu/update", method = RequestMethod.POST)
    public String update(
            @RequestParam(value = "id", required = false) List<Integer> ids,
            @RequestParam(value = "date", required = false) List<String> dates,
            @RequestParam(value = "classification", required = false) List<String> classifications,
            @RequestParam(value = "amount", required = false) List<Integer> amounts,
            @RequestParam(value = "shop", required = false) List<String> shops,
            @RequestParam(value = "payment", required = false) List<String> payments,
            @RequestParam(value = "memo", required = false) List<String> memos,
            @RequestParam(value = "yearMonth", required = false) String yearMonth
    ) {
        // 各行の id を参照して payout テーブルを更新
        if (ids != null && !ids.isEmpty()) {
        	String updateSql = "UPDATE payout SET "
                    + "date = CAST(? AS DATE), "
                    + "classification = ?, "
                    + "amount = ?, "
                    + "shop = ?, "
                    + "payment = ?, "
                    + "memo = ? "
                    + "WHERE id = CAST(? AS VARCHAR)"; // ★ ? を VARCHAR にキャスト

            for (int i = 0; i < ids.size(); i++) {
                jdbcTemplate.update(updateSql,
                    dates.get(i),
                    classifications.get(i),
                    amounts.get(i),
                    shops.get(i),
                    payments.get(i),
                    memos.get(i),
                    ids.get(i)
                );
            }
        }

        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = "2025-01";
        }

        // 更新後は PRG パターンで GET /shisyutsu へリダイレクト
        // これにより当月分データが [日付昇順、分類昇順、金額昇順] で自動的に全件取得・表示されます
        return "redirect:/shisyutsu?yearMonth=" + yearMonth;
    }
    
 // 新規データ登録処理 (POST)
    @RequestMapping(value = "/shisyutsu/register", method = RequestMethod.POST)
    public String register(
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "classification", required = false) String classification,
            @RequestParam(value = "amount", required = false) Integer amount,
            @RequestParam(value = "shop", required = false) String shop,
            @RequestParam(value = "payment", required = false) String payment,
            @RequestParam(value = "memo", required = false) String memo
    ) {
        // 1. 日付が入力されている場合、payout テーブルへ INSERT 実行
        if (date != null && !date.isEmpty()) {
            String insertSql = "INSERT INTO payout (date, classification, amount, shop, payment, memo) "
                             + "VALUES (CAST(? AS DATE), ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql, date, classification, amount, shop, payment, memo);
        }

        // 2. 登録した日付の年月（YYYY-MM）を抽出（例: "2025-01-01" -> "2025-01"）
        String yearMonth = "2025-01";
        if (date != null && date.length() >= 7) {
            yearMonth = date.substring(0, 7);
        }

        // 3. PRGパターン適用：登録後に GET /shisyutsu へリダイレクト
        // 既存の GET メソッドが呼び出され、該当月データが「日付昇順、分類昇順、金額昇順」で自動取得・表示されます
        return "redirect:/shisyutsu?yearMonth=" + yearMonth;
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