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

	// 支出入力画面の表示と年月での絞り込み (GET) [1] 
	@RequestMapping(value = "/shisyutsu", method = RequestMethod.GET) 
	public String shisyutsu( 
			@RequestParam(value = "yearMonth", required = false) String yearMonth, 
			Model model 
			) { 
		// 1. payoutテーブルからデータが存在する年月リスト（YYYY-MM）を取得 [1] 
		String ymSql = "SELECT DISTINCT TO_CHAR(date, 'YYYY-MM') AS ym FROM payout ORDER BY ym ASC"; 
		List<String> yearMonthList = jdbcTemplate.queryForList(ymSql, String.class);
		
		// 2. 年月が未指定の場合は最新の年月を設定 [1] 
		if (yearMonth == null || yearMonth.isEmpty()) { 
			if (!yearMonthList.isEmpty()) { 
				yearMonth = yearMonthList.get(yearMonthList.size() - 1);
			} else { 
				yearMonth = "2025-01";
			} 
		} 
		// 3. 当月分の全カラムデータを [日付昇順、分類昇順、金額昇順] で取得 [1] 
		String sql = "SELECT id, no, date, classification, amount, shop, payment, memo FROM payout " 
				+ "WHERE TO_CHAR(date, 'YYYY-MM') = ? " 
				+ "ORDER BY date ASC, classification ASC, amount ASC"; 
		
		List<Map<String, Object>> shisyutsuList = jdbcTemplate.queryForList(sql, yearMonth); // または TO_CHAR 使用時のパラメータ

		System.out.println("★ [DEBUG] 取得件数 = " + shisyutsuList.size());
		System.out.println("★ [DEBUG] 取得データの中身 = " + shisyutsuList);

		model.addAttribute("shisyutsuList", shisyutsuList); 
		model.addAttribute("yearMonthList", yearMonthList); 
		model.addAttribute("selectedYearMonth", yearMonth);
		
//		System.out.println("★ [DEBUG] 検索対象年月 yearMonth = " + yearMonth);
		
		return"shisyutsu";
	}

	// テーブル各行の一括更新処理 (POST) [1] 
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
		// 1. フォームから送信された各行データを更新 [1] 
		if (ids != null && !ids.isEmpty()) { 
			String updateSql = "UPDATE payout SET " 
					+ "date = CAST(? AS DATE), " 
					+ "classification = ?, " 
					+ "amount = ?, " 
					+ "shop = ?, " 
					+ "payment = ?, " 
					+ "memo = ? " 
					+ "WHERE id = CAST(? AS VARCHAR)"; 
			
			for (int i = 0; i < ids.size(); i++) { 
				String dateVal = (dates != null && i < dates.size()) ? dates.get(i) : null; 
				String classVal = (classifications != null && i < classifications.size()) ? classifications.get(i) : null; 
				Integer amountVal = (amounts != null && i < amounts.size()) ? amounts.get(i) : null; 
				String shopVal = (shops != null && i < shops.size()) ? shops.get(i) : null; 
				String paymentVal = (payments != null && i < payments.size()) ? payments.get(i) : null; 
				String memoVal = (memos != null && i < memos.size()) ? memos.get(i) : null; 
				
				jdbcTemplate.update(updateSql, 
						dateVal, 
						classVal, 
						amountVal, 
						shopVal, 
						paymentVal, 
						memoVal, 
						ids.get(i) 
						); 
				} 
			}
		// 2. 年月が取得できなかった場合のフォールバック処理 [1] 
		if (yearMonth == null || yearMonth.isEmpty()) { 
			String ymSql = "SELECT DISTINCT TO_CHAR(date, 'YYYY-MM') AS ym FROM payout ORDER BY ym ASC";
			List<String> yearMonthList = jdbcTemplate.queryForList(ymSql, String.class);
	        if (!yearMonthList.isEmpty()) {
	            yearMonth = yearMonthList.get(yearMonthList.size() - 1);
	        } else {
	            yearMonth = "2025-01";
	        }
	    }

	    // 3. 選択中の年月を引き継いで GET /shisyutsu へリダイレクト [1, 2]
	    return "redirect:/shisyutsu?yearMonth=" + yearMonth;

	}
			
	// レコード削除処理 (POST) 
	@RequestMapping(value = "/shisyutsu/delete", method = RequestMethod.POST) 
	public String delete( 
			@RequestParam(value = "deleteNo", required = false) String deleteNo, 
			@RequestParam(value = "yearMonth", required = false) String yearMonth 
			) { 
		// 1.削除Noが入力されている場合、payout テーブルから該当の id/no レコードを削除 
		if (deleteNo != null && !deleteNo.trim().isEmpty()) { 
			// PostgreSQLの型不一致エラーを防ぐため、安全にキャストして指定Noを削除します 
			String deleteSql = "DELETE FROM payout WHERE CAST(no AS VARCHAR) = ?"; 
			jdbcTemplate.update(deleteSql, deleteNo.trim());
			} 
		
		// 2\. 表示していた当月（yearMonth）を維持（未指定の場合はデフォルト "2025-01"） 
		if (yearMonth == null || yearMonth.isEmpty()) { 
			yearMonth = "2025-01"; 
		} 
		
		// 3\. PRGパターン適用：削除完了後に GET /shisyutsu へリダイレクト 
		// 既存の GET メソッドにより、最新データが「日付昇順、分類昇順、金額昇順」で自動取得・再表示されます 
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
			@RequestParam(value = "memo", required = false) String memo,
			@RequestParam(value = "yearMonth", required = false) String yearMonth, 
			Model model
			) {
		// 1. 日付が入力されている場合、payout テーブルへ INSERT 実行
		// ★【追加】「日付」「分類」「金額」のいずれかが未入力（nullまたは空文字）かチェック 
		if (date == null || date.trim().isEmpty() || classification == null || classification.trim().isEmpty() || amount == null) { 
			// 赤字表示用のエラーメッセージをModelにセット 
			model.addAttribute("registerError", "「日付」「分類」「金額」は必ず入力してください"); 
			// 再描画に必要な表示データ（年月リスト・一覧リスト）の取得 
			String ymSql = "SELECT DISTINCT TO_CHAR(date, 'YYYY-MM') AS ym FROM payout ORDER BY ym ASC"; 
			List<String> yearMonthList = jdbcTemplate.queryForList(ymSql, String.class);
			if (yearMonth == null || yearMonth.isEmpty()) { 
				if (!yearMonthList.isEmpty()) { 
					yearMonth = yearMonthList.get(yearMonthList.size() - 1); 
				} else { 
					yearMonth = "2025-01"; 
				} 
			} 
			
			String sql = "SELECT id, no, date, classification, amount, shop, payment, memo FROM payout " 
						+ "WHERE TO_CHAR(date, 'YYYY-MM') = ? " // ★ CAST から TO_CHAR(=) へ変更
						+ "ORDER BY date ASC, classification ASC, amount ASC"; 
					
			List<Map<String, Object>> shisyutsuList = jdbcTemplate.queryForList(sql, yearMonth);
			
			model.addAttribute("shisyutsuList", shisyutsuList);
			model.addAttribute("yearMonthList", yearMonthList);
			model.addAttribute("selectedYearMonth", yearMonth);
			
			return "shisyutsu"; // 自画面に戻りエラー文言を表示
			}
		// 1\. 全必須項目が入力されている場合、payout テーブルへ INSERT 実行 
		String insertSql = "INSERT INTO payout (date, classification, amount, shop, payment, memo) " 
		+ "VALUES (CAST(? AS DATE), ?, ?, ?, ?, ?)"; 
		
		jdbcTemplate.update(insertSql, date, classification, amount, shop, payment, memo); 
		
		// 2\. 登録した日付の年月（YYYY-MM）を抽出 
		if (date.length() >= 7) { 
			yearMonth = date.substring(0, 7); 
		} else if (yearMonth == null || yearMonth.isEmpty()) { 
			yearMonth = "2025-01"; 
		} 
		
		// 3\. PRGパターン適用：登録後に GET /shisyutsu へリダイレクト 
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