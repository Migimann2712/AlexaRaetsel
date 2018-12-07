package com.amazon.customskill;

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
    
    // Erwachsenen-Teil
    public static String eRaetsel[] = {"Alle Tage geh ich raus, bleibe dennoch stets zuhaus. Was bin ich?",
    									"Was hängt an der Wand und hält ohne Nagel und Band?",
    									"Wer wirft mit Geld um sich?",
    									"Erst bin ich groß, dann bin ich klein. Ich leuchte hell, der Wind ist mein Feind. Was bin ich?",
    									"Was geht über Wasser und wird nicht nass?",
    									"Was ist der Albtraum eines Luftballons?"};
    public static String eTipps[] = {"Ich bin ein Lebewesen.",
    									"Ein Tier macht es.",
    									"Denk an Autoteile.",
    									"Ich spende Licht.",
    									"Es ist nichts Lebendiges.", 
    									"Denk an sein Todesurteil."};
    public static String eLoesung[] = {"schnecke","spinnennetz","scheinwerfer","kerze","brücke","platzangst"};
    
    // Kinder-Teil
    public static String kRaetsel[] = {"Was hat keine Füße und läuft trotzdem?",
    									"Es ist voller Löcher, aber dennoch hält es das Wasser.",
    									"Welchen Fall kann ein Detektiv nicht auflösen?",
    									"Im Winter steht er still und stumm dort draußen weiß herum. Wer ist es?",
    									"Welcher Hahn kann nicht krähen?",
    									"Welche Brille trägt man nicht auf der Nase?"};
    public static String kTipps[] =	{"Es ist etwas am Menschen.",
    									"Es ist meist viereckig.",
    									"Denk an Wasser.", 
    									"In der Sonne fängt er an zu schmelzen.",
    									"Jeder hat es Zuhause.",
    									"Man setzt sich drauf."};
    public static String kLoesung[] = {"nase","schwamm","wasserfall","schneemann","wasserhahn","klobrille"};
    
    // Index zum Aufrufen
    public static int index = 0;
    
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
        
        logger.info("Received following text: [" + userRequest + "]");

        // Falls man nicht mehr spielen möchte
        if(userRequest.contains("schluss")) {
        	alexaResponse = "welcome message";
        	// Falls man weniger als 2 Rätsel geschafft hat (zum Spaß)
        	if(index < 2) {
            	index = 0;
            	return response("Viel hast du ja nicht geschafft. Trotzdem bis zum nächsten Mal.");
        	}
        	else {
            	index = 0;
            	return response("War schön mit dir gerätselt zu haben.");
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
        
        // Nachdem man Kinderrätsel gewählt oder Tipp bekommen hat
        else if(alexaResponse.equals("Kinderrätsel") || alexaResponse.equals("Kinderrätsel_Tipp") ) {
        	return askUserResponse(responseKinderRaetsel(userRequest));
        }
        
        // Nachdem man Erwachsenenrätsel gewählt oder Tipp bekommen hat
        else if(alexaResponse.equals("Erwachsenenrätsel") || alexaResponse.equals("Erwachsenenrätsel_Tipp")) {
        	return askUserResponse(responseErwachsenenRaetsel(userRequest));
        }
        
        // Erwachsenenrätsel weiterspielen oder nicht
        else if(alexaResponse.equals("Antwort_Erwachsenenrätsel")) {
        	if(userRequest.contains("Ja")) {
        		return askUserResponse(responseRaetselart("erwachsenen rätsel"));
        	}
        	else if(userRequest.contains("nein")) {
        		alexaResponse = "welcome message";
        		return response("Es war schön mit dir gerätselt zu haben.");
        	}  	
        }
        
        // Kinderrätsel weiterspielen oder nicht
        else if(alexaResponse.equals("Antwort_Kinderrätsel")) {
        	if(userRequest.contains("Ja")) {
        		return askUserResponse(responseRaetselart("kinder rätsel"));
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
    		result = "Okay. Wenn du einen Tipp willst, sag einfach: Tipp! "
    				+ "Zum Wiederholen des Rätsels, sag: Wiederhole! "
    				+ "Mit 'Lösung' sag ich dir die Lösung des Rätsels und falls wir unser Spiel beenden sollen, sag einfach: Schluss. "
    				+ "Das wärs dann auch schon. Möchtest du ein Kinder- oder Erwachsenenrätsel?";   		
    	}
    	else if(request.contains("nein")) {
    		alexaResponse = "Spielregeln";
    		result = "Alles klar, du Allwissender. Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	}
    	return result;
    }
    
    // Rätselart wählen
    private String responseRaetselart(String request) {
    	String result = "";
    	if(request.contains("kinder rätsel")) {
    		alexaResponse = "Kinderrätsel";
    		// Falls 1. Kinderrätsel
    		if(index == 0)
    			result = "Hier ist das erste Kinderrätsel: " + kRaetsel[index];
    		else result = kRaetsel[index];
    	}
    	else if (request.contains("erwachsenen rätsel")) {
    		alexaResponse = "Erwachsenenrätsel";
    		// Falls 1. Erwachsenenrätsel
    		if(index == 0)
    			result = "Hier ist das erste Erwachsenenrätsel: " + eRaetsel[index];
    		else result = eRaetsel[index];
    	}
    	else result = "Das habe ich leider nicht verstanden. Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	
    	return result;
    }
    
    // Erwachsenen Rätsel  
    private String responseErwachsenenRaetsel(String request) {
    	String result = "";
    	
    	// Rätsel wiederholen
    	if(request.contains("wiederhole")) 
    		result = eRaetsel[index];	
    	
    	// Lösung ausgeben
    	else if(request.contains("lösung")) {
    		// Falls letztes Rätsel ist
    		if(index == eRaetsel.length-1) {
    			result = "Die Lösung ist " + eLoesung[index] + ". War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			index = 0;
    		}
    		else {
    			int tmpIndex = index;
        		tmpIndex++;
        		result = "Die Lösung ist " + eLoesung[index] + ". Hier ist das nächste Rätsel: " + eRaetsel[tmpIndex];
        		index++;
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspringe")) {
    		// Falls letztes Rätsel ist
    		if(index == eRaetsel.length-1) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";	
    		else {
    			index++;
        		result = "Dann eben nicht. Mal sehen, ob du das hier schon kennst: " + eRaetsel[index];
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(eLoesung[index])) {			
    		if(index == eRaetsel.length-1) {
    			result = "Das ist richtig. War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			index = 0;
    		}
    		else {
    			index++;
    			result = "Das ist richtig. Hier ist das nächste Rätsel: " + eRaetsel[index];
    		}	
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp")) {	
    		alexaResponse = "Erwachsenenrätsel_Tipp";
    		result = eTipps[index];
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = "Leider falsch. Versuch es noch einmal.";
    	
    	return result;
    }
   
    // Kinder Rätsel
    private String responseKinderRaetsel(String request) {
    	String result = "";
    	
    	// Rätsel wiederholen
    	if(request.contains("wiederhole")) 
    		result = kRaetsel[index];
    	  	
    	// Lösung ausgeben
    	else if(request.contains("lösung")) {
    		// Falls letztes Rätsel ist
    		if(index == kRaetsel.length-1) {
    			result = "Die Lösung ist " + kLoesung[index] + ". War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			index = 0;
    		}
    		else {
    			int tmpIndex = index;
        		tmpIndex++;
        		result = "Die Lösung ist " + kLoesung[index] + ". Hier ist das nächste Rätsel: " + kRaetsel[tmpIndex];
        		index++;
    		}
    	}
    	
    	// Rätsel überspringen
    	else if(request.contains("überspringe")) {
    		// Falls letztes Rätsel ist
    		if(index == kRaetsel.length-1) 
    			result = "Das ist das letzte Rätsel. Versuche es doch nochmal.";   		
    		else {
    			index++;
    			result = "Dann eben nicht. Mal sehen, ob du das hier schon kennst: " + kRaetsel[index];
    		}
    	}
    	
    	// Falls Lösung genannt wird
    	else if(request.contains(kLoesung[index])) {	
    		if(index == eRaetsel.length-1) {
    			result = "Das ist richtig. War schön mit dir gerätselt zu haben";
    			alexaResponse = "welcome message";
    			index = 0;
    		}
    		else {
    			index++;
    			result = "Das ist richtig. Hier ist das nächste Rätsel: " + kRaetsel[index];
    		}
    	}
    	
    	// Tipp bekommen
    	else if(request.contains("tipp")) {
    		alexaResponse = "Kinderrätsel_Tipp";
    		result = kTipps[index];
    	}
    	
    	// Ungültige Eingabe
    	else 
    		result = "Leider falsch. Versuch es noch einmal.";
    	
    	return result;
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
    }

    /*
     * The first question presented to the skill user (entry point)
     */
    private SpeechletResponse getWelcomeResponse(){    
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
        repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis> Bist du noch da?</speak>");

        Reprompt rep = new Reprompt();
        rep.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, rep);
    }

}
