package fr.xebia.xke.rx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Screen;
import javafx.stage.Stage;
import rx.Observable;
import rx.observables.GroupedObservable;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Xebia 2015
 */
public class Ui extends Application {


    private Watcher watcher;

    static public void main(String[] args) throws IOException, InterruptedException {
        launch();
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        watcher = new Watcher();
        stageSetup(primaryStage);
        graphSetup(primaryStage);
        primaryStage.show();

    }


    private void graphSetup(Stage stage) {
        ObservableList<XYChart.Series<String, Float>> lineChartData = FXCollections
                .observableArrayList();
        NumberAxis yAxis = createYAxis();
        final CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Temps");
        LineChart chart = new LineChart(xAxis, yAxis, lineChartData);
        chart.setPrefWidth(1010);
        chart.setPrefHeight(400);

        stage.setScene(new Scene(chart));

        Observable<GroupedObservable<String, String>> obs = watcher.getObservable()
                .doOnError(t -> t.printStackTrace())
                .map(m -> new String(m.getPayload()))
                .groupBy(s -> s.split(":")[0]);

        obs.filter(gpr -> gpr.getKey().equals("temperature"))
           .map(gpr -> gpr.asObservable())
           .subscribe(stringObs -> {
               stringObs.map(s -> new Entry<>(s.substring(13), Instant.now().toEpochMilli()))
                       .buffer(5)
                       .subscribe(map -> {
                           refresh(lineChartData, map, "temperature", 0);
                       });
           });
        reactOnPositionEvent(obs, lineChartData, "x", 1);
        reactOnPositionEvent(obs, lineChartData, "y", 2);
        reactOnPositionEvent(obs, lineChartData, "z", 3);

    }

    private void reactOnPositionEvent(Observable<GroupedObservable<String, String>> obs,
                                      ObservableList<XYChart.Series<String, Float>> lineChartData,
                                      String coordonate,
                                      int index){

        obs.filter(gpr -> gpr.getKey().equals("position"))
                .map(gpr -> gpr.asObservable())
                .subscribe(stringObs -> {
                    stringObs
                            .flatMap(s1 -> Observable.from(s1.split(" ")))
                            .filter(s2 -> s2.contains(coordonate))
                            .map(s -> new Entry<>(s.substring(2), Instant.now().toEpochMilli()))
                            .buffer(5)
                            .subscribe(map -> {
                                refresh(lineChartData, map, coordonate, index);
                            });
                });

    }

    private void stageSetup(Stage stage) {
        stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
        stage.setHeight(Screen.getPrimary().getVisualBounds()
                .getHeight());
        stage.setX(Screen.getPrimary().getVisualBounds().getMinX());
        stage.setY(Screen.getPrimary().getVisualBounds().getMinY());
    }

    private void refresh(ObservableList<XYChart.Series<String, Float>> lineChartData, List<Entry<String, Long>> datas, String label, int pos) {
        System.out.println("Refresh");
        Platform.runLater(() -> {
            if(pos < lineChartData.size() ){
                lineChartData.remove(pos);
            }
            lineChartData.add(pos, createSerie(datas, label));
        });
    }

    private NumberAxis createYAxis() {
        return new NumberAxis("Variation", -100, 100, 5);
    }

    private XYChart.Series<String, Float> createSerie(List<Entry<String, Long>> source, String name) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH-mm-ss");
        final ObservableList<XYChart.Data<String, Float>> observableList = FXCollections
                .observableArrayList();
        source.stream().forEach(pair -> {
            XYChart.Data<String, Float> data = new XYChart.Data<>(
                    dateFormat.format(new Date(pair.getValue())),
                    Float.parseFloat(pair.getKey()));
            observableList.add(data);
        });
        return new XYChart.Series<>(
                name, observableList);
    }

}
