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
    int anzahlKinderrätsel;
    int anzahlErwachsenenrätsel;
    
    //Anzahl der erhaltenen Tipps
    int tippsErhalten = 0;
      
    // Index zum Aufrufen
    ArrayList<Integer> rätselFertig=new ArrayList<Integer>();
    
    //ID des aktuellen Rätsels
    int zufallszahl;
    
    int anzahlFertigeRätsel=0;
    
    // Gibt letzte Antwort von Alexa an
    String alexaResponse = "welcome message";
    
    //aktuelles Rätsel
    String rätsel;
    String lösung;
    String tipp1;
    String tipp2;
    
    //Sounds
    String soundPositive = "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_positive_response_01'/>";
    String soundNegative = "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_negative_response_01'/>";
    String soundLoop = "<audio src='soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_countdown_loop_32s_full_01'/>";
    
    //Alexa Antwortmöglichkeiten
    String[] richtigeAntwort = {"Das ist richtig.", "Super.", "Korrekt.", "Toll gemacht.", "Sehr gut.", "Klasse."};
    String[] nächstesRätsel = {" Hier ist das nächste Rätsel: ", " Jetzt kommt das nächste Rätsel: ", " Weiter gehts: ", " Wie wärs damit: ", " Versuch das mal: ", " Mal sehen ob du das rausbekommst: "};
    String[] ende = {" War schön mit dir gerätselt zu haben.", " Bis zum nächsten mal.", " Lass uns bald wieder rätseln.", " Das Rätseln mit dir hat Spaß gemacht. Bis dann."};
    String[] überspringe = {"Dann eben nicht. Mal sehen ob du das hier schon kennst: ", "Na gut. Wie wärs dann hiermit: ", "Schade. Probier das mal: ", "Dann halt nicht, Wie wäre es damit: "};   
    String[] spielregeln = {"Alles klar, du Allwissender. ", "Okay du Schlaumeier. ", "Na gut, wenn du die schon kennst. ", "Dann halt nicht. "};
    String[] falscheAntwort = {"Leider falsch. Versuch es noch einmal.", "Das war leider nicht richtig.", "Das war leider die falsche Antwort.", "Die Antwort war falsch. Denk nochmal drüber nach.", "Leider nicht richtig. Probiers nochmal."};
    
    //userrequest Möglichkeiten
    String[] requestSchluss = {"schluss", "stopp", "ende", "aufhören", "hör auf", "keine Lust"};
	String[] requestJa = {"Ja", "klar", "mach", "hau raus"};
	String[] requestNein = {"nein", "nö", "ne", "nicht"};
	String[] requestLösung = {"lösung", "keine ahnung", "keinen plan", "weiß nicht", "fällt mir nicht ein", "keine idee", "nicht drauf", "nicht sagen", "keinen blassen schimmer", "auf dem schlauch", "ratlos", "weiß ich nicht"};
    
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
        logger.info("Received following text: [" + userRequest + "]");
        
        // Falls man nicht mehr spielen möchte
        if(contains(userRequest, requestSchluss) == true) {
        	alexaResponse = "welcome message";
        	rätselFertig.clear();
        	// Falls man weniger als 3 Rätsel geschafft hat (zum Spaß)
        	if(anzahlFertigeRätsel < 3) {
            	anzahlFertigeRätsel=0;
            	tippsErhalten = 0;
            	return response("Viel hast du ja nicht geschafft. Trotzdem bis zum nächsten Mal.");
        	}
        	else {
            	anzahlFertigeRätsel = 0;
            	tippsErhalten = 0;
            	return response(ende[(int)(Math.random()*ende.length)]);
        	}
        }
        
        // Nach Welcome Message fragt Alexa nach Spielregeln
        else if(alexaResponse.equals("welcome message")) {
        	return askUserResponse(responseSpielregeln(userRequest));
        }
        
        // Nach Spielregeln fragt Alexa nach Rätselart
        else if(alexaResponse.equals("Spielregeln")) {
        	return askUserResponse(responseRaetselart(userRequest));
        }   
        
        // Nachdem man Kinderrätsel gewählt hat
        else if(alexaResponse.equals("Kinderrätsel")) {
        	if(anzahlFertigeRätsel == anzahlKinderrätsel && (contains(userRequest, requestLösung)))
            	return response(responseRaetsel(userRequest, "kinderrätsel", anzahlKinderrätsel));
        	else 
        		return askUserResponse(responseRaetsel(userRequest, "kinderrätsel", anzahlKinderrätsel));
        }
        
        // Nachdem man Erwachsenenrätsel gewählt hat
        else if(alexaResponse.equals("Erwachsenenrätsel")) {
        	if(anzahlFertigeRätsel == anzahlErwachsenenrätsel && (contains(userRequest, requestLösung)))
            	return response(responseRaetsel(userRequest, "erwachsenenrätsel", anzahlErwachsenenrätsel));
        	else
        		return askUserResponse(responseRaetsel(userRequest, "erwachsenenrätsel", anzahlErwachsenenrätsel));
        }
        else return response("");
    }
   
    // Spielregeln hören oder nicht
    private String responseSpielregeln(String request) {
    	String result = "";
    	if(contains(request, requestJa) == true) { 
    		alexaResponse = "Spielregeln";
    		result = "Okay. Mit Tipp kann ich dir bis zu zwei Tipps geben!"
    				+ "Du kannst Rätsel überspringen oder wiederholen! "
    				+ "Mit 'Lösung' sag ich dir die Lösung des Rätsels und falls wir unser Spiel beenden sollen, sag einfach: Schluss. "
    				+ "Das wärs dann auch schon. Möchtest du ein Kinder- oder Erwachsenenrätsel?";   		
    	}
    	else if(contains(request, requestNein) == true) {
    		alexaResponse = "Spielregeln";
    		result = spielregeln[(int)(Math.random()*spielregeln.length)] + "Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	}
    	else 
    		result = "Das habe ich leider nicht verstanden. Soll ich dir die Spielregeln erklären?";
    	
    	return result;
    }
    
    // Rätselart wählen
    private String responseRaetselart(String request) {
    	String result = "";
    	
    	if(request.contains("kinder rätsel") || request.contains("kinder")) {
    		alexaResponse = "Kinderrätsel";     
    		zufallszahl = (int) (Math.random()*(anzahlKinderrätsel+1));
    		getRaetsel("kinderrätsel", zufallszahl);
    		//Fall 1. Kinderrätsel
    		result = "Hier ist das erste Kinderrätsel: " + rätsel /*+ soundLoop*/;
			rätselFertig.add(zufallszahl);   		
    	}	
    		
    	else if (request.contains("erwachsenen rätsel") || request.contains("erwachsen")) {
    		alexaResponse = "Erwachsenenrätsel";  
    		zufallszahl = (int) (Math.random()*(anzahlErwachsenenrätsel+1));
    		getRaetsel("erwachsenenrätsel", zufallszahl);
    		//Fall 1. Erwachsenenrätsel
    		result = "Hier ist das erste Erwachsenenrätsel: " + rätsel/* + soundLoop*/;
			rätselFertig.add(zufallszahl);   		
    	}
    	else result = "Das habe ich leider nicht verstanden. Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	
    	return result;
    }
    
    //Rätsel, Lösung, Tipp ausgeben
    private String responseRaetsel(String request, String rätselart, int anzahlRätsel) {
    	String result = "";
    	
    	// Rätsel wiederholen
    	if(request.contains("wiederhol") || request.contains("noch mal")) 
    		result = rätsel /*+ soundLoop*/;
    	
    	// Lösung ausgeben
    	else if(contains(request, requestLösung)) {
    		// Falls letztes Rätsel ist
    		if(anzahlFertigeRätsel == anzahlRätsel) {
    			result = "Die Lösung ist " + lösung + "." + ende[(int)(Math.random()*ende.length)];
    			alexaResponse = "welcome message";
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
    			zufallszahl = getZufallszahl(anzahlRätsel);   
    			String letzteLösung = lösung;
        		getRaetsel(rätselart, zufallszahl);
        		result = "Die Lösung ist " + letzteLösung + "." + nächstesRätsel[(int)(Math.random()*nächstesRätsel.length)] + rätsel /*+ soundLoop*/;
        		anzahlFertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahl);
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspring") || request.contains("nächste")) {
    		// Falls letztes Rätsel ist
    		if(anzahlFertigeRätsel == anzahlRätsel) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";	
    		else {
    			zufallszahl = getZufallszahl(anzahlRätsel);  
    			getRaetsel(rätselart, zufallszahl); 
        		result = überspringe[(int)(Math.random()*überspringe.length)] + rätsel/* + soundLoop*/;
        		anzahlFertigeRätsel++;
        		tippsErhalten = 0;
        		rätselFertig.add(zufallszahl);
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(lösung)) {			
    		if(anzahlFertigeRätsel == anzahlRätsel) {
    			result= soundPositive + richtigeAntwort[(int)(Math.random()*richtigeAntwort.length)] + ende[(int)(Math.random()*ende.length)];
    			alexaResponse = "welcome message";
    			anzahlFertigeRätsel = 0;
    			tippsErhalten = 0;
    			rätselFertig.clear();
    		}
    		else {
    			zufallszahl = getZufallszahl(anzahlRätsel);  
    			getRaetsel(rätselart, zufallszahl); 
    			result = soundPositive + richtigeAntwort[(int)(Math.random()*richtigeAntwort.length)] + nächstesRätsel[(int)(Math.random()*nächstesRätsel.length)] + rätsel /*+ soundLoop*/;
    			anzahlFertigeRätsel++;
    			tippsErhalten = 0;
    			rätselFertig.add(zufallszahl);
    		}	
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp") || request.contains("hinweis")) {	
    		if(tippsErhalten==0) {
    			result = tipp1/* + soundLoop*/;
    			tippsErhalten++;
    		}
    		else if(tippsErhalten==1) {
    			result = tipp2 /*+ soundLoop*/;
    			tippsErhalten++;
    		}
    		else
    			result = "Du hast bereits alle Tipps erhalten. Ich wiederhole dir die beiden Tipps nochmal. Hier ist Tipp Nummer 1: " + tipp1 + " Und hier ist Tipp Nummer 2: " + tipp2 /* + soundLoop*/;
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = soundNegative + falscheAntwort[(int)(Math.random()*falscheAntwort.length)] /*+ soundLoop*/;
    	
    	return result;
    } 
    
    //gebe Zufallszahl aus
    private int getZufallszahl(int anzahlRätsel) {
    	zufallszahl = (int) (Math.random()*(anzahlRätsel+1));
		while(rätselFertig.contains(zufallszahl)) {
			zufallszahl = (int) (Math.random()*(anzahlRätsel+1));
		}
		return zufallszahl;
    }
    
    //überprüft ob der Request einen String des Arrays enthält
    private boolean contains(String request, String[] array) {
    	return Arrays.stream(array).parallel().anyMatch(request::contains);
    }
    
    //Rufe Rätsel aus der Datenbank auf
    private void getRaetsel(String rätselart, int index) {
    	try{
    		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + rätselart + " WHERE id=?");		
    		pstmt.setString(1, Integer.toString(index));
    		ResultSet rs= pstmt.executeQuery();
    		while(rs.next()) {
    			rätsel = rs.getString(2);
    			lösung = rs.getString(3);
    			tipp1 = rs.getString(4);
    			tipp2 = rs.getString(5);
    			pstmt.close();
    		}	   		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
    	}
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope)
    {
        logger.info("Alexa session ends now");
		alexaResponse = "welcome message";
        anzahlFertigeRätsel = 0;
        tippsErhalten = 0;
        rätselFertig.clear();
    }

    /*
     * The first question presented to the skill user (entry point)
     */
    private SpeechletResponse getWelcomeResponse(){  	
    	//Datenbank aufrufen
    	try {
			con = DriverManager.getConnection("jdbc:sqlite:C:/Users/migim/git/AlexaRaetsel/de.unidue.ltl.alexa/raetsel_final.db");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//Id des letzten Rätsels
    	try{
    		PreparedStatement pstmt = con.prepareStatement("SELECT MAX(id) FROM kinderrätsel");	
    		ResultSet rs= pstmt.executeQuery();
    		anzahlKinderrätsel = rs.getInt(1);
    		
    		pstmt = con.prepareStatement("SELECT MAX(id) FROM erwachsenenrätsel");	
    		rs= pstmt.executeQuery();
    		anzahlErwachsenenrätsel = rs.getInt(1);
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
        if (alexaResponse.equals("Kinderrätsel") || alexaResponse.equals("Erwachsenenrätsel"))
        	//Wenn man sich bei den Rätseln befindet
        	repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis>Wenn du Hilfe brauchst kannst du mich auch nach einem Tipp fragen.</speak>");
        
        else if(alexaResponse.equals("welcome message"))
        	//Wenn man sich in der welcome message befindet
        	repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis>Soll ich dir die Spielregeln erklären?</speak>");
        
        else
        	//Wenn man sich bei den Spielregeln befindet
        	repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis>Möchtest du ein Kinder- oder Erwachsenenrätsel?</speak>");
        
        Reprompt rep = new Reprompt();
        rep.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, rep);
    }

}
