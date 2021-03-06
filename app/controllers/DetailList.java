package controllers;

import java.util.*;

import models.*;

import play.Play;
import play.db.Model;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.WebSocketController;
import play.mvc.With;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.postgresql.ssl.MakeSSL;

import models.*;

import static play.mvc.Http.WebSocketEvent.TextFrame;

@With(Secure.class)
public class DetailList extends Controller {
	
	@Before
	static void setConnectedUser() {
		if (Security.isConnected()) {
			HaUser haUser  = HaUser.find("byEmail", Security.connected()).first();
			renderArgs.put("haUser", haUser);
		}
	}
	
	
	/**
	 * 収支明細
	 * @param page
	 * @param h_secret_rec_flg
	 * @param h_payment_date_fr
	 * @param h_payment_date_to
	 * @param h_balance_type_id
	 * @param h_handling_id
	 * @param h_parllet_id
	 * @param h_item_id
	 * @param h_debit_date_fr
	 * @param h_debit_date_to
	 * @param e_id
	 * @param e_payment_date
	 * @param e_item_id
	 * @param n_payment_date
	 * @param n_item_id
	 * @param srch
	 * @param save
	 */
	public static void dl_balance(
    		Integer page,					/* 現在ページ */
    		Integer h_secret_rec_flg,		/* 絞込非公開フラグ */
    		String h_payment_date_fr,		/* 絞込支払日範囲（開始） */
    		String h_payment_date_to,		/* 絞込支払日範囲（終了） */
    		Long h_balance_type_id,  		/* 絞込収支種類ID */
    		Long h_handling_id,				/* 絞込取扱(実際)ID */
    		Long h_parllet_id,				/* 絞込取扱(Parllet)ID */
    		Long h_item_id,					/* 絞込項目ID */
    		String h_debit_date_fr,			/* 絞込引落日範囲（開始） */
    		String h_debit_date_to,			/* 絞込引落日範囲（終了） */
    		List<Long> e_id,				/* 変更行のID */
    		List<String> e_payment_date,	/* 変更行の支払日 */
    		List<Long> e_item_id,			/* 変更行の項目ID */
    		List<String> n_payment_date,	/* 変更行の支払日 */
    		List<Long> n_item_id,			/* 変更行の項目ID */
    		String srch,					/* 「絞込」ボタン */
    		String save						/* 「保存」ボタン */
    		) {
		
		String sqlSrchRec = "";
		//意図的に絞り込まれていない時
		if (srch==null) {
			//セッションに絞込条件が入っている時はそれぞれセット
			if ((session.get(Common.FLTR_DL_BAL_EXST_FLG) != null) &&
					(session.get(Common.FLTR_DL_BAL_EXST_FLG).equals("true"))) {
		    	if (!session.get(Common.FLTR_DL_BAL_SCRT_REC_FLG).equals(""))
		    		h_secret_rec_flg = Integer.parseInt(session.get(Common.FLTR_DL_BAL_SCRT_REC_FLG));
				h_payment_date_fr = session.get(Common.FLTR_DL_BAL_PDTE_FR);
				h_payment_date_to = session.get(Common.FLTR_DL_BAL_PDTE_TO);
		    	h_balance_type_id = null;
				if (!session.get(Common.FLTR_DL_BAL_BTYPE_ID).equals(""))
					h_balance_type_id = Long.parseLong(session.get(Common.FLTR_DL_BAL_BTYPE_ID));
		    	h_handling_id = null;
				if (!session.get(Common.FLTR_DL_BAL_HDLG_ID).equals(""))
					h_handling_id = Long.parseLong(session.get(Common.FLTR_DL_BAL_HDLG_ID));
				h_parllet_id = null;
				if (!session.get(Common.FLTR_DL_BAL_PRLT_ID).equals(""))
					h_parllet_id = Long.parseLong(session.get(Common.FLTR_DL_BAL_PRLT_ID));
		    	h_item_id = null;
		    	if (!session.get(Common.FLTR_DL_BAL_ITEM_ID).equals(""))
		    		h_item_id = Long.parseLong(session.get(Common.FLTR_DL_BAL_ITEM_ID));
		    	if (!session.get(Common.FLTR_DL_BAL_DDTE_FR).equals(""))
		    		h_debit_date_fr = session.get(Common.FLTR_DL_BAL_DDTE_FR);
		    	if (!session.get(Common.FLTR_DL_BAL_DDTE_TO).equals(""))
		    		h_debit_date_to = session.get(Common.FLTR_DL_BAL_DDTE_TO);
		    	
		    //初回読み込み時は絞込支払日範囲は現在日付の前後日
			} else {
	    		Calendar calendar = Calendar.getInstance();
	    		calendar.add(Calendar.DATE, 1);
	    		h_payment_date_to = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
	    		calendar.add(Calendar.DATE, -2);
	    		h_payment_date_fr = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
			}
		}
		
		//初回読み込み時のページは１ページとする
		if (page==null || page==0)
			page = 1;
	
		List<Record> records = null;
		List<BalanceTypeMst> bTypes = null;
		List<ParlletMst> prlts = null;
		List<HandlingMst> handlings = null;
		BalanceTypeMst bTypeIn = null;
		BalanceTypeMst bTypeOut = null;
		List<ItemMst> itemsIn = null;
		List<ItemMst> itemsOut = null;
		
		long count = 0L;	//レコード数
		int iLinage = Integer.parseInt(Play.configuration.getProperty("dl.iLinage"));	//１ページあたりの行数
		int nbPages = 0;	//ページ数
		
		// 「保存」ボタンが押された場合
		if (save != null) {
	    	if (save.equals("保存")) {
			    records = new ArrayList<Record>();
	
			    // 更新
	    		Iterator<String> strEPayDt = e_payment_date.iterator();
	    		Iterator<Long> lngEItemId = e_item_id.iterator();
	    		for (Long lId : e_id) {
	    			Record rec = Record.findById(lId);
	    			try {
	    				ItemMst item = ItemMst.findById(lngEItemId.next());
	    				// 変更有無チェック用のレコードにセット
	    				Record eRec = new Record(
	    						rec.ha_user,
	    						DateFormat.getDateInstance().parse(strEPayDt.next()),
	    						rec.balance_type_mst,  //変更しない
	    						rec.handling_mst,
	    						null,
	    						item,
	    						"",
	    						0,
	    						0,
	    						0,
	    						null,
	    						"",
	    						"",
	    						"",
	    						"",
	    						false
	    						);
	    				
		    			// Validate
					    validation.valid(eRec);
					    if (validation.hasErrors()) {
					    	// 以下の描画では駄目かも？
					    	render(bTypes, handlings, prlts, itemsIn, itemsOut, records, h_payment_date_fr, h_payment_date_to, h_balance_type_id, h_parllet_id, h_item_id);
					    }
	    				// 何れかの項目が変更されていた行だけ更新
	    				if (rec.payment_date != eRec.payment_date ||
	    						rec.item_mst != eRec.item_mst) {
							rec.payment_date = eRec.payment_date;
			    			rec.item_mst = eRec.item_mst;
						    
						    // 保存
						    rec.save();
	    				}
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
	    			// 編集後の行をそのまま戻す
				    records.add(rec);
	    		}
	
	
//			    // 新規作成
//	    		Iterator<Integer> intNItemId = n_item_id.iterator();
//	    		for (String spDt : n_payment_date) {
//	    			Record nRec = null;
//	    			try {
//	    				// 新規作成用のレコードにセット
//	    				nRec = new Record(
//	    						DateFormat.getDateInstance().parse(spDt),
//	    						intNItemId.next(),
//	    						"",
//	    						0,
//	    						"",
//	    						0,
//	    						"",
//	    						0,
//	    						0,
//	    						0,
//	    						"",
//	    						null,
//	    						"",
//	    						"",
//	    						"",
//	    						0,
//	    						"",
//	    						0,
//	    						"");
//	    				
//		    			// Validate
//					    validation.valid(nRec);
//					    if (validation.hasErrors()) {
//					    	// 以下の描画では駄目かも？
//					        render(records, h_payment_date_fr, h_payment_date_to, h_item_id);
//					    }
//					    // 保存
//					    nRec.save();
//					} catch (ParseException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//	
//	    			// 作成後の行をそのまま戻す
//				    records.add(nRec);
//	    		}
//	    		
	    	}
	    	
	    	
	    	
		} else {
			//引数の型等チェック
			Common cm = new Common();
			if (h_payment_date_fr!=null && !h_payment_date_fr.equals(""))
				if (!cm.checkDate(h_payment_date_fr))
					h_payment_date_fr = session.get(Common.FLTR_DL_BAL_PDTE_FR);
			if (h_payment_date_to!=null && !h_payment_date_to.equals(""))
				if (!cm.checkDate(h_payment_date_to))
					h_payment_date_to = session.get(Common.FLTR_DL_BAL_PDTE_TO);
			if (h_debit_date_fr!=null && !h_debit_date_fr.equals(""))
				if (!cm.checkDate(h_debit_date_fr))
					h_debit_date_fr = session.get(Common.FLTR_DL_BAL_DDTE_FR);
			if (h_debit_date_to!=null && !h_debit_date_to.equals(""))
				if (!cm.checkDate(h_debit_date_to))
					h_debit_date_to = session.get(Common.FLTR_DL_BAL_DDTE_TO);
			
			//検索条件をセッションに保存
			session.put(Common.FLTR_DL_BAL_EXST_FLG, "true");
			session.put(Common.FLTR_DL_BAL_SCRT_REC_FLG, h_secret_rec_flg==null ? "" : h_secret_rec_flg);
			session.put(Common.FLTR_DL_BAL_PDTE_FR, h_payment_date_fr==null ? "" : h_payment_date_fr);
			session.put(Common.FLTR_DL_BAL_PDTE_TO, h_payment_date_to==null ? "" : h_payment_date_to);
			session.put(Common.FLTR_DL_BAL_BTYPE_ID, h_balance_type_id==null ? "" : h_balance_type_id);
			session.put(Common.FLTR_DL_BAL_HDLG_ID, h_handling_id==null ? "" : h_handling_id);
			session.put(Common.FLTR_DL_BAL_PRLT_ID, h_parllet_id==null ? "" : h_parllet_id);
			session.put(Common.FLTR_DL_BAL_ITEM_ID, h_item_id==null ? "" : h_item_id);
			session.put(Common.FLTR_DL_BAL_DDTE_FR, h_debit_date_fr==null ? "" : h_debit_date_fr);
			session.put(Common.FLTR_DL_BAL_DDTE_TO, h_debit_date_to==null ? "" : h_debit_date_to);
			
			HaUser haUser = (HaUser)renderArgs.get("haUser");
			
			// 検索処理(BalanceTypeMst)
			bTypes = BalanceTypeMst.find("order by id").fetch();
			
			// 検索処理(ParlletMst)
			prlts = ParlletMst.find("ha_user = ? order by order_seq", haUser).fetch();
			
			// 検索処理(HandlingMst)
			handlings = HandlingMst.find("ha_user = ? order by handling_type_mst.handling_type_order, order_seq", haUser).fetch();
			
			// 検索処理(ItemMst(収入))
			bTypeIn = BalanceTypeMst.find("balance_type_name = '収入'").first();
			itemsIn = ItemMst.find("ha_user = ? and balance_type_mst = ? order by order_seq ", haUser, bTypeIn).fetch();
			
			// 検索処理(ItemMst(支出))
			bTypeOut = BalanceTypeMst.find("balance_type_name = '支出'").first();
			itemsOut = ItemMst.find("ha_user = ? and balance_type_mst = ? order by order_seq ", haUser, bTypeOut).fetch();
			
			// 検索処理(Record)
			sqlSrchRec += "ha_user = " + haUser.id;
			//  非公開フラグ
			if ((session.get("actionMode") != null) &&
					(session.get("actionMode").equals("Edit")))
				if (h_secret_rec_flg != null)
					if (h_secret_rec_flg != 0)
						sqlSrchRec += " and secret_rec_flg = " + (h_secret_rec_flg==2 ? "true" : "false");
			
			//  支払日範囲(自)
			if (h_payment_date_fr != null && !h_payment_date_fr.equals(""))
				sqlSrchRec += " and payment_date >= '" + h_payment_date_fr + " 00:00:00'";
			//  支払日範囲(至)
			if (h_payment_date_to != null && !h_payment_date_to.equals(""))
				sqlSrchRec += " and payment_date <= '" + h_payment_date_to + " 23:59:59'";
			//  引落日範囲(自)
			if (h_debit_date_fr != null && !h_debit_date_fr.equals(""))
				sqlSrchRec += " and debit_date >= '" + h_debit_date_fr + " 00:00:00'";
			//  引落日範囲(至)
			if (h_debit_date_to != null && !h_debit_date_to.equals(""))
				sqlSrchRec += " and debit_date <= '" + h_debit_date_to + " 23:59:59'";
			
			//  収支種類
			if (h_balance_type_id != null)
				if (h_balance_type_id != 0L)
					sqlSrchRec += " and balance_type_mst.id = " + h_balance_type_id;
			//  取扱(実際)
			if (h_handling_id != null)
				if (h_handling_id != 0L)
					sqlSrchRec += " and handling_mst.id " +
							(h_handling_id==-1 ? "is null "
									: (h_handling_id==-2 ? "is not null "
											: "= " + h_handling_id));
			//  取扱(Parllet)
			if (h_parllet_id!=null)
				if (h_parllet_id!=0L)
					sqlSrchRec += " and parllet_mst.id " +
							(h_parllet_id==-1 ? "is null "
									: (h_parllet_id==-2 ? "is not null "
											: "= " + h_parllet_id));
			//  項目
			if (h_item_id != null)
				if (h_item_id != 0L)
					sqlSrchRec += " and item_mst.id " +
							(h_item_id==-1 ? "is null "
									: (h_item_id==-2 ? "is not null "
											: "= " + h_item_id));
			
			sqlSrchRec += "" +
					((session.get("actionMode")).equals("View") ? " and secret_rec_flg = false " : "") +
					"";
			count = Record.count(sqlSrchRec);
			nbPages = (int) (Math.ceil((double)count/iLinage));
			if (!(count==0 && page==1) && (page<1 || page>nbPages)) {
				validation.addError("pageError", Messages.get("validation.pageError"));
				page = 1;
			}
			sqlSrchRec += " order by payment_date, id";
			records = Record.find(
					sqlSrchRec).from(0).fetch(page, iLinage);
		}
		
		render(bTypes, handlings, prlts, itemsIn, itemsOut, records, h_secret_rec_flg, h_payment_date_fr, h_payment_date_to, h_balance_type_id, h_handling_id, h_parllet_id, h_item_id, h_debit_date_fr, h_debit_date_to, count, nbPages, page);
    }
	
	
	/**
	 * 残高明細（口座系）
	 * @param page
	 * @param h_secret_rec_flg
	 * @param h_debit_date_fr
	 * @param h_debit_date_to
	 * @param h_handling_id
	 * @param srch
	 */
	public static void dl_remainderBank(
    		Integer page,					/* 現在ページ */
    		Integer h_secret_rec_flg,		/* 絞込非公開フラグ */
    		String h_debit_date_fr,			/* 絞込引落日範囲（開始） */
    		String h_debit_date_to,			/* 絞込引落日範囲（終了） */
    		Long h_handling_id,				/* 絞込取扱(実際)ID */
    		String srch						/* 「絞込」ボタン */
    		) {
		
		//意図的に絞り込まれていない時
		if (srch==null) {
			//セッションに絞込条件が入っている時はそれぞれセット
			if ((session.get(Common.FLTR_DL_RB_EXST_FLG) != null) &&
					(session.get(Common.FLTR_DL_RB_EXST_FLG).equals("true"))) {
		    	if (!session.get(Common.FLTR_DL_RB_SCRT_REC_FLG).equals(""))
		    		h_secret_rec_flg = Integer.parseInt(session.get(Common.FLTR_DL_RB_SCRT_REC_FLG));
				h_debit_date_fr = session.get(Common.FLTR_DL_RB_DDTE_FR);
				h_debit_date_to = session.get(Common.FLTR_DL_RB_DDTE_TO);
		    	h_handling_id = null;
				if (!session.get(Common.FLTR_DL_RB_HDLG_ID).equals(""))
					h_handling_id = Long.parseLong(session.get(Common.FLTR_DL_RB_HDLG_ID));
		    	
		    //初回読み込み時は絞込引落日範囲は1か月前から半年後
			} else {
	    		Calendar calendar = Calendar.getInstance();
	    		calendar.add(Calendar.MONTH, -1);
	    		h_debit_date_fr = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
	    		calendar.add(Calendar.MONTH, 7);
	    		h_debit_date_to = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
			}
		}
		
		//初回読み込み時のページは１ページとする
		if (page==null || page==0)
			page = 1;
	
		List<Record> records = null;
		List<HandlingMst> handlings = null;
		
		long count = 0L;	//レコード数
		int iLinage = Integer.parseInt(Play.configuration.getProperty("dl.iLinage"));	//１ページあたりの行数
		int nbPages = 0;	//ページ数
		
		//引数の型等チェック
		Common cm = new Common();
		if (h_debit_date_fr!=null && !h_debit_date_fr.equals(""))
			if (!cm.checkDate(h_debit_date_fr))
				h_debit_date_fr = session.get(Common.FLTR_DL_RB_DDTE_FR);
		if (h_debit_date_to!=null && !h_debit_date_to.equals(""))
			if (!cm.checkDate(h_debit_date_to))
				h_debit_date_to = session.get(Common.FLTR_DL_RB_DDTE_TO);
		
		//検索条件をセッションに保存
		session.put(Common.FLTR_DL_RB_EXST_FLG, "true");
		session.put(Common.FLTR_DL_RB_SCRT_REC_FLG, h_secret_rec_flg==null ? "" : h_secret_rec_flg);
		session.put(Common.FLTR_DL_RB_DDTE_FR, h_debit_date_fr==null ? "" : h_debit_date_fr);
		session.put(Common.FLTR_DL_RB_DDTE_TO, h_debit_date_to==null ? "" : h_debit_date_to);
		session.put(Common.FLTR_DL_RB_HDLG_ID, h_handling_id==null ? "" : h_handling_id);
		
		HaUser haUser = (HaUser)renderArgs.get("haUser");
		
		// 検索処理(HandlingMst)
		handlings = HandlingMst.find("ha_user = ? and (handling_type_mst.handling_type_name = ? or handling_type_mst.handling_type_name = ?) order by handling_type_mst.handling_type_order, order_seq", haUser, Messages.get("HandlingType.bank"), Messages.get("HandlingType.emoney")).fetch();
		
		if (handlings.size()==0)
			render();
		
		/* 残高明細行作成 */
		String sql = "";
		DetailList dl = new DetailList();
		//  取扱(実際)
		//意図的に絞り込まれていない時は口座・電子マネーの先頭のモノで絞り込む
		if (srch==null && h_handling_id==null)
			h_handling_id = handlings.get(0).id;
		// JOIN句 固定部分
		String sqlJoinPhrase = "" +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON r.balance_type_mst_id = b.id " +
				" LEFT JOIN HandlingMst h " +
				"   ON r.handling_mst_id = h.id " +
				" LEFT JOIN HandlingTypeMst ht " +
				"   ON h.handling_type_mst_id = ht.id " +
				"";
		//  非公開フラグ
		String sqlSecretRecFlg = "";
		if ((session.get("actionMode") != null) && (session.get("actionMode").equals("Edit")))
			if (h_secret_rec_flg != null)
				if (h_secret_rec_flg != 0)
					sqlSecretRecFlg = " AND r.secret_rec_flg = " + (h_secret_rec_flg==2 ? "true" : "false");
		
		
		/* 繰越金作成 */
		List<WkDlRmRec> lWDRR = new ArrayList<WkDlRmRec>();
		
		//  繰越金取得用SQL作成
		sql = dl.makeSqlDlRbBalanceBroughtForward(haUser, h_debit_date_fr, h_handling_id, sqlJoinPhrase, sqlSecretRecFlg);
		
		BigInteger biRemainder = (BigInteger)JPA.em().createNativeQuery(sql).getSingleResult();
		Long lngRemainder = biRemainder.longValue();
		
		
		/* 残高明細行作成 */
		
		//残高明細行取得用SQL作成
		sql = dl.makeSqlDlRbRec(haUser, h_debit_date_fr, h_debit_date_to, h_handling_id, sqlJoinPhrase, sqlSecretRecFlg);

		List<Object[]> lstObjEach = JPA.em().createNativeQuery(sql).getResultList();
		
		count = lstObjEach.size();
		nbPages = (int) (Math.ceil((double)count/iLinage));
		if (!(count==0 && page==1) && (page<1 || page>nbPages)) {
			validation.addError("pageError", Messages.get("validation.pageError"));
			page = 1;
		}
		if (page==1) {
			WkDlRmRec wkDlRmRec = new WkDlRmRec();
			wkDlRmRec.setStrPaymentDate("");
			wkDlRmRec.setStrBalanceTypeName(Messages.get("views.detaillist.remainderBank.balanceBroughtForward"));
			wkDlRmRec.setLngRemainder(lngRemainder);
			lWDRR.add(wkDlRmRec);
		}
		//全行ループし、現在ページのデータにセットして抜ける
		for(int i = 0; i < count; i++) {
			Object[] objEach = lstObjEach.get(i);
			Long intRemainderEach = objEach[7]==null ? 0 : Long.parseLong(String.valueOf(objEach[7]));
			String strBalanceTypeName = objEach[4]==null ? "" : String.valueOf(objEach[4]);
			if (strBalanceTypeName.equals(Common.BALANCE_TYPE_IN) || strBalanceTypeName.equals(Common.BALANCE_TYPE_BANK_IN))
				lngRemainder += intRemainderEach;
			if (strBalanceTypeName.equals(Common.BALANCE_TYPE_OUT) || strBalanceTypeName.equals(Common.BALANCE_TYPE_BANK_OUT))
				lngRemainder -= intRemainderEach;
			if (i >= (page*iLinage)-iLinage) {
				WkDlRmRec wkDlRmRecEach = new WkDlRmRec();
				wkDlRmRecEach.setLngId(objEach[0]==null ? 0 :  Long.parseLong(String.valueOf(objEach[0])));
				wkDlRmRecEach.setBolSecretRecFlg(objEach[1]==null ? false : Boolean.valueOf(String.valueOf(objEach[1])));
				wkDlRmRecEach.setStrDebitDate(String.valueOf(objEach[2]));
				wkDlRmRecEach.setStrPaymentDate(objEach[3]==null ? "" : String.valueOf(objEach[3]));
				wkDlRmRecEach.setStrBalanceTypeName(objEach[4]==null ? "" : String.valueOf(objEach[4]));
				wkDlRmRecEach.setStrHandlingName(objEach[5]==null ? "" : String.valueOf(objEach[5]));
				wkDlRmRecEach.setStrParlletName(objEach[6]==null ? "" : String.valueOf(objEach[6]));
				wkDlRmRecEach.setLngAmount(objEach[7]==null ? 0 : Long.parseLong(String.valueOf(objEach[7])));
				wkDlRmRecEach.setStrStore(objEach[8]==null ? "" : String.valueOf(objEach[8]));
				wkDlRmRecEach.setLngRemainder(lngRemainder);
				
				//収支明細へジャンプのための引数をセット
				wkDlRmRecEach.setLngBalanceTypeId(objEach[9]==null ? null : Long.parseLong(String.valueOf(objEach[9])));
				wkDlRmRecEach.setLngHandlingId(objEach[10]==null ? null : Long.parseLong(String.valueOf(objEach[10])));
				
				lWDRR.add(wkDlRmRecEach);
			}
			if (i == (count<page*iLinage ? count : page*iLinage)-1)
				break;
		}
		
		render(h_debit_date_fr, h_debit_date_to, handlings, records, lWDRR, h_secret_rec_flg, h_handling_id, count, nbPages, page);
    }
	
	/**
	 * 残高明細（口座系）繰越金取得用SQL作成
	 * @param haUser
	 * @param h_debit_date_fr
	 * @param h_handling_id
	 * @param sqlJoinPhrase
	 * @param sqlSecretRecFlg
	 * @return
	 */
	private String makeSqlDlRbBalanceBroughtForward(
			HaUser haUser,
    		String h_debit_date_fr,			/* 絞込引落日範囲（開始） */
    		Long h_handling_id,				/* 絞込取扱(実際)ID */
    		String sqlJoinPhrase,
    		String sqlSecretRecFlg
			) {
		String sql = "";
			// 収入：加算、支出：減算
			String sqlSumBalanceInOut = "" +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_IN + "' THEN r.amount " +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_OUT + "' THEN -r.amount " +
					"";
			// 口座引出：減算、口座預入：加算
			String sqlSumBalanceBankInOut = "" +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_BANK_OUT + "' THEN -r.amount" +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_BANK_IN + "' THEN r.amount" +
					"";
			
			sql = "   SELECT " +
					"   COALESCE(SUM(" +
					"     CASE " +
					sqlSumBalanceInOut +		//収入加算・支出減算
					sqlSumBalanceBankInOut +	//口座引出減算・口座預入加算
					"     END" +
					"   ), 0) " +
					" FROM Record r " +
					sqlJoinPhrase +
					" LEFT JOIN ItemMst i " +
					"   ON r.item_mst_id = i.id " +
					" LEFT JOIN HandlingMst hb " +
					"   ON h.debit_bank_id = hb.id " +
					" WHERE r.ha_user_id = " + haUser.id +
					"   AND cast(r.debit_date as date) < to_date('" + h_debit_date_fr + "', 'YYYY/MM/DD') " +
					"   AND (" +
						" CASE " +
						"   WHEN ht.handling_type_name = '" + Common.HANDLING_TYPE_CRECA + "' THEN hb.id " +
						"   ELSE h.id " +
						" END) = " + h_handling_id +
					"   AND b.balance_type_name in('" + Common.BALANCE_TYPE_OUT + "','" + Common.BALANCE_TYPE_IN + "','" + Common.BALANCE_TYPE_BANK_OUT + "','" + Common.BALANCE_TYPE_BANK_IN + "') " +
					sqlSecretRecFlg +
					"";
		
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（口座系）行取得用SQL作成
	 * @param haUser
	 * @param h_debit_date_fr
	 * @param h_debit_date_to
	 * @param h_handling_id
	 * @param sqlJoinPhrase
	 * @param sqlSecretRecFlg
	 * @return
	 */
	private String makeSqlDlRbRec(
			HaUser haUser,
    		String h_debit_date_fr,			/* 絞込引落日範囲（開始） */
    		String h_debit_date_to,			/* 絞込引落日範囲（終了） */
    		Long h_handling_id,				/* 絞込取扱(実際)ID */
    		String sqlJoinPhrase,
    		String sqlSecretRecFlg
			) {
		String sql = "";
		
		// WHERE句 固定部分
		String sqlDebitDateFr = "";
		String sqlDebitDateTo = "";
		//  引落日範囲(自)
		if (h_debit_date_fr != null && !h_debit_date_fr.equals(""))
			sqlDebitDateFr = "   AND cast(r.debit_date as date) >= to_date('" + h_debit_date_fr + "', 'YYYY/MM/DD') ";
		//  引落日範囲(至)
		if (h_debit_date_to != null && !h_debit_date_to.equals(""))
			sqlDebitDateTo = "   AND cast(r.debit_date as date) <= to_date('" + h_debit_date_to + "', 'YYYY/MM/DD') ";
		String sqlWherePhrase = "" +
				" WHERE r.ha_user_id = " + haUser.id +
				sqlDebitDateFr + sqlDebitDateTo +
				"";
		
		//残高明細行取得用SQL作成(クレジットカード)
		String sqlDlRbRecCreca = makeSqlDlRbRecCreca(haUser, sqlJoinPhrase, sqlWherePhrase, sqlSecretRecFlg, h_handling_id);
		
		//残高明細行取得用SQL作成(クレジットカード以外)
		String sqlDlRbRecNotCreca = makeSqlDlRbRecNotCreca(haUser, sqlJoinPhrase, sqlWherePhrase, sqlSecretRecFlg, h_handling_id);
		
		sql = "" +
				" ( " + sqlDlRbRecCreca + " ) " +
				" UNION ALL " +
				" ( " + sqlDlRbRecNotCreca + " ) " +
				" ORDER BY rm_debit_date, rm_payment_date_order" +
				"";
		
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（口座系）行取得用SQL作成(クレジットカード)
	 * @param haUser
	 * @param sqlJoinPhrase
	 * @param sqlWherePhrase
	 * @param sqlSecretRecFlg
	 * @param h_handling_id
	 * @return
	 */
	private String makeSqlDlRbRecCreca(
			HaUser haUser,
    		String sqlJoinPhrase,
    		String sqlWherePhrase,
    		String sqlSecretRecFlg,
    		Long h_handling_id				/* 絞込取扱(実際)ID */
			) {
		String sql = "";
		
		sql = "" +
				" SELECT " +
				"   null as rm_id " +
				"  ,cast(null as boolean) as rm_secret_rec_flg " +
				"  ,to_char(r.debit_date, 'YYYY/MM/DD') as rm_debit_date " +
				"  ,cast('' as character varying(255)) as rm_payment_date " +
				"  ,b.balance_type_name as rm_balance_type_name " +
				"  ,h.handling_name as rm_handling_name " +
				"  ,cast('' as character varying(255)) as rm_parllet_name " +
				"  ,COALESCE(SUM(r.amount), 0) as rm_amount " +
				"  ,cast('' as character varying(255)) as rm_store " +
				"  ,b.id as rm_balance_type_id " +
				"  ,h.id as rm_handling_id " +
				"  ,r.debit_date as rm_payment_date_order " +
				" FROM Record r " +
				sqlJoinPhrase +
				" LEFT JOIN HandlingMst hb " +
				"   ON h.debit_bank_id = hb.id " +
				sqlWherePhrase +
				"   AND hb.id = " + h_handling_id +
				"   AND b.balance_type_name in('" + Common.BALANCE_TYPE_OUT + "','" + Common.BALANCE_TYPE_IN + "') " +
				"   AND ht.handling_type_name = '" + Common.HANDLING_TYPE_CRECA + "' " +
				sqlSecretRecFlg +
				" GROUP BY rm_debit_date, rm_balance_type_name, rm_handling_name, rm_balance_type_id, rm_handling_id, rm_payment_date_order " +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（口座系）行取得用SQL作成(クレジットカード以外)
	 * @param haUser
	 * @param sqlJoinPhrase
	 * @param sqlWherePhrase
	 * @param sqlSecretRecFlg
	 * @param h_handling_id
	 * @return
	 */
	private String makeSqlDlRbRecNotCreca(
			HaUser haUser,
    		String sqlJoinPhrase,
    		String sqlWherePhrase,
    		String sqlSecretRecFlg,
    		Long h_handling_id				/* 絞込取扱(実際)ID */
			) {
		String sql = "";
		
		sql = "" +
				" SELECT " +
				"   r.id as rm_id " +
				"  ,r.secret_rec_flg as rm_secret_rec_flg " +
				"  ,to_char(r.debit_date, 'YYYY/MM/DD') as rm_debit_date " +
				"  ,to_char(r.payment_date, 'YYYY/MM/DD HH24:MI') as rm_payment_date " +
				"  ,b.balance_type_name as rm_balance_type_name " +
				"  ,h.handling_name as rm_handling_name " +
				"  ,pm.parllet_name as rm_parllet_name " +
				"  ,r.amount as rm_amount " +
				"  ,r.store as rm_store " +
				"  ,null as rm_balance_type_id " +
				"  ,null as rm_handling_id " +
				"  ,r.payment_date as rm_payment_date_order " +
				" FROM Record r " +
				sqlJoinPhrase +
				" LEFT JOIN ParlletMst pm " +
				"   ON r.parllet_mst_id = pm.id " +
				sqlWherePhrase +
				"   AND h.id = " + h_handling_id +
				"   AND b.balance_type_name in('" + Common.BALANCE_TYPE_OUT + "','" + Common.BALANCE_TYPE_IN + "','" + Common.BALANCE_TYPE_BANK_OUT + "','" + Common.BALANCE_TYPE_BANK_IN + "') " +
				"   AND ht.handling_type_name != '" + Common.HANDLING_TYPE_CRECA + "' " +
				sqlSecretRecFlg +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（Parllet）
	 * @param page
	 * @param dlRpHdSecretRecFlg
	 * @param dlRpHdDebitDateFr
	 * @param dlRpHdDebitDateTo
	 * @param h_handling_id
	 * @param dlRpSrch
	 */
	public static void dl_remainderPrlt(
    		Integer page,					/* 現在ページ */
    		Integer dlRpHdSecretRecFlg,		/* 絞込非公開フラグ */
    		String dlRpHdDebitDateFr,		/* 絞込引落日範囲（開始） */
    		String dlRpHdDebitDateTo,		/* 絞込引落日範囲（終了） */
    		Long dlRpHdParlletId,			/* 絞込取扱(Parllet)ID */
    		String dlRpSrch					/* 「絞込」ボタン */
    		) {
		
		//意図的に絞り込まれていない時
		if (dlRpSrch==null) {
			//セッションに絞込条件が入っている時はそれぞれセット
			if ((session.get(Common.FLTR_DL_RI_EXST_FLG) != null) &&
					(session.get(Common.FLTR_DL_RI_EXST_FLG).equals("true"))) {
		    	if (!session.get(Common.FLTR_DL_RI_SCRT_REC_FLG).equals(""))
		    		dlRpHdSecretRecFlg = Integer.parseInt(session.get(Common.FLTR_DL_RI_SCRT_REC_FLG));
				dlRpHdDebitDateFr = session.get(Common.FLTR_DL_RI_DDTE_FR);
				dlRpHdDebitDateTo = session.get(Common.FLTR_DL_RI_DDTE_TO);
		    	dlRpHdParlletId = null;
				if (!session.get(Common.FLTR_DL_RI_PRLT_ID).equals(""))
					dlRpHdParlletId = Long.parseLong(session.get(Common.FLTR_DL_RI_PRLT_ID));
		    	
		    //初回読み込み時は絞込引落日範囲は1か月前から半年後
			} else {
	    		Calendar calendar = Calendar.getInstance();
	    		calendar.add(Calendar.MONTH, -1);
	    		dlRpHdDebitDateFr = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
	    		calendar.add(Calendar.MONTH, 7);
	    		dlRpHdDebitDateTo = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
			}
		}
		
		//初回読み込み時のページは１ページとする
		if (page==null || page==0)
			page = 1;
	
		List<Record> records = null;
		List<ParlletMst> prlts = null;
		
		
		long count = 0L;	//レコード数
		int iLinage = Integer.parseInt(Play.configuration.getProperty("dl.iLinage"));	//１ページあたりの行数
		int nbPages = 0;	//ページ数
		
		//引数の型等チェック
		Common cm = new Common();
		if (dlRpHdDebitDateFr!=null && !dlRpHdDebitDateFr.equals(""))
			if (!cm.checkDate(dlRpHdDebitDateFr))
				dlRpHdDebitDateFr = session.get(Common.FLTR_DL_RI_DDTE_FR);
		if (dlRpHdDebitDateTo!=null && !dlRpHdDebitDateTo.equals(""))
			if (!cm.checkDate(dlRpHdDebitDateTo))
				dlRpHdDebitDateTo = session.get(Common.FLTR_DL_RI_DDTE_TO);
		
		//検索条件をセッションに保存
		session.put(Common.FLTR_DL_RI_EXST_FLG, "true");
		session.put(Common.FLTR_DL_RI_SCRT_REC_FLG, dlRpHdSecretRecFlg==null ? "" : dlRpHdSecretRecFlg);
		session.put(Common.FLTR_DL_RI_DDTE_FR, dlRpHdDebitDateFr==null ? "" : dlRpHdDebitDateFr);
		session.put(Common.FLTR_DL_RI_DDTE_TO, dlRpHdDebitDateTo==null ? "" : dlRpHdDebitDateTo);
		session.put(Common.FLTR_DL_RI_PRLT_ID, dlRpHdParlletId==null ? "" : dlRpHdParlletId);
		
		HaUser haUser = (HaUser)renderArgs.get("haUser");
		
		// 検索処理(ParlletMst)
		prlts = ParlletMst.find("ha_user = ? order by order_seq", haUser).fetch();
		
		
		/* 残高明細行作成 */
		String sql = "";
		DetailList dl = new DetailList();
		//  取扱(Parllet)
		//意図的に絞り込まれていない時は取扱(Parllet)の先頭のモノで絞り込む
		if (dlRpSrch==null && dlRpHdParlletId==null)
			dlRpHdParlletId = prlts.get(0).id;
		// JOIN句 固定部分
		String sqlJoinPhrase = "" +
				" LEFT JOIN BalanceTypeMst b " +
				"   ON r.balance_type_mst_id = b.id " +
				" LEFT JOIN ParlletMst pm " +
				"   ON r.parllet_mst_id = pm.id " +
				"";
		// WHERE句 固定部分
		String sqlWherePhrase = "" +
				" WHERE r.ha_user_id = " + haUser.id +
				"   AND pm.id = " + dlRpHdParlletId +
				"";
		//  非公開フラグ
		String sqlSecretRecFlg = "";
		if ((session.get("actionMode") != null) && (session.get("actionMode").equals("Edit")))
			if (dlRpHdSecretRecFlg != null)
				if (dlRpHdSecretRecFlg != 0)
					sqlSecretRecFlg = " AND r.secret_rec_flg = " + (dlRpHdSecretRecFlg==2 ? "true" : "false");
		
		
		/* 繰越金作成 */
		List<WkDlRmRec> lWDRR = new ArrayList<WkDlRmRec>();
		
		//  繰越金取得用SQL作成
		sql = dl.makeSqlDlRpBalanceBroughtForward(dlRpHdDebitDateFr, sqlJoinPhrase, sqlWherePhrase, sqlSecretRecFlg);
		
		BigInteger biRemainder = (BigInteger)JPA.em().createNativeQuery(sql).getSingleResult();
		Long lngRemainder = biRemainder.longValue();
		
		
		/* 残高明細行作成 */
		
		//残高明細行取得用SQL作成
		sql = dl.makeSqlDlRpRec(dlRpHdDebitDateFr, dlRpHdDebitDateTo, dlRpHdParlletId, sqlJoinPhrase, sqlWherePhrase, sqlSecretRecFlg);

		List<Object[]> lstObjEach = JPA.em().createNativeQuery(sql).getResultList();
		
		count = lstObjEach.size();
		nbPages = (int) (Math.ceil((double)count/iLinage));
		if (!(count==0 && page==1) && (page<1 || page>nbPages)) {
			validation.addError("pageError", Messages.get("validation.pageError"));
			page = 1;
		}
		if (page==1) {
			WkDlRmRec wkDlRmRec = new WkDlRmRec();
			wkDlRmRec.setStrPaymentDate("");
			wkDlRmRec.setStrBalanceTypeName(Messages.get("views.detaillist.remainderBank.balanceBroughtForward"));
			wkDlRmRec.setLngRemainder(lngRemainder);
			lWDRR.add(wkDlRmRec);
		}
		//全行ループし、現在ページのデータにセットして抜ける
		for(int i = 0; i < count; i++) {
			Object[] objEach = lstObjEach.get(i);
			Long intRemainderEach = objEach[7]==null ? 0 : Long.parseLong(String.valueOf(objEach[7]));
			String strBalanceTypeName = objEach[4]==null ? "" : String.valueOf(objEach[4]);
			if (strBalanceTypeName.equals(Common.BALANCE_TYPE_IN) || strBalanceTypeName.equals(Common.BALANCE_TYPE_PARLLET_IN))
				lngRemainder += intRemainderEach;
			if (strBalanceTypeName.equals(Common.BALANCE_TYPE_OUT) || strBalanceTypeName.equals(Common.BALANCE_TYPE_PARLLET_OUT))
				lngRemainder -= intRemainderEach;
			if (i >= (page*iLinage)-iLinage) {
				WkDlRmRec wkDlRmRecEach = new WkDlRmRec();
				wkDlRmRecEach.setLngId(objEach[0]==null ? 0 :  Long.parseLong(String.valueOf(objEach[0])));
				wkDlRmRecEach.setBolSecretRecFlg(objEach[1]==null ? false : Boolean.valueOf(String.valueOf(objEach[1])));
				wkDlRmRecEach.setStrDebitDate(String.valueOf(objEach[2]));
				wkDlRmRecEach.setStrPaymentDate(objEach[3]==null ? "" : String.valueOf(objEach[3]));
				wkDlRmRecEach.setStrBalanceTypeName(objEach[4]==null ? "" : String.valueOf(objEach[4]));
				wkDlRmRecEach.setStrParlletName(objEach[5]==null ? "" : String.valueOf(objEach[5]));
				wkDlRmRecEach.setStrHandlingName(objEach[6]==null ? "" : String.valueOf(objEach[6]));
				wkDlRmRecEach.setLngAmount(objEach[7]==null ? 0 : Long.parseLong(String.valueOf(objEach[7])));
				wkDlRmRecEach.setStrStore(objEach[8]==null ? "" : String.valueOf(objEach[8]));
				wkDlRmRecEach.setLngRemainder(lngRemainder);
				
				//収支明細へジャンプのための引数をセット
				wkDlRmRecEach.setLngBalanceTypeId(objEach[9]==null ? null : Long.parseLong(String.valueOf(objEach[9])));
				wkDlRmRecEach.setLngParlletId(objEach[10]==null ? null : Long.parseLong(String.valueOf(objEach[10])));
				
				lWDRR.add(wkDlRmRecEach);
			}
			if (i == (count<page*iLinage ? count : page*iLinage)-1)
				break;
		}
		
		render(dlRpHdDebitDateFr, dlRpHdDebitDateTo, prlts, records, lWDRR, dlRpHdSecretRecFlg, dlRpHdParlletId, count, nbPages, page);
    }
	
	/**
	 * 残高明細（Parllet）繰越金取得用SQL作成
	 * @param dlRpHdDebitDateFr
	 * @param sqlJoinPhrase
	 * @param sqlWherePhrase
	 * @param sqlSecretRecFlg
	 * @return
	 */
	private String makeSqlDlRpBalanceBroughtForward(
			String dlRpHdDebitDateFr,		/* 絞込引落日範囲（開始） */
			String sqlJoinPhrase,
			String sqlWherePhrase,
			String sqlSecretRecFlg
			) {
		String sql = "";
			// 収入：加算、支出：減算
			String sqlSumBalanceInOut = "" +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_IN + "' THEN r.amount " +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_OUT + "' THEN -r.amount " +
					"";
			// Parllet引出：減算、Parllet預入：加算
			String sqlSumParlletInOut = "" +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_PARLLET_OUT + "' THEN -r.amount" +
					" WHEN b.balance_type_name = '" + Common.BALANCE_TYPE_PARLLET_IN + "' THEN r.amount" +
					"";
			
			sql = "   SELECT " +
					"   COALESCE(SUM(" +
					"     CASE " +
					sqlSumBalanceInOut +	//収入加算・支出減算
					sqlSumParlletInOut +	//Parllet引出減算・Parllet預入加算
					"     END" +
					"   ), 0) " +
					" FROM Record r " +
					sqlJoinPhrase +
					sqlWherePhrase +
					"   AND cast(r.debit_date as date) < to_date('" + dlRpHdDebitDateFr + "', 'YYYY/MM/DD') " +
					"   AND (   (    b.balance_type_name in('" + Common.BALANCE_TYPE_OUT + "', '" + Common.BALANCE_TYPE_IN + "') " +
								"AND r.parllet_mst_id IS NOT NULL " +
								") " +
							"OR b.balance_type_name in('" + Common.BALANCE_TYPE_PARLLET_IN + "', '" + Common.BALANCE_TYPE_PARLLET_OUT + "') " +
							") " +
					"" +
					sqlSecretRecFlg +
					"";
		
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（Parllet）行取得用SQL作成
	 * @param dlRpHdDebitDateFr
	 * @param dlRpHdDebitDateTo
	 * @param dlRpHdParlletId
	 * @param sqlJoinPhrase
	 * @param sqlSecretRecFlg
	 * @return
	 */
	private String makeSqlDlRpRec(
    		String dlRpHdDebitDateFr,	/* 絞込引落日範囲（開始） */
    		String dlRpHdDebitDateTo,	/* 絞込引落日範囲（終了） */
    		Long dlRpHdParlletId,		/* 絞込取扱(Parllet)ID */
    		String sqlJoinPhrase,
			String sqlWherePhrase,
    		String sqlSecretRecFlg
			) {
		String sql = "";
		
		// WHERE句 固定部分
		String sqlDebitDateFr = "";
		String sqlDebitDateTo = "";
		//  引落日範囲(自)
		if (dlRpHdDebitDateFr != null && !dlRpHdDebitDateFr.equals(""))
			sqlDebitDateFr = " AND cast(r.debit_date as date) >= to_date('" + dlRpHdDebitDateFr + "', 'YYYY/MM/DD') ";
		//  引落日範囲(至)
		if (dlRpHdDebitDateTo != null && !dlRpHdDebitDateTo.equals(""))
			sqlDebitDateTo = " AND cast(r.debit_date as date) <= to_date('" + dlRpHdDebitDateTo + "', 'YYYY/MM/DD') ";
		sqlWherePhrase += sqlDebitDateFr + sqlDebitDateTo;
		
		//残高明細行取得用SQL作成(クレジットカード)
		String sqlDlRbRecCreca = makeSqlDlRpRecCreca(sqlJoinPhrase, sqlWherePhrase, sqlSecretRecFlg, dlRpHdParlletId);
		
		//残高明細行取得用SQL作成(クレジットカード以外)
		String sqlDlRbRecNotCreca = makeSqlDlRpRecNotCreca(sqlJoinPhrase, sqlWherePhrase, sqlSecretRecFlg, dlRpHdParlletId);
		
		sql = "" +
				" ( " + sqlDlRbRecCreca + " ) " +
				" UNION ALL " +
				" ( " + sqlDlRbRecNotCreca + " ) " +
				" ORDER BY rm_debit_date, rm_payment_date_order" +
				"";
		
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（Parllet）行取得用SQL作成(クレジットカード)
	 * @param sqlJoinPhrase
	 * @param sqlWherePhrase
	 * @param sqlSecretRecFlg
	 * @param h_parllet_id
	 * @return
	 */
	private String makeSqlDlRpRecCreca(
    		String sqlJoinPhrase,
    		String sqlWherePhrase,
    		String sqlSecretRecFlg,
    		Long h_parllet_id			/* 絞込取扱(Parllet)ID */
			) {
		String sql = "";
		
		sql = "" +
				" SELECT " +
				"   null as rm_id " +
				"  ,cast(null as boolean) as rm_secret_rec_flg " +
				"  ,to_char(r.debit_date, 'YYYY/MM/DD') as rm_debit_date " +
				"  ,cast('' as character varying(255)) as rm_payment_date " +
				"  ,b.balance_type_name as rm_balance_type_name " +
				"  ,pm.parllet_name as rm_parllet_name " +
				"  ,h.handling_name as rm_handling_name " +
				"  ,COALESCE(SUM(r.amount), 0) as rm_amount " +
				"  ,cast('' as character varying(255)) as rm_store " +
				"  ,b.id as rm_balance_type_id " +
				"  ,pm.id as rm_parllet_id " +
				"  ,r.debit_date as rm_payment_date_order " +
				" FROM Record r " +
				sqlJoinPhrase +
				" LEFT JOIN HandlingMst h " +
				"   ON r.handling_mst_id = h.id " +
				" LEFT JOIN HandlingTypeMst ht " +
				"   ON h.handling_type_mst_id = ht.id " +
				sqlWherePhrase +
				"   AND ht.handling_type_name = '" + Common.HANDLING_TYPE_CRECA + "' " +
				sqlSecretRecFlg +
				" GROUP BY rm_debit_date, rm_balance_type_name, rm_parllet_name, rm_handling_name, rm_balance_type_id, rm_parllet_id, rm_payment_date_order " +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	/**
	 * 残高明細（Parllet）行取得用SQL作成(クレジットカード以外)
	 * @param sqlJoinPhrase
	 * @param sqlWherePhrase
	 * @param sqlSecretRecFlg
	 * @param h_parllet_id
	 * @return
	 */
	private String makeSqlDlRpRecNotCreca(
    		String sqlJoinPhrase,
    		String sqlWherePhrase,
    		String sqlSecretRecFlg,
    		Long h_parllet_id			/* 絞込取扱(Parllet)ID */
			) {
		String sql = "";
		
		sql = "" +
				" SELECT " +
				"   r.id as rm_id " +
				"  ,r.secret_rec_flg as rm_secret_rec_flg " +
				"  ,to_char(r.debit_date, 'YYYY/MM/DD') as rm_debit_date " +
				"  ,to_char(r.payment_date, 'YYYY/MM/DD HH24:MI') as rm_payment_date " +
				"  ,b.balance_type_name as rm_balance_type_name " +
				"  ,pm.parllet_name as rm_parllet_name " +
				"  ,h.handling_name as rm_handling_name " +
				"  ,r.amount as rm_amount " +
				"  ,r.store as rm_store " +
				"  ,null as rm_balance_type_id " +
				"  ,null as rm_parllet_id " +
				"  ,r.payment_date as rm_payment_date_order " +
				" FROM Record r " +
				sqlJoinPhrase +
				" LEFT JOIN HandlingMst h " +
				"   ON r.handling_mst_id = h.id " +
				" LEFT JOIN HandlingTypeMst ht " +
				"   ON h.handling_type_mst_id = ht.id " +
				sqlWherePhrase +
				"   AND (   r.handling_mst_id IS NULL " +
						"OR ht.handling_type_name != '" + Common.HANDLING_TYPE_CRECA + "' " +
						") " +
				sqlSecretRecFlg +
				"";
		while(!(sql.equals(sql.replaceAll("  ", " "))))
			sql = sql.replaceAll("  ", " ");
		
		return sql;
	}
	
	
	
	public static class WebSocketApp extends WebSocketController {
		public static void listen() {
			// WebSocketが接続されている間、isbound.isOpen()はtrue
			while(inbound.isOpen()) {
				// クライアントから送られるメッセージを、継続を使って非同期で待ちます。
				Http.WebSocketEvent event = await(inbound.nextEvent());

				// メッセージがテキストであればfor内が実行されます。
				// パターンマッチにfor文を使うのは珍しいですね。
				for(String data : TextFrame.match(event)) {
					// クライアントにメッセージを返送します。(今のところ返送する意味はない。)
					outbound.send(data);
					
					ObjectMapper mapper = new ObjectMapper();
					// JSON文字列 を Bean に変換する
					RecSingleColumn eRec;
					try {
						eRec = mapper.readValue(data, RecSingleColumn.class);
						
//						// Bean の内容を標準出力に書き出す
//						System.out.println(bean);
//						// Bean を JSON文字列 に変換して標準出力に書き出す
//						mapper.writeValue(System.out, bean);

						// 変更
						if (eRec.act_type.equals("update")) {
							Record rec = Record.findById(eRec.id);
		    				
			    			// Validate
						    validation.valid(eRec);
						    if (validation.hasErrors()) {
						    	// 以下の描画では駄目かも？
//						        render(records, h_payment_date_fr, h_payment_date_to, h_item_id);
						    }
						    
						    // 支払日
						    if (eRec.edited_col.equals("payment_date")) {
			    				// 項目が変更されていた場合だけ更新
							    Date ePayDate = DateFormat.getDateInstance().parse(eRec.col_val);
			    				if (rec.payment_date != ePayDate) {
									rec.payment_date = ePayDate;
								    // 保存
								    rec.save();
			    				}
			    				
//						    // 収支種類
//						    } else if (eRec.edited_col.equals("balance_type_id")) {
//			    				// 項目が変更されていた場合だけ更新
//							    int eBTypeId = Integer.parseInt(eRec.col_val);
//			    				if (rec.balance_type_id != eBTypeId) {
//									rec.balance_type_id = eBTypeId;
//								    // 保存
//								    rec.save();
//			    				}
//						    	
						    }
						}
	    			} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JsonParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JsonMappingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
	}
}
