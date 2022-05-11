package com.example.demo;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class NewsSSEController {

    // CopyOnWriteArrayList is a thread-safe list
    //private static List<SseEmitter> emitters = new CopyOnWriteArrayList<SseEmitter>(); //store all the SSE-emitters for the clients connected
    //private static Map<String, SseEmitter> emitters = new HashMap<String, SseEmitter>(); //store each emitter with its Client-user

    //FIXME ogni volta che avviene una subscribtion di un client web su un topic viene creato un nuovo emitter,
    // anche se il client in questione era già iscritto al topic
    public static Multimap<Pair<String,SseEmitter>,String> emitters = ArrayListMultimap.create(); //K: <UserID,Emitter> V: Topic

    //This map contains the last message sent by the broker for each topic
    public static Map<String,String> statusMessages = new HashMap<String, String>();
    private static int msgID = 1;

    // method for client subscription, permits the connection releasing a SSE-emitter
    // that allows clients to listen the specified channel for receiving the events from the server-side
    //FIXME Su tutti i Browsers sono ammesse non più di 6 connessioni TCP contemporanee da questa sorgente
    // (Dovrei passare ad HTTP/2 ma può essere abilitato solo con la sicurezza TLS (https))
    @CrossOrigin
    @RequestMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE) //define the endpoint for the REST request
    public SseEmitter subscribe(@RequestParam String userID, @RequestParam String topic) throws IOException {
        //SseEmitter is the object that holds the connection between Client and Server
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE); //the time in millis before the emitter get timed-out
        sendInitEvent(sseEmitter);
        emitters.put(new Pair<String, SseEmitter>(userID,sseEmitter), topic);

        String statusPayload = statusMessages.get(topic);
        System.out.println("Lo status message è: " + statusPayload);
        if (statusPayload != null) {
            String eventFormatted = new JSONObject().put("title", topic).put("text", statusPayload).toString();
            sseEmitter.send(SseEmitter.event().id("msg ID: " + 0).name("latestNews").data(eventFormatted));
        }

        //emitters.put(userID, sseEmitter);
        //emitters.add(sseEmitter);
        //lambda func to remove emitters already completed from the list
        sseEmitter.onCompletion(() -> {
            emitters.remove(new Pair<String,SseEmitter>(userID, sseEmitter),topic);
            System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " is completed");
        });
        //FIXME funziona bene in tutti i casi, tranne qualche volta quando disconnetto più client contemporaneamente
        //sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        /*sseEmitter.onError((e) -> {
            emitters.remove(userID);
            System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " had an error");
        });*/
        //FIXME funziona bene se disconnetto un client, ma se poi lo riconnetto e disconnetto un altro contemp esplode

        return sseEmitter;
    }

    /* NOTA: Dato che arrivano singoli messaggi dal broker, ogni Emitter si occupa di inviare al suo client lo stream
    * di dati che in questo caso è composto da un singolo messaggio, quindi una volta consegnato al client l'Emitter
    * viene considerato completo (per questo se chiamo complete() si chiude la connessione), quindi quando chiudo il
    * client viene sempre eseguito il codice dell'onComplition().
    * Sarebbe diverso se uno stream dell'Emitter contenesse più messaggi da consegnare, ma dato che arrivano
    * singolarmente da un Broker MQTT, questo è un caso da considerare? */

    private void sendInitEvent(SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event().id("msg ID: " + msgID).name("INIT"));
            //sseEmitter.complete(); //complete the transmission and closes the connection with clients
        } catch (IOException e){
            sseEmitter.completeWithError(e);
        }
    }

    //method to dispatch events for specific clients
    //get the HTTP request in POST from the server and dispatch the events at clients
    //@PostMapping(value = "/dispatchEvent") //Post method for not letting public the parameters in Rest-Request
    public static void dispatchEventsToClients(@RequestParam String topic, @RequestParam String payload) throws MessageHandlingException {

        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries())
            System.out.println("User client attivo: " + it.getKey() + "  listening topic:  " + it.getValue());
        //System.out.println("parametri: " + topic + " " + payload);
        String eventFormatted = new JSONObject().put("title", topic).put("text", payload).toString(); //stringify the JSON msg

        ArrayList<Pair<String,SseEmitter>> emittersByTopic = searchByTopic(topic);

        for (Pair<String,SseEmitter> pair : emittersByTopic) { //for each emitter on the list (so each client connected with server)
            System.out.println("New msg in topic: " + topic + " for User: " + pair.getKey()
                    + " with Emitter: " + pair.getValue());
            SseEmitter emitter = pair.getValue();
            try {
                //send the event for each emitter
                emitter.send(SseEmitter.event().id("msg ID: " + msgID++).name("latestNews").data(eventFormatted));
                //saves the message in the status map, replacing the previous element if the topic was already there

                //emitter.complete();
            } catch (IOException e) {
                System.out.println("Dispatcher Error");
                /* for (Map.Entry<String, SseEmitter> couple : emitters.entrySet()) {
                     if (couple.getValue() == emitter)
                        System.out.println("Client: " + couple.getKey() + " is not connected anymore");
                        //emitters.remove(couple.getKey()); //FIXME esplode alla disconnessione di un client
                 }*/
            }
        }
    }

    private static ArrayList<Pair<String,SseEmitter>> searchByTopic(String topic) {
        ArrayList<Pair<String,SseEmitter>> emitter = new ArrayList<Pair<String, SseEmitter>>();
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getValue().equals(topic)){
                //System.out.println(it.getKey());
                emitter.add(it.getKey());
            }
        }
        return emitter;
    }

    public static void addStatusMessage(String topic, String payload) {
        statusMessages.put(topic,payload);
    }

    public static void unsubscribeTopic(String user, String topic) {
        SseEmitter sseEmitter = null;
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getValue().equals(topic) && it.getKey().getKey().equals(user)){
               // System.out.println(it.getKey().getKey());
                sseEmitter = it.getKey().getValue();
            }
        }
        try {
            emitters.remove(new Pair<String,SseEmitter>(user, sseEmitter),topic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}