package com.stats.source;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ServeStats
 */
@WebServlet("/ServeStats")
public class ServeStats extends HttpServlet {
	private static String accessToken;
	private static final long serialVersionUID = 1L;
	private static final String logAuthToken = "*****";
	private static final String filePath = "/home/test/Documents/NotifLogAnomalies/";
	private static final String appid = "36064526";
	public static final String dQuote = "%22";
	public static final String logURL = "https://logs.zoho.com";
	private static final String service = "RecruitCrm";
	private static final String urlShrotenerURL = "https://www.googleapis.com/urlshortener/v1/url/?key=*****";
	private static JSONObject fileContent;
	public static final String urlResPath = logURL+"/search";
	public static final String urlLinkMessage = logURL+"/app/36064526/zlogs.zl#/zlh/RecruitCrm/search/query/date=Today&range=1-10&order=descending&query=logtype=%22application%22%20and%20throwable%20CONTAINS%20%22";
	private static HashMap<String,JSONObject> detectorClassesVsData = new HashMap<String,JSONObject>();
	public static ArrayList<String> invalidServers = new ArrayList<String>();
	public static String[][] staticExcludes = null;
    static{
    	try {
    		staticExcludes = new String[][]{{"RetryThread","run","ConnectException"},{"LogWriterProcessor","processQueueData","IOException"},{"HTTPRequestInvoker","invoke","ConnectException"},{"IAMRedisClusterNode","getResource","JedisConnectionException"}};
    		invalidServers.add("172.25.247.5");
    		invalidServers.add("172.25.237.145");
    		invalidServers.add("172.25.237.197");
    		invalidServers.add("172.25.237.232");
    		invalidServers.add("172.25.247.42");
    		initializeFileContent();
			detectorClassesVsData.put("com.stats.source.ClassStatsDetection", 
						new JSONObject()
							.put("URL", logURL+"/api/stats/classexceptionstats/day/")
							.put("total", "true")
							.put("appid", appid)
							.put("authtoken", logAuthToken)
							.put("service", service)
					);
		} catch (JSONException e) {}
    }
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServeStats() {
        super();
    }
    
    public static void initializeFileContent(){
    	try{
        	TimeZone gmtTZ = TimeZone.getTimeZone("PST");            
            Calendar toCal = new GregorianCalendar();
            toCal.setTimeInMillis(System.currentTimeMillis());
            Date dd = toCal.getTime();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(gmtTZ);
            String todayDate = format.format(dd);
            fileContent = (JSONObject)readFile(filePath+todayDate+".txt");
    	}
    	catch(Exception e){
    		fileContent = null;
    	}
    }
    
    public static void logPushedStatus(String Id, String todayDate, JSONObject value) throws Exception{
    	String url = filePath+todayDate+".txt";
    	JSONObject jsonObj = (JSONObject)readFile(url);
    	jsonObj.put(Id, value);
    	if(jsonObj != null){
    		writeToFile(url,jsonObj);
    	}
    }
    
    public static void writeToFile(String url, JSONObject writeObj) throws Exception{
    	File f = new File(filePath);
    	if(!f.exists()){
    		f.mkdirs();
    	}
    	f = new File(url);
    	if(!f.exists()){
    		f.createNewFile();
    	}
    	OutputStream os = new FileOutputStream(f.getAbsoluteFile());
    	byte[] bytes = writeObj.toString().getBytes();
    	os.write(bytes);
    	os.flush();
    	os.close();
    	fileContent = new JSONObject(writeObj.toString());
    }
    
    public static Object readFile(String url) throws Exception{
    	Object retObj = null;
    	File f = new File(url);
        if(f!=null && f.exists())
        {
        	InputStream is = new FileInputStream(f.getAbsoluteFile());
        	Scanner in = new Scanner(is);
        	if(in != null && in.hasNext()){
        		JSONObject currObj = new JSONObject(in.nextLine());
        		retObj = currObj;
        	}
        	is.close();
        	in.close();
        }
        return retObj == null ? new JSONObject() : retObj;
    }
    
    public static boolean getEligibilityToPush(String Id,String todayDate){
    	boolean retVal = false;
    	try
        {
    		JSONObject jsonObj = (fileContent == null || fileContent.length() == 0) ? (JSONObject)readFile(filePath+todayDate+".txt") : fileContent;
    		if(jsonObj == null || !jsonObj.has(Id)){
    			retVal = true;
    		}
        }
        catch(Exception e){
        	System.out.println(ExceptionUtils.getFullStackTrace(e));
        }
    	return retVal;
    }
    
    static class sendMessage implements Runnable{
    	
