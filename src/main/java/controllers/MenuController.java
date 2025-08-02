package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import Logic.SessionManager;
import MetodosFrecuentes.MetodosFrecuentes;
import java.net.URL;
import java.util.ResourceBundle;

public class MenuController implements Initializable {
    
    @FXML
    private Button btnMasterMode;
    
    @FXML
    private Button btnRemoteMode;
    
    @FXML
    private Button btnLogOut;
    
    @FXML
    private Label lblUser;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Establecer el nombre del usuario logueado
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            lblUser.setText(currentUser.toUpperCase());
        }
    }
    
    @FXML
    private void handleMasterMode() {
        MetodosFrecuentes.cambiarVentana((Stage) btnMasterMode.getScene().getWindow(), "/views/Master-view.fxml");
    }
    
    @FXML
    private void handleRemoteMode() {
        MetodosFrecuentes.cambiarVentana((Stage) btnRemoteMode.getScene().getWindow(), "/views/Remote-view.fxml");
    }
    
    @FXML
    private void handleLogOut() {
        try {
            // Cerrar la sesión
            SessionManager.getInstance().logout();
            
            // Cambiar a la ventana de login
            MetodosFrecuentes.cambiarVentana((Stage) btnLogOut.getScene().getWindow(), "/views/Login-view.fxml");
            
            // Mostrar mensaje de éxito
            MetodosFrecuentes.mostrarAlerta("Logout", "Sesión cerrada con éxito");
            
        } catch (Exception e) {
            MetodosFrecuentes.mostrarAlerta("Error", "Fallo al cerrar sesión");
            e.printStackTrace();
        }
    }
}
