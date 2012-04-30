package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class ItemMst extends Model {

	@Required
	@ManyToOne
	public HaUser ha_user;						//家計簿ユーザー
	
	@ManyToOne
	public ActualTypeMst actual_type_mst;
	
	public String item_name;
	
	public ItemMst(
			HaUser ha_user,
			ActualTypeMst actual_type_mst,
			String item_name
			) {
		this.ha_user = ha_user;
		this.actual_type_mst = actual_type_mst;
		this.item_name = item_name;
	}
	
	public String toString() {
        return item_name;
	}
}
