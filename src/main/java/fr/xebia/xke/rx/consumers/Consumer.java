package fr.xebia.xke.rx.consumers;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import rx.functions.Action1;

/**
 * Xebia 2015
 */
public interface Consumer {

    public Action1<MqttMessage> getSubscriberAction();
}
