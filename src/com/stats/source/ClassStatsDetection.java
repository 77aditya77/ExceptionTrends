//$Id$
package com.stats.source;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class ClassStatsDetection implements Runnable{
	
	JSONObject queryObj;
	String fromDate;
	String todayDate;
	String todayDateTime;

	public ClassStatsDetection(){
		queryObj = null;
		fromDate = "";
		todayDate = "";
		todayDateTime = "";
	}
	
	public ClassStatsDetection(JSONObject obj){
		queryObj = obj;
	}
	
	private String constructQueryURL(String currExceptionClass) throws Exception{
		StringBuilder queryUrl = new StringBuilder(ServeStats.urlResPath);
		queryUrl.append("?service=RecruitCrm&appid=36064526&authtoken=****&range=1-500&fromDateTime="+todayDateTime+"%2000:00&toDateTime="+todayDateTime+"%2023:59&query=logtype=%22application%22%20and%20class_name%20CONTAINS%20%22"+currExceptionClass+"%22%20and%20IsNotEmpty(throwable)%20and%20%28group_name%3D%22sas%22%20or%20group_name%3D%22recruit4%22%20or%20group_name%3D%22recruit5%22%20or%20group_name%3D%22rec4_aspose%22%20or%20group_name%3D%22recruitkafkaservice%22%29");
		return queryUrl.toString();
	}
	
//	private String constructQueryURL(String currExceptionClass) throws Exception{
//		StringBuilder queryUrl = new StringBuilder(ServeStats.urlResPath);
//		queryUrl.append("?service=RecruitCrm&appid=36064526&authtoken=95be91ea11e80bd32eca978c0a0140f9&range=1-10&fromDateTime="+todayDateTime+"%2000:00&toDateTime="+todayDateTime+"%2023:59&query=logtype=%22application%22%20and%20throwable%20CONTAINS%20%22"+currExceptionClass+"%22%20groupbyclass_name");
//		return queryUrl.toString();
//	}
	
	private String constructURL(boolean classEnable) throws Exception{
		StringBuilder url = new StringBuilder();
		url.append(ServeStats.logURL+"/api/stats/"+(classEnable ? "class" : "")+"exceptionstats/day/?");
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
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
        format.setTimeZone(gmtTZ);
        format1.setTimeZone(gmtTZ);
        fromDate = format.format(dd);
		url.append("date="+fromDate+"&");
        toCal.add(Calendar.DAY_OF_YEAR, 14);
        dd = toCal.getTime();
        todayDate = format.format(dd);
        todayDateTime = format1.format(dd);
		url.append("date="+todayDate);
		return url.toString();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run() {
		int itercount = 0;
		while(true){
			System.out.println("Iter count - "+itercount+" Time - "+new Date(System.currentTimeMillis())+"\n");
			try{
				Object xhrResp = null;
				try{
					xhrResp = ServeStats.getDataFromLogs(constructURL(false));
				}
				catch(Exception exp1){
					System.out.println("Exception at ServeStats.getDataFromLogs(constructURL(false)) - "+ExceptionUtils.getFullStackTrace(exp1));
					Thread.sleep(100);
					continue;
				}
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				IOUtils.copyLarge((InputStream)xhrResp, baos);
				String xhrRespStr = new String(baos.toByteArray());
				JSONObject jsonResp = new JSONObject(xhrRespStr.toString());
				JSONArray explistArr = (JSONArray)jsonResp.get("result");
				baos = null;xhrResp = null;xhrRespStr = null;jsonResp = null;
				
				try{
				xhrResp = ServeStats.getDataFromLogs(constructURL(true));
				}
				catch(Exception exp2){
					System.out.println("Exception at ServeStats.getDataFromLogs(constructURL(true)) - "+ExceptionUtils.getFullStackTrace(exp2));
					Thread.sleep(100);
					continue;
				}
				baos = new ByteArrayOutputStream();
				IOUtils.copyLarge((InputStream)xhrResp, baos);
				xhrRespStr = new String(baos.toByteArray());
				jsonResp = new JSONObject(xhrRespStr.toString());
				JSONArray result = (JSONArray)jsonResp.get("result");
				baos = null;xhrResp = null;xhrRespStr = null;jsonResp = null;
				
				for(int resCount = 0; resCount < result.length() && result.length() < 500; resCount++){
					JSONObject resObj = (JSONObject)result.get(resCount);
					JSONArray listArr = (JSONArray)resObj.get("List");
					String currExceptionClass = (String)resObj.get("CLASS_NAME");
					double initialCount = 0;
					double avg = 0;
					int iter = 0;
					for(;iter < listArr.length(); iter++){
						JSONObject listObj = (JSONObject)listArr.get(iter);
						if(listObj != null && listObj.has("COUNT")){
							if(iter == 0){
								initialCount = Long.valueOf((Integer)listObj.get("COUNT"));
							}
							else{
								avg += Long.valueOf((Integer)listObj.get("COUNT"));
							}
						}
					}
					avg = avg/(iter-1);
					avg = avg == 0 ? 0.01 : avg;
					if(initialCount > avg){
						double percSpike = ((initialCount - avg)/avg)*100;
							try{
								try{
								xhrResp = ServeStats.getDataFromLogs(constructQueryURL(currExceptionClass));
								}
								catch(Exception exp3){
									System.out.println("Exception at ServeStats.getDataFromLogs(constructQueryURL(currExceptionClass)) - "+ExceptionUtils.getFullStackTrace(exp3));
									continue;
								}
								baos = new ByteArrayOutputStream();
								IOUtils.copyLarge((InputStream)xhrResp, baos);
								xhrRespStr = new String(baos.toByteArray());
								JSONObject queryDetails = new JSONObject(xhrRespStr);
								baos = null;xhrResp = null;xhrRespStr = null;
								if(queryDetails != null && queryDetails.has("docs")){
									JSONArray jsonArr = (JSONArray)queryDetails.get("docs");
									if(jsonArr != null && jsonArr.length() > 0){
										
										
										//This is for finding top throwable in the current class and throwable's unique users
										HashMap<String,Long> repCount = new HashMap<String,Long>();
										for(int count = 0;count<jsonArr.length();count++){
											JSONObject currObj = (JSONObject)jsonArr.get(count);
											String methName = (String)currObj.get("method");
											String throwStr = (String)currObj.get("throwable");
											String key = methName+"|"+(throwStr.contains("\n") ? throwStr.split("\n")[0] : throwStr);
											if(repCount.get(key) == null){
												repCount.put(key, 1l);
											}
											else{
												Long curr = repCount.get(key);
												long currLong = curr + 1;
												repCount.put(key,currLong);
												curr = null;
											}
										}
										int currMax = 0;
										String currThrowStr = null;
										Iterator keyIter = repCount != null ? repCount.keySet().iterator() : null;
										while(keyIter != null && keyIter.hasNext()){
											String currKey = (String)keyIter.next();
											Long occLongVal = (Long)repCount.get(currKey);
											if(occLongVal != null && occLongVal.intValue() > currMax){
												currMax = occLongVal.intValue();
												currThrowStr = currKey;
											}
										}
										
										
										
										if(currThrowStr != null && currThrowStr.contains("Exception")){
											JSONArray currExpArray = null;
											for(int exCount = 0; exCount < explistArr.length(); exCount++){
												JSONObject expObj = (JSONObject)explistArr.get(exCount);
												String curExpName = (String)expObj.get("EXCEPTION_NAME");
												String errName = (String)expObj.get("ERROR_NAME");
												if(errName != null && !errName.equals("-1")){
													curExpName = curExpName +": "+ errName;
												}
												
												//Check this exception history and find average spike
												if(curExpName != null && currThrowStr.contains(curExpName)){
													currExpArray = (JSONArray)expObj.get("List");

													if(currExpArray != null && currExpArray.length() > 0){
														double initialExpCount = 0;
														double avgExp = 0;
														int iterExp = 0;
														for(;iterExp < currExpArray.length(); iterExp++){
															JSONObject listObj = (JSONObject)currExpArray.get(iterExp);
															if(listObj != null && listObj.has("COUNT")){
																if(iterExp == 0){
																	initialExpCount = Long.valueOf((Integer)listObj.get("COUNT"));
																}
																else{
																	avgExp += Long.valueOf((Integer)listObj.get("COUNT"));
																}
															}
														}
														avgExp = avgExp/(iterExp-1);
														avgExp = avgExp == 0 ? 0.01 : avgExp;
														double percExpSpike = (initialExpCount > avgExp) ? (((initialExpCount - avgExp)/avgExp)*100) : 0;
														if(percExpSpike == 0){
															initialExpCount = 0;
														}
														//percSpike - Class appeared in logs prints spike percent
														//initialCount - How many times class appeared
														//perExpSpike - Current exception spike percent
														//initialExpCount - How many times exception appeared regardless of class
														//currMax - Maximum times this exception occurred in the present class
														boolean classSpikeFact = percSpike > 500 && initialCount > 50;
														boolean classSpikeFact1 = percExpSpike == 0 || classSpikeFact;
														boolean classSpikeFact2 = initialExpCount == 0 || classSpikeFact;
														//For below condition - 
														//If current exception count is > 500 and In this class that exception count is > 75 then consider it
														//(or)
														//In the same way for values  - current exception count between 50-500 and class exception count > 30 
														boolean conditionForExpInClass = (initialExpCount > 500 && currMax > 75) || (initialExpCount > 50 && initialExpCount < 500 && currMax > 30);
														if(classSpikeFact || (percExpSpike > 120 && conditionForExpInClass)){
															double printSpike = new BigDecimal(classSpikeFact1 ? String.valueOf(percSpike) : String.valueOf(percExpSpike)).setScale(1, BigDecimal.ROUND_HALF_EVEN).doubleValue();
															int totalCount = new BigDecimal(classSpikeFact2 ? String.valueOf(initialCount) : String.valueOf(initialExpCount)).setScale(1, BigDecimal.ROUND_HALF_EVEN).intValue();
															String methodName = currThrowStr.split("\\|")[0];
															String excepName = currThrowStr.split("\\|")[1];
															if(ServeStats.getEligibilityToPush(excepName, todayDate) && ServeStats.passStaticChecks(currExceptionClass,methodName,excepName)){
																String logURL = null;
																try{
																	String queryExpClass = excepName.contains(" ") ? excepName.split(" ")[0] : excepName;
																	logURL = ServeStats.getShortenedURL(ServeStats.logURL+"/app/36064526/zlogs.zl#/zlh/RecruitCrm/search/query/date="+todayDateTime.replaceAll("/", "%252F")+" 00:00 23:59&range=1-100&order=descending&query=logtype=\"application\" and throwable CONTAINS \""+queryExpClass+"\""+" and method CONTAINS \""+methodName+"\" and class_name CONTAINS \""+currExceptionClass+"\"");
														    	}
														    	catch(Exception e){
														    		System.out.println("Exception occurred when getting shortened URL\n");
														    	}
																if(logURL == null || logURL.isEmpty()){
																	String queryExpStr = excepName.length() > 70 ? excepName.substring(0, 70) : excepName;
																	queryExpStr = URLEncoder.encode(queryExpStr, "UTF-8");
																	logURL = ServeStats.urlLinkMessage+queryExpStr+ServeStats.dQuote;
																}
																MessageCard obj = new MessageCard("Spike of "+(printSpike > 1000 ? ">1000" : printSpike)+"% on the "+(classSpikeFact1 ? "log prints from the class below" : " below exception")+". Today's count - "+totalCount, logURL, currExceptionClass, methodName, curExpName);
																try{
																ServeStats.sendMessage(obj);
																}
																catch(Exception exp4){
																	System.out.println("Exception at ServeStats.sendMessage(obj) - class - "+currExceptionClass+" - curExpName - "+curExpName+" - "+ExceptionUtils.getFullStackTrace(exp4));
																}
																ServeStats.logPushedStatus(excepName, todayDate, new JSONObject().put("class_name",methodName));
															}
														}
													}
												}
											}
										}
									}
								}
							}
							catch(Exception e){
								System.out.println("Exception occurred when sending message after spike for class "+currExceptionClass+". Exception - "+ExceptionUtils.getFullStackTrace(e));
							}
					}
				}
				explistArr = null;explistArr = null;
				itercount++;
			}
			catch(Exception e){
				 System.out.println(ExceptionUtils.getFullStackTrace(e));
				 itercount++;
			}
			System.out.println("Done time - "+new Date(System.currentTimeMillis())+"\n\n");
			try {
				Thread.sleep(900000);
			} catch (InterruptedException e) {
				System.out.println(ExceptionUtils.getFullStackTrace(e));
			}
		}
	}
	
}