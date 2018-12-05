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
    
    // Rätsel, Tipps und Lösungen als Arrays (mit Indizes aufrufbar)
    // Erwachsenen-Teil
    public static String eRaetsel[] = {"Alle Tage geh ich raus, bleibe dennoch stets zuhaus. Wer bin ich?","Was hängt an der Wand und hält ohne Nagel und Band?", "Der es macht, der will es nicht. Der es trägt, behält es nicht. Der es kauft, braucht es nicht. Der es hat, weiß es nicht.", "Ein Bauer zählt bei seinen Hühnern und Schafen 40 Augen und 64 Beine. Wie viele Schafe gibt es auf dem Bauernhof?", "Was kommt einmal in jeder Minute, zweimal in jedem Moment aber nie in tausend Jahren vor?", "Höher ist`s als jeder Baum. Wurzeln hat`s, die sieht man kaum. Auch im Licht wächst es nicht. Was bin ich?"};
    public static String eTipps[] = {"Es ist ein Lebewesen.", "Ein Tier macht es.", "Es hat etwas mit dem Ableben zu tun.", "Wie viele Beine hat ein Schaf und wie viele ein Huhn?", "Die Antwort liegt in der Frage.", "Es kommt in der Natur vor."};
    public static String eLoesung[] = {"schnecke","spinnennetz", "sarg", "zwölf", "buchstabe m", "berg"};
    
    // Kinder-Teil
    public static String kRaetsel[] = {"Was hat keine Füße und läuft trotzdem?","Fliegt, aber hat keine Flügel. Weint, aber hat keine Augen. Was ist es?", "Harte Schale, leckerer Kern, wer mich knackt, der isst mich gern?", "Im Winter steht er still und stumm dort draußen weiß herum. Wer ist es?", "Was grünt im Sommer und im Winter und erfreut zur Weihnachtszeit die Kinder?", "Welche Brille trägt man nicht auf der Nase?"};
    public static String kTipps[] =	{"Es ist etwas am Menschen.", "Es gibt viele davon.", "Es ist klein.", "In der Sonne fängt er an zu schmelzen.", "Zur Weihnachtszeit holt man es ins Haus.", "Man setzt sich drauf."};
    public static String kLoesung[] = {"nase","wolke", "nuss", "schneemann", "tannenbaum", "klobrille"};
    
    // Index
    public static int index = 0;
    
    // Gibt letzte Antwort von Alexa an
    String alexaResponse="welcome message";

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

        if(userRequest.contains("schluss")) {
        	alexaResponse="welcome message";
        	return response("War schön mit dir gerätselt zu haben");
        }
        // Nach der Welcome Message fragt Alexa nach den Spielregeln
        else if(alexaResponse.equals("welcome message")) {
        	return askUserResponse(responseSpielregeln(userRequest));
        }
        
        // Nach den Spielregeln fragt Alexa nach der Rätselart
        else if(alexaResponse.equals("Spielregeln")) {
        	return askUserResponse(responseRaetselart(userRequest));
        }
        
        // Wird ausgeführt, wenn man ein Kinderrätsel als Rätselart gewählt hat
        else if(alexaResponse.equals("Kinderrätsel") || alexaResponse.equals("Kinderrätsel_Tipp") ) {
        	return askUserResponse(responseKinderRaetsel(userRequest));
        }
        
        // Wird ausgeführt, wenn man ein Erwachsenenrätsel als Rätselart gewählt hat
        else if(alexaResponse.equals("Erwachsenenrätsel") || alexaResponse.equals("Erwachsenenrätsel_Tipp")) {
        	return askUserResponse(responseErwachsenenRaetsel(userRequest));
        }
        
        else if(alexaResponse.equals("Antwort_Erwachsenenrätsel")) {
        	if(userRequest.equals("Ja")) {
        		return askUserResponse(responseRaetselart("erwachsenenrätsel"));
        	}
        	else if(userRequest.equals("nein")) {
        		alexaResponse="welcome message";
        		return response("Es war schön mit dir gerätselt zu haben");
        	}
        	
        }
        
        else if(alexaResponse.equals("Antwort_Kinderrätsel")) {
        	if(userRequest.equals("Ja")) {
        		return askUserResponse(responseRaetselart("kinderrätsel"));
        	}
        	else if(userRequest.equals("nein")) {
        		alexaResponse="welcome message";
        		return response("Es war schön mit dir gerätselt zu haben");
        	}
        }
                
        return response(result);
    }
   
    private String responseSpielregeln(String request) {
    	String result = "";
    	if(request.equals("Ja")) { 
    		alexaResponse = "Spielregeln";
    		result = "Okay. Ich nenn dir ein Rätsel. "
    				+ "Wenn du einen Tipp benötigst, sag: Tipp! "
    				+ "Wenn ich das Rätsel wiederholen soll, sage: Wiederhole"
    				+ "Wenn du die Antwort nicht weißt kann ich dir auch die Lösung verraten. "
    				+ "Möchtest du ein Kinder- oder Erwachsenenrätsel?";   		
    	}
    	else if(request.equals("nein")) {
    		alexaResponse="Spielregeln";
    		result = "Möchtest du ein Kinder- oder Erwachsenenrätsel?";
    	}
    	return result;
    }
    
    private String responseRaetselart(String request) {
    	String result = "";
    	if(request.contains("kinderrätsel")) {
    		alexaResponse="Kinderrätsel";
    		result = "Hier ist ein Kinderrätsel: "
    				+ kRaetsel[index];						// Kinderrätsel mit dem Index index (damit z.B. index+1 machbar)
    	}
    	else if (request.contains("erwachsenenrätsel")) {
    		alexaResponse="Erwachsenenrätsel";
    		result = "Hier ist ein Erwachsenenrätsel: "
    				+ eRaetsel[index];   					// Erwachsenenrätsel mit dem Index index
    	}
    	return result;
    }
    
    private String responseErwachsenenRaetsel(String request) {
    	String result = "";
    	
    	if(request.contains("wiederhole")) {
    		result=eRaetsel[index];
    	}
    	else if(request.contains("lösung")) {
    		alexaResponse = "Antwort_Erwachsenenrätsel";
    		result="Die Lösung ist " + eLoesung[index] + ". Möchtest du ein weiteres Rätsel?";
    		index++;
    	}
    	else if(request.contains("überspringe")) {
    		result="Hier ist das nächste Rätsel: " + eRaetsel[index++];
    	}
    	else if(request.contains(eLoesung[index])) {				// Falls Lösung genannt wird
    		alexaResponse = "Antwort_Erwachsenenrätsel";
    		if(index==eRaetsel.length-1) {
    			result="Korrekt. War schön mit dir gerätselt zu haben";
    			alexaResponse="welcome message";
    			index=0;
    		}
    		else {
    			result = "Korrekt. Möchtest du ein weiteres Rätsel?";
    			index++;
    		}
    		
    	}
    	else if(request.contains("tipp")) {					// Falls man nach einem Tipp fragt
    		alexaResponse = "Erwachsenenrätsel_Tipp";
    		result = eTipps[index];
    	}
    	
    	else {
    		result="Das war leider nicht richtig.";
    	}
    	return result;
    }
   
    private String responseKinderRaetsel(String request) {
    	String result = "";
    	
    	
    	if(request.contains("wiederhole")) {
    		result=kRaetsel[index];
    	}
    	else if(request.contains("lösung")) {
    		alexaResponse = "Antwort_Kinderrätsel";
    		result="Die Lösung ist " + kLoesung[index] + ". Möchtest du ein weiteres Rätsel?";
    		index++;
    	}
    	else if(request.contains("überspringe")) {
    		result="Hier ist das nächste Rätsel: " + kRaetsel[index++];
    	}
    	else if(request.contains(kLoesung[index])) {				// Falls Lösung genannt wird
    		alexaResponse = "Antwort_Kinderrätsel";
    		if(index==eRaetsel.length-1) {
    			result="Das ist richtig. War schön mit dir gerätselt zu haben";
    			alexaResponse="welcome message";
    			index=0;
    		}
    		else {
    			result = "Das ist richtig. Möchtest du ein weiteres Rätsel?";
    			index++;
    		}
    	}
    	else if(request.contains("tipp")) {					// Falls man nach einem Tipp fragt
    		alexaResponse = "Kinderrätsel_Tipp";
    		result = kTipps[index];
    	}
    	else {
    		result="Das war leider nicht richtig.";
    	}
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
