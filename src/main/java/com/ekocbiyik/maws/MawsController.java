package com.ekocbiyik.maws;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MawsController {

    @FXML
    private Canvas canvas;
    @FXML
    private TextField txtIpAddress;
    @FXML
    private TextField txtPort;
    @FXML
    private Button btnConnect;
    @FXML
    private Label lblWindSpeed;
    @FXML
    private Label lblWindDirection;
    @FXML
    private Label lblAirTemperature;
    @FXML
    private Label lblBarometricPressure;
    @FXML
    private Label lblHumidity;
    @FXML
    private Label lblCompass;
    @FXML
    private Label lblLatitude;
    @FXML
    private Label lblLongitude;
    @FXML
    private Label lblExternalTemp;
    @FXML
    private TextArea txtConsole;
    
    private final double centerX = 175;
    private final double centerY = 175;
    private final double radius = 150;

    private Timeline timeline;
    private GraphicsContext gc;
    private Map<String, String> infoMap;

    @FXML
    public void initialize() {
        infoMap = new HashMap<>();
        timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> updateFields()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        drawCompass();
    }

    private void drawCompass() {
        gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
        drawNeedle(0);
    }

    private void drawNeedle(double needleAngle) {
        gc.clearRect(0, 0, 2 * radius, 2 * radius);
        gc.setStroke(Color.RED);
        gc.setLineWidth(4);

        double needleLength = radius * 0.8;
        double needleX = centerX + needleLength * Math.sin(Math.toRadians(needleAngle));
        double needleY = centerY - needleLength * Math.cos(Math.toRadians(needleAngle));
        gc.strokeLine(centerX, centerY, needleX, needleY);
    }

    public void updateFields() {

        lblWindSpeed.setText(infoMap.getOrDefault("lblWindSpeed", "---"));
        lblWindDirection.setText(infoMap.getOrDefault("lblWindDirection", "---"));
        lblAirTemperature.setText(infoMap.getOrDefault("lblAirTemperature", "---"));
        lblBarometricPressure.setText(infoMap.getOrDefault("lblBarometricPressure", "---"));
        lblHumidity.setText(infoMap.getOrDefault("lblHumidity", "---"));
        lblCompass.setText(infoMap.getOrDefault("lblCompass", "---"));
        lblLatitude.setText(infoMap.getOrDefault("lblLatitude", "---"));
        lblLongitude.setText(infoMap.getOrDefault("lblLongitude", "---"));
        lblExternalTemp.setText(infoMap.getOrDefault("lblExternalTemp", "---"));

        drawNeedle(Double.parseDouble(infoMap.getOrDefault("compassValue", "0")));
    }

    private void lockComponents(boolean isEnable) {
        btnConnect.setDisable(!isEnable);
        txtIpAddress.setDisable(!isEnable);
        txtPort.setDisable(!isEnable);
    }

    private void printLog(String line) {
        txtConsole.appendText(line + "\n");
    }

    public void btnConnectOnClick(ActionEvent event) {

        if (txtIpAddress.getText().isEmpty()) {
            MessageDialog.showError("Ip Address can not be empty!");
            return;
        }

        if (txtPort.getText().isEmpty()) {
            MessageDialog.showError("Port can not be empty!");
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {

                    byte[] mawsData = new byte[26];
                    txtConsole.clear();
                    infoMap.clear();
                    timeline.play();

                    lockComponents(false);
                    printLog("Connecting to " + txtIpAddress.getText() + ":" + txtPort.getText());

                    Socket socket = new Socket(txtIpAddress.getText(), Integer.parseInt(txtPort.getText()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    int index = 0;

                    printLog("Connection established!");

                    while (true) {
                        char[] buffer = new char[1];
                        in.read(buffer);
                        if (buffer[0] == 85) {
                            decodeMawsData(mawsData);
                            mawsData[0] = (byte) buffer[0];
                            index = 1;
                        } else {
                            mawsData[index] = (byte) buffer[0];
                            index++;
                        }
                    }

                } catch (Exception e) {
                    printLog("Connection closed!");
                    e.printStackTrace();
                } finally {
                    lockComponents(true);
                    timeline.stop();
                }
                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Arka planda çalışan bir görev olduğunu belirtir
        thread.start();
    }

    private void decodeMawsData(byte[] mawsData) {
        infoMap.put("lblWindSpeed", convertHighLowByteToFloat(new byte[]{mawsData[5], mawsData[6]}) + " m/s");
        infoMap.put("lblWindDirection", convertHighLowByteToFloat(new byte[]{mawsData[7], mawsData[8]}) + " °");
        infoMap.put("lblAirTemperature", convertHighLowByteToFloat(new byte[]{mawsData[9], mawsData[10]}) + " °C");
        infoMap.put("lblBarometricPressure", convertHighLowByteToFloat(new byte[]{mawsData[11], mawsData[12]}) + " hPa");
        infoMap.put("lblHumidity", convertHighLowByteToFloat(new byte[]{mawsData[13], mawsData[14]}) + " %");
        infoMap.put("lblCompass", convertHighLowByteToFloat(new byte[]{mawsData[15], mawsData[16]}) + " °");
        infoMap.put("lblLatitude", convertHighMediumLowByteToFloat(new byte[]{mawsData[17], mawsData[18], mawsData[19]}) + " °");
        infoMap.put("lblLongitude", convertHighMediumLowByteToFloat(new byte[]{mawsData[20], mawsData[21], mawsData[22]}) + " °");
        infoMap.put("lblExternalTemp", convertHighLowByteToFloat(new byte[]{mawsData[23], mawsData[24]}) + " °");

        infoMap.put("compassValue", convertHighLowByteToFloat(new byte[]{mawsData[15], mawsData[16]}) + "");
    }

    public static float convertHighLowByteToFloat(byte[] data) {
        return ((data[0] * 256) + data[1]) / 16.0f;
    }

    public static float convertHighMediumLowByteToFloat(byte[] data) {
        return (((data[0] & 127) * 65536) + (data[1] * 256) + data[2]) / (512 * 60.0f);
    }

}
