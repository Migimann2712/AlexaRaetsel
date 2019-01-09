package com.amazon.customskill;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

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
    
    int zufallszahlK=(int) (Math.random()*(lastKinderrätsel+1));
    int zufallszahlE=(int) (Math.random()*(lastErwachsenenrätsel+1));
    int fertigeRätsel=0;
    
    // Gibt letzte Antwort von Alexa an
    String alexaResponse = "welcome message";

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
        String result = "";
        
        userRequest = intent.getSlot("Alles").getValue();
        
        System.out.println("UserRequest: " + userRequest);
        System.out.println("AlexaResponse: " + alexaResponse);
        System.out.println("ZufallszahlK " + zufallszahlK);
        System.out.println("ZufallszahlE " + zufallszahlE);
        System.out.println(rätselFertig.toString());
        
        String[] kinderRätsel = getRaetsel("kinderrätsel", zufallszahlK);
        String[] erwachsenenRätsel = getRaetsel("erwachsenenrätsel", zufallszahlE);
        
        
        logger.info("Received following text: [" + userRequest + "]");
        
        // Falls man nicht mehr spielen möchte
        if(userRequest.contains("schluss")) {
        	alexaResponse = "welcome message";
        	rätselFertig.clear();
        	// Falls man weniger als 2 Rätsel geschafft hat (zum Spaß)
        	if(fertigeRätsel < 2) {
            	fertigeRätsel=0;       	
            	return response("Viel hast du ja nicht geschafft. Trotzdem bis zum nächsten Mal.");
        	}
        	else {
            	fertigeRätsel = 0;
            	return response("War schön mit dir gerätselt zu haben.");
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
        	if(fertigeRätsel == lastKinderrätsel && (userRequest.contains(kinderRätsel[1]) || userRequest.contains("lösung")))
            	return response(responseKinderRaetsel(userRequest, kinderRätsel));
        	else
        		return askUserResponse(responseKinderRaetsel(userRequest, kinderRätsel));
        }
        
        // Nachdem man Erwachsenenrätsel gewählt oder Tipp bekommen hat
        else if(alexaResponse.equals("Erwachsenenrätsel") || alexaResponse.equals("Erwachsenenrätsel_Tipp")) {
        	if(fertigeRätsel == lastErwachsenenrätsel && (userRequest.contains(erwachsenenRätsel[1]) || userRequest.contains("lösung")))
            	return response(responseErwachsenenRaetsel(userRequest, erwachsenenRätsel));
        	else
        		return askUserResponse(responseErwachsenenRaetsel(userRequest,erwachsenenRätsel));
        }
        
        // Erwachsenenrätsel weiterspielen oder nicht
        else if(alexaResponse.equals("Antwort_Erwachsenenrätsel")) {
        	if(userRequest.contains("Ja")) {
        		return askUserResponse(responseRaetselart("erwachsenen rätsel", kinderRätsel, erwachsenenRätsel));
        	}
        	else if(userRequest.contains("nein")) {
        		alexaResponse = "welcome message";
        		return response("Es war schön mit dir gerätselt zu haben.");
        	}  	
        }
        
        // Kinderrätsel weiterspielen oder nicht
        else if(alexaResponse.equals("Antwort_Kinderrätsel")) {
        	if(userRequest.contains("Ja")) {
        		return askUserResponse(responseRaetselart("kinder rätsel", kinderRätsel, erwachsenenRätsel));
        	}
        	else if(userRequest.contains("nein")) {
        		alexaResponse = "welcome message";
        		return response("Es war schön mit dir gerätselt zu haben.");
        	}
        }
                
        return response(result);
    }
   
    // Spielregeln hören oder nicht
    private String responseSpielregeln(String request) {
    	String result = "";
    	if(request.contains("Ja")) { 
    		alexaResponse = "Spielregeln";
    		result = "Okay. Wenn du einen Tipp willst, sag einfach 'Tipp'! "
    				+ "Du kannst Rätsel überspringen oder wiederholen! "
    				+ "Mit 'Lösung' sag ich dir die Lösung des Rätsels und falls wir unser Spiel beenden sollen, sag einfach: Schluss. "
    				+ "Das wärs dann auch schon. Möchtest du ein Kinder- oder Erwachsenenrätsel?";   		
    	}
    	else if(request.contains("nein")) {
    		alexaResponse = "Spielregeln";
    		result = "Alles klar, du Allwissender. Möchtest du ein Kinder- oder Erwachsenenrätsel?";
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
    		if(fertigeRätsel == 0) {
    			result = "Hier ist das erste Kinderrätsel: " + kinderRätsel[0];
    			rätselFertig.add(zufallszahlK);
    		}
    		else {
    			while(rätselFertig.contains(zufallszahlK)) {
    				zufallszahlK = (int) (Math.random()*lastKinderrätsel+1);
    			}
    			result = kinderRätsel[0];	
    			rätselFertig.add(zufallszahlK);
    		}
    	}	
    		
    	else if (request.contains("erwachsenen rätsel") || request.contains("erwachsenen")) {
    		alexaResponse = "Erwachsenenrätsel";   		
    		//Fall 1. Erwachsenenrätsel
    		if(fertigeRätsel == 0) {
    			result = "Hier ist das erste Erwachsenenrätsel: " + erwachsenenRätsel[0];
    			rätselFertig.add(zufallszahlE);
    		}
    		else {
    			while(rätselFertig.contains(zufallszahlE)) {
    				zufallszahlE = (int) (Math.random()*lastErwachsenenrätsel+1);
    			}
    			result = erwachsenenRätsel[0];	
    			rätselFertig.add(zufallszahlE);
    		}
    	}
    	else result = "Das habe ich leider nicht verstanden. Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	
    	return result;
    }
    
    // Erwachsenen Rätsel  
    private String responseErwachsenenRaetsel(String request, String[] rätsel) {
    	String result = "";
    	
    	// Rätsel wiederholen
    	if(request.contains("wiederhole")) 
    		result = rätsel[0];
    	
    	// Lösung ausgeben
    	else if(request.contains("lösung")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel == lastErwachsenenrätsel) {
    			result = "Die Lösung ist " + rätsel[1] + ". War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
        		zufallszahlE = (int) (Math.random()*lastErwachsenenrätsel+1);
        		while(rätselFertig.contains(zufallszahlE)) {
        			zufallszahlE = (int) (Math.random()*lastErwachsenenrätsel+1);
        		}
        		String[] nextRätsel = getRaetsel("erwachsenenrätsel", zufallszahlE);
        		result = "Die Lösung ist " + rätsel[1] + ". Hier ist das nächste Rätsel: " + nextRätsel[0];
        		fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlE);
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspringe")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel == lastErwachsenenrätsel) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";	
    		else {
    			fertigeRätsel++;
    	        zufallszahlE=(int) (Math.random()*lastErwachsenenrätsel+1);
    	        while(rätselFertig.contains(zufallszahlE)) {
        			zufallszahlE = (int) (Math.random()*lastErwachsenenrätsel+1);
        		}
    			String[] nextRätsel= getRaetsel("erwachsenenrätsel", zufallszahlE); 
        		result = "Dann eben nicht. Mal sehen, ob du das hier schon kennst: " + nextRätsel[0];
        		fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlE);
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(rätsel[1])) {			
    		if(fertigeRätsel == lastErwachsenenrätsel) {
    			result = "Das ist richtig. War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			fertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
    			fertigeRätsel++;
    	        zufallszahlE=(int) (Math.random()*lastErwachsenenrätsel+1);
    	        while(rätselFertig.contains(zufallszahlE)) {
        			zufallszahlE = (int) (Math.random()*lastErwachsenenrätsel+1);
        		}
    			String[] nextRätsel = getRaetsel("erwachsenenrätsel", zufallszahlE); 
    			result = "Das ist richtig. Hier ist das nächste Rätsel: " + nextRätsel[0];
    			fertigeRätsel++;
    			tippsErhalten = 0;
    			rätselFertig.add(zufallszahlE);
    		}	
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp")) {	
    		alexaResponse = "Erwachsenenrätsel_Tipp";
    		if(tippsErhalten==0) {
    			result = rätsel[2];
    			tippsErhalten++;
    		}
    		else if(tippsErhalten==1) {
    			result = rätsel[3];
    			tippsErhalten++;
    		}
    		else
    			result = "Du hast bereits alle Tipps erhalten.";
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = "Leider falsch. Versuch es noch einmal.";
    	
    	return result;
    }
   
    
    // Kinder Rätsel
    private String responseKinderRaetsel(String request, String[] rätsel) {
    	String result = "";
    	
    	// Rätsel wiederholen
    	if(request.contains("wiederhole")) 
    		result = rätsel[0];
    	
    	// Lösung ausgeben
    	else if(request.contains("lösung")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel == lastKinderrätsel) {
    			result = "Die Lösung ist " + rätsel[1] + ". War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			fertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
        		zufallszahlK = (int) (Math.random()*lastKinderrätsel+1);
        		while(rätselFertig.contains(zufallszahlK)) {
        			zufallszahlK = (int) (Math.random()*lastKinderrätsel+1);
        		}
        		String[] nextRätsel = getRaetsel("kinderrätsel", zufallszahlK);
        		result = "Die Lösung ist " + rätsel[1] + ". Hier ist das nächste Rätsel: " + nextRätsel[0];
        		fertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlK);
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspringe")) {
    		// Falls letztes Rätsel ist
    		if(fertigeRätsel == lastKinderrätsel) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";	
    		else {
    			fertigeRätsel++;
    			zufallszahlK = (int) (Math.random()*lastKinderrätsel+1);
        		while(rätselFertig.contains(zufallszahlK)) {
        			zufallszahlK = (int) (Math.random()*lastKinderrätsel+1);
        		}
    			String[] nextRätsel = getRaetsel("kinderrätsel", zufallszahlK); 
        		result = "Dann eben nicht. Mal sehen, ob du das hier schon kennst: " + nextRätsel[0];
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahlK);
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(rätsel[1])) {			
    		if(fertigeRätsel == lastKinderrätsel) {
    			result = "Das ist richtig. War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			fertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
    			fertigeRätsel++;
    			zufallszahlK = (int) (Math.random()*lastKinderrätsel);
        		while(rätselFertig.contains(zufallszahlK)) {
        			zufallszahlK = (int) (Math.random()*lastKinderrätsel);
        		}
    			String[] nextRätsel = getRaetsel("kinderrätsel", zufallszahlK); 
    			result = "Das ist richtig. Hier ist das nächste Rätsel: " + nextRätsel[0];
    			tippsErhalten = 0;
    			rätselFertig.add(zufallszahlK);
    		}	
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp")) {	
    		alexaResponse = "Kinderrätsel_Tipp";
    		if(tippsErhalten==0) {
    			result = rätsel[2];
    			tippsErhalten++;
    		}
    		else if(tippsErhalten==1) {
    			result = rätsel[3];
    			tippsErhalten++;
    		}
    		else
    			result = "Du hast bereits alle Tipps erhalten.";
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = "Leider falsch. Versuch es noch einmal.";
    	
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
			con = DriverManager.getConnection("jdbc:sqlite:C:/Users/Lenovo/git/AlexaRaetsel/de.unidue.ltl.alexa/raetsel.db");
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
    	
        return askUserResponse("Willkommen bei Rätsel Master. Soll ich dir die Spielregeln erklären?");
    }

    /**
     * Tell the user something - the Alexa session ends after a 'tell'
     */
    private SpeechletResponse response(String text)
    {
        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(text);

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
