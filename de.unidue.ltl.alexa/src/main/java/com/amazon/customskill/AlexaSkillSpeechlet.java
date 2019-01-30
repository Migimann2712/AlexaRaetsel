package com.amazon.customskill;

import java.sql.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;

import nlp.dkpro.backend.PosTagger;
import nlp.dkpro.backend.NlpSingleton;

public class AlexaSkillSpeechlet implements SpeechletV2 {
	
    public static String userRequest;
    Connection con = null;
    
    //Id der letzten Rätsel
    int lastKinderrätsel;
    int lastErwachsenenrätsel;
    
    //Anzahl der erhaltenen Tipps
    int tippsErhalten = 0;
      
    // Index zum Aufrufen
    ArrayList<Integer> rätselFertig=new ArrayList<Integer>();
    
    int zufallszahlK;
    int zufallszahlE;
    int fertigeRätsel=0;
    
    // Gibt letzte Antwort von Alexa an
    String alexaResponse = "welcome message";
    
    String[] richtigeAntwort = {"Das ist richtig.", "Super.", "Korrekt.", "Toll gemacht.", "Sehr gut.", "Klasse."};
    String[] nächstesRätsel = {" Hier ist das nächste Rätsel: ", " Jetzt kommt das nächste Rätsel: ", " Weiter gehts: ", " Wie wärs damit: ", " Versuch das mal: ", " Mal sehen ob du das rausbekommst: "};
    String[] ende = {" War schön mit dir gerätselt zu haben.", " Bis zum nächsten mal.", " Lass uns bald wieder rätseln.", " Das Rätseln mit dir hat Spaß gemacht. Bis dann."};
    String[] überspringe = {"Dann eben nicht. Mal sehen ob du das hier schon kennst: ", "Na gut. Wie wärs dann hiermit: ", "Schade. Probier das mal: ", "Dann halt nicht, Wie wäre es damit: "};   
    String[] spielregeln = {"Alles klar, du Allwissender. ", "Okay du Schlaumeier. ", "Na gut, wenn du die schon kennst. ", "Dann halt nicht. "};
    String[] falscheAntwort = {"Leider falsch. Versuch es noch einmal.", "Das war leider nicht richtig.", "Das war leider die falsche Antwort.", "Die Antwort war falsch. Denk nochmal drüber nach.", "Leider nicht richtig. Probiers nochmal."};
    
