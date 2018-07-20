//$Id$
package com.stats.source;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class APICallsDetection implements Runnable{
	
	JSONObject queryObj;
	String fromDate;
	String todayDate;

	public APICallsDetection(){
		queryObj = null;
		fromDate = "";
		todayDate = "";
	}
	
	public APICallsDetection(JSONObject obj){
		queryObj = obj;
	}
	
	private String constructURL() throws Exception{
		StringBuilder url = new StringBuilder();
		url.append(queryObj.get("URL")+"?");
		url.append("total="+queryObj.get("total")+"&");
		url.append("appid="+queryObj.get("appid")+"&");
		url.append("authtoken="+queryObj.get("authtoken")+"&");
		url.append("service="+queryObj.get("service")+"&");
		TimeZone gmtTZ = TimeZone.getTimeZone("PST");            
        Calendar toCal = new GregorianCalendar();
        toCal.setTimeInMillis(System.currentTimeMillis());
        toCal.add(Calendar.DAY_OF_YEAR, -14);
        Date dd = toCal.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(gmtTZ);
        fromDate = format.format(dd);
		url.append("date="+fromDate+"&");
        toCal.add(Calendar.DAY_OF_YEAR, 14);
        dd = toCal.getTime();
        todayDate = format.format(dd);
		url.append("date="+todayDate);
		return url.toString();
	}

	@Override
	public void run() {
		try{
			Object xhrResp = ServeStats.getDataFromLogs(constructURL());
			xhrResp = IOUtils.toString((InputStream)xhrResp);
			JSONObject jsonResp = new JSONObject(xhrResp.toString());
			JSONArray result = (JSONArray)jsonResp.get("result");
			JSONObject resObj = (JSONObject)result.get(0);
			JSONArray listArr = (JSONArray)resObj.get("List");
			double initialCount = 0;
			double avg = 0;
			int iter = 0;
			for(;iter < listArr.length(); iter++){
				JSONObject listObj = (JSONObject)listArr.get(iter);
				if(listObj != null && listObj.has("API")){
					if(iter == 0){
						initialCount = Long.valueOf((Integer)listObj.get("API"));
					}
					else{
						avg += Long.valueOf((Integer)listObj.get("API"));
					}
				}
			}
			avg = avg/iter;
			
			if(initialCount > avg){
				double percSpike = ((initialCount - avg)/avg)*100;
				if(percSpike > 100 && ServeStats.getEligibilityToPush("APICallsDetection",todayDate)){
					ServeStats.sendMessage(constructMessage(percSpike,initialCount));
					ServeStats.logPushedStatus("APICallsDetection",todayDate,new JSONObject().put("todayCount",String.valueOf(initialCount)));
				}
			}
		}
		catch(Exception e){
			 System.out.println(ExceptionUtils.getFullStackTrace(e));
		}
	}
	
	public Object constructMessage(double percSpike, double initialCount){
		String message = "Today there is a spike anomaly in number of API requests by "+percSpike+"%. Total number of requests as of now is "+initialCount;
		return message;
	}
	
}