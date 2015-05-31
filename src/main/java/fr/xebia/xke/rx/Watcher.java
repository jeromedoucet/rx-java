package fr.xebia.xke.rx;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;


/**
 * Xebia 2015
 */
public class Watcher {

    private final MqttClient cli;
    private final CallBack callBack;
    private Observable<MqttMessage> observable;

    public Watcher() throws MqttException {
        cli = new MqttClient("tcp://localhost:1883", "domoWatcher", new MemoryPersistence());
        callBack = new CallBack();
        create("devices");
    }

    private void create(String topic) {
        observable = Observable.create(subscriber -> {
            System.out.println("new subscriber");
            try {
                callBack.addSubscriber(subscriber);
                if (!cli.isConnected()) {
                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setCleanSession(true);
                    cli.setCallback(callBack);
                    cli.connect(connOpts);
                    cli.subscribe(topic);
                }
            } catch (MqttException e) {
                subscriber.onError(e);
            }
        });
        observable
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.io())
        .doOnError(throwable -> throwable.printStackTrace())
        .publish();
    }

    public Watcher addSubscribeAction(Action1<MqttMessage> onNext) {
        observable.subscribe(onNext);
        return this;
    }

    public Observable<MqttMessage> getObservable(){
        return observable;
    }

    private class CallBack implements MqttCallback {

        private List<Subscriber<? super MqttMessage>> subscribers = new ArrayList<>();

        public void addSubscriber(Subscriber<? super MqttMessage> subscriber) {
            subscribers.add(subscriber);
        }

        @Override
        public void connectionLost(final Throwable throwable) {
            Observable.from(subscribers).filter(s -> !s.isUnsubscribed()).subscribe(s -> s.onError(throwable));
        }

        @Override
        public void messageArrived(String topic, final MqttMessage mqttMessage) throws Exception {
            Observable.from(subscribers).filter(s -> !s.isUnsubscribed()).subscribe(s -> s.onNext(mqttMessage));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            Observable.from(subscribers).filter(s -> !s.isUnsubscribed()).subscribe(s -> s.onCompleted());
        }
    }


}
