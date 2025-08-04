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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class VistaController implements Initializable {
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnScript;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnEliminarFila;
    
    @FXML
    private Button btnModificarFila;
    
    @FXML
    private Button btnInsertarFila;
    
    @FXML
    private Label lblVistaName;
    
    @FXML
    private TableView<ObservableList<String>> tableViewData;
    
    @FXML
    private TextArea txtScript;
    
    private String selectedVistaOrTable;
    private String tipoSeleccionado; // "VISTA" o "TABLA"
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Obtener la vista o tabla seleccionada del ControllerDataManager
        String vistaName = ControllerDataManager.getInstance().getSelectedVistaName();
        String tableName = ControllerDataManager.getInstance().getSelectedTableName();
        
        // Inicialmente ocultar los botones de operaciones de tabla
        btnEliminarFila.setVisible(false);
        btnModificarFila.setVisible(false);
        btnInsertarFila.setVisible(false);
        
        if (vistaName != null) {
            setSelectedVistaOrTable(vistaName, "VISTA");
        } else if (tableName != null) {
            setSelectedVistaOrTable(tableName, "TABLA");
        }
    }
    
    /**
     * Establece la vista o tabla seleccionada y carga su contenido
     */
    public void setSelectedVistaOrTable(String name, String tipo) {
        this.selectedVistaOrTable = name;
        this.tipoSeleccionado = tipo;
        lblVistaName.setText(tipo + ": " + name);
        
        // Mostrar u ocultar botones según si es vista o tabla
        boolean esTabla = "TABLA".equals(tipo);
        btnEliminarFila.setVisible(esTabla);
        btnModificarFila.setVisible(esTabla);
        btnInsertarFila.setVisible(esTabla);
        
        loadContent();
    }
    
    /**
     * Carga todo el contenido de la vista o tabla seleccionada
     */
    private void loadContent() {
        if (selectedVistaOrTable == null || !SessionManager.getInstance().getConnectionUser().estaConectado()) {
            return;
        }
        
        try {
            String sql = "SELECT * FROM " + selectedVistaOrTable + " WHERE ROWNUM <= 500";
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            System.out.println("Cargando datos de: " + selectedVistaOrTable);
            
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
                    "Se cargaron las primeras 500 filas de " + tipoSeleccionado.toLowerCase() + " " + selectedVistaOrTable + 
                    ". Use filtros SQL para ver datos específicos.");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlertaError("Error", "Error al cargar el contenido de " + tipoSeleccionado.toLowerCase() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleScript() {
        String script = txtScript.getText().trim();
        
        if (script.isEmpty()) {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error de Validación", "Ingrese una consulta SQL para ejecutar");
            return;
        }
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay conexión a la base de datos");
            return;
        }
        
        // En modo remoto, solo permitir consultas SELECT
        String scriptUpper = script.toUpperCase().trim();
        if (!scriptUpper.startsWith("SELECT")) {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error de Validación", 
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
            
            while (rs.next() && count < 500) {
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
            MetodosFrecuentes.mostrarAlertaError("Error en Consulta", 
                "Error al ejecutar la consulta SQL:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBack() {
        // Limpiar los datos del ControllerDataManager
        ControllerDataManager.getInstance().clearData();
        MetodosFrecuentes.cambiarVentana((Stage) btnBack.getScene().getWindow(), "/views/Remote-view.fxml");
    }
    
    @FXML
    private void handleRefresh() {
        if (selectedVistaOrTable != null) {
            // Recargar el contenido completo
            loadContent();
            MetodosFrecuentes.mostrarAlerta("Información", "Datos de " + tipoSeleccionado.toLowerCase() + " " + selectedVistaOrTable + " actualizados correctamente.");
        } else {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error", "No hay " + tipoSeleccionado.toLowerCase() + " seleccionada para actualizar.");
        }
    }
    
    @FXML
    private void handleEliminarFila() {
        ObservableList<String> filaSeleccionada = tableViewData.getSelectionModel().getSelectedItem();
        
        if (filaSeleccionada == null) {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error de Selección", "Seleccione una fila para eliminar");
            return;
        }
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay conexión a la base de datos");
            return;
        }
        
        try {
            // Obtener las claves primarias de la tabla
            List<String> primaryKeyColumns = obtenerClavesPrimarias(selectedVistaOrTable);
            
            if (primaryKeyColumns.isEmpty()) {
                MetodosFrecuentes.mostrarAlertaAdvertencia("Error", "No se pueden eliminar filas: la tabla no tiene clave primaria definida");
                return;
            }
            
            // Construir la consulta DELETE
            StringBuilder sql = new StringBuilder("DELETE FROM " + selectedVistaOrTable + " WHERE ");
            List<String> valoresWhere = new ArrayList<>();
            
            for (int i = 0; i < primaryKeyColumns.size(); i++) {
                if (i > 0) {
                    sql.append(" AND ");
                }
                
                String columnName = primaryKeyColumns.get(i);
                sql.append(columnName).append(" = ?");
                
                // Obtener el índice de la columna en el TableView
                int columnIndex = obtenerIndiceColumna(columnName);
                if (columnIndex >= 0 && columnIndex < filaSeleccionada.size()) {
                    valoresWhere.add(filaSeleccionada.get(columnIndex));
                }
            }
            
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            
            // Establecer parámetros
            for (int i = 0; i < valoresWhere.size(); i++) {
                pstmt.setString(i + 1, valoresWhere.get(i));
            }
            
            int filasAfectadas = pstmt.executeUpdate();
            pstmt.close();
            
            if (filasAfectadas > 0) {
                MetodosFrecuentes.mostrarAlerta("Éxito", "Fila eliminada correctamente");
                loadContent(); // Recargar datos
            } else {
                MetodosFrecuentes.mostrarAlertaAdvertencia("Advertencia", "No se eliminó ninguna fila");
            }
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlertaError("Error en Base de Datos", 
                "Error al eliminar la fila:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleModificarFila() {
        ObservableList<String> filaSeleccionada = tableViewData.getSelectionModel().getSelectedItem();
        
        if (filaSeleccionada == null) {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error de Selección", "Seleccione una fila para modificar");
            return;
        }
        
        // Configurar datos para el UpdateController
        ControllerDataManager.getInstance().setSelectedTableName(selectedVistaOrTable);
        ControllerDataManager.getInstance().setOperacion("UPDATE");
        ControllerDataManager.getInstance().setDatosOriginales(filaSeleccionada);
        ControllerDataManager.getInstance().setSelectedVistaName(null);
        
        // Cambiar a la vista Update-view
        MetodosFrecuentes.cambiarVentana((Stage) btnModificarFila.getScene().getWindow(), "/views/Update-view.fxml");
    }
    
    @FXML
    private void handleInsertarFila() {
        // Configurar datos para el UpdateController
        ControllerDataManager.getInstance().setSelectedTableName(selectedVistaOrTable);
        ControllerDataManager.getInstance().setOperacion("INSERT");
        ControllerDataManager.getInstance().setDatosOriginales(null);
        ControllerDataManager.getInstance().setSelectedVistaName(null);
        
        // Cambiar a la vista Update-view
        MetodosFrecuentes.cambiarVentana((Stage) btnInsertarFila.getScene().getWindow(), "/views/Update-view.fxml");
    }
    
    /**
     * Obtiene las columnas de clave primaria de una tabla
     */
    private List<String> obtenerClavesPrimarias(String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        
        try {
            String sql;
            String baseTableName;
            
            // Verificar si es una tabla remota
            if (tableName.contains("@PROYECTO_REAL")) {
                baseTableName = tableName.replace("@PROYECTO_REAL", "");
                sql = "SELECT COLUMN_NAME " +
                      "FROM USER_CONS_COLUMNS@PROYECTO_REAL ucc " +
                      "JOIN USER_CONSTRAINTS@PROYECTO_REAL uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                      "WHERE uc.TABLE_NAME = ? AND uc.CONSTRAINT_TYPE = 'P' " +
                      "ORDER BY ucc.POSITION";
            } else {
                baseTableName = tableName;
                sql = "SELECT COLUMN_NAME " +
                      "FROM USER_CONS_COLUMNS ucc " +
                      "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                      "WHERE uc.TABLE_NAME = ? AND uc.CONSTRAINT_TYPE = 'P' " +
                      "ORDER BY ucc.POSITION";
            }
            
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, baseTableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo claves primarias: " + e.getMessage());
        }
        
        return primaryKeys;
    }
    
    /**
     * Obtiene el índice de una columna en el TableView actual
     */
    private int obtenerIndiceColumna(String columnName) {
        for (int i = 0; i < tableViewData.getColumns().size(); i++) {
            if (columnName.equals(tableViewData.getColumns().get(i).getText())) {
                return i;
            }
        }
        return -1;
    }
}
