package com.example.demo;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DemoSseApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoSseApplication.class, args);

        /*
		//MqttClient is synchronous, for the asynchronous one create MqttAsyncClient
        IMqttClient client = new MqttClient("tcp://localhost:1883", "Gabriele05");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        client.connect();

        SensorMock sm = new SensorMock(client);
        sm.call();

        CountDownLatch receivedSignal = new CountDownLatch(10);

        client.subscribe(SensorMock.TOPIC, (topic, msg) -> {
            byte[] payload = msg.getPayload();
            // ... payload handling omitted
            System.out.println(Arrays.toString(payload));
            receivedSignal.countDown();
        });
        try {
            receivedSignal.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

	}

}
