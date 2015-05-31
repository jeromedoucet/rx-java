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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Xebia 2015
 */
public class Ui extends Application {

    private List<String> temperatures;

    private List<Position> positions;

    private Watcher watcher;

    static public void main(String[] args) throws IOException, InterruptedException {
        launch();
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        watcher = new Watcher();
        temperatures = new ArrayList<>();
        stageSetup(primaryStage);
        graphSetup(primaryStage);
        primaryStage.show();

    }


    private void graphSetup(Stage stage) {
        ObservableList<XYChart.Series<String, Float>> lineChartData = FXCollections
                .observableArrayList();
        final XYChart.Series<String, Float> series = createSerie();
        lineChartData.add(series);

        NumberAxis yAxis = createYAxis();
        final CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Temps");
        LineChart chart = new LineChart(xAxis, yAxis, lineChartData);
        chart.setPrefWidth(1010);
        chart.setPrefHeight(400);

        stage.setScene(new Scene(chart));
        watcher.getObservable().subscribe(m -> {
            Observable.just(m)
                    .map(mqttMessage -> new String(mqttMessage.getPayload()))
                    .filter(s -> s.startsWith("temperature"))
                    .subscribe(s1 -> {
                        System.out.println(s1);
                        temperatures.add(s1.substring(13));
                        refresh(lineChartData);
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

    private void refresh(ObservableList<XYChart.Series<String, Float>> lineChartData) {
        System.out.println("Refresh");
        Platform.runLater(() -> {
            lineChartData.clear();
            lineChartData.add(createSerie());
        });
    }

    private NumberAxis createYAxis() {
        return new NumberAxis("Variation", 0, 40, 0.1);
    }

    private XYChart.Series<String, Float> createSerie() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH-mm-ss");
        final ObservableList<XYChart.Data<String, Float>> observableList = FXCollections
                .observableArrayList();
        temperatures.stream().forEach(temperature -> {
            XYChart.Data<String, Float> data = new XYChart.Data<>(
                    dateFormat.format(new Date(Instant.now().toEpochMilli())),
                    Float.parseFloat(temperature));
            observableList.add(data);
        });
        return new XYChart.Series<>(
                "Sensor", observableList);
    }
}
