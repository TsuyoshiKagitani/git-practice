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
import com.example.demo.form.ShisyutsuForm; // ★【追加】ShisyutsuFormのインポート
import com.example.demo.form.SyuunyuuForm;

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
		List<Map<String, Object>> userList = jdbcTemplate.queryForList("SELECT id, password FROM login WHERE id = ?",
				inputId);

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
			@ModelAttribute("shisyutsuForm") ShisyutsuForm form, // ★【追加】画面バインド用フォームオブジェクトを受け取る
			@RequestParam(value = "yearMonth", required = false) String yearMonth,
			Model model) {
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

		return "shisyutsu";
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
			@RequestParam(value = "yearMonth", required = false) String yearMonth) {
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
				String classVal = (classifications != null && i < classifications.size()) ? classifications.get(i)
						: null;
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
						ids.get(i));
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
			@RequestParam(value = "yearMonth", required = false) String yearMonth) {
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
			@ModelAttribute("shisyutsuForm") @Validated ShisyutsuForm form, // ★【変更】@Validated による検証と Form 受取り 
			BindingResult result, // ★【追加】バリデーション結果の判定オブジェクト
			@RequestParam(value = "yearMonth", required = false) String yearMonth,
			Model model
	) {
		// 1. 日付が入力されている場合、payout テーブルへ INSERT 実行
		// ★【変更】Validationエラー（未入力など）が存在する場合の処理		
		if (result.hasErrors()) {
			
			// ★【追加】金額（amount）項目で文字入力（型エラー）または @PositiveOrZero エラーが発生しているかチェック 
			boolean isAmountTypeError = result.hasFieldErrors("amount") && 
					(result.getFieldError("amount").isBindingFailure() || 
					result.getFieldError("amount").getCode().contains("PositiveOrZero")); 
			
			if (isAmountTypeError) { 
				// ★ 数値以外の入力時メッセージをセット 
				result.reject("amountError", "「金額」には数値を入力してください"); 
				} else { 
				// ★ 未入力時メッセージをセット 
				result.reject("registerError", "「日付」「分類」「金額」は必ず入力してください"); 
			}
			
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

		// 1. payout テーブルへ INSERT 実行（form.get〇〇() で取得します） 
		String insertSql = "INSERT INTO payout (date, classification, amount, shop, payment, memo) "
				+ "VALUES (CAST(? AS DATE), ?, ?, ?, ?, ?)";

		jdbcTemplate.update(insertSql,
				form.getDate(),
				form.getClassification(),
				form.getAmount(),
				form.getShop(),
				form.getPayment(),
				form.getMemo());
		// 2. 登録した日付の年月（YYYY-MM）を抽出 
		if (form.getDate() != null && form.getDate().length() >= 7) {
			yearMonth = form.getDate().substring(0, 7);
		} else if (yearMonth == null || yearMonth.isEmpty()) {
			yearMonth = "2025-01";
		}

		// 3. PRGパターン適用：登録後に GET /shisyutsu へリダイレクト 
		return "redirect:/shisyutsu?yearMonth=" + yearMonth;
	}

	// 「収入入力」ボタンが押された時の遷移処理
	@RequestMapping(value = "/syuunyuu", method = RequestMethod.GET)
	public String syuunyuu(
			@ModelAttribute("syuunyuuForm") SyuunyuuForm form, // ★【追加】画面バインド用フォームオブジェクトを受け取る
			@RequestParam(value = "yearMonth", required = false) String yearMonth,
			Model model) {
		// 1. incomeテーブルからデータが存在する年月リスト（YYYY-MM）を取得 [1] 
		String ymSql = "SELECT DISTINCT TO_CHAR(date, 'YYYY-MM') AS ym FROM income ORDER BY ym ASC";
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
		String sql = "SELECT id, no, date, classification, amount, shop, payment, memo FROM income "
				+ "WHERE TO_CHAR(date, 'YYYY-MM') = ? "
				+ "ORDER BY date ASC, classification ASC, amount ASC";

		List<Map<String, Object>> syuunyuuList = jdbcTemplate.queryForList(sql, yearMonth); // または TO_CHAR 使用時のパラメータ

		System.out.println("★ [DEBUG] 取得件数 = " + syuunyuuList.size());
		System.out.println("★ [DEBUG] 取得データの中身 = " + syuunyuuList);

		model.addAttribute("syuunyuuList", syuunyuuList);
		model.addAttribute("yearMonthList", yearMonthList);
		model.addAttribute("selectedYearMonth", yearMonth);

		//		System.out.println("★ [DEBUG] 検索対象年月 yearMonth = " + yearMonth);

		return "syuunyuu";
	}

	// テーブル各行の一括更新処理 (POST) [1] 
	@RequestMapping(value = "/syuunyuu/update", method = RequestMethod.POST)
	public String update2(
			@RequestParam(value = "id", required = false) List<Integer> ids,
			@RequestParam(value = "date", required = false) List<String> dates,
			@RequestParam(value = "classification", required = false) List<String> classifications,
			@RequestParam(value = "amount", required = false) List<Integer> amounts,
			@RequestParam(value = "shop", required = false) List<String> shops,
			@RequestParam(value = "payment", required = false) List<String> payments,
			@RequestParam(value = "memo", required = false) List<String> memos,
			@RequestParam(value = "yearMonth", required = false) String yearMonth) {
		// 1. フォームから送信された各行データを更新 [1] 
		if (ids != null && !ids.isEmpty()) {
			String updateSql = "UPDATE syuunyuu SET "
					+ "date = CAST(? AS DATE), "
					+ "classification = ?, "
					+ "amount = ?, "
					+ "shop = ?, "
					+ "payment = ?, "
					+ "memo = ? "
					+ "WHERE id = CAST(? AS VARCHAR)";

			for (int i = 0; i < ids.size(); i++) {
				String dateVal = (dates != null && i < dates.size()) ? dates.get(i) : null;
				String classVal = (classifications != null && i < classifications.size()) ? classifications.get(i)
						: null;
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
						ids.get(i));
			}
		}
		// 2. 年月が取得できなかった場合のフォールバック処理 [1] 
		if (yearMonth == null || yearMonth.isEmpty()) {
			String ymSql = "SELECT DISTINCT TO_CHAR(date, 'YYYY-MM') AS ym FROM income ORDER BY ym ASC";
			List<String> yearMonthList = jdbcTemplate.queryForList(ymSql, String.class);
			if (!yearMonthList.isEmpty()) {
				yearMonth = yearMonthList.get(yearMonthList.size() - 1);
			} else {
				yearMonth = "2025-01";
			}
		}

		// 3. 選択中の年月を引き継いで GET /shisyutsu へリダイレクト [1, 2]
		return "redirect:/syuunyuu?yearMonth=" + yearMonth;

	}

	// レコード削除処理 (POST) 
	@RequestMapping(value = "/syuunyuu/delete", method = RequestMethod.POST)
	public String delete2(
			@RequestParam(value = "deleteNo", required = false) String deleteNo,
			@RequestParam(value = "yearMonth", required = false) String yearMonth) {
		// 1.削除Noが入力されている場合、payout テーブルから該当の id/no レコードを削除 
		if (deleteNo != null && !deleteNo.trim().isEmpty()) {
			// PostgreSQLの型不一致エラーを防ぐため、安全にキャストして指定Noを削除します 
			String deleteSql = "DELETE FROM income WHERE CAST(no AS VARCHAR) = ?";
			jdbcTemplate.update(deleteSql, deleteNo.trim());
		}

		// 2. 表示していた当月（yearMonth）を維持（未指定の場合はデフォルト "2025-01"） 
		if (yearMonth == null || yearMonth.isEmpty()) {
			yearMonth = "2025-01";
		}

		// 3. PRGパターン適用：削除完了後に GET /syuunyuu へリダイレクト 
		// 既存の GET メソッドにより、最新データが「日付昇順、分類昇順、金額昇順」で自動取得・再表示されます 
		return "redirect:/syuunyuu?yearMonth=" + yearMonth;
	}

	// 新規データ登録処理 (POST)
	@RequestMapping(value = "/syuunyuu/register", method = RequestMethod.POST)
	public String register2(
			@ModelAttribute("syuunyuuForm") @Validated SyuunyuuForm form, // ★【変更】@Validated による検証と Form 受取り 
			BindingResult result, // ★【追加】バリデーション結果の判定オブジェクト
			@RequestParam(value = "yearMonth", required = false) String yearMonth,
			Model model
	) {
		// 1. 日付が入力されている場合、payout テーブルへ INSERT 実行
		// ★【変更】Validationエラー（未入力など）が存在する場合の処理		
		if (result.hasErrors()) {
			
			// ★【追加】金額（amount）項目で文字入力（型エラー）または @PositiveOrZero エラーが発生しているかチェック 
			boolean isAmountTypeError = result.hasFieldErrors("amount") && 
					(result.getFieldError("amount").isBindingFailure() || 
					result.getFieldError("amount").getCode().contains("PositiveOrZero")); 
			
			if (isAmountTypeError) { 
				// ★ 数値以外の入力時メッセージをセット 
				result.reject("amountError", "「金額」には数値を入力してください"); 
				} else { 
				// ★ 未入力時メッセージをセット 
				result.reject("registerError", "「日付」「分類」「金額」は必ず入力してください"); 
			}
			
			String ymSql = "SELECT DISTINCT TO_CHAR(date, 'YYYY-MM') AS ym FROM income ORDER BY ym ASC";
			List<String> yearMonthList = jdbcTemplate.queryForList(ymSql, String.class);

			if (yearMonth == null || yearMonth.isEmpty()) {
				if (!yearMonthList.isEmpty()) {
					yearMonth = yearMonthList.get(yearMonthList.size() - 1);
				} else {
					yearMonth = "2025-01";
				}
			}

			String sql = "SELECT id, no, date, classification, amount, shop, payment, memo FROM income "
					+ "WHERE TO_CHAR(date, 'YYYY-MM') = ? " // ★ CAST から TO_CHAR(=) へ変更
					+ "ORDER BY date ASC, classification ASC, amount ASC";

			List<Map<String, Object>> syuunyuuList = jdbcTemplate.queryForList(sql, yearMonth);

			model.addAttribute("syuunyuuList", syuunyuuList);
			model.addAttribute("yearMonthList", yearMonthList);
			model.addAttribute("selectedYearMonth", yearMonth);

			return "syuunyuu"; // 自画面に戻りエラー文言を表示

		}

		// 1. income テーブルへ INSERT 実行（form.get〇〇() で取得します） 
		String insertSql = "INSERT INTO income (date, classification, amount, shop, payment, memo) "
				+ "VALUES (CAST(? AS DATE), ?, ?, ?, ?, ?)";

		jdbcTemplate.update(insertSql,
				form.getDate(),
				form.getClassification(),
				form.getAmount(),
				form.getShop(),
				form.getPayment(),
				form.getMemo());
		// 2. 登録した日付の年月（YYYY-MM）を抽出 
		if (form.getDate() != null && form.getDate().length() >= 7) {
			yearMonth = form.getDate().substring(0, 7);
		} else if (yearMonth == null || yearMonth.isEmpty()) {
			yearMonth = "2025-01";
		}

		// 3. PRGパターン適用：登録後に GET /shisyutsu へリダイレクト 
		return "redirect:/syuunyuu?yearMonth=" + yearMonth;
	}

	// 「集計データ」ボタンが押された時の遷移処理
	@RequestMapping(value = "/syuukei", method = RequestMethod.GET)
	public String syuukei() {
		// 表示したい集計データ画面のHTML名（例: syuukei.html）を指定します
		return "syuukei";
	}
}