# AlexaRaetsel
 
Gruppenmitglieder: 
Miguel Schrader (3052554), Oleg Schinkin (3031465), Jaqueline Kruse (), Hannah Rossow (), Lina Peters (), Annika Steinkamp () 
 
___________________________________________________________________________ 
 
The goal of our project was to develop and implement a fun, interactive and intuitive game as an Alexa skill. We started out with the idea for a classical quiz with different categories and four default answers from which the user could choose. We envisioned a single-player mode with a reward system in which correct answers would earn you points to add on your personal high-score and false answers would take away life-points. If the user reached zero life-points, the game would be terminated. In the two-player mode users would be able to compete with each other by alternating answering questions, 5 questions each. The player with the most correct answers would win, a tie would be resolved by a tie breaker question. 
After consideration we decided that we wanted to step away from the rather conventional idea of a quiz but keep the fun and voice-interaction focussed approach from the game. 
 
Eventually we decided on what would become Rätselmaster, an intergenerational riddle-game. With the invocation name “rätselmaster” we wanted to develop a game with two modes – an adult riddle category and a kids category with appropriate and interesting riddles for the respective age-group. 
Rätselmaster chooses the riddles randomly, the user is able to skip and repeat riddles. Alexa offers a short explanation of the game, which can also be skipped if the user already knows how the application works.  
The User is also able to request two tips per riddle, one rather general and one a bit more specific to be helpful. All tips were written by us to match the specific riddles we collected from various sources, which can be found at the end of this documentation.  
Alexa herself also proposes to give a tip after waiting for a response from the user to further support them. The user is able to request the correct solution to the riddle, if he is not able to solve it. Furthermore, he can end the game at any time. 
 
In the following we will describe three possible interactions with Rästelmaster. 
 
The first szenario describes an adult, who played Rätselmaster before.    
U: Alexa, Rätselmaster  
A: Wilkommen bei Rätselmaster. Soll ich dir die Spielregeln erklären?  
U: Nein.  
A: Möchtest du ein Kinder- oder Erwachsenenrätsel?  
U: Ein Erwachsenenrätsel.  
A: Hier ist das erste Erwachsenenrätsel: Ich kenne einen Peter, den kennt jeder, doch gern hat ihn keiner. Was ist das für einer?  
U: Ist es der schwarze Peter?  
A: *Sound* Leider falsch. Versuch es noch einmal.  
U: Dann muss es der miesepeter sein!  
A: *Sound* Korrekt! Jetzt kommt das nächste Rät...  
U: Schluss!  
A: Viel hast du ja nicht geschafft. Trotzdem bis zum nächsten Mal.  
  
The following szenario describes a kid, who is playing for the first time.  
U: Alexa, Rätselmaster  
A: Wilkommen bei Rätselmaster. Soll ich dir die Spielregeln erklären?  
U: Ja.  
A: Okay. Mit Tipp kann ich dir bis zu zwei Tipps geben! Du kannst Rätsel überspringen oder wiederholen! Mit ‘Lösung’ sag ich dir die Lösung des Rätsels und falls wir unser Spiel beenden sollen sag einfach ‘Schluss’. Das wärs dann auch schon. Möchtest du ein Kinder- oder Erwachsenenrätsel?  
U: Eins für Kinder!  
A: Hier ist das erste Kinderrätsel:  Welche Brille trägt man nicht auf der Nase?  
U:(says nothing)  
A: Wenn du Hilfe brauchst, kannst du mich auch nach einem Tipp fragen.  
U: Mhm, dann gib mir mal einen Tipp.  
A: Man setzt sich drauf.  
U: Ah, die Klobrille!  
A: *Sound* Korrekt! Weiter gehts: Was hat keine Füße und läuft trotzdem?  
U: Ich brauche einen Hinweis.  
A: Es ist etwas am Menschen.  
U: Mhm, ich brauch noch einen Tipp!  
A: Es befindet sich in unserem Gesicht.  
U: Sag mir noch einen Tipp!  
A: Du hast bereits alle Tipps erhalten. Ich wiederhole dir die beiden Tipps nochmal. Hier ist Tipp Nummer 1: Es ist etwas am Menschen. Und hier ist Tipp Nummer 2:  Es befindet sich in unserem Gesicht.  
U: Ich weiß es nicht.  
A: Die Lösung ist: Nase. Hier ist das nächste R...  
U: Ich habe keine Lust mehr!  
A: War schön mit dir gerätselt zu haben.  
  