    	public sendMessage(Object messageObj){
    		this.messageObj = messageObj;
    	}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
				int xhrResp = sendMessageToChannel(messageObj);
				 if(xhrResp != 200){
					 xhrResp = refreshAccessTokenAndSend(messageObj);
				 }
			}
			catch(Exception e){
				System.out.println("Exception in message sending thread - "+ExceptionUtils.getFullStackTrace(e));
			}
		}
    	Object messageObj;
    }
    
    public static void sendMessage(Object obj) throws Exception{
//    	new ServeStats.sendMessage(obj).run();
    	int xhrResp = sendMessageToChannel(obj);
		if(xhrResp != 200 && xhrResp != 204){
			 xhrResp = refreshAccessTokenAndSend(obj);
		}
    }
    
    public static synchronized String getShortenedURL(String logURL) throws Exception{
    	URL url = new URL(urlShrotenerURL);
		HttpURLConnection httpcon = (HttpURLConnection) (url.openConnection());
		httpcon.setRequestProperty("Content-Type", "application/json");
		httpcon.setRequestMethod("POST");
		httpcon.setDoOutput(true);
		OutputStream os = httpcon.getOutputStream();
		os.write(((new JSONObject().put("longUrl", logURL)).toString()).getBytes("UTF-8"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copyLarge((InputStream)httpcon.getContent(), baos);
		JSONObject resp = new JSONObject(new String(baos.toByteArray()));
		os.flush();
		os.close();
		return (String)resp.get("id");
    }
    
    private static synchronized int refreshAccessTokenAndSend(Object obj) throws Exception{

    		String urlStr= "https://accounts.zoho.com/oauth/v2/token"; 
    		PostMethod getResult = new PostMethod(urlStr);
    		getResult.setParameter("refresh_token", "*****");
    		getResult.setParameter("grant_type", "refresh_token");
    		getResult.setParameter("scope", "ZohoCliq.Webhooks.CREATE");
    		getResult.setParameter("client_id", "****");
    		getResult.setParameter("client_secret", "****");
    		HashMap<String,Object> urlResult = new HashMap<String,Object>();
    		String resultStr = "";
    		HttpClient httpClient=new HttpClient();
    		MultiThreadedHttpConnectionManager cm = new MultiThreadedHttpConnectionManager();
    		cm.getParams().setConnectionTimeout(30000);
    		httpClient.setHttpConnectionManager(cm);
    		httpClient.executeMethod(getResult);
			resultStr = getResult.getResponseBodyAsString();
			urlResult.put("response", resultStr);
    		accessToken = (String)(new JSONObject((String)urlResult.get("response"))).get("access_token");
    		return sendMessageToChannel(obj);
    }
    
    private static synchronized int sendMessageToChannel(Object obj) throws Exception{
    		int x1 = 0;
    		String urlStr= "https://cliq.zoho.com/api/v2/channelsbyname/exceptiontrends/message";
			URL url  =new URL(urlStr);
			HttpURLConnection httpcon = (HttpURLConnection) (url.openConnection());
			httpcon.setRequestProperty("Authorization", "Zoho-oauthtoken "+accessToken);
			httpcon.setRequestProperty("Content-Type", "application/json");
			httpcon.setRequestMethod("POST");
			httpcon.setDoOutput(true);
			OutputStream os = httpcon.getOutputStream();
			os.write((obj.toString()).getBytes("UTF-8"));
			x1 = httpcon.getResponseCode();
			os.flush();
			os.close();
			return x1;
    	
    }
    
    public static boolean passStaticChecks(String className, String method, String excepClass){
    	for(int i=0;i<staticExcludes.length;i++){
    		String[] staticExclude = staticExcludes[i];
    		if(className != null && className.contains(staticExclude[0]) && method != null && method.contains(staticExclude[1]) && excepClass != null && excepClass.contains(staticExclude[2])){
        		return false;
        	}
    	}
    	return true;
    }
    
    public static Object getDataFromLogs(String urlStr) throws Exception{
		GetMethod getResult = new GetMethod(urlStr);
		HttpClient httpClient=new HttpClient();
		MultiThreadedHttpConnectionManager cm = new MultiThreadedHttpConnectionManager();
		cm.getParams().setConnectionTimeout(60000);
		httpClient.setHttpConnectionManager(cm);
		httpClient.executeMethod(getResult);
		InputStream resultStr = getResult.getResponseBodyAsStream();
		int x1 = getResult.getStatusCode();
		if(x1 == 200){
			return resultStr;
		}
		return null;
    }
    

    @SuppressWarnings("rawtypes")
	void startAnamolyDetection(){
    	
    		detectorClassesVsData.forEach((String x,JSONObject y) -> {
    			try{
    				Constructor constructor = Class.forName(x).getConstructor(JSONObject.class);
    				Object classObj = constructor.newInstance(y);
        	   		if(classObj != null){
        	       		new Thread((Runnable)classObj).run();
        	   		}
    			}
    	    	catch(Exception e){
    	    		System.out.println(ExceptionUtils.getFullStackTrace(e));
    	    		System.exit(1);
    	    	}
    		});
	   	
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		startAnamolyDetection();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request,response);
	}

}