    static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);
    private PosTagger p;

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope)
    {
        p = NlpSingleton.getInstance();
        logger.info("Alexa session begins");
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope)
    {
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope)
    {
        IntentRequest request = requestEnvelope.getRequest();
        Intent intent = request.getIntent();
        
        userRequest = intent.getSlot("Alles").getValue();
        
        System.out.println("UserRequest: " + userRequest);
        System.out.println("AlexaResponse: " + alexaResponse);
        System.out.println("ZufallszahlK " + zufallszahlK);
        System.out.println("ZufallszahlE " + zufallszahlE);
        System.out.println(rätselFertig.toString());
        System.out.println("lastE: " + lastErwachsenenrätsel);
        System.out.println("lastK: " + lastKinderrätsel);
        System.out.println("fertig: " + fertigeRätsel);
        
        
        String[] kinderRätsel = getRaetsel("kinderrätsel", zufallszahlK);
        String[] erwachsenenRätsel = getRaetsel("erwachsenenrätsel", zufallszahlE);
        
        System.out.println(kinderRätsel[1]);
        System.out.println(erwachsenenRätsel[1]);
        
        
        logger.info("Received following text: [" + userRequest + "]");
        
        // Falls man nicht mehr spielen möchte
        if(userRequest.contains("schluss") || userRequest.contains("stopp") || userRequest.contains("ende") || userRequest.contains("aufhören") || userRequest.contains("hör auf") || userRequest.contains("keine Lust")) {
        	alexaResponse = "welcome message";
        	rätselFertig.clear();
        	// Falls man weniger als 2 Rätsel geschafft hat (zum Spaß)
        	if(fertigeRätsel < 3) {
            	fertigeRätsel=0;       	
            	return response("Viel hast du ja nicht geschafft. Trotzdem bis zum nächsten Mal.");
        	}
        	else {
            	fertigeRätsel = 0;
            	return response(ende[(int)(Math.random()*ende.length)]);
        	}
        }
        
        // Nach Welcome Message fragt Alexa nach Spielregeln
        else if(alexaResponse.equals("welcome message")) {
        	return askUserResponse(responseSpielregeln(userRequest));
        }
        
        // Nach Spielregeln fragt Alexa nach Rätselart
        else if(alexaResponse.equals("Spielregeln")) {
        	return askUserResponse(responseRaetselart(userRequest,kinderRätsel,erwachsenenRätsel));
        }
        
        // Nachdem man Kinderrätsel gewählt oder Tipp bekommen hat
        else if(alexaResponse.equals("Kinderrätsel") || alexaResponse.equals("Kinderrätsel_Tipp") ) {
        	if(fertigeRätsel >= lastKinderrätsel && (userRequest.contains(kinderRätsel[1]) || userRequest.contains("lösung")))
            	return response(responseKinderRaetsel(userRequest, kinderRätsel));
        	else 
        		return askUserResponse(responseKinderRaetsel(userRequest, kinderRätsel));
        }
        
        // Nachdem man Erwachsenenrätsel gewählt oder Tipp bekommen hat
        else if(alexaResponse.equals("Erwachsenenrätsel") || alexaResponse.equals("Erwachsenenrätsel_Tipp")) {
        	if(fertigeRätsel >= lastErwachsenenrätsel && (userRequest.contains(erwachsenenRätsel[1]) || userRequest.contains("lösung")))
            	return response(responseErwachsenenRaetsel(userRequest, erwachsenenRätsel));
        	else
        		return askUserResponse(responseErwachsenenRaetsel(userRequest,erwachsenenRätsel));
        }
        else return response("Falsch");
    }
   
    // Spielregeln hören oder nicht
    private String responseSpielregeln(String request) {
    	String result = "";
    	if(request.contains("Ja")) { 
    		alexaResponse = "Spielregeln";
    		result = "Okay. Mit Tipp kann ich dir bis zu zwei Tipps geben!"
    				+ "Du kannst Rätsel überspringen oder wiederholen! "
    				+ "Mit 'Lösung' sag ich dir die Lösung des Rätsels und falls wir unser Spiel beenden sollen, sag einfach: Schluss. "
    				+ "Das wärs dann auch schon. Möchtest du ein Kinder- oder Erwachsenenrätsel?";   		
    	}
    	else if(request.contains("nein")) {
    		alexaResponse = "Spielregeln";
    		result = spielregeln[(int)(Math.random()*spielregeln.length)] + "Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	}
    	else 
    		result = "Das habe ich leider nicht verstanden. Soll ich dir die Spielregeln erklären?";
    	
    	return result;
    }
    
    // Rätselart wählen
    private String responseRaetselart(String request, String[] kinderRätsel, String[] erwachsenenRätsel) {
    	String result = "";
    	if(request.contains("kinder rätsel") || request.contains("kinder")) {
    		alexaResponse = "Kinderrätsel";       		
    		//Fall 1. Kinderrätsel
    		result = "Hier ist das erste Kinderrätsel: " + kinderRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
			rätselFertig.add(zufallszahlK);
    		
    	}	
    		
    	else if (request.contains("erwachsenen rätsel") || request.contains("erwachsenen")) {
    		alexaResponse = "Erwachsenenrätsel";   		
    		//Fall 1. Erwachsenenrätsel
    		result = "Hier ist das erste Erwachsenenrätsel: " + erwachsenenRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
			rätselFertig.add(zufallszahlE);
    		
    	}
    	else result = "Das habe ich leider nicht verstanden. Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	
    	return result;
    }
    
    // Erwachsenen Rätsel  
    private String responseErwachsenenRaetsel(String request, String[] rätsel) {
    	String result = "";
    	
    	// Rätsel holen
    	if(request.contains("wiederhol") || request.contains("noch mal")) 
    		result = rätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    	
    	// Lösung ausgeben
    	else if(request.contains("lösung") || request.contains("keine ahnung") || request.contains("keinen plan") || request.contains("weiß nicht") || request.contains("fällt mir nicht ein") || request.contains("keine idee")|| request.contains("nicht drauf") || request.contains("nicht sagen") || request.contains("keinen blassen schimmer") || request.contains("auf dem schlauch") || request.contains("ratlos")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel >= lastErwachsenenrätsel) {
    			result = "Die Lösung ist " + rätsel[1] + "." + ende[(int)(Math.random()*ende.length)];
    			alexaResponse = "welcome message";
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
        		zufallszahlE = (int) (Math.random()*(lastErwachsenenrätsel+1));
        		while(rätselFertig.contains(zufallszahlE)) {
        			zufallszahlE = (int) (Math.random()*(lastErwachsenenrätsel+1));
        		}
        		String[] nextRätsel = getRaetsel("erwachsenenrätsel", zufallszahlE);
        		result = "Die Lösung ist " + rätsel[1] + "." + nächstesRätsel[(int)(Math.random()*nächstesRätsel.length)] + nextRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
        		fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlE);
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspring") || request.contains("nächste")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel >= lastErwachsenenrätsel) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";	
    		else {
    	        zufallszahlE=(int) (Math.random()*(lastErwachsenenrätsel+1));
    	        while(rätselFertig.contains(zufallszahlE)) {
        			zufallszahlE = (int) (Math.random()*(lastErwachsenenrätsel+1));
        		}
    			String[] nextRätsel= getRaetsel("erwachsenenrätsel", zufallszahlE); 
        		result = überspringe[(int)(Math.random()*überspringe.length)] + nextRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
        		fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlE);
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(rätsel[1])) {			
    		if(fertigeRätsel >= lastErwachsenenrätsel) {
    			result= "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_positive_response_01'/>" + richtigeAntwort[(int)(Math.random()*richtigeAntwort.length)] + ende[(int)(Math.random()*ende.length)];
    			alexaResponse = "welcome message";
    			fertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
    	        zufallszahlE=(int) (Math.random()*(lastErwachsenenrätsel+1));
    	        while(rätselFertig.contains(zufallszahlE)) {
        			zufallszahlE = (int) (Math.random()*(lastErwachsenenrätsel+1));
        		}
    			String[] nextRätsel = getRaetsel("erwachsenenrätsel", zufallszahlE); 
    			result ="<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_positive_response_01'/>" + richtigeAntwort[(int)(Math.random()*richtigeAntwort.length)] + nächstesRätsel[(int)(Math.random()*nächstesRätsel.length)] + nextRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			fertigeRätsel++;
    			tippsErhalten = 0;
    			rätselFertig.add(zufallszahlE);
    		}	
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp") || request.contains("hilf") || request.contains("hinweis")) {	
    		alexaResponse = "Erwachsenenrätsel_Tipp";
    		if(tippsErhalten==0) {
    			result = rätsel[2] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			tippsErhalten++;
    		}
    		else if(tippsErhalten==1) {
    			result = rätsel[3] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			tippsErhalten++;
    		}
    		else
    			result = "Du hast bereits alle Tipps erhalten. <audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_negative_response_01'/> " + falscheAntwort[(int)(Math.random()*falscheAntwort.length)] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    	
    	return result;
    }
   
    
    // Kinder Rätsel
    private String responseKinderRaetsel(String request, String[] rätsel) {
    	String result = "";
    	// Rätsel wiederholen
    	if(request.contains("wiederhol") || request.contains("noch mal")) 
    		result = rätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    	
    	// Lösung ausgeben
    	else if(request.contains("lösung") || request.contains("keine ahnung") || request.contains("keinen plan") || request.contains("weiß nicht") || request.contains("fällt mir nicht ein") || request.contains("keine idee")|| request.contains("nicht drauf") || request.contains("nicht sagen") || request.contains("keinen blassen schimmer") || request.contains("auf dem schlauch") || request.contains("ratlos")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel >= lastKinderrätsel) {
    			result = "Die Lösung ist " + rätsel[1] + "." + ende[(int)(Math.random()*ende.length)];
    			alexaResponse = "welcome message";
    			fertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
        		zufallszahlK = (int) (Math.random()*(lastKinderrätsel+1));
        		while(rätselFertig.contains(zufallszahlK)) {
        			zufallszahlK = (int) (Math.random()*(lastKinderrätsel+1));
        		}
        		String[] nextRätsel = getRaetsel("kinderrätsel", zufallszahlK);
        		result = "Die Lösung ist " + rätsel[1] + ". " + nächstesRätsel[(int)(Math.random()*nächstesRätsel.length)] + nextRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
        		fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlK);
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspring") || request.contains("nächste")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel >= lastKinderrätsel) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";	
    		else {
    			zufallszahlK = (int) (Math.random()*(lastKinderrätsel+1));
        		while(rätselFertig.contains(zufallszahlK)) {
        			zufallszahlK = (int) (Math.random()*(lastKinderrätsel+1));
        		}
    			String[] nextRätsel = getRaetsel("kinderrätsel", zufallszahlK); 
        		result = überspringe[(int)(Math.random()*überspringe.length)] + nextRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlK);
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(rätsel[1])) {	
    		if(fertigeRätsel >= lastKinderrätsel) {
    			result ="<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_positive_response_01'/>" + richtigeAntwort[(int)(Math.random()*richtigeAntwort.length)] + ende[(int)(Math.random()*ende.length)];
    			alexaResponse = "welcome message";
    			fertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
    			zufallszahlK = (int) (Math.random()*(lastKinderrätsel+1));
        		while(rätselFertig.contains(zufallszahlK)) {
        			zufallszahlK = (int) (Math.random()*(lastKinderrätsel+1));
        		}
    			String[] nextRätsel = getRaetsel("kinderrätsel", zufallszahlK); 
    			result= "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_positive_response_01'/>" + richtigeAntwort[(int)(Math.random()*richtigeAntwort.length)] + nächstesRätsel[(int)(Math.random()*nächstesRätsel.length)] + nextRätsel[0] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			fertigeRätsel++;
    			tippsErhalten = 0;
    			rätselFertig.add(zufallszahlK);
    		}	
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp") || request.contains("hilf") || request.contains("hinweis")) {	
    		alexaResponse = "Kinderrätsel_Tipp";
    		if(tippsErhalten==0) {
    			result = rätsel[2] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			tippsErhalten++;
    		}
    		else if(tippsErhalten==1) {
    			result = rätsel[3] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    			tippsErhalten++;
    		}
    		else
    			result = "Du hast bereits alle Tipps erhalten. <audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_negative_response_01'/> " + falscheAntwort[(int)(Math.random()*falscheAntwort.length)] + "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    	
    	return result;
    }
    
    //Rufe Rätsel aus der Datenbank auf
    private String[] getRaetsel(String rätselart, int index) {
    	String[] rätsel = new String[4];
    	
    	try{
    		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + rätselart + " WHERE id=?");		
    		pstmt.setString(1, Integer.toString(index));
    		ResultSet rs= pstmt.executeQuery();
    		while(rs.next()) {
    			rätsel[0] = rs.getString(2); //Rätsel
    			rätsel[1] = rs.getString(3); //Lösung
    			rätsel[2] = rs.getString(4); //Tipp1
    			rätsel[3] = rs.getString(5); //Tipp2
    			pstmt.close();
    		}	   		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
    	}
    	return rätsel;
    }
        
    private SpeechletResponse responseWithFlavour(String text, int i) {
       
    	SsmlOutputSpeech speech = new SsmlOutputSpeech();
    	 switch(i){ 
         case 0: 
        	 speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
             break; 
         case 1: 
        	 speech.setSsml("<speak><emphasis level=\"strong\">" + text + "</emphasis></speak>");
             break; 
         case 2: 
        	 String half1=text.split(" ")[0];
        	 String[] rest = Arrays.copyOfRange(text.split(" "), 1, text.split(" ").length);
        	 speech.setSsml("<speak>"+half1+"<break time=\"3s\"/>"+ StringUtils.join(rest," ") + "</speak>");
             break; 
         case 3: 
        	 String firstNoun="erstes erkanntes nomen";
        	 String firstN=text.split(" ")[3];
        	 speech.setSsml("<speak>"+firstNoun+ "<say-as interpret-as=\"spell-out\">"+firstN+"</say-as>"+"</speak>");
             break; 
         case 4: 
        	 speech.setSsml("<speak><audio src='soundbank://soundlibrary/transportation/amzn_sfx_airplane_takeoff_whoosh_01'/></speak>");
             break;
         default: 
        	 speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
         } 

        return SpeechletResponse.newTellResponse(speech);
	}

	private String analyze(String request)
    {
        List<String> nouns = new ArrayList<>();
        try {
            nouns = p.findNouns(userRequest);
            logger.info("Detected following nouns: [" + StringUtils.join(nouns, " ") + "]");
        }
        catch (Exception e) {
            throw new UnsupportedOperationException();
        }

        if (nouns.isEmpty()) {
            return("Ich habe keine Nomen erkannt");
        }
        
        return StringUtils.join(nouns, " und ");
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope)
    {
        logger.info("Alexa session ends now");
		alexaResponse = "welcome message";
        fertigeRätsel = 0;
        rätselFertig.clear();
    }

    /*
     * The first question presented to the skill user (entry point)
     */
    private SpeechletResponse getWelcomeResponse(){  	
    	//Datenbank aufrufen
    	try {
			con = DriverManager.getConnection("jdbc:sqlite:C:/Users/Lenovo/git/AlexaRaetsel/de.unidue.ltl.alexa/raetsel_final.db");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//Id des letzten Rätsels
    	try{
    		PreparedStatement pstmt = con.prepareStatement("SELECT MAX(id) FROM kinderrätsel");	
    		ResultSet rs= pstmt.executeQuery();
    		lastKinderrätsel = rs.getInt(1);
    		
    		pstmt = con.prepareStatement("SELECT MAX(id) FROM erwachsenenrätsel");	
    		rs= pstmt.executeQuery();
    		lastErwachsenenrätsel = rs.getInt(1);
    		pstmt.close();   			   		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
    	}
    	zufallszahlK= (int) (Math.random()*(lastKinderrätsel+1));
    	zufallszahlE = (int) (Math.random()*(lastErwachsenenrätsel+1));
        return askUserResponse("Willkommen bei Rätsel Master. Soll ich dir die Spielregeln erklären?");
    }

    /**
     * Tell the user something - the Alexa session ends after a 'tell'
     */
    private SpeechletResponse response(String text)
    {
        // Create the plain text output.
//        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
//        speech.setText(text);
    	SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml("<speak>" + text + "</speak>");

        return SpeechletResponse.newTellResponse(speech);
    }

    /**
     * A response to the original input - the session stays alive after an ask request was send.
     *  have a look on https://developer.amazon.com/de/docs/custom-skills/speech-synthesis-markup-language-ssml-reference.html
     * @param text
     * @return
     */
    private SpeechletResponse askUserResponse(String text)
    {
        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml("<speak>" + text + "</speak>");

        SsmlOutputSpeech repromptSpeech = new SsmlOutputSpeech();
        repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis>Wenn du Hilfe brauchst kannst du mich auch nach einem Tipp fragen.</speak>");

        Reprompt rep = new Reprompt();
        rep.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, rep);
    }

}
