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

public class TableController implements Initializable {
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnScript;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Label lblTableName;
    
    @FXML
    private TableView<ObservableList<String>> tableViewData;
    
    @FXML
    private TextArea txtScript;
    
    private String selectedTable;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Obtener la tabla seleccionada del ControllerDataManager
        String tableName = ControllerDataManager.getInstance().getSelectedTableName();
        if (tableName != null) {
            setSelectedTable(tableName);
        }
    }
    
    /**
     * Establece la tabla seleccionada y carga su contenido
     */
    public void setSelectedTable(String tableName) {
        this.selectedTable = tableName;
        lblTableName.setText("Tabla: " + tableName);
        loadTableContent();
    }
    
    /**
     * Carga todo el contenido de la tabla seleccionada
     */
    private void loadTableContent() {
        if (selectedTable == null || !SessionManager.getInstance().getConnectionUser().estaConectado()) {
            return;
        }
        
        try {
            String sql = "SELECT * FROM " + selectedTable;
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
                    "Se cargaron las primeras 500 filas de la tabla " + selectedTable + 
                    ". Use filtros SQL para ver datos específicos.");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlerta("Error", "Error al cargar el contenido de la tabla: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleScript() {
        String script = txtScript.getText().trim();
        
        if (script.isEmpty()) {
            MetodosFrecuentes.mostrarAlerta("Error de Validación", "Ingrese un script SQL para ejecutar");
            return;
        }
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            MetodosFrecuentes.mostrarAlerta("Error", "No hay conexión a la base de datos");
            return;
        }
        
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            Statement stmt = conn.createStatement();
            
            // Determinar el tipo de script
            String scriptUpper = script.toUpperCase().trim();
            boolean isSelect = scriptUpper.startsWith("SELECT");
            
            if (isSelect) {
                // Para SELECT, ejecutar y mostrar resultados en el TableView
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
                
            } else {
                // Para INSERT, UPDATE, DELETE
                int rowsAffected = stmt.executeUpdate(script);
                
                String operationType;
                if (scriptUpper.startsWith("INSERT")) {
                    operationType = "INSERT";
                } else if (scriptUpper.startsWith("UPDATE")) {
                    operationType = "UPDATE";
                } else if (scriptUpper.startsWith("DELETE")) {
                    operationType = "DELETE";
                } else {
                    operationType = "Script";
                }
                
                MetodosFrecuentes.mostrarAlerta("Éxito", 
                    operationType + " ejecutado correctamente. " + rowsAffected + " filas afectadas.");
                
                // Recargar el contenido de la tabla después de modificaciones
                if (!scriptUpper.startsWith("SELECT")) {
                    loadTableContent();
                }
            }
            
            stmt.close();
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlerta("Error en Script", 
                "Error al ejecutar el script SQL:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBack() {
        MetodosFrecuentes.cambiarVentana((Stage) btnBack.getScene().getWindow(), "/views/Master-view.fxml");
    }
    
    @FXML
    private void handleRefresh() {
        if (selectedTable != null) {
            // Recargar el contenido completo de la tabla
            loadTableContent();
            MetodosFrecuentes.mostrarAlerta("Información", "Datos de la tabla " + selectedTable + " actualizados correctamente.");
        } else {
            MetodosFrecuentes.mostrarAlerta("Error", "No hay tabla seleccionada para actualizar.");
        }
    }
}
