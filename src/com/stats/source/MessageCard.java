//$Id$
package com.stats.source;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageCard {
	private static final String senderImage = "https://img.zohostatic.com/chat/default/officechat/images/default/taz.png";
	private static final String cardType = "modern-inline";
	private static final String cardSenderName = "Anomaly Detector";
	private String text;
	private Bot bot;
	private Card card;
	private Buttons buttons;
	private Slides slides;
	class Bot{
		private String name;
		private String image;
		public Bot(String name, String image){
			this.name = name;
			this.image = image;
		}
		public JSONObject getStr() throws JSONException{
			return new JSONObject().put("name", name).put("image", image);
		}
	}
	class Card{
		private String title;
		private String theme;
		public Card(String title, String theme){
			this.title = title;
			this.theme = theme;
		}
		public JSONObject getStr() throws JSONException{
			return new JSONObject().put("title", title).put("theme", theme);
		}
	}
	class Buttons{
		private String label;
		private String type;
		private Action action;
		class Action{
			private String type;
			private String web;
			public Action(String type, String web){
				this.type = type;
				this.web = web;
			}
			public JSONObject getStr() throws JSONException{
				return new JSONObject().put("type", type).put("data", new JSONObject().put("web", web));
			}
		}
		public Buttons(String label,String type, String type1, String web){
			this.label = label;
			this.type = type;
			this.action = new Action(type1, web);
		}
		public JSONObject getStr() throws JSONException{
			return new JSONObject().put("label", label).put("type", type).put("action", action.getStr());
		}
	}
	class Slides{
		class Data{
			private String key;
			private String value;
			public Data(String key, String value){
				this.key = key;
				this.value = value;
			}
			public JSONObject getStr() throws JSONException{
				return new JSONObject().put(key, value);
			}
		}
		private String type;
		private String title;
		private Data[] data;
		public Slides(String type, String title){
			this.type = type;
			this.title = title;
			this.data = new Data[3];
		}
		public void setClassName(String key, String value){
			this.data[0] = new Data(key, value);
		}
		public void setMethodName(String key, String value){
			this.data[1] = new Data(key, value);
		}
		public void setExceptionType(String key, String value){
			this.data[2] = new Data(key, value);
		}
		private JSONArray getDataArray() throws JSONException{
			JSONArray arr = new JSONArray();
			arr.put(data[0].getStr());
			arr.put(data[1].getStr());
			arr.put(data[2].getStr());
			return arr;
		}
		public JSONObject getStr() throws JSONException{
			return new JSONObject().put("type", type).put("title", title).put("data", getDataArray());
		}
	}
	
	public MessageCard(String text, String excepURL, String className, String methodName, String exceptionType){
		try{
			this.text = text;
			this.bot = new Bot(cardSenderName,senderImage);
			this.card = new Card("Exception",cardType);
			this.buttons = new Buttons("View Exception", "-", "open.url", excepURL);
			this.slides = new Slides("label","");
			String[] splittedCName = className.contains(".") ? className.split("\\.") : new String[]{className};
			this.slides.setClassName("Class Name", splittedCName[splittedCName.length - 1]);
			String[] splittedMName = methodName.contains(".") ? methodName.split("\\.") : new String[]{methodName};
			this.slides.setMethodName("Method Name", splittedMName[splittedMName.length - 1]);
			String[] subType = exceptionType.contains(":") ? exceptionType.split(":") : null;
			String[] splittedEType = exceptionType.contains(".") ? exceptionType.split("\\.") : new String[]{exceptionType};
			this.slides.setExceptionType("Exception Type", (splittedEType[splittedEType.length - 1])+(subType != null && subType.length > 1 ? subType[1] : ""));
		}
		catch(Exception e){
			System.out.println("Exception while initializing message card");
		}
	}
	
	public String toString(){
		JSONObject jsonObj = new JSONObject();
		JSONArray slidesArr = new JSONArray();
		JSONArray buttonsArr = new JSONArray();
		try{
			jsonObj.put("text", text)
				.put("bot", bot.getStr())
				.put("card", card.getStr())
				.put("buttons", buttonsArr.put(buttons.getStr()))
				.put("slides", slidesArr.put(slides.getStr()));
		}
		catch(Exception e){
			System.out.println("Exception while generating message card");
		}
		return jsonObj.toString();
	}
	
}
