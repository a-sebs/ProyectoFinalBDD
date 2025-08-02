package application;

import MetodosFrecuentes.MetodosFrecuentes;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MetodosFrecuentes.mostrarVentana("/views/Login-view.fxml", "Login - Base de Datos Distribuidas");
    }

    public static void main(String[] args){
        launch(args);
    }

}
