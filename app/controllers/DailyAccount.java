package controllers;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import models.BalanceTypeMst;
import models.Budget;
import models.HaUser;
import models.IdealDepositMst;
import models.ItemMst;
import models.WkDaToDl;
import models.WkDailyAccount;
import models.WkDailyAccountRender;

import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class DailyAccount extends Controller {

	@Before
	static void setConnectedUser() {
		if(!Security.isConnected())
			return;
		
		HaUser haUser  = HaUser.find("byEmail", Security.connected()).first();
		renderArgs.put("haUser", haUser);
	}
	
	static final String BALANCE_TYPE_IN = Messages.get("BalanceType.in");
	static final String BALANCE_TYPE_OUT = Messages.get("BalanceType.out");
	static final String BALANCE_TYPE_BANK_IN = Messages.get("BalanceType.bank_in");
	static final String BALANCE_TYPE_BANK_OUT = Messages.get("BalanceType.bank_out");
	static final String BALANCE_TYPE_IDEAL_DEPOSIT_IN = Messages.get("BalanceType.ideal_deposit_in");
	static final String BALANCE_TYPE_IDEAL_DEPOSIT_OUT = Messages.get("BalanceType.ideal_deposit_out");
	static final String BALANCE_TYPE_OUT_IDEAL_DEPOSIT = Messages.get("BalanceType.out_ideal_deposit");
	static final String REMAINDER_TYPE_REAL = Messages.get("RemainderType.real");
	static final String REMAINDER_TYPE_IDEAL_DEPOSIT = Messages.get("RemainderType.ideal_deposit");
	static final String REMAINDER_TYPE_NOT_IDEAL_DEPOSIT = Messages.get("RemainderType.not_ideal_deposit");
	static final String HANDLING_TYPE_CASH = Messages.get("HandlingType.cash");
	static final String HANDLING_TYPE_BANK = Messages.get("HandlingType.bank");
	static final String HANDLING_TYPE_EMONEY = Messages.get("HandlingType.emoney");
	static final String HANDLING_TYPE_CRECA = Messages.get("HandlingType.creca");
	static final String VIEWS_DAILY_ACCOUNT = Messages.get("views.dailyaccount.dailyaccount");
	static final String VIEWS_BALANCE_TABLE = Messages.get("views.dailyaccount.balancetable");
	
	
	/**
	 * 日計表
	 * @param sBasisDate
	 */
	public static void dailyAccount(
			String sBasisDate
			) {
		DailyAccount da = new DailyAccount();
		//単純に呼ばれた時の基準日のセット
		if(sBasisDate==null)
			sBasisDate = da.setBasisDate();
		
		Integer intBasisDate = null; 
		if(sBasisDate!=null)
			intBasisDate = Integer.parseInt(sBasisDate.replace("/", ""));
		dailyAccountDisp(intBasisDate);
	}
	
	/**
	 * 残高表
	 * @param sBasisDate
	 */
	public static void balanceTable(
			String sBasisDate
			) {
		DailyAccount da = new DailyAccount();
   		//単純に呼ばれた時の基準日のセット(残高表)
		if(sBasisDate==null)
			sBasisDate = da.setBasisDateBt();

		Integer intBasisDate = null;
		if(sBasisDate!=null)
			intBasisDate = Integer.parseInt(sBasisDate.replace("/", ""));
		balanceTableDisp(intBasisDate);
	}
	
	/**
	 * 日計表の表示
	 * @param intBasisDate
	 */
	public static void dailyAccountDisp(
			Integer intBasisDate
			) {
		String sBasisDate = null;
		DailyAccount da = new DailyAccount();
		Common cm = new Common();
		//単純に呼ばれた時の基準日のセット
		if(intBasisDate==null) {
			sBasisDate = da.setBasisDate();
		} else {
			if(cm.checkIntDate(intBasisDate)) {
				String strTmp = intBasisDate.toString();
				sBasisDate = strTmp.substring(0, 4) + "/" + strTmp.substring(4, 6) + "/" + strTmp.substring(6, 8);
			} else {
				validation.addError("dateError", Messages.get("validation.dateError"));
				sBasisDate = da.setBasisDate();
			}
				
		}
//		日計表で1ヶ月分表示していた時のコード
//		String strDispDate = sBasisDate;
//		if (!sBasisDate.substring(sBasisDate.length()-2).equals("01")) {
//			sBasisDate = sBasisDate.substring(0, sBasisDate.length()-2) + "01";
//		}
		
		String strTableType = VIEWS_DAILY_ACCOUNT;

		//日計表・残高表の表示用ワーク作成
		WkDailyAccountRender wkDAR = makeWkDAR(sBasisDate, strTableType);
		
		int month = wkDAR.getIntMonth();
//		sBasisDate = wkDAR.getStrBasisDate();
		String[] sAryDays = wkDAR.getStrAryDays();
		List<WkDailyAccount> lWDA = wkDAR.getlWDA();
		int iWidth = wkDAR.getIntWidth();
		
//		render("@dailyAccount",  month, sBasisDate, strDispDate, strTableType, sAryDays, lWDA, iWidth);
		render("@dailyAccount",  month, sBasisDate, strTableType, sAryDays, lWDA, iWidth);
	}
	
	/**
	 * 残高表の表示
	 * @param sBasisDate
	 */
	public static void balanceTableDisp(
			Integer intBasisDate
			) {
		String sBasisDate = null;
		DailyAccount da = new DailyAccount();
		Common cm = new Common();
		//単純に呼ばれた時の基準日のセット(残高表)
		if(intBasisDate==null) {
			sBasisDate = da.setBasisDateBt();
		} else {
			if(cm.checkIntDate(intBasisDate)) {
				String strTmp = intBasisDate.toString();
				sBasisDate = strTmp.substring(0, 4) + "/" + strTmp.substring(4, 6) + "/" + strTmp.substring(6, 8);
			} else {
				validation.addError("dateError", Messages.get("validation.dateError"));
				sBasisDate = da.setBasisDateBt();
			}
				
		}
		
		String strTableType = VIEWS_BALANCE_TABLE;

		//日計表・残高表の表示用ワーク作成
		WkDailyAccountRender wkDAR = makeWkDAR(sBasisDate, strTableType);
		
		int month = wkDAR.getIntMonth();
		sBasisDate = wkDAR.getStrBasisDate();
		String[] sAryDays = wkDAR.getStrAryDays();
		List<WkDailyAccount> lWDA = wkDAR.getlWDA();
		int iWidth = wkDAR.getIntWidth();
		
		render("@balanceTable",  month, sBasisDate, strTableType, sAryDays, lWDA, iWidth);
	}
	
	/**
	 * 日計表・残高表の表示用ワーク作成
	 * @param strBasisDate
	 * @param strTableType
	 * @return
	 */
	public static WkDailyAccountRender makeWkDAR(
			String strBasisDate,
			String strTableType
			) {

		WkDailyAccountRender wkDAR = new WkDailyAccountRender();
   		Calendar calendar = Calendar.getInstance();
   		
   		wkDAR.setStrBasisDate(strBasisDate);
   		
		//検索条件をセッションに保存
		if(strTableType.equals(VIEWS_DAILY_ACCOUNT)) {
			session.put("daFilExistFlg", "true");
			session.put("daStrBasisDate", strBasisDate);
		}
		if(strTableType.equals(VIEWS_BALANCE_TABLE)) {
			session.put("btFilExistFlg", "true");
			session.put("btStrBasisDate", strBasisDate);
		}
		
		Date dBasis = null;
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
		try {
//			dBasis = DateFormat.getDateInstance().parse(strBasisDate);
			dBasis = df.parse(strBasisDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		calendar.setTime(dBasis);
		int year = calendar.get(Calendar.YEAR);
		wkDAR.setIntMonth(calendar.get(Calendar.MONTH) + 1);

		int iDaysCnt = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		
//		日計表の表示日数を3日にしていた時のコード
		if(strTableType.equals(VIEWS_DAILY_ACCOUNT)) {
			iDaysCnt = 5;
			calendar.add(Calendar.DATE, -2);
		}
		if(strTableType.equals(VIEWS_BALANCE_TABLE)) {
			iDaysCnt = 1;
		}
		
   		
		//日計表のヘッダーの日付の配列
		String[] strAryDays = new String[iDaysCnt];
		Date dStartDay = calendar.getTime();
		SimpleDateFormat sdf1 = new SimpleDateFormat("M/d");
		
		for(int iDay = 0; iDay < iDaysCnt; iDay++) {
			strAryDays[iDay] = sdf1.format(calendar.getTime());
			calendar.add(Calendar.DATE, 1);
		}
		wkDAR.setStrAryDays(strAryDays);
		

		DailyAccount da = new DailyAccount();
		
		//日計表の行に相当するリストの作成
		wkDAR.setlWDA(da.makeWorkList(year, wkDAR.getIntMonth(), dStartDay, iDaysCnt, strTableType));
   		
   		//日計表の日付ごとの部分のスクロール内の幅の設定
		wkDAR.setIntWidth(iDaysCnt * 101);
   		
		return wkDAR;
	}

	
	/**
	 * 基準日変更による移動
	 * @param e_basis_date
	 * @param strTableType
	 * @param move
	 */
	public static void jump(
    		String e_basis_date,
    		String strTableType,
    		String strMoveType,
    		Integer intMoveNum,
    		String move			/* 「移動」ボタン */
			) {
		
		// 「移動」ボタンが押されていない時は処理を抜ける
		if(move==null)
			return;
		
		//ジャンプ後の日付を取得
		DailyAccount da = new DailyAccount();
		if(e_basis_date!=null && strMoveType!=null && intMoveNum!=null )
			e_basis_date = da.dteAfterJump(e_basis_date, strMoveType, intMoveNum);
		
		if(strTableType.equals(VIEWS_DAILY_ACCOUNT)) {
			dailyAccount(e_basis_date);
			return;
		} 
		if(strTableType.equals(VIEWS_BALANCE_TABLE)) {
			balanceTable(e_basis_date);
			return;
		}
	}
	
	/**
	 * ジャンプ後の日付を取得
	 * @param e_basis_date
	 * @param strMoveType
	 * @param intMoveNum
	 * @return
	 */
	private String dteAfterJump(
    		String e_basis_date,
    		String strMoveType,
    		Integer intMoveNum
			) {
		Calendar calendar = Calendar.getInstance();
		Date dBasis = null;
		try {
			dBasis = DateFormat.getDateInstance().parse(e_basis_date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		calendar.setTime(dBasis);
		if(strMoveType.equals(Messages.get("views.dailyaccount.movetype.year"))) {
			calendar.add(Calendar.YEAR, intMoveNum);
		}
		if(strMoveType.equals(Messages.get("views.dailyaccount.movetype.month"))) {
			calendar.add(Calendar.MONTH, intMoveNum);
		}
		if(strMoveType.equals(Messages.get("views.dailyaccount.movetype.days"))) {
			calendar.add(Calendar.DATE, intMoveNum);
		}
		e_basis_date = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
		
		return e_basis_date;
	}
	
	/**
	 * 日計表・残高表の行に相当するリストの作成
	 * @param year
	 * @param month
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param strTableType
	 * @return
	 */
	private List<WkDailyAccount> makeWorkList(
			Integer year,
			Integer month,
			Date dStartDay,
			int iDaysCnt,
			String strTableType
			) {
		
		//日計表・残高表の行に相当するリスト
   		List<WkDailyAccount> lWDA = new ArrayList<WkDailyAccount>();
   		
   		Calendar calendar = Calendar.getInstance();
   		
		calendar.set(year, month - 1, 1);
		String sFirstDay =  String.format("%1$tY%1$tm%1$td", calendar.getTime());
   		calendar.add(Calendar.MONTH, 1);
   		String sNextFirst = String.format("%1$tY%1$tm%1$td", calendar.getTime());
   		
		HaUser haUser = (HaUser)renderArgs.get("haUser");

		if(strTableType.equals(VIEWS_BALANCE_TABLE))
			sFirstDay =  String.format("%1$tY%1$tm%1$td", dStartDay);
		
		//全行取得用SQL作成
		String sql = makeSql(year, month, dStartDay, iDaysCnt, strTableType, haUser, sFirstDay, sNextFirst);
		
		List<Object[]> lstObjEach = JPA.em().createNativeQuery(sql).getResultList();
		
//		long[] lAryDaysRlAll = new long[iDaysCnt];		//合計行の日毎金額
		
		int intCnt = 0;
		int intLwdaCnt = lWDA.size();
		String strOldLargeCategoryName = "";	//大分類ブレイク用の旧大分類名
		for(Object[] objEach : lstObjEach) {
			String strLargeCategoryName = String.valueOf(objEach[4]);
			
			if(intCnt==0)
				strOldLargeCategoryName = strLargeCategoryName;
			
			//大分類が変わる時
			if(!strOldLargeCategoryName.equals(strLargeCategoryName)) {
				lWDA.get(intLwdaCnt+intCnt-1).setBolLastItemFlg(true);	//大分類ブレイク時に、一つ前の行に最終行フラグを立てる
				strOldLargeCategoryName = strLargeCategoryName;
			}
			intCnt++;
			
			
			WkDailyAccount wDaEach = new WkDailyAccount();
			
			wDaEach.setsLargeCategory(strLargeCategoryName);
			wDaEach.setsItem(String.valueOf(objEach[2]));
			wDaEach.setbBudgetFlg(false);
	
			//「収入」・「支出」・「My貯金預入」の場合、予算有無フラグを立てる
			if(strLargeCategoryName.equals(BALANCE_TYPE_IN) ||
					strLargeCategoryName.equals(BALANCE_TYPE_OUT) ||
					strLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN)) {
				wDaEach.setbBudgetFlg(true);
			}
			
			// ある時だけ予算をセット
			if(objEach[6]!=null) {
				Long lngBgId = Long.parseLong(String.valueOf(objEach[5]));
				Long lngBgAmount = Long.parseLong(String.valueOf(objEach[6]));
				
				String sBudgetAmount = String.format("%1$,3d", lngBgAmount).trim();
				
				wDaEach.setlBudgetId(lngBgId);
				wDaEach.setlBudgetAmount(lngBgAmount);
				wDaEach.setsBudgetAmount(sBudgetAmount);
			}
			
			// 日毎
			calendar.setTime(dStartDay);
			long lngEach = 0L;
//			long lngSum = 0L;
			List<WkDaToDl> lstWdtd = new ArrayList<WkDaToDl>();
			for(int iDay = 0; iDay < iDaysCnt; iDay++) {
				//「実残高」・「My貯金してないお金」・「My貯金残高」の時は2日目以降は加算
				if(strLargeCategoryName.equals(REMAINDER_TYPE_REAL) ||
						strLargeCategoryName.equals(REMAINDER_TYPE_NOT_IDEAL_DEPOSIT) ||
						strLargeCategoryName.equals(REMAINDER_TYPE_IDEAL_DEPOSIT)) {
					lngEach += Long.parseLong(String.valueOf(objEach[iDay+7]));	//日毎金額
//					lngSum = lngEach;											//月計
				} else {
					lngEach = Long.parseLong(String.valueOf(objEach[iDay+7]));	//日毎金額
//					lngSum += lngEach;											//月計
				}
//				lAryDaysRlAll[iDay] += lngEach;	//合計行の日毎金額
				
				//日計表から明細表へのリンクのための引数をセット
				WkDaToDl wkDaToDl = makeWkDaToDl(
						calendar,
						strLargeCategoryName,
						lngEach,
						Long.parseLong(String.valueOf(objEach[0]))
						);

				lstWdtd.add(wkDaToDl);
				
				calendar.add(Calendar.DATE, 1);
			}
			wDaEach.setLstWdtd(lstWdtd);
//			wDaEach.setLSumMonth(lngSum);
			//「収入」・「支出」・「My貯金預入」・「My貯金から支払」の場合、月計をセット
			if(strLargeCategoryName.equals(BALANCE_TYPE_IN) ||
					strLargeCategoryName.equals(BALANCE_TYPE_OUT) ||
					strLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN) ||
					strLargeCategoryName.equals(BALANCE_TYPE_OUT_IDEAL_DEPOSIT)) {
				wDaEach.setLSumMonth(Long.parseLong(String.valueOf(objEach[iDaysCnt+7])));
			}
			
			lWDA.add(wDaEach);
		}
		
		return lWDA;
	}

	/**
	 * 全行取得用SQL作成
	 * @param year
	 * @param month
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param strTableType
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @return
	 */
	private String makeSql(
			Integer year,
			Integer month,
			Date dStartDay,
			int iDaysCnt,
			String strTableType,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst
			) {
		
		String sql = "";
		if(strTableType.equals(VIEWS_DAILY_ACCOUNT)) {
			//収支取得用SQL作成(収入)(合計)
			String sqlBalInAll = makeSqlBalInOut(
					false,
					BALANCE_TYPE_IN,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(収入)(項目毎)
			String sqlBalIn = makeSqlBalInOut(
					true,
					BALANCE_TYPE_IN,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(支出)(合計)
			String sqlBalOutAll = makeSqlBalInOut(
					false,
					BALANCE_TYPE_OUT,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(支出)(項目毎)
			String sqlBalOut = makeSqlBalInOut(
					true,
					BALANCE_TYPE_OUT,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(My貯金預入)(合計)
			String sqlBalIdealInAll = makeSqlBalIdeal(
					false,
					BALANCE_TYPE_IDEAL_DEPOSIT_IN,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(My貯金預入)(項目毎)
			String sqlBalIdealIn = makeSqlBalIdeal(
					true,
					BALANCE_TYPE_IDEAL_DEPOSIT_IN,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(My貯金から支払)(合計)
			String sqlBalOutIdealAll = makeSqlBalIdeal(
					false,
					BALANCE_TYPE_OUT_IDEAL_DEPOSIT,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			//収支取得用SQL作成(My貯金から支払)(項目毎)
			String sqlBalOutIdeal = makeSqlBalIdeal(
					true,
					BALANCE_TYPE_OUT_IDEAL_DEPOSIT,
					year, month, dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
					);
			
			sql = "" +
					" ( " + sqlBalInAll + " ) " +
					" UNION ALL " +
					" ( " + sqlBalIn + " ) " +
					" UNION ALL " +
					" ( " + sqlBalOutAll + " ) " +
					" UNION ALL " +
					" ( " + sqlBalOut + " ) " +
					" UNION ALL " +
					" ( " + sqlBalIdealInAll + " ) " +
					" UNION ALL " +
					" ( " + sqlBalIdealIn + " ) " +
					" UNION ALL " +
					" ( " + sqlBalOutIdealAll + " ) " +
					" UNION ALL " +
					" ( " + sqlBalOutIdeal + " ) " +
//					" UNION ALL " +
					" ORDER BY cate_order, item_order " +
					"";
			
			while(!(sql.equals(sql.replaceAll("  ", " "))))
				sql = sql.replaceAll("  ", " ");
			
			return sql;
			
		}
		//残高取得用SQL作成(実残高)(合計)
		String sqlRemRealAll = makeSqlRemReal(
						false,
						dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
				);
		//残高取得用SQL作成(実残高)(取扱毎)
		String sqlRemReal = makeSqlRemReal(
						true,
						dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
				);
		//残高取得用SQL作成(My貯金してないお金)
		String sqlRemNotIdealDeposit = makeSqlRemNotIdealDeposit(
						dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
				);
		//残高取得用SQL作成(My貯金残高)(合計)
		String sqlRemIdealDepositAll = makeSqlRemIdealDeposit(
						false,
						dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
				);
		//残高取得用SQL作成(My貯金残高)(取扱毎)
		String sqlRemIdealDeposit = makeSqlRemIdealDeposit(
						true,
						dStartDay, iDaysCnt, haUser, sFirstDay, sNextFirst
				);
		
		sql += "" +
				" ( " + sqlRemRealAll + " ) " +
				" UNION ALL " +
				" ( " + sqlRemReal + " ) " +
				" UNION ALL " +
				" ( " + sqlRemNotIdealDeposit + " ) " +
				" UNION ALL " +
				" ( " + sqlRemIdealDepositAll + " ) " +
				" UNION ALL " +
				" ( " + sqlRemIdealDeposit + " ) " +
				" ORDER BY cate_order, item_order " +
				"";
		
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高取得用SQL作成(実残高)
	 * @param bolEach
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @return
	 */
	private String makeSqlRemReal(
			boolean bolEach,
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst
			) {
		
		String sql = "";
		
		/** 
   		 * SQL固定部分作成
   		 */
		
		//  FROM句
		String sqlFromPhrase = "" +
				" FROM Record r " +
				" LEFT JOIN ItemMst i " +
				"   ON r.item_mst_id = i.id " +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON r.balance_type_mst_id = b.id " +
				" LEFT JOIN HandlingMst h " +
				"   ON r.handling_mst_id = h.id " +
				" LEFT JOIN HandlingTypeMst ht " +
				"   ON h.handling_type_mst_id = ht.id " +
				" LEFT JOIN HandlingMst hb " +
				"   ON h.debit_bank_id = hb.id " +
				"";
		
		
		/* CASE文内の加減算の条件 */
		
		// 収入：加算、支出：減算
		String sqlSumAllCaseInOut = "" +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_IN + "' THEN r.amount " +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_OUT + "' THEN -r.amount " +
				"";
		// 口座引出：加算、口座預入：減算
		String sqlSumCashCaseBankInOut = "" +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_BANK_OUT + "' THEN r.amount " +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_BANK_IN + "' THEN -r.amount " +
				"";
		// 口座引出：減算、口座預入：加算
		String sqlSumNotCashCaseBankInOut = "" +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_BANK_OUT + "' THEN -r.amount" +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_BANK_IN + "' THEN r.amount" +
				"";
		
		if(!bolEach) {
			//残高取得用SQL作成(実残高)(取扱合計)
			sql = makeSqlRemRealAll(
					dStartDay,
					iDaysCnt,
					haUser,
					sFirstDay,
					sNextFirst,
					sqlFromPhrase,
					sqlSumAllCaseInOut
					);
			
			return sql;
		}
		
		
		String sqlDailyLater = "";
		String sqlDailyZero = "";
   		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dStartDay);
		for(int iDay = 1; iDay <= iDaysCnt; iDay++) {
			sqlDailyZero += "" +
					" OR  sum_day_" + iDay + " <> 0 ";
			calendar.add(Calendar.DATE, 1);
			if(iDay > 1)
				sqlDailyLater += "" +
						" ,COALESCE(rem_later.sum_day_" + iDay + ", 0) as sum_day_" + iDay + " " +
						"";
		}
		
		//残高取得用SQL作成(実残高)(取扱毎)
		sql = makeSqlRemRealEach(
				dStartDay,
				iDaysCnt,
				haUser,
				sFirstDay,
				sNextFirst,
				sqlFromPhrase,
				sqlSumAllCaseInOut,
				sqlSumCashCaseBankInOut,
				sqlSumNotCashCaseBankInOut,
				sqlDailyLater,
				sqlDailyZero);
		
		return sql;
	}
				
	/**
	 * 残高取得用SQL作成(実残高)(取扱合計)
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumAllCaseInOut
	 * @return
	 */
	private String makeSqlRemRealAll(
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst,
			String sqlFromPhrase,
			String sqlSumAllCaseInOut
			) {
	
		String sql = "";
		
		//初日の残高取得用SQL作成（取扱合計）
		String sqlAllFirstDay = makeSqlRemRealAllFirstDay(
				haUser,
				sFirstDay,
				sqlFromPhrase,
				sqlSumAllCaseInOut
				);
		sql = "" +
				" SELECT " +
				"   *" +
				" FROM ( SELECT 0 as item_id, 0 as item_order, cast('' as character varying(255)) as item_name, 50 as cate_order " +
				"  ,cast('" + REMAINDER_TYPE_REAL + "' as character varying(255)) as cate_name " +
				"  ,0 as bg_id " +
				"  ,0 as bg_amount " +
				" ) rem_item " +
				" CROSS JOIN ( " + sqlAllFirstDay + " ) rem_firstday " +
				"";
		//2日目以降の残高取得用SQL作成（取扱合計）
		if(iDaysCnt>=2) {
			String sqlAllLater = makeSqlRemRealAllLater(
					dStartDay,
					iDaysCnt,
					haUser,
					sFirstDay,
					sNextFirst,
					sqlFromPhrase,
					sqlSumAllCaseInOut
					);
			sql += " CROSS JOIN (" + sqlAllLater + " ) rem_later ";
		}
		
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		
		return sql;
	}
	
	/**
	 * 初日の残高取得用SQL作成(実残高)(取扱合計)
	 * @param haUser
	 * @param sFirstDay
	 * @param sqlFromPhrase
	 * @param sqlSumAllCaseInOut
	 * @return
	 */
	private String makeSqlRemRealAllFirstDay(
			HaUser haUser,
			String sFirstDay,
			String sqlFromPhrase,
			String sqlSumAllCaseInOut
			) {

   		String sqlDaily = "";

		//日付毎の合計取得部分のSQL
		sqlDaily += "" +
				" COALESCE(SUM(" +
				"   CASE " +
				sqlSumAllCaseInOut +	//収入加算・支出減算
				"   END " +
				" ), 0) as sum_day_1";
   		
		String sqlFirstDay = "" +
   				" SELECT " +
   				sqlDaily +
				sqlFromPhrase +		//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) <= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				"";
   		
		while(!(sqlFirstDay.equals(sqlFirstDay.replaceAll("  ", " "))))
			sqlFirstDay = sqlFirstDay.replaceAll("  ", " ");
		
		return sqlFirstDay;
	}
	
	/**
	 * 2日目以降の残高取得用SQL作成(実残高)(取扱合計)
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumAllCaseInOut
	 * @return
	 */
	private String makeSqlRemRealAllLater(
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst,
			String sqlFromPhrase,
			String sqlSumAllCaseInOut
			) {
		
   		String sqlDaily = "";
   		
   		Calendar calendar = Calendar.getInstance();

   		calendar.setTime(dStartDay);
   		
		//日付毎の合計取得部分のSQL(現金)
		calendar.setTime(dStartDay);
		for(int iDay = 2; iDay <= iDaysCnt; iDay++) {
			calendar.add(Calendar.DATE, 1);
			sqlDaily += "" +
					(iDay==2 ? " " : ",") +
					" COALESCE(SUM(" +
					"   CASE " +
					"     WHEN cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) = to_date('" + String.format("%1$tY%1$tm%1$td", calendar.getTime()) + "', 'YYYYMMDD') THEN " +
					"       CASE " +
					sqlSumAllCaseInOut +	//収入加算・支出減算
					"       END " +
					"     ELSE 0 " +
					"   END" +
					" ), 0) as sum_day_" + iDay + " ";
		}

		String sqlLater = "" +
   				" SELECT " +
   				sqlDaily +
				sqlFromPhrase +		//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) >= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) < to_date('" + sNextFirst + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				"";
   		
		while(!(sqlLater.equals(sqlLater.replaceAll("  ", " "))))
			sqlLater = sqlLater.replaceAll("  ", " ");
		
		return sqlLater;
	}
	
	
	/**
	 * 残高取得用SQL作成(実残高)(取扱毎)
	 * @param bolEach
	 * @param haUser
	 * @param sFirstDay
	 * @param sqlFromPhrase
	 * @param sqlSumAllCaseInOut
	 * @param sqlSumCashCaseBankInOut
	 * @param sqlSumNotCashCaseBankInOut
	 * @return
	 */
	private String makeSqlRemRealEach(
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst,
			String sqlFromPhrase,
			String sqlSumAllCaseInOut,
			String sqlSumCashCaseBankInOut,
			String sqlSumNotCashCaseBankInOut,
			String sqlDailyLater,
			String sqlDailyZero
			) {
		
		String sql = "";
		
		//初日の残高取得用SQL作成（取扱(実際)毎）
		String sqlEachFirstDay = makeSqlRemRealEachFirstDay(
				haUser,
				sFirstDay,
				sqlFromPhrase,
				sqlSumAllCaseInOut,
				sqlSumCashCaseBankInOut,
				sqlSumNotCashCaseBankInOut
				);
		sql = "" +
				" SELECT " +
				"   hm.id as item_id " +
				"  ,(htm.handling_type_order*10000 + hm.order_seq) as item_order " +
				"  ,hm.handling_name as item_name " +
				"  ,50 as cate_order " +
				"  ,cast('" + REMAINDER_TYPE_REAL + "' as character varying(255)) as cate_name " +
				"  ,0 as bg_id " +
				"  ,0 as bg_amount " +
				"  ,COALESCE(rem_firstday.sum_day_1, 0) as sum_day_1 " +
				" FROM HandlingMst hm " +
				" LEFT JOIN HandlingTypeMst htm " +
				"   ON hm.handling_type_mst_id = htm.id " +
				" LEFT JOIN ( " + sqlEachFirstDay + " ) rem_firstday " +
				"   ON hm.id = rem_firstday.hd_id " +
				"";
		//2日目以降の残高取得用SQL作成（取扱(実際)毎）
		if(iDaysCnt>=2) {
			String sqlEachLater = makeSqlRemRealEachLater(
					dStartDay,
					iDaysCnt,
					haUser,
					sFirstDay,
					sNextFirst,
					sqlFromPhrase,
					sqlSumAllCaseInOut,
					sqlSumCashCaseBankInOut,
					sqlSumNotCashCaseBankInOut
					);
			sql += "" +
					" LEFT JOIN (" + sqlEachLater + " ) rem_later " +
					"   ON hm.id = rem_later.hd_id " +
					"";
		}
		sql += "" +
				" WHERE hm.ha_user_id = " + haUser.id +
				"   AND htm.handling_type_name != '" + HANDLING_TYPE_CRECA + "' " +
				"   AND (hm.zero_hidden = false " + sqlDailyZero + ") " +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 初日の残高取得用SQL作成(実残高)(取扱毎)
	 * @param bolEach
	 * @param haUser
	 * @param sFirstDay
	 * @param sqlFromPhrase
	 * @param sqlSumAllCaseInOut
	 * @param sqlSumCashCaseBankInOut
	 * @param sqlSumNotCashCaseBankInOut
	 * @return
	 */
	private String makeSqlRemRealEachFirstDay(
			HaUser haUser,
			String sFirstDay,
			String sqlFromPhrase,
			String sqlSumAllCaseInOut,
			String sqlSumCashCaseBankInOut,
			String sqlSumNotCashCaseBankInOut
			) {
		
		String sqlCashDaily = "";
		String sqlNotCashDaily = "";
		
		//日付毎の合計取得部分のSQL(現金)
		sqlCashDaily += "" +
				" COALESCE(SUM(" +
				"   CASE " +
				sqlSumAllCaseInOut +		//収入加算・支出減算
				sqlSumCashCaseBankInOut +	//口座引出加算・口座預入減算
				"   END " +
				" ), 0) as sum_day_1";
		
		//日付毎の合計取得部分のSQL(現金以外)
		sqlNotCashDaily += "" +
				" COALESCE(SUM(" +
				"   CASE " +
				sqlSumAllCaseInOut +			//収入加算・支出減算
				sqlSumNotCashCaseBankInOut +	//口座引出減算・口座預入加算
				"   END " +
				" ), 0) as sum_day_1";

		
		//現金取得用SQL
		String sqlCash = "" +
				" SELECT " +
				sqlCashDaily +		//日付毎の合計取得部分(現金)
				"  ,hcash.handling_name as hd_handling_name " +
				"  ,hcash.id as hd_id" +
				"  ,hcash.zero_hidden as hd_zero_hidden " +
				sqlFromPhrase +		//FROM句
				" CROSS JOIN HandlingMst hcash " +
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND (   (    ht.handling_type_name = '" + HANDLING_TYPE_CASH + "' " +
				"            AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"            ) " +
				"        OR (    ht.handling_type_name in('" + HANDLING_TYPE_BANK + "','" + HANDLING_TYPE_EMONEY + "') " +
				"            AND b.balance_type_name in('" + BALANCE_TYPE_BANK_OUT + "','" + BALANCE_TYPE_BANK_IN + "') " +
				"            ) " +
				"        ) " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) <= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				"   AND hcash.handling_name = '" + HANDLING_TYPE_CASH + "' " +
				" GROUP BY hd_id, hd_handling_name, hd_zero_hidden " +
				"";
		
		//現金以外取得用SQL
		String sqlNotCash = "" +
				" SELECT " +
				sqlNotCashDaily +		//日付毎の合計取得部分(現金以外)
				"  ,CASE " +
				"     WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN " +
				"       hb.handling_name " +
				"     ELSE " +
				"       h.handling_name " +
				"     END as hd_handling_name " +
				"  ,CASE " +
				"     WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN " +
				"       hb.id " +
				"     ELSE " +
				"       h.id " +
				"   END as hd_id " +
				"  ,CASE " +
				"     WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN " +
				"       hb.zero_hidden " +
				"     ELSE " +
				"       h.zero_hidden " +
				"   END as hd_zero_hidden " +
				sqlFromPhrase +			//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "','" + BALANCE_TYPE_BANK_OUT + "','" + BALANCE_TYPE_BANK_IN + "') " +
				"   AND h.handling_name <> '" + HANDLING_TYPE_CASH + "' " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) <= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				" GROUP BY hd_id, hd_handling_name, hd_zero_hidden " +
				"";
		
		
		//現金と現金以外を合わせて取得用SQL
		String sqlFirstDay = "" +
				" ( " + sqlCash + " ) " +
				"  UNION ALL " +
				" ( " + sqlNotCash + " ) " +
				"";
		
		while(!(sqlFirstDay.equals(sqlFirstDay.replaceAll("  ", " "))))
			sqlFirstDay = sqlFirstDay.replaceAll("  ", " ");
		
		return sqlFirstDay;
	}
	
	/**
	 * 2日目以降の残高取得用SQL作成(実残高)(取扱毎)
	 * @param bolEach
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumAllCaseInOut
	 * @param sqlSumCashCaseBankInOut
	 * @param sqlSumNotCashCaseBankInOut
	 * @return
	 */
	private String makeSqlRemRealEachLater(
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst,
			String sqlFromPhrase,
			String sqlSumAllCaseInOut,
			String sqlSumCashCaseBankInOut,
			String sqlSumNotCashCaseBankInOut
			) {
		
		Calendar calendar = Calendar.getInstance();
		
		String sqlCashDaily = "";
		String sqlNotCashDaily = "";
		
		//日付毎の合計取得部分のSQL(現金)
		calendar.setTime(dStartDay);
		for(int iDay = 2; iDay <= iDaysCnt; iDay++) {
			calendar.add(Calendar.DATE, 1);
			sqlCashDaily += "" +
					(iDay==2 ? " " : ",") +
					" COALESCE(SUM(" +
					"   CASE " +
					"     WHEN cast(r.payment_date as date) = to_date('" + String.format("%1$tY%1$tm%1$td", calendar.getTime()) + "', 'YYYYMMDD') THEN " +
					"       CASE " +
					sqlSumAllCaseInOut +		//収入加算・支出減算
					sqlSumCashCaseBankInOut +	//口座引出加算・口座預入減算
					"       END " +
					"     ELSE 0 " +
					"   END" +
					" ), 0) as sum_day_" + iDay + " ";
		}
		
		//日付毎の合計取得部分のSQL(現金以外)
		calendar.setTime(dStartDay);
		for(int iDay = 2; iDay <= iDaysCnt; iDay++) {
			calendar.add(Calendar.DATE, 1);
			sqlNotCashDaily += "" +
					(iDay==2 ? " " : ",") +
					" COALESCE(SUM(" +
					"   CASE " +
					"     WHEN cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) = to_date('" + String.format("%1$tY%1$tm%1$td", calendar.getTime()) + "', 'YYYYMMDD') THEN " +
					"       CASE " +
					sqlSumAllCaseInOut +			//収入加算・支出減算
					sqlSumNotCashCaseBankInOut +	//口座引出減算・口座預入加算
					"       END " +
					"     ELSE 0 " +
					"   END" +
					" ), 0) as sum_day_" + iDay + " ";
		}
		

		//現金取得用SQL
		String sqlCash = "" +
				" SELECT " +
				sqlCashDaily +		//日付毎の合計取得部分(現金)
				"  ,hcash.handling_name as hd_handling_name " +
				"  ,hcash.id as hd_id" +
				"  ,hcash.zero_hidden as hd_zero_hidden " +
				sqlFromPhrase +		//FROM句
				" CROSS JOIN HandlingMst hcash " +
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND (   (    ht.handling_type_name = '" + HANDLING_TYPE_CASH + "' " +
				"            AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"            ) " +
				"        OR (    ht.handling_type_name in('" + HANDLING_TYPE_BANK + "','" + HANDLING_TYPE_EMONEY + "') " +
				"            AND b.balance_type_name in('" + BALANCE_TYPE_BANK_OUT + "','" + BALANCE_TYPE_BANK_IN + "') " +
				"            ) " +
				"        ) " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) >= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) < to_date('" + sNextFirst + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				"   AND hcash.handling_name = '" + HANDLING_TYPE_CASH + "' " +
				" GROUP BY hd_id, hd_handling_name, hd_zero_hidden " +
				"";
		
		//現金以外取得用SQL
		String sqlNotCash = "" +
				" SELECT " +
				sqlNotCashDaily +		//日付毎の合計取得部分(現金以外)
				"  ,CASE " +
				"     WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN " +
				"       hb.handling_name " +
				"     ELSE " +
				"       h.handling_name " +
				"     END as hd_handling_name " +
				"  ,CASE " +
				"     WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN " +
				"       hb.id " +
				"     ELSE " +
				"       h.id " +
				"   END as hd_id " +
				"  ,CASE " +
				"     WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN " +
				"       hb.zero_hidden " +
				"     ELSE " +
				"       h.zero_hidden " +
				"   END as hd_zero_hidden " +
				sqlFromPhrase +			//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "','" + BALANCE_TYPE_BANK_OUT + "','" + BALANCE_TYPE_BANK_IN + "') " +
				"   AND h.handling_name <> '" + HANDLING_TYPE_CASH + "' " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) < to_date('" + sNextFirst + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				" GROUP BY hd_id, hd_handling_name, hd_zero_hidden " +
				"";

		//現金と現金以外を合わせて取得用SQL
		String sqlLater = "" +
				" ( " + sqlCash + " ) " +
				"  UNION ALL " +
				" ( " + sqlNotCash + " ) " +
				"";
		
		while(!(sqlLater.equals(sqlLater.replaceAll("  ", " "))))
			sqlLater = sqlLater.replaceAll("  ", " ");
		
		return sqlLater;
	}
	
	/**
	 * 残高取得用SQL作成(My貯金残高)
	 * @param bolEach
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumCaseIdealDepoInOut
	 * @return
	 */
	private String makeSqlRemIdealDeposit(
			boolean bolEach,
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst
			) {
		
		String sql = "";

   		/** 
   		 * SQL固定部分作成
   		 */
		
		//  FROM句
		String sqlFromPhrase = "" +
   				" FROM Record r " +
				" LEFT JOIN ItemMst i " +
				"   ON r.item_mst_id = i.id " +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON r.balance_type_mst_id = b.id " +
				" LEFT JOIN IdealDepositMst id " +
				"   ON r.ideal_deposit_mst_id = id.id " +
				" LEFT JOIN HandlingMst h " +
				"   ON r.handling_mst_id = h.id " +
				" LEFT JOIN HandlingTypeMst ht " +
				"   ON h.handling_type_mst_id = ht.id" +
				"";
		
		/* CASE文内の加減算の条件 */
		// My貯金から直接支払：減算、My貯金に直接入金：加算、My貯金預入：加算、My貯金引出：減算
		String sqlSumCaseIdealDepoInOut = "" +
   				" WHEN (    b.balance_type_name = '" + BALANCE_TYPE_OUT + "' " +
   				"       AND r.ideal_deposit_mst_id IS NOT NULL " +
   				"       ) THEN -r.amount " +
   				" WHEN (    b.balance_type_name = '" + BALANCE_TYPE_IN + "' " +
   				"       AND r.ideal_deposit_mst_id IS NOT NULL " +
   				"       ) THEN r.amount " +
   				" WHEN b.balance_type_name = '" + BALANCE_TYPE_IDEAL_DEPOSIT_IN + "' THEN r.amount" +
   				" WHEN b.balance_type_name = '" + BALANCE_TYPE_IDEAL_DEPOSIT_OUT + "' THEN -r.amount" +
				"";

		if(!bolEach) {
			//初日の残高取得用SQL作成（My貯金合計）
			String sqlAllFirstDay = makeSqlRemIdealFirstDay(
					false,
					haUser,
					sFirstDay,
					sqlFromPhrase,
					sqlSumCaseIdealDepoInOut
					);
			sql = "" +
					" SELECT " +
					"   *" +
					" FROM ( SELECT 0 as item_id, 0 as item_order, cast('' as character varying(255)) as item_name, 70 as cate_order" +
					"  ,cast('" + REMAINDER_TYPE_IDEAL_DEPOSIT + "' as character varying(255)) as cate_name " +
					"  ,0 as bg_id " +
					"  ,0 as bg_amount " +
					" ) rem_item " +
					" CROSS JOIN ( " + sqlAllFirstDay + " ) rem_firstday " +
					"";
			//2日目以降の残高取得用SQL作成（My貯金合計）
			if(iDaysCnt>=2) {
				String sqlAllLater = makeSqlRemIdealLater(
						false,
						dStartDay,
						iDaysCnt,
						haUser,
						sFirstDay,
						sNextFirst,
						sqlFromPhrase,
						sqlSumCaseIdealDepoInOut
						);
				sql += " CROSS JOIN (" + sqlAllLater + " ) rem_later ";
			}
			while(!(sql.equals(sql.replaceAll("  ", " "))))
				sql = sql.replaceAll("  ", " ");
			
			return sql;
		}

		
		
		
		String sqlDailyLater = "";
		String sqlDailyZero = "";
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dStartDay);
		for(int iDay = 1; iDay <= iDaysCnt; iDay++) {
			sqlDailyZero += "" +
					" OR  sum_day_" + iDay + " <> 0 ";
			calendar.add(Calendar.DATE, 1);
			if(iDay>=2)
				sqlDailyLater += "" +
						" ,COALESCE(rem_later.sum_day_" + iDay + ", 0) as sum_day_" + iDay + " ";
		}
		
		//初日の残高取得用SQL作成（取扱(My貯金)毎）
		String sqlEachFirstDay = makeSqlRemIdealFirstDay(
				true,
				haUser,
				sFirstDay,
				sqlFromPhrase,
				sqlSumCaseIdealDepoInOut
				);
		//2日目以降の残高取得用SQL作成（取扱(My貯金)毎）
		String sqlJoinPhrase = "";
		String sqlEachLater = "";
		if(iDaysCnt>=2) {
			sqlEachLater = makeSqlRemIdealLater(
					true,
					dStartDay,
					iDaysCnt,
					haUser,
					sFirstDay,
					sNextFirst,
					sqlFromPhrase,
					sqlSumCaseIdealDepoInOut
					);
			sqlJoinPhrase = "" +
					" LEFT JOIN (" + sqlEachLater + " ) rem_later " +
					"   ON rem_firstday.id_id = rem_later.id_id " +
					"";
		}
		sql = "" +
				" SELECT " +
				"   idm.id as item_id " +
				"  ,idm.order_seq as item_order " +
				"  ,idm.ideal_deposit_name as item_name " +
				"  ,70 as cate_order " +
				"  ,cast('" + REMAINDER_TYPE_IDEAL_DEPOSIT + "' as character varying(255)) as cate_name " +
				"  ,0 as bg_id " +
				"  ,0 as bg_amount " +
				"  ,COALESCE(rem_firstday.sum_day_1, 0) as sum_day_1 " + sqlDailyLater +
				" FROM IdealDepositMst idm " +
				" LEFT JOIN ( " + sqlEachFirstDay + " ) rem_firstday " +
				"   ON idm.id = rem_firstday.id_id" +
				sqlJoinPhrase +
				" WHERE idm.ha_user_id = " + haUser.id +
				"   AND (idm.zero_hidden = false " + sqlDailyZero + ") " +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 初日の残高取得用SQL作成(My貯金残高)
	 * @param bolEach
	 * @param haUser
	 * @param sFirstDay
	 * @param sqlFromPhrase
	 * @param sqlSumCaseIdealDepoInOut
	 * @return
	 */
	private String makeSqlRemIdealFirstDay(
			boolean bolEach,
			HaUser haUser,
			String sFirstDay,
			String sqlFromPhrase,
			String sqlSumCaseIdealDepoInOut
			) {
		
		//日付毎の合計取得部分のSQL
		String sqlDaily = "" +
				" COALESCE(SUM(" +
				"   CASE " +
				sqlSumCaseIdealDepoInOut +		//My貯金から直接支払減算・My貯金に直接入金加算・My貯金預入加算・My貯金引出減算
				"   END " +
				" ), 0) as sum_day_1";
		
		//SQL
		String sqlFirstDay = "" +
				" SELECT " +
   				(bolEach ?
   				"   id.ideal_deposit_name as id_ideal_deposit_name " +
   				"  ,id.id as id_id " +
   				"  ,id.zero_hidden as id_zero_hidden , "
   				: "") +
				sqlDaily +		//日付毎の合計取得部分
				sqlFromPhrase +	//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND (   (    b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"            AND r.ideal_deposit_mst_id IS NOT NULL " +
				"            ) " +
   				"        OR b.balance_type_name in('" + BALANCE_TYPE_IDEAL_DEPOSIT_IN + "','" + BALANCE_TYPE_IDEAL_DEPOSIT_OUT + "') " +
   				"        ) " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) <= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
   				(bolEach ?
				" GROUP BY id.id, id.ideal_deposit_name, id.zero_hidden "
   				: "") +
				"";

		while(!(sqlFirstDay.equals(sqlFirstDay.replaceAll("  ", " "))))
			sqlFirstDay = sqlFirstDay.replaceAll("  ", " ");
		
		return sqlFirstDay;
	}
	
	/**
	 * 2日目以降の残高取得用SQL作成(My貯金残高)
	 * @param bolEach
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumCaseIdealDepoInOut
	 * @return
	 */
	private String makeSqlRemIdealLater(
			boolean bolEach,
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst,
			String sqlFromPhrase,
			String sqlSumCaseIdealDepoInOut
			) {
		
		Calendar calendar = Calendar.getInstance();
		
		String sqlDaily = "";
		
		//日付毎の合計取得部分のSQL
		calendar.setTime(dStartDay);
		for(int iDay = 2; iDay <= iDaysCnt; iDay++) {
			calendar.add(Calendar.DATE, 1);
			sqlDaily += "" +
					(iDay==2 ? " " : ",") +
					" COALESCE(SUM(" +
					"   CASE " +
					"     WHEN cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) = to_date('" + String.format("%1$tY%1$tm%1$td", calendar.getTime()) + "', 'YYYYMMDD') THEN " +
					"       CASE " +
					sqlSumCaseIdealDepoInOut +		//My貯金から直接支払減算・My貯金に直接入金加算・My貯金預入加算・My貯金引出減算
					"       END " +
					"     ELSE 0 " +
					"   END" +
					" ), 0) as sum_day_" + iDay + " ";
		}
		
		//SQL
		String sqlLater = "" +
				" SELECT " +
   				(bolEach ?
   				"   id.ideal_deposit_name as id_ideal_deposit_name " +
   				"  ,id.id as id_id " +
   				"  ,id.zero_hidden as id_zero_hidden , "
   				: "") +
				sqlDaily +		//日付毎の合計取得部分
				sqlFromPhrase +		//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND (   (    b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"            AND r.ideal_deposit_mst_id IS NOT NULL " +
				"            ) " +
   				"        OR b.balance_type_name in('" + BALANCE_TYPE_IDEAL_DEPOSIT_IN + "','" + BALANCE_TYPE_IDEAL_DEPOSIT_OUT + "') " +
   				"        ) " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) >= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) < to_date('" + sNextFirst + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
   				(bolEach ?
				" GROUP BY id.id, id.ideal_deposit_name, id.zero_hidden "
   				: "") +
				"";
		
		while(!(sqlLater.equals(sqlLater.replaceAll("  ", " "))))
			sqlLater = sqlLater.replaceAll("  ", " ");
		
		return sqlLater;
	}
	
	/**
	 * 残高取得用SQL作成(My貯金してないお金)
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumCaseNotIdealInOut
	 * @return
	 */
	private String makeSqlRemNotIdealDeposit(
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst
			) {
		
		String sql = "";
		
		/** 
		 * SQL固定部分作成
		 */
		
		//  FROM句
		String sqlFromPhrase = "" +
				" FROM Record r " +
				" LEFT JOIN ItemMst i " +
				"   ON r.item_mst_id = i.id " +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON r.balance_type_mst_id = b.id " +
				" LEFT JOIN IdealDepositMst id " +
				"   ON r.ideal_deposit_mst_id = id.id " +
				" LEFT JOIN HandlingMst h " +
				"   ON r.handling_mst_id = h.id " +
				" LEFT JOIN HandlingTypeMst ht " +
				"   ON h.handling_type_mst_id = ht.id" +
				"";
		
		/* CASE文内の加減算の条件 */
		// 収入でMy貯金未選択：加算、支出でMy貯金未選択：減算、My貯金引出：加算、My貯金預入：減算
		String sqlSumCaseNotIdealInOut = "" +
				" WHEN (    b.balance_type_name = '" + BALANCE_TYPE_IN + "' " +
				"       AND r.ideal_deposit_mst_id IS NULL " +
				"       ) THEN r.amount " +
				" WHEN (    b.balance_type_name = '" + BALANCE_TYPE_OUT + "' " +
				"       AND r.ideal_deposit_mst_id IS NULL " +
				"       ) THEN -r.amount " +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_IDEAL_DEPOSIT_OUT + "' THEN r.amount" +
				" WHEN b.balance_type_name = '" + BALANCE_TYPE_IDEAL_DEPOSIT_IN + "' THEN -r.amount" +
				"";
		
		//初日の残高取得用SQL作成（My貯金してないお金合計）
		String sqlAllFirstDay = makeSqlRemNotIdealFirstDay(
				haUser,
				sFirstDay,
				sqlFromPhrase,
				sqlSumCaseNotIdealInOut
				);
		sql = "" +
				" SELECT " +
				"   *" +
				" FROM ( SELECT 0 as item_id, 0 as item_order, cast('' as character varying(255)) as item_name, 60 as cate_order " +
				"  ,cast('" + REMAINDER_TYPE_NOT_IDEAL_DEPOSIT + "' as character varying(255)) as cate_name " +
				"  ,0 as bg_id " +
				"  ,0 as bg_amount " +
				" ) rem_item " +
				" CROSS JOIN ( " + sqlAllFirstDay + " ) rem_firstday " +
				"";
		//2日目以降の残高取得用SQL作成（My貯金してないお金合計）
		if(iDaysCnt>=2) {
			String sqlAllLater = makeSqlRemNotIdealLater(
					dStartDay,
					iDaysCnt,
					haUser,
					sFirstDay,
					sNextFirst,
					sqlFromPhrase,
					sqlSumCaseNotIdealInOut
					);
			sql += " CROSS JOIN (" + sqlAllLater + " ) rem_later ";
		}
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 初日の残高取得用SQL作成(My貯金してないお金)
	 * @param haUser
	 * @param sFirstDay
	 * @param sqlFromPhrase
	 * @param sqlSumCaseNotIdealInOut
	 * @return
	 */
	private String makeSqlRemNotIdealFirstDay(
			HaUser haUser,
			String sFirstDay,
			String sqlFromPhrase,
			String sqlSumCaseNotIdealInOut
			) {
		
		//日付毎の合計取得部分のSQL
		String sqlDaily = "" +
				" COALESCE(SUM(" +
				"   CASE " +
				sqlSumCaseNotIdealInOut +		//収入でMy貯金未選択：加算、支出でMy貯金未選択：減算、My貯金引出：加算、My貯金預入：減算
				"   END " +
				" ), 0) as sum_day_1";
		
		//SQL
		String sqlFirstDay = "" +
				" SELECT " +
				sqlDaily +		//日付毎の合計取得部分
				sqlFromPhrase +	//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND (   (    b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"            AND r.ideal_deposit_mst_id IS NULL " +
				"            ) " +
   				"        OR b.balance_type_name in('" + BALANCE_TYPE_IDEAL_DEPOSIT_IN + "','" + BALANCE_TYPE_IDEAL_DEPOSIT_OUT + "') " +
   				"        ) " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) <= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				"";

		while(!(sqlFirstDay.equals(sqlFirstDay.replaceAll("  ", " "))))
			sqlFirstDay = sqlFirstDay.replaceAll("  ", " ");
		
		return sqlFirstDay;
	}
	
	/**
	 * 2日目以降の残高取得用SQL作成(My貯金残高)
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @param sqlFromPhrase
	 * @param sqlSumCaseNotIdealInOut
	 * @return
	 */
	private String makeSqlRemNotIdealLater(
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst,
			String sqlFromPhrase,
			String sqlSumCaseNotIdealInOut
			) {
		
		Calendar calendar = Calendar.getInstance();
		
		String sqlDaily = "";
		
		//日付毎の合計取得部分のSQL
		calendar.setTime(dStartDay);
		for(int iDay = 2; iDay <= iDaysCnt; iDay++) {
			calendar.add(Calendar.DATE, 1);
			sqlDaily += "" +
					(iDay==2 ? " " : ",") +
					" COALESCE(SUM(" +
					"   CASE " +
					"     WHEN cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) = to_date('" + String.format("%1$tY%1$tm%1$td", calendar.getTime()) + "', 'YYYYMMDD') THEN " +
					"       CASE " +
					sqlSumCaseNotIdealInOut +		//My貯金から直接支払減算・My貯金に直接入金加算・My貯金預入加算・My貯金引出減算
					"       END " +
					"     ELSE 0 " +
					"   END" +
					" ), 0) as sum_day_" + iDay + " ";
		}
		
		//SQL
		String sqlLater = "" +
				" SELECT " +
				sqlDaily +		//日付毎の合計取得部分
				sqlFromPhrase +		//FROM句
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND (   (    b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "') " +
				"            AND r.ideal_deposit_mst_id IS NULL " +
				"            ) " +
   				"        OR b.balance_type_name in('" + BALANCE_TYPE_IDEAL_DEPOSIT_IN + "','" + BALANCE_TYPE_IDEAL_DEPOSIT_OUT + "') " +
   				"        ) " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) >= to_date('" + sFirstDay + "', 'YYYYMMDD') " +
				"   AND cast((CASE WHEN r.debit_date IS NULL THEN r.payment_date ELSE r.debit_date END) as date) < to_date('" + sNextFirst + "', 'YYYYMMDD') " +
				((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				"";
		
		while(!(sqlLater.equals(sqlLater.replaceAll("  ", " "))))
			sqlLater = sqlLater.replaceAll("  ", " ");
		
		return sqlLater;
	}
	
	/**
	 * 日毎合計取得用SQL作成
	 * @param dStartDay
	 * @param iDaysCnt
	 * @return
	 */
	private String makeSqlBalDaily(
			Date dStartDay,
			int iDaysCnt
			) {
   		String sqlDaily = "";
   		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dStartDay);
		for(int iDay = 1; iDay <= iDaysCnt; iDay++) {
			sqlDaily += "" +
					",COALESCE(SUM(" +
					"  CASE " +
					"    WHEN cast(r.payment_date as date) = to_date('" + String.format("%1$tY%1$tm%1$td", calendar.getTime()) + "', 'YYYYMMDD') THEN r.amount " +
					"    ELSE 0 " +
					"  END" +
					"  ), 0) as sum_day_" + iDay + "";
			calendar.add(Calendar.DATE, 1);
		}
		return sqlDaily;
	}
	
	/**
	 * 日毎合計取得用SQL作成(合計行)
	 * @param dStartDay
	 * @param iDaysCnt
	 * @return
	 */
	private String makeSqlBalDailyAll(
			Date dStartDay,
			int iDaysCnt
			) {
   		String sqlDaily = "";
   		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dStartDay);
		for(int iDay = 1; iDay <= iDaysCnt; iDay++) {
			sqlDaily += ",sum_day_" + iDay + "";
			calendar.add(Calendar.DATE, 1);
		}
		return sqlDaily;
	}
	
	/**
	 * 収支取得用SQL作成(収入・支出)
	 * @param bolEach
	 * @param sLargeCategoryName
	 * @param year
	 * @param month
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @return
	 */
	private String makeSqlBalInOut(
			boolean bolEach,
			String sLargeCategoryName,	// 大分類行の名称「収入」・「支出」
			Integer year,
			Integer month,
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst
			) {
		
		String sqlDaily = makeSqlBalDaily(dStartDay, iDaysCnt);
		String sqlDailyAll = bolEach ? "" : makeSqlBalDailyAll(dStartDay, iDaysCnt);
		
		String sqlSelGroupby = "" +
   				"   0 as item_id, 0 as item_order" +
   				"  ,cast('' as character varying(255)) as item_name" +
   				"  ," + (sLargeCategoryName.equals(BALANCE_TYPE_IN) ? 10 : 20) + " as cate_order " +
				"  ,cast('" + sLargeCategoryName + "' as character varying(255)) as cate_name " +
   				"  ,0 as bg_id " +
				"";
		String sqlGroupby = "" +
				" ) a " +
				" CROSS JOIN " +
				" (" +
				" SELECT " +
				"   COALESCE(SUM(bg.amount), NULL) as bg_amount " +
				" FROM Budget bg " +
				" LEFT JOIN ItemMst i " +
				"   ON i.id = bg.item_mst_id" +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON i.balance_type_mst_id = b.id " +
				" WHERE b.balance_type_name = '" + sLargeCategoryName + "' " +
				"   AND bg.ha_user_id = " + haUser.id +
				"   AND bg.year = " + year +
				"   AND bg.month = " + month +
				" ) b ) " +
				"";
		if(bolEach) {
			sqlSelGroupby = "" +
	   				"   i.id as item_id " +
	   				"  ,i.order_seq as item_order" +
	   				"  ,i.item_name as item_name" +
	   				"  ," + (sLargeCategoryName.equals(BALANCE_TYPE_IN) ? 10 : 20) + " as cate_order " +
					"  ,cast('" + sLargeCategoryName + "' as character varying(255)) as cate_name " +
	   				"  ,bg.id as bg_id " +
	   				"  ,bg.amount as bg_amount " +
					"";
			sqlGroupby = "" +
					" GROUP BY item_id, item_order, item_name, bg_id, bg_amount " +
					"";
		}
		
		String sql = "" +
				(!bolEach ? 
				" SELECT " +
				"   item_id, " +
				"   item_order, " +
				"   item_name, " +
				"   cate_order, " +
				"   cate_name, " +
				"   bg_id, " +
				"   bg_amount " +
				sqlDailyAll +
				"  ,sum_month " +
				" FROM ( ( "
						: "") +
   				" SELECT " +
   				sqlSelGroupby +
   				sqlDaily +
   				"  ,COALESCE(SUM(r.amount), 0) as sum_month" +
   				" FROM ItemMst i " +
				" LEFT JOIN Record r " +
				"   ON r.item_mst_id = i.id " +
				"   AND r.ha_user_id = " + haUser.id +
				"   AND cast(r.payment_date as date) >= to_date('" + sFirstDay + "', 'YYYYMMDD')" +
				"   AND cast(r.payment_date as date) < to_date('" + sNextFirst + "', 'YYYYMMDD')" +
				"   AND r.ideal_deposit_mst_id IS NULL "
				 + ((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON i.balance_type_mst_id = b.id " +
				" LEFT JOIN IdealDepositMst id " +
				"   ON r.ideal_deposit_mst_id = id.id " +
				(bolEach ?
				" LEFT JOIN Budget bg " +
				"   ON i.id = bg.item_mst_id" +
				"   AND bg.ha_user_id = " + haUser.id +
				"   AND bg.year = " + year +
				"   AND bg.month = " + month 
				: "") +
				" WHERE i.ha_user_id = " + haUser.id +
				"   AND b.balance_type_name = '" + sLargeCategoryName + "' " +
				sqlGroupby +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 収支取得用SQL作成(My貯金預入・My貯金から支払)
	 * @param bolEach
	 * @param sLargeCategoryName
	 * @param year
	 * @param month
	 * @param dStartDay
	 * @param iDaysCnt
	 * @param haUser
	 * @param sFirstDay
	 * @param sNextFirst
	 * @return
	 */
	private String makeSqlBalIdeal(
			boolean bolEach,
			String sLargeCategoryName,	// 大分類行の名称「My貯金預入」・「My貯金から支払」
			Integer year,
			Integer month,
			Date dStartDay,
			int iDaysCnt,
			HaUser haUser,
			String sFirstDay,
			String sNextFirst
			) {
		
		String sqlDaily = makeSqlBalDaily(dStartDay, iDaysCnt);
		String sqlDailyAll = bolEach ? "" : makeSqlBalDailyAll(dStartDay, iDaysCnt);
		
		String sqlSelGroupby = "" +
   				"   0 as item_id, 0 as item_order " +
   				"  ,cast('' as character varying(255)) as item_name" +
   				"  ," + (sLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN) ? 30 : 40) + " as cate_order " +
				"  ,cast('" + sLargeCategoryName + "' as character varying(255)) as cate_name " +
   				"  ,0 as bg_id " +
				"";
		String sqlGroupby = "" +
				" ) a " +
				" CROSS JOIN " +
				" (" +
				" SELECT " +
				"   COALESCE(SUM(bg.amount), NULL) as bg_amount " +
				" FROM Budget bg " +
				" WHERE bg.ideal_deposit_mst_id IS NOT NULL " +
				"   AND bg.ha_user_id = " + haUser.id +
				"   AND bg.year = " + year +
				"   AND bg.month = " + month +
				" ) b ) " +
				"";
		if(bolEach) {
			sqlSelGroupby = "" +
	   				"   id.id as item_id " +
	   				"  ,id.order_seq as item_order" +
	   				"  ,id.ideal_deposit_name as item_name" +
	   				"  ," + (sLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN) ? 30 : 40) + " as cate_order " +
					"  ,cast('" + sLargeCategoryName + "' as character varying(255)) as cate_name " +
	   				"  ,bg.id as bg_id " +
	   				"  ,bg.amount as bg_amount " +
					"";
			sqlGroupby = "" +
					" GROUP BY item_id, item_order, item_name, bg_id, bg_amount " +
					"";
		}
		
		String sqlBalanceTypeName = "";
		if(sLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN))
			sqlBalanceTypeName = BALANCE_TYPE_IDEAL_DEPOSIT_IN;
		if(sLargeCategoryName.equals(BALANCE_TYPE_OUT_IDEAL_DEPOSIT))
			sqlBalanceTypeName = BALANCE_TYPE_OUT;
		
		String sql = "" +
				(!bolEach ? 
				" SELECT " +
				"   item_id, " +
				"   item_order, " +
				"   item_name, " +
				"   cate_order, " +
				"   cate_name, " +
				"   bg_id, " +
				"   bg_amount " +
   				sqlDailyAll +
				"  ,sum_month " +
				" FROM ( ( "
						: "") +
   				" SELECT " +
   				sqlSelGroupby +
   				sqlDaily +
   				"  ,COALESCE(SUM(r.amount), 0) as sum_month" +
   				" FROM Record r " +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON r.balance_type_mst_id = b.id " +
				" LEFT JOIN IdealDepositMst id " +
				"   ON r.ideal_deposit_mst_id = id.id " +
				" LEFT JOIN Budget bg " +
				"   ON id.id = bg.ideal_deposit_mst_id" +
				"   AND bg.ha_user_id = " + haUser.id +
				"   AND bg.year = " + year +
				"   AND bg.month = " + month +
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND cast(r.payment_date as date) >= to_date('" + sFirstDay + "', 'YYYYMMDD')" +
				"   AND cast(r.payment_date as date) < to_date('" + sNextFirst + "', 'YYYYMMDD')" +
				"   AND b.balance_type_name = '" + sqlBalanceTypeName + "' " +
				"   AND r.ideal_deposit_mst_id IS NOT NULL "
				 + ((session.get("actionMode")).equals("View") ? " AND r.secret_rec_flg = FALSE " : "") +
				sqlGroupby +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 日計表から明細表へのリンクのための引数をセット
	 * @param calendar
	 * @param lngEach
	 * @param lngItemId
	 * @return
	 */
	private WkDaToDl makeWkDaToDl(
			Calendar calendar,
			String strLargeCategoryName,
			long lngEach,
			Long lngItemId
			) {
		
		WkDaToDl wkDaToDl = new WkDaToDl();
		wkDaToDl.setlAmount(lngEach);
		String sDate = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
		wkDaToDl.setsPaymentDateFr(sDate);
		wkDaToDl.setsPaymentDateTo(sDate);
		wkDaToDl.setlHandlingId(null);

		
		/** 合計行の場合 **/
		if(lngItemId==0) {
			// 「収入」・「支出」・「My貯金預入」
			if(strLargeCategoryName.equals(BALANCE_TYPE_IN) ||
					strLargeCategoryName.equals(BALANCE_TYPE_OUT) ||
					strLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN)) {
				wkDaToDl.setlBalanceTypeId(((BalanceTypeMst)(BalanceTypeMst.find("byBalance_type_name", strLargeCategoryName)).first()).id);
			}
			//「収入」・「支出」の時は、My貯金＝NULL
			if(strLargeCategoryName.equals(BALANCE_TYPE_IN) ||
					strLargeCategoryName.equals(BALANCE_TYPE_OUT)) {
				wkDaToDl.setlIdealDepositId((long) -1);
			}
			// 「My貯金預入」
			if(strLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN)) {
				wkDaToDl.setlBalanceTypeId(((BalanceTypeMst)(BalanceTypeMst.find("byBalance_type_name", strLargeCategoryName)).first()).id);
				wkDaToDl.setlIdealDepositId((long) -2);		//My貯金＝NULLでない
			}
			// 「My貯金から支払」
			if(strLargeCategoryName.equals(BALANCE_TYPE_OUT_IDEAL_DEPOSIT)) {
				wkDaToDl.setlBalanceTypeId(((BalanceTypeMst)(BalanceTypeMst.find("byBalance_type_name", BALANCE_TYPE_OUT)).first()).id);
				wkDaToDl.setlIdealDepositId((long) -2);		//My貯金＝NULLでない
			}
			
			return wkDaToDl;
		}
		
		
		/** 明細行の場合 **/
		
		// 「収入」・「支出」
		if(strLargeCategoryName.equals(BALANCE_TYPE_IN) ||
				strLargeCategoryName.equals(BALANCE_TYPE_OUT)) {
			wkDaToDl.setlBalanceTypeId(((BalanceTypeMst)(BalanceTypeMst.find("byBalance_type_name", strLargeCategoryName)).first()).id);
			//取扱(My貯金)＝NULL
			wkDaToDl.setlIdealDepositId((long) -1);
//			workDaToDl.setiItemId(((ItemMst)(ItemMst.find("byItem_name", itemMst.item_name)).first()).id);
			wkDaToDl.setiItemId(lngItemId);
		}
		// 「My貯金預入」
		if(strLargeCategoryName.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN)) {
			wkDaToDl.setlBalanceTypeId(((BalanceTypeMst)(BalanceTypeMst.find("byBalance_type_name", strLargeCategoryName)).first()).id);
			wkDaToDl.setlIdealDepositId(lngItemId);
		}
		// 「My貯金から支払」
		if(strLargeCategoryName.equals(BALANCE_TYPE_OUT_IDEAL_DEPOSIT)) {
			wkDaToDl.setlBalanceTypeId(((BalanceTypeMst)(BalanceTypeMst.find("byBalance_type_name", BALANCE_TYPE_OUT)).first()).id);
			wkDaToDl.setlIdealDepositId(lngItemId);
		}
		// 「実残高」・「My貯金」
		if(strLargeCategoryName.equals(REMAINDER_TYPE_REAL) ||
				strLargeCategoryName.equals(REMAINDER_TYPE_IDEAL_DEPOSIT)) {
			calendar.add(Calendar.MONTH, -1);
			String sDateFr = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
			wkDaToDl.setsDebitDateFr(sDateFr);
			calendar.add(Calendar.MONTH, 7);
			String sDateTo = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
			wkDaToDl.setsDebitDateTo(sDateTo);
			// 「実残高」
			if(strLargeCategoryName.equals(REMAINDER_TYPE_REAL)) {
				wkDaToDl.setlHandlingId(lngItemId);
			}
			// 「My貯金」
			if(strLargeCategoryName.equals(REMAINDER_TYPE_IDEAL_DEPOSIT)) {
				wkDaToDl.setlIdealDepositId(lngItemId);
			}
		}
		
		return wkDaToDl;
	}
	
	
	/**
	 * 単純に呼ばれた時の基準日のセット
	 * @param calendar
	 * @return
	 */
	private String setBasisDate() {
		//セッションに絞込条件が入っている時はセット
		if((session.get("daFilExistFlg") != null) &&
				(session.get("daFilExistFlg").equals("true")))
			return session.get("daStrBasisDate");
   		Calendar calendar = Calendar.getInstance();
//		//単純に呼ばれた時（初回等）は、今月１日をセット
//		return  String.format("%1$tY/%1$tm/01", calendar.getTime());
		//単純に呼ばれた時（初回等）は、本日をセット
		return  String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
	}
	
	/**
	 * 単純に呼ばれた時の基準日のセット(残高表)
	 * @param calendar
	 * @return
	 */
	private String setBasisDateBt() {
		//セッションに絞込条件が入っている時はセット
		if((session.get("btFilExistFlg") != null) &&
				(session.get("btFilExistFlg").equals("true")))
			return session.get("btStrBasisDate");
   		//単純に呼ばれた時（初回等）は、今日をセット
   		Calendar calendar = Calendar.getInstance();
		return  String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
	}
	
	/**
	 * 予算更新
	 * @param bg_basis_date
	 * @param e_budget_id
	 * @param e_budget_amount
	 */
	public static void updBudget(
			String bg_basis_date,
    		List<Long> e_budget_id,			/* 変更行のID */
    		List<String> e_large_category,	/* 変更行の大分類行の名称「収入」・「支出」・「My貯金預入」 */
    		List<String> e_item,			/* 変更行の項目 */
    		List<String> e_budget_amount	/* 変更行の金額 */
			) {
		
		Iterator<String> sELargeCategory = e_large_category.iterator();
		Iterator<String> sEItem = e_item.iterator();
   		Iterator<String> sEBudgetAmount = e_budget_amount.iterator();
		for (Long lngId : e_budget_id) {
			String sEBudgetAmountVal = sEBudgetAmount.next(); 
			String sELargeCategoryVal = sELargeCategory.next();
			String sEItemVal = sEItem.next();

			//予算が空白にされた時
			if(sEBudgetAmountVal.equals("")) {
				//既存レコードが無ければなにもしない
				if(lngId==0L)
					continue;
				//既存レコードがある場合レコード削除
				Budget budget = Budget.findById(lngId);
				budget.delete();
				
				continue;
			}
			//予算が入力された時
			//予算更新実処理
			actUpdBudget(
					bg_basis_date,
					lngId,
					sELargeCategoryVal,
					sEItemVal,
					sEBudgetAmountVal
					);
		}
		
    	dailyAccount(bg_basis_date);
	}
	
	/**
	 * 予算更新実処理
	 * @param bg_basis_date
	 * @param lngId
	 * @param sELargeCategoryVal
	 * @param sEItemVal
	 * @param sEBudgetAmountVal
	 */
	private static void actUpdBudget(
			String bg_basis_date,
			Long lngId,
			String sELargeCategoryVal,
			String sEItemVal,
			String sEBudgetAmountVal
			) {
		//カンマ区切りの数値文字列を数値型に変換するNumberFormatクラスのインスタンスを取得する
		NumberFormat nf = NumberFormat.getInstance();

		try {
			//数値文字列をNumber型のオブジェクトに変換する
			Number nEBudgetAmount = nf.parse(sEBudgetAmountVal);
			//Number型のオブジェクトからInteger値を取得する
			Integer iEBudgetAmount = nEBudgetAmount.intValue();
			
			//既存レコードがある場合は更新
			if(lngId!=0L) {
				Budget budget = Budget.findById(lngId);
				// 変更有無チェック用のレコードにセット
				Budget eBudget = new Budget(
						budget.ha_user,
						budget.year,
						budget.month,
						iEBudgetAmount,
						budget.item_mst,
						budget.ideal_deposit_mst
						);
				
				// Validate
			    validation.valid(eBudget);
			    if(validation.hasErrors()) {
			    	dailyAccount(bg_basis_date);
			    }
				// 項目が変更されていた行だけ更新
				if (budget.amount != eBudget.amount) {
					budget.amount = eBudget.amount;
				    
				    // 保存
				    budget.save();
				}
				
				return;
			}
			//既存レコードが無い場合は新規登録
			HaUser haUser = (HaUser)renderArgs.get("haUser");
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(DateFormat.getDateInstance().parse(bg_basis_date));
			ItemMst itemMst = null;
			IdealDepositMst idealDepositMst = null;
			//大分類が「My貯金預入」の場合は「取扱(My貯金)」ごとの登録
			if(sELargeCategoryVal.equals(BALANCE_TYPE_IDEAL_DEPOSIT_IN)) {
				idealDepositMst = IdealDepositMst.find("ha_user = " + haUser.id + " and ideal_deposit_name = '" + sEItemVal + "'").first();
			//大分類が「My貯金預入」でない場合は「項目」ごとの登録
			} else {
				itemMst = ItemMst.find("ha_user = " + haUser.id + " and item_name = '" + sEItemVal + "'").first();
			}
			Budget budget = new Budget(
					haUser,
					calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH) + 1,
					iEBudgetAmount,
					itemMst,
					idealDepositMst
					);
			
			// Validate
		    validation.valid(budget);
		    if(validation.hasErrors()) {
		    	dailyAccount(bg_basis_date);
		    }
		    // 保存
		    budget.save();
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	/**
//	 * 日付数値の妥当性チェック
//	 * @param intDate
//	 * @return 存在する日付の場合true
//	 */
//	private boolean checkIntDate(Integer intDate) {
//			if(intDate<10000101 ||
//					intDate>99991231)
//				return false;
//				
//			String strTmp = intDate.toString();
//			String sBasisDate = strTmp.substring(0, 4) + "/" + strTmp.substring(4, 6) + "/" + strTmp.substring(6);
//			//日付の妥当性チェック
//			return checkDate(sBasisDate);
//	}
//	
//	/**
//	 * 日付の妥当性チェックを行います。
//	 * 指定した日付文字列（yyyy/MM/dd or yyyy-MM-dd）が
//	 * カレンダーに存在するかどうかを返します。
//	 * @param strDate チェック対象の文字列
//	 * @return 存在する日付の場合true
//	 */
//	public static boolean checkDate(String strDate) {
//		if (strDate == null || strDate.length() != 10)
//			return false;
//		strDate = strDate.replace('-', '/');
//		DateFormat format = DateFormat.getDateInstance();
//		// 日付/時刻解析を厳密に行うかどうかを設定する。
//		format.setLenient(false);
//		try {
//			format.parse(strDate);
//			return true;
//		} catch (Exception e) {
//			return false;
//		}
//	}
}
