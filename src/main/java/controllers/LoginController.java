package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import Logic.ConnectionUser;
import Logic.SessionManager;
import MetodosFrecuentes.MetodosFrecuentes;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    
    @FXML
    private TextField txtUsuario;
    
    @FXML
    private PasswordField txtContrasenia;
    
    @FXML
    private Button btnIngresar;
    
    // Instancia de conexión a Oracle
    private ConnectionUser connectionUser;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("LoginController inicializado correctamente");
        connectionUser = new ConnectionUser();
    }
    
    @FXML
    private void handleLogin() {
        String usuario = txtUsuario.getText().trim();
        String password = txtContrasenia.getText().trim();
        
        // Validar que los campos no estén vacíos
        if (usuario.isEmpty()) {
            MetodosFrecuentes.mostrarAlerta("Error de Validación", 
                "El campo usuario es obligatorio.");
            txtUsuario.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            MetodosFrecuentes.mostrarAlerta("Error de Validación", 
                "El campo contraseña es obligatorio.");
            txtContrasenia.requestFocus();
            return;
        }
        
        // Deshabilitar el botón mientras se conecta
        btnIngresar.setDisable(true);
        btnIngresar.setText("Conectando...");
        
        try {
            // Intentar conectar a Oracle
            boolean conexionExitosa = connectionUser.conectar(usuario, password);
            
            if (conexionExitosa) {
                // Guardar la sesión del usuario
                SessionManager.getInstance().setCurrentUser(usuario);
                SessionManager.getInstance().setConnectionUser(connectionUser);
                
                // Obtener información del servidor
                String infoServidor = connectionUser.obtenerInfoServidor();

                // Mostrar mensaje de éxito con información del servidor
                String mensaje = "¡Conexión exitosa!\n\n";
                if (infoServidor != null) {
                    mensaje += infoServidor + "\n";
                }
                
                MetodosFrecuentes.mostrarAlerta("Conexión Exitosa", mensaje);
                
                // Cambiar a la ventana del menú después del login exitoso
                MetodosFrecuentes.cambiarVentana((javafx.stage.Stage) btnIngresar.getScene().getWindow(), "/views/Menu-view.fxml");
                
                // Limpiar campos por seguridad
                limpiarCampos();
                
            } else {
                MetodosFrecuentes.mostrarAlerta("Error de Conexión", 
                    "No se pudo conectar a la base de datos Oracle.\n\n" +
                    "Verifique:\n" +
                    "• Usuario y contraseña correctos\n" +
                    "• Que el servicio Oracle esté ejecutándose\n" +
                    "• Conectividad de red (localhost:1521)\n" +
                    "• Que la base de datos ORCL esté disponible");
            }
            
        } catch (Exception e) {
            MetodosFrecuentes.mostrarAlerta("Error Inesperado", 
                "Ocurrió un error inesperado al intentar conectar:\n\n" + e.getMessage());
            e.printStackTrace();
        } finally {
            // Rehabilitar el botón
            btnIngresar.setDisable(false);
            btnIngresar.setText("INGRESAR");
        }
    }
    
    /**
     * Limpia los campos de usuario y contraseña
     */
    private void limpiarCampos() {
        txtUsuario.clear();
        txtContrasenia.clear();
        txtUsuario.requestFocus();
    }
    
    /**
     * Obtiene la instancia de conexión actual
     * @return ConnectionUser instance
     */
    public ConnectionUser getConnectionUser() {
        return connectionUser;
    }
    
    /**
     * Método para cerrar la conexión cuando se cierre la ventana
     */
    public void cerrarConexion() {
        if (connectionUser != null && connectionUser.estaConectado()) {
            connectionUser.desconectar();
        }
    }
}
