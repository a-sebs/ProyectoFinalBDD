package application;

import MetodosFrecuentes.MetodosFrecuentes;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("PROYECTO BASE DE DATOS");
        MetodosFrecuentes.cambiarVentana(primaryStage, "/views/Login-view.fxml");
        primaryStage.show();
    }

    public static void main(String[] args){
        launch(args);
    }

}
