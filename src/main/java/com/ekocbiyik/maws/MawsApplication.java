package com.ekocbiyik.maws;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MawsApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.setScene(new Scene(new FXMLLoader(MawsApplication.class.getResource("/layouts/maws.fxml")).load(), 710, 610));
        stage.getIcons().add(new Image("/compass2.png"));
        stage.setTitle("Maws 6056HCGPS");
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}