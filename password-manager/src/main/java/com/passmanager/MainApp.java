package com.passmanager;

import com.passmanager.service.FileService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FileService.initDataDirectory();

        loadScene("login");

        stage.setTitle("🔐 SecureVault – Password Manager");
        stage.setMinWidth(800);
        stage.setMinHeight(580);
        stage.show();
    }

    public static void loadScene(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/passmanager/" + fxml + ".fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/passmanager/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
