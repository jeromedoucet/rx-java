package fr.xebia.xke.rx.consumers;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import rx.Observable;
import rx.functions.Action1;



/**
 * Xebia 2015
 */
public class TemperatureConsumer implements Consumer {

    public TemperatureConsumer(){
    }

    public final Action1<MqttMessage> getSubscriberAction(){
        return m -> {
            Observable.just(m)
                    .map(mqttMessage -> new String(mqttMessage.getPayload()))
                    .filter(s -> s.startsWith("temperature"))
                    .subscribe(s1 -> System.out.println("temperature : " + s1));

        };
    }

}
