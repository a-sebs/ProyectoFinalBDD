package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import Logic.SessionManager;
import Logic.ControllerDataManager;
import MetodosFrecuentes.MetodosFrecuentes;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class RemoteController implements Initializable {
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnScript;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Label lblVistaName;
    
    @FXML
    private TableView<ObservableList<String>> tableViewData;
    
    @FXML
    private TextArea txtScript;
    
    private String selectedVista;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Obtener la vista seleccionada del ControllerDataManager
        String vistaName = ControllerDataManager.getInstance().getSelectedVistaName();
        if (vistaName != null) {
            setSelectedVista(vistaName);
        }
    }
    
    /**
     * Establece la vista seleccionada y carga su contenido
     */
    public void setSelectedVista(String vistaName) {
        this.selectedVista = vistaName;
        lblVistaName.setText("Vista: " + vistaName);
        loadVistaContent();
    }
    
    /**
     * Carga todo el contenido de la vista seleccionada
     */
    private void loadVistaContent() {
        if (selectedVista == null || !SessionManager.getInstance().getConnectionUser().estaConectado()) {
            return;
        }
        
        try {
            String sql = "SELECT * FROM " + selectedVista;
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            // Limpiar el TableView antes de cargar nuevos datos
            tableViewData.getColumns().clear();
            tableViewData.getItems().clear();
            
            // Obtener metadatos para crear las columnas
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Crear columnas dinámicamente
            for (int i = 0; i < columnCount; i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i + 1));
                column.setCellValueFactory(cellData -> {
                    ObservableList<String> row = cellData.getValue();
                    return new SimpleStringProperty(colIndex < row.size() ? row.get(colIndex) : "");
                });
                column.setPrefWidth(120); // Ancho fijo para las columnas
                tableViewData.getColumns().add(column);
            }
            
            // Cargar datos
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            int rowCount = 0;
            
            while (rs.next() && rowCount < 500) { // Limitar a 500 filas para rendimiento
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "NULL");
                }
                data.add(row);
                rowCount++;
            }
            
            tableViewData.setItems(data);
            
            // Mostrar información sobre los datos cargados
            if (rowCount == 500) {
                MetodosFrecuentes.mostrarAlerta("Información", 
                    "Se cargaron las primeras 500 filas de la vista " + selectedVista + 
                    ". Use filtros SQL para ver datos específicos.");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlerta("Error", "Error al cargar el contenido de la vista: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleScript() {
        String script = txtScript.getText().trim();
        
        if (script.isEmpty()) {
            MetodosFrecuentes.mostrarAlerta("Error de Validación", "Ingrese una consulta SQL para ejecutar");
            return;
        }
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            MetodosFrecuentes.mostrarAlerta("Error", "No hay conexión a la base de datos");
            return;
        }
        
        // En modo remoto, solo permitir consultas SELECT
        String scriptUpper = script.toUpperCase().trim();
        if (!scriptUpper.startsWith("SELECT")) {
            MetodosFrecuentes.mostrarAlerta("Error de Validación", 
                "En modo remoto solo se permiten consultas SELECT.\n" +
                "No se permiten operaciones de modificación (INSERT, UPDATE, DELETE).");
            return;
        }
        
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            Statement stmt = conn.createStatement();
            
            // Ejecutar solo consultas SELECT
            ResultSet rs = stmt.executeQuery(script);
            
            // Limpiar el TableView
            tableViewData.getColumns().clear();
            tableViewData.getItems().clear();
            
            // Obtener metadatos para crear las columnas
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Crear columnas dinámicamente
            for (int i = 0; i < columnCount; i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i + 1));
                column.setCellValueFactory(cellData -> {
                    ObservableList<String> row = cellData.getValue();
                    return new SimpleStringProperty(colIndex < row.size() ? row.get(colIndex) : "");
                });
                column.setPrefWidth(120);
                tableViewData.getColumns().add(column);
            }
            
            // Cargar datos del resultado
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            int count = 0;
            
            while (rs.next() && count < 200) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "NULL");
                }
                data.add(row);
                count++;
            }
            
            tableViewData.setItems(data);
            MetodosFrecuentes.mostrarAlerta("Éxito", "Consulta SELECT ejecutada correctamente. " + count + " filas encontradas.");
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlerta("Error en Consulta", 
                "Error al ejecutar la consulta SQL:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBack() {
        MetodosFrecuentes.cambiarVentana((Stage) btnBack.getScene().getWindow(), "/views/Menu-view.fxml");
    }
    
    @FXML
    private void handleRefresh() {
        if (selectedVista != null) {
            // Recargar el contenido completo de la vista
            loadVistaContent();
            MetodosFrecuentes.mostrarAlerta("Información", "Datos de la vista " + selectedVista + " actualizados correctamente.");
        } else {
            MetodosFrecuentes.mostrarAlerta("Error", "No hay vista seleccionada para actualizar.");
        }
    }
}