In this szenario the adult playing knows the rules and has played Rätselmaster many times.  
U: Alexa, Rätselmaster  
A: Wilkommen bei Rätselmaster. Soll ich dir die Spielregeln erklären?  
U: Nein.  
A: Möchtest du ein Kinder- oder Erwachsenenrätsel?  
U: Eins für Erwachsene.  
A: Hier ist das erste Erwachsenenrätsel: Was hängt an der Wand und hält ohne Nagel und Band?  
U: Das kenn ich schon, überspring das.  
A: Na gut. Wie wärs dann hiermit: Wer wirft mit Geld um sich?  
U: Ein Bänker?  
A: *Sound* Das war leider nicht richtig.  
U: Wiederhol das Rätsel bitte.  
A: Wer wirft mit Geld um sich?  
U: Sag mir einen Tipp.  
A: Es ist ein Wortspiel.  
U: Ach klar, der Scheinwerfer!  
A: *Sound* Sehr gut. Hier ist das nächste Rätsel: Was ist der Albtraum eines Luftballons?  
U: Platzangst.  
A: *Sound* Super. Versuch das mal: Was geht über Wasser und wird nicht nass?  
U: Überspring das.  
A: Okay du Schlaumeier. Mal sehen ob du das rausbekommst: Erst bin ich groß,…  
U: Hör auf!  
A: Lass uns bald wieder rätseln.  
  
  
Two things that were very important to us during the development of the application were 1) a natural approach to the voice-guided interaction between the user and Alexa and 2) a simple way to add new riddles to keep the game interesting. 
We achieved the first goal by making the tone of the game familiar and giving Alexa a variety of, partly humorous, responses to randomly choose from, instead of having just one default answer for every scenario, which makes the interactions seem more natural and flexible.  
Our second goal was eventually achieved by implementing the riddles in a databank. We started out by implementing the first riddles directly into the code, but decided to use a database (sqlite) to make adding and editing riddles as easily accessible as possible.  
   
To use Rätselmaster the user needs to install Tomcat, Java, ngrok, Eclipse and have a github account to access the files. There they can attain the framework (main skelleton) and import it as a maven project. The user needs to open Eclipse and the folder Alexaskelleton. With the right mouse click on team – pull he can download the code. 
The user has to create the .war file. To do this, the user click with the right mouse button on our project and then Run As -> Maven Install. After the .war file is created and the Tomcat is installed the user has to export the .war-file in the Tomcat folder webapps. After that, the user click on the Tomcat folder, then on the folder bin and then he opens startup.bat. He also needs to open ngrok and write the endpoint link ngrok http 8080 in the first row. He then needs to copy the https part in the browser and add myskill/ behind. 
By configuring third party libraries as …. we were able to access and use those as well.  
In order to test the skill, we needed to install the Java Servlet, a platform-independent method for building our own webbased application. The servlet was used to connect to the Java Web Server and the Server Container.  
Additionally a SSL-tunnel (ngrok) is needed to run the application.  
  
To use our database, you have to download sqlite (https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.18.0.jar). The jar file that you dowloaded (sqlite-jdbc-3.18.0.jar) goes into the lib folder from the tomcat folder.  
Furthermore, the jar has to add to the libraries in eclipse too (Project -> Properties -> Java Build Path -> libraries -> add external jar and add the sqlite jar file.  
Then you have to modify the address on line 324, where you find the database (e.g. "jdbc:sqlite:C:/Users/migim/git/AlexaRaetsel/de.unidue.ltl.alexa/raetsel_final.db").
  
ZENTRALE MECHANISMEN ERKLÄREN/SKIZZIEREN  
AlexaSkillSpeechlet.java:  
The Speechlet contains the main interactions such as the invocation to start the game or the request to end it.  
The method to start the game is the getWelcome Response function (321), which invokes the database and gets the amount of riddles containted in it. After that Alexa will start the game with “Willkommen bei Rätselmaster. Soll ich dir die Spielregeln erklären?” - the welcome message of our skill.  
  
The Speechlet onIntent function contains the main interactions of the game: explaining the rules (118), asking to choose a mode (123) and Alexa giving us either a kids riddle (128) or an adult riddle (136). 
With the string variable alexaResponse we keep track of the last interaction, so that we know what part of the game we are on.  
  
Choosing a mode is coded in the responseRaetselart function (167).  
By implementing request containts (179) we tried to decrease that Alexa might not understand the user request.  
  
Riddles, tips and solutions are coded within the responseReatsel function (193).  
  
The class AlexaSkillSpeechlet includes the basic declarations, such as the last answer given from Alexa and the number of the given tips.  
The sounds are hard-coded in the class as well (57). Therefor we decided for a negative and a positive sound regarding the answer the user could give.  
To keep the interactions more flexible and fun, we implemented Strings containing different responses such as “Alles klar, du Alwissender!” or “Dann halt nicht.”. (62)  
To provide for all contingencies of answers, we tried to implement many possible answers the user could say. These are also coded as Strings.  
  
Things to consider in the future would be to implement more riddles and categories to continue to keep the game enjoyable. Also, one could implement a feature to be able to repeat the rules if needed, as well as being able to dive right into the game without being asked if they wanted to hear the rules, if that is stated by the user at the beginning of the interaction. Additionally, a kind of waiting sound during the time in which Alexa is awaiting the user’s answer could be a fun implementation.  
  
  
  
  
  
QUELLEN DER RÄTSEL  
https://www.raetselstunde.de/text-raetsel/knobelaufgaben/knobeleien-001.html  
http://www.raetselwahn.de/raetselarten-raetselaufgaben/  
http://www.denksport-raetsel.de/R%C3%A4tsel/Kinderr%C3%A4tsel  
https://www.kleineschule.com.de/raetsel.html  
http://www.derfaulepoet.de/?page_id=1840  
https://www.raetsel-fuer-kinder.de/  

Link zur benutzten Datenbank: https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.18.0.jar  

 
