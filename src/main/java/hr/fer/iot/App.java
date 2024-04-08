package hr.fer.iot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hello world!
 *
 */
public class App {

    private static String broker = "tcp://localhost:1884";
    private static String clientId = "demo_client";
    private static String topic = "device/+/sensor/+";

    private static Map<String, List<StoredMessage>> db = new HashMap<>();

    private static void readAndSendActuatorMessage(String deviceId, int movement, MqttClient client) {
        long currentTimeMillis = System.currentTimeMillis();
        int count = 0;
        List<StoredMessage> readings = db.get(deviceId);

        if (movement != 1)
            return;

        if (deviceId.equals("ESP")) {
            for (StoredMessage message : readings) {
                if (currentTimeMillis - message.getTime() <= 60 * 1000) {
                    count++;
                }
            }

            if (count > 2) {
                MqttMessage newMessage = new MqttMessage(
                        String.format("Device \"%s\" had %d movements in last two minutes.", deviceId, count)
                                .getBytes());
                newMessage.setQos(1);
                try {
                    client.publish("/device/WED", newMessage);
                } catch (MqttException e) {
                    System.err.println("Error sending message to broker");
                    e.printStackTrace();
                }
            }
        } else if (deviceId.equals("WED")) {
            MqttMessage newMessage = new MqttMessage(
                    String.format("Device \"%s\" had %d movement.", deviceId)
                            .getBytes());
            newMessage.setQos(1);
            try {
                client.publish("/device/ESP", newMessage);
            } catch (MqttException e) {
                System.err.println("Error sending message to broker");
                e.printStackTrace();
            }

        }

    }

    private static void listen(String topic, MqttMessage message, ObjectMapper objectMapper, MqttClient client) {

        try {
            String[] parts = topic.split("/");
            assert parts.length == 4;

            String deviceId = parts[1];
            String sensorType = parts[3];

            Message receivedMessage = objectMapper.readValue(message.getPayload(), Message.class);

            if (!db.containsKey(deviceId)) {
                db.put(deviceId, new ArrayList<StoredMessage>());
            }

            List<StoredMessage> messages = db.get(deviceId);
            StoredMessage storedMessage = new StoredMessage(deviceId, receivedMessage, sensorType,
                    System.currentTimeMillis());
            messages.add(storedMessage);

            if (receivedMessage.getMovement() == 1 && deviceId.equals("ESP")) {
                readAndSendActuatorMessage(deviceId, receivedMessage.getMovement(), client);
            }

        } catch (IOException e) {
            System.err.println("Error parising message to JSON type: \"" + new String(message.getPayload()) + "\"");
        }
    }

    public static void main(String[] args) {
        System.out.println("CTRL started");
        ObjectMapper objectMapper = new ObjectMapper();

        try (MqttClient client = new MqttClient(broker, clientId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            client.setCallback(new MqttCallback() {
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    listen(topic, message, objectMapper, client);
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost: " + cause.getMessage());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete: " + token.isComplete());
                }
            });

            int qos = 1;
            client.subscribe(topic, qos);

        } catch (MqttException e) {
            System.err.println("Error connecting to broker");
            e.printStackTrace();
            if (!e.getMessage().equals("Client is connected")) {
            }
        }
    }
}
