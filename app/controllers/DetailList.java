package controllers;

import java.util.*;

import models.*;

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

import models.*;

import static play.mvc.Http.WebSocketEvent.TextFrame;

@With(Secure.class)
public class DetailList extends Controller {
	
	@Before
	static void setConnectedUser() {
		if(Security.isConnected()) {
			HaUser haUser  = HaUser.find("byEmail", Security.connected()).first();
			renderArgs.put("haUser", haUser);
		}
	}
	
	static final String BALANCE_TYPE_IN = Messages.get("BalanceType.in");
	static final String BALANCE_TYPE_OUT = Messages.get("BalanceType.out");
	static final String BALANCE_TYPE_BANK_IN = Messages.get("BalanceType.bank_in");
	static final String BALANCE_TYPE_BANK_OUT = Messages.get("BalanceType.bank_out");
	static final String HANDLING_TYPE_CRECA = Messages.get("HandlingType.creca");
	
	public static void detailList(
    		int page,						/* 現在ページ */
    		Integer h_secret_rec_flg,		/* 絞込非公開フラグ */
    		String h_payment_date_fr,		/* 絞込支払日範囲（開始） */
    		String h_payment_date_to,		/* 絞込支払日範囲（終了） */
    		Long h_balance_type_id,  		/* 絞込収支種類ID */
    		Long h_handling_id,				/* 絞込取扱(実際)ID */
    		Long h_ideal_deposit_id,		/* 絞込取扱(My貯金)ID */
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
		if(srch==null) {
			//セッションに絞込条件が入っている時はそれぞれセット
			if((session.get("daBlFilExistFlg") != null) &&
					(session.get("daBlFilExistFlg").equals("true"))) {
		    	if(!session.get("hSecretRecFlg").equals(""))
		    		h_secret_rec_flg = Integer.parseInt(session.get("hSecretRecFlg"));
				h_payment_date_fr = session.get("hPaymentDateFr");
				h_payment_date_to = session.get("hPaymentDateTo");
		    	h_balance_type_id = null;
				if(!session.get("hBalanceTypeId").equals(""))
					h_balance_type_id = Long.parseLong(session.get("hBalanceTypeId"));
		    	h_handling_id = null;
				if(!session.get("hHandlingId").equals(""))
					h_handling_id = Long.parseLong(session.get("hHandlingId"));
				h_ideal_deposit_id = null;
				if(!session.get("hIdealDepositId").equals(""))
					h_ideal_deposit_id = Long.parseLong(session.get("hIdealDepositId"));
		    	h_item_id = null;
		    	if(!session.get("hItemId").equals(""))
		    		h_item_id = Long.parseLong(session.get("hItemId"));
		    	
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
		if(page == 0)
			page = 1;
	
		List<Record> records = null;
		List<BalanceTypeMst> bTypes = null;
		List<IdealDepositMst> iDepos = null;
		List<HandlingMst> handlings = null;
		BalanceTypeMst bTypeIn = null;
		BalanceTypeMst bTypeOut = null;
		List<ItemMst> itemsIn = null;
		List<ItemMst> itemsOut = null;
		
		long count = 0L;		//レコード数
		int iLinage = 30;		//１ページあたりの行数
		int nbPages = 0;		//ページ数
		
		// 「保存」ボタンが押された場合
		if(save != null) {
	    	if(save.equals("保存")) {
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
	    						false,
	    						null
	    						);
	    				
		    			// Validate
					    validation.valid(eRec);
					    if(validation.hasErrors()) {
					    	// 以下の描画では駄目かも？
					    	render(bTypes, handlings, iDepos, itemsIn, itemsOut, records, h_payment_date_fr, h_payment_date_to, h_balance_type_id, h_ideal_deposit_id, h_item_id);
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
//					    if(validation.hasErrors()) {
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
			//検索条件をセッションに保存
			session.put("daBlFilExistFlg", "true");
			session.put("hSecretRecFlg", h_secret_rec_flg==null ? "" : h_secret_rec_flg);
			session.put("hPaymentDateFr", h_payment_date_fr==null ? "" : h_payment_date_fr);
			session.put("hPaymentDateTo", h_payment_date_to==null ? "" : h_payment_date_to);
			session.put("hBalanceTypeId", h_balance_type_id==null ? "" : h_balance_type_id);
			session.put("hHandlingId", h_handling_id==null ? "" : h_handling_id);
			session.put("hIdealDepositId", h_ideal_deposit_id==null ? "" : h_ideal_deposit_id);
			session.put("hItemId", h_item_id==null ? "" : h_item_id);
				
			HaUser haUser = (HaUser)renderArgs.get("haUser");
			
			// 検索処理(BalanceTypeMst)
			bTypes = BalanceTypeMst.find("order by id").fetch();
			
			// 検索処理(IdealDepositMst)
			iDepos = IdealDepositMst.find("ha_user = ? order by order_seq", haUser).fetch();
			
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
			if((session.get("actionMode") != null) &&
					(session.get("actionMode").equals("Edit")))
				if(h_secret_rec_flg != null)
					if(h_secret_rec_flg != 0)
						sqlSrchRec += " and secret_rec_flg = " + (h_secret_rec_flg==2 ? "true" : "false");
			
			//  支払日範囲(自)
			if(h_payment_date_fr != null && !h_payment_date_fr.equals(""))
				sqlSrchRec += " and payment_date >= '" + h_payment_date_fr + " 00:00:00'";
			//  支払日範囲(至)
			if(h_payment_date_to != null && !h_payment_date_to.equals(""))
				sqlSrchRec += " and payment_date <= '" + h_payment_date_to + " 23:59:59'";
			//  引落日範囲(自)
			if(h_debit_date_fr != null && !h_debit_date_fr.equals(""))
				sqlSrchRec += " and debit_date >= '" + h_debit_date_fr + " 00:00:00'";
			//  引落日範囲(至)
			if(h_debit_date_to != null && !h_debit_date_to.equals(""))
				sqlSrchRec += " and debit_date <= '" + h_debit_date_to + " 23:59:59'";
			
			//  収支種類
			if(h_balance_type_id != null)
				if(h_balance_type_id != 0L)
					sqlSrchRec += " and balance_type_mst.id = " + h_balance_type_id;
			//  取扱(実際)
			if(h_handling_id != null)
				if(h_handling_id != 0)
					sqlSrchRec += " and handling_mst.id " +
							(h_handling_id==-1 ? "is null "
									: (h_handling_id==-2 ? "is not null "
											: "= " + h_handling_id));
			//  取扱(My貯金)
			if(h_ideal_deposit_id != null)
				if(h_ideal_deposit_id != 0)
					sqlSrchRec += " and ideal_deposit_mst.id " +
							(h_ideal_deposit_id==-1 ? "is null "
									: (h_ideal_deposit_id==-2 ? "is not null "
											: "= " + h_ideal_deposit_id));
			//  項目
			if(h_item_id != null)
				if(h_item_id != 0)
					sqlSrchRec += " and item_mst.id = " + h_item_id;
			
			sqlSrchRec += "" +
					((session.get("actionMode")).equals("View") ? " and secret_rec_flg = false " : "") +
					"";
			count = Record.count(sqlSrchRec);
			nbPages = (int) (Math.ceil((double)count/iLinage));
			sqlSrchRec += " order by payment_date, id";
			records = Record.find(
					sqlSrchRec).from(0).fetch(page, 30);
		}
		
		render(bTypes, handlings, iDepos, itemsIn, itemsOut, records, h_secret_rec_flg, h_payment_date_fr, h_payment_date_to, h_balance_type_id, h_handling_id, h_ideal_deposit_id, h_item_id, h_debit_date_fr, h_debit_date_to, count, nbPages, page);
    }
	
	public static void dl_remainderBank(
    		int page,						/* 現在ページ */
    		Integer h_secret_rec_flg,		/* 絞込非公開フラグ */
    		String h_debit_date_fr,			/* 絞込引落日範囲（開始） */
    		String h_debit_date_to,			/* 絞込引落日範囲（終了） */
    		Long h_handling_id,				/* 絞込取扱(実際)ID */
    		String srch						/* 「絞込」ボタン */
    		) {
		
		String sqlSrchRec = "";
		//意図的に絞り込まれていない時
		if(srch==null) {
			//セッションに絞込条件が入っている時はそれぞれセット
			if((session.get("daRbFilExistFlg") != null) &&
					(session.get("daRbFilExistFlg").equals("true"))) {
		    	if(!session.get("hSecretRecFlg").equals(""))
		    		h_secret_rec_flg = Integer.parseInt(session.get("hSecretRecFlg"));
				h_debit_date_fr = session.get("daRbHdDebitDateFr");
				h_debit_date_to = session.get("daRbHdDebitDateTo");
		    	h_handling_id = null;
				if(!session.get("daRbHdHandlingId").equals(""))
					h_handling_id = Long.parseLong(session.get("daRbHdHandlingId"));
		    	
		    //初回読み込み時は絞込引落日範囲は1か月前から現在日付
			} else {
	    		Calendar calendar = Calendar.getInstance();
	    		h_debit_date_to = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
	    		calendar.add(Calendar.MONTH, -1);
	    		h_debit_date_fr = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
			}
		}
		
		//初回読み込み時のページは１ページとする
		if(page == 0)
			page = 1;
	
		List<Record> records = null;
		List<HandlingMst> handlings = null;
		BalanceTypeMst bTypeIn = null;
		BalanceTypeMst bTypeOut = null;
		
		long count = 0L;		//レコード数
		int iLinage = 30;		//１ページあたりの行数
		int nbPages = 0;		//ページ数
		
		//検索条件をセッションに保存
		session.put("daRbFilExistFlg", "true");
		session.put("hSecretRecFlg", h_secret_rec_flg==null ? "" : h_secret_rec_flg);
		session.put("daRbHdDebitDateFr", h_debit_date_fr==null ? "" : h_debit_date_fr);
		session.put("daRbHdDebitDateTo", h_debit_date_to==null ? "" : h_debit_date_to);
		session.put("daRbHdHandlingId", h_handling_id==null ? "" : h_handling_id);
			
		HaUser haUser = (HaUser)renderArgs.get("haUser");
		
		// 検索処理(HandlingMst)
		handlings = HandlingMst.find("ha_user = ? and (handling_type_mst.handling_type_name = ? or handling_type_mst.handling_type_name = ?) order by handling_type_mst.handling_type_order, order_seq", haUser, Messages.get("HandlingType.bank"), Messages.get("HandlingType.emoney")).fetch();
		
		// 検索処理(Record)
		sqlSrchRec += "ha_user = " + haUser.id;
		//  非公開フラグ
		if((session.get("actionMode") != null) &&
				(session.get("actionMode").equals("Edit")))
			if(h_secret_rec_flg != null)
				if(h_secret_rec_flg != 0)
					sqlSrchRec += " and secret_rec_flg = " + (h_secret_rec_flg==2 ? "true" : "false");
		
		//  引落日範囲(自)
		if(h_debit_date_fr != null && !h_debit_date_fr.equals(""))
			sqlSrchRec += " and debit_date >= '" + h_debit_date_fr + " 00:00:00'";
		//  引落日範囲(至)
		if(h_debit_date_to != null && !h_debit_date_to.equals(""))
			sqlSrchRec += " and debit_date <= '" + h_debit_date_to + " 23:59:59'";
		
		//  取扱(実際)
		//意図的に絞り込まれていない時は口座・電子マネーの先頭のモノで絞り込む
		if(srch==null && h_handling_id==null)
			h_handling_id = handlings.get(0).id;
		sqlSrchRec += " and " +
				" ((    handling_mst.handling_type_mst.handling_type_name = '" + HANDLING_TYPE_CRECA + "' " +
				"   and handling_mst.debit_bank.id = " + h_handling_id +
				"   ) " +
				"  or handling_mst.id = " + h_handling_id +
				"  )"
				;
		
		sqlSrchRec += "" +
				((session.get("actionMode")).equals("View") ? " and secret_rec_flg = false " : "") +
				"";
		count = Record.count(sqlSrchRec);
		nbPages = (int) (Math.ceil((double)count/iLinage));
		sqlSrchRec += " order by debit_date, payment_date, id";
		records = Record.find(
				sqlSrchRec).from(0).fetch(page, 30);
//			System.out.println(records.get(0).payment_date);
		
		if(count>0) {
			// 収入：加算、支出：減算
			String sqlSumAllCaseInOut = "" +
					" WHEN b.balance_type_name = '" + BALANCE_TYPE_IN + "' THEN r.amount " +
					" WHEN b.balance_type_name = '" + BALANCE_TYPE_OUT + "' THEN -r.amount " +
					"";
			// 口座引出：減算、口座預入：加算
			String sqlSumNotCashCaseBankInOut = "" +
					" WHEN b.balance_type_name = '" + BALANCE_TYPE_BANK_OUT + "' THEN -r.amount" +
					" WHEN b.balance_type_name = '" + BALANCE_TYPE_BANK_IN + "' THEN r.amount" +
					"";
			
			String sql = "" +
					" SELECT " +
					"   COALESCE(SUM(" +
					"     CASE " +
					sqlSumAllCaseInOut +			//収入加算・支出減算
					sqlSumNotCashCaseBankInOut +	//口座引出減算・口座預入加算
					"     END" +
					"   ), 0) " +
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
					" WHERE r.ha_user_id = " + haUser.id +
					"   AND (" +
						" CASE " +
						"   WHEN ht.handling_type_name = '" + HANDLING_TYPE_CRECA + "' THEN hb.id " +
						"   ELSE h.id " +
						" END) = " + h_handling_id +
					"   AND b.balance_type_name in('" + BALANCE_TYPE_OUT + "','" + BALANCE_TYPE_IN + "','" + BALANCE_TYPE_BANK_OUT + "','" + BALANCE_TYPE_BANK_IN + "') " +
					"   AND (   cast(r.debit_date as timestamp) < to_timestamp('" + records.get(0).debit_date + "', 'YYYY-MM-DD HH24:MI:SS') " +
						"    OR (    cast(r.debit_date as timestamp) = to_timestamp('" + records.get(0).debit_date + "', 'YYYY-MM-DD HH24:MI:SS') " +
							"    AND (   cast(r.payment_date as timestamp) < to_timestamp('" + records.get(0).payment_date + "', 'YYYY-MM-DD HH24:MI:SS') " +
									" OR (    cast(r.payment_date as timestamp) = to_timestamp('" + records.get(0).payment_date + "', 'YYYY-MM-DD HH24:MI:SS') " +
										" AND r.id <= " + records.get(0).id +
										" )" +
									" )" +
							"    ) " +
						"    ) " +
					"   AND r.secret_rec_flg = FALSE " +
					"  " +
					"";
			BigInteger biRemainder = (BigInteger)JPA.em().createNativeQuery(sql).getSingleResult();
			
			Integer intRemainder = biRemainder.intValue();
			records.get(0).remainder = intRemainder;
			for(int i = 1;i < records.size();i++) {
				if(records.get(i).balance_type_mst.balance_type_name.equals(BALANCE_TYPE_IN) ||
						records.get(i).balance_type_mst.balance_type_name.equals(BALANCE_TYPE_BANK_IN))
					intRemainder += records.get(i).amount;
				if(records.get(i).balance_type_mst.balance_type_name.equals(BALANCE_TYPE_OUT) ||
						records.get(i).balance_type_mst.balance_type_name.equals(BALANCE_TYPE_BANK_OUT))
					intRemainder -= records.get(i).amount;
				records.get(i).remainder = intRemainder;
			}
		}
		
		render(h_debit_date_fr, h_debit_date_to, handlings, records, h_secret_rec_flg, h_handling_id, count, nbPages, page);
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
						if(eRec.act_type.equals("update")) {
							Record rec = Record.findById(eRec.id);
		    				
			    			// Validate
						    validation.valid(eRec);
						    if(validation.hasErrors()) {
						    	// 以下の描画では駄目かも？
//						        render(records, h_payment_date_fr, h_payment_date_to, h_item_id);
						    }
						    
						    // 支払日
						    if(eRec.edited_col.equals("payment_date")) {
			    				// 項目が変更されていた場合だけ更新
							    Date ePayDate = DateFormat.getDateInstance().parse(eRec.col_val);
			    				if (rec.payment_date != ePayDate) {
									rec.payment_date = ePayDate;
								    // 保存
								    rec.save();
			    				}
			    				
//						    // 収支種類
//						    } else if(eRec.edited_col.equals("balance_type_id")) {
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
