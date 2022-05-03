package com.example.demo;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class NewsSSEController {

    // CopyOnWriteArrayList is a thread-safe list
    //private List<SseEmitter> emitters = new CopyOnWriteArrayList<SseEmitter>(); //store all the SSE-emitters for the clients connected
    private Map<String, SseEmitter> emitters = new HashMap<String, SseEmitter>(); //store each emitter with its Client-user
    private static int msgID = 0;

    // method for client subscription, permits the connection releasing a SSE-emitter
    // that allows clients to listen the specified channel for receiving the events from the server-side
    @CrossOrigin
    @RequestMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE) //define the endpoint for the REST request
    public SseEmitter subscribe(@RequestParam String userID) throws IOException {
        //SseEmitter is the object that holds the connection between Client and Server
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE); //the time in millis before the emitter get timed-out
        sendInitEvent(sseEmitter);
        emitters.put(userID, sseEmitter);
        //emitters.add(sseEmitter);
        sseEmitter.onCompletion(() -> emitters.remove(userID)); //lambda func to remove emitters already completed from the list

        return sseEmitter;
    }

    private void sendInitEvent(SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event().id("msg ID: " + msgID).name("INIT"));
            //sseEmitter.complete();
        } catch (IOException e){
            sseEmitter.completeWithError(e);
        }
    }

    //method to dispatch events for specific clients
    //get the HTTP request in POST from the server and dispatch the events at clients
    @PostMapping(value = "/dispatchEvent") //Post method for not letting public the parameters in Rest-Request
    public void dispatchEventsToClients(@RequestParam String title, @RequestParam String text, @RequestParam String userID) {

        String eventFormatted = new JSONObject().put("title", title).put("text", text).toString(); //stringify the JSON msg
        //retrieve the emitter stored in the map with requested userID
        SseEmitter sseEmitter = emitters.get(userID);

        if (sseEmitter != null) {
            try {
                //send the event for each emitter
                sseEmitter.send(SseEmitter.event().id("msg ID: " + msgID++).name("latestNews").data(eventFormatted));
                //emitter.complete();
            } catch (IOException e) {
                emitters.remove(userID);
                //because if the client is not connected anymore, the SSE-emitter need to be removed from the list
            }
        }

        /*
        for (SseEmitter emitter : emitters) { //for each emitter on the list (so each client connected with server)
            try {
                //send the event for each emitter
                emitter.send(SseEmitter.event().id("msg ID: " + msgID++).name("latestNews").data(eventFormatted));
                //emitter.complete();
            } catch (IOException e) {
                emitters.remove(emitter);
                //because if the client is not connected anymore, the SSE-emitter need to be removed from the list
            }
        }
        */

    }

}
