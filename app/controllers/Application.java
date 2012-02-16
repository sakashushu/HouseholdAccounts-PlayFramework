package controllers;

import play.*;
import play.mvc.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void detailList(String dateFr, String yearTo) {
    	String sQuely = "";
    	if(dateFr == null) {
    		Calendar calendar = Calendar.getInstance();
    		yearTo = String.format("%1$tY", calendar.getTime());
    		calendar.add(Calendar.YEAR, -1);
    		dateFr = String.format("%1$tY/%1$tm/%1$td", calendar.getTime());
    	}
   		sQuely = "payment_date between '" + dateFr + "' and '" + yearTo + "/12/31'";
    	sQuely += " order by payment_date desc";
    	
    	List<Record> records = Record.find(
    			sQuely).from(0).fetch(50);
    	
    	String dFr = dateFr;
    	String yTo = yearTo;
    	
    	render(records, dFr, yTo);
    }
	
    public static void dtlSrch(String yearFr) {
    }

    public static void dtlEdit() {
    }
    
}