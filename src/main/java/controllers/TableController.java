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
import java.util.List;
import java.util.ArrayList;

public class TableController implements Initializable {
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnScript;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnInsertar;
    
    @FXML
    private Button btnModificar;
    
    @FXML
    private Button btnEliminar;
    
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
                    String value = MetodosFrecuentes.getColumnValue(rs, i, metaData);
                    row.add(value);
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
                        String value = MetodosFrecuentes.getColumnValue(rs, i, metaData);
                        row.add(value);
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
    
    /**
     * Maneja el botón insertar - abre el formulario para insertar una nueva fila
     */
    @FXML
    private void handleInsertar() {
        if (selectedTable == null) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay tabla seleccionada");
            return;
        }
        
        // Verificar que es una tabla (no vista) para permitir INSERT
        if (!esTablaModificable(selectedTable)) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No se puede insertar en vistas o tablas de solo lectura");
            return;
        }
        
        // Configurar datos para el formulario de inserción
        ControllerDataManager.getInstance().setSelectedTableName(selectedTable);
        ControllerDataManager.getInstance().setOperacion("INSERT");
        ControllerDataManager.getInstance().setDatosOriginales(null);
        ControllerDataManager.getInstance().setOrigenContexto("MASTER"); // Marcar origen como MASTER
        
        // Cambiar a formulario de actualización
        MetodosFrecuentes.cambiarVentana((Stage) btnInsertar.getScene().getWindow(), "/views/Update-view.fxml");
    }
    
    /**
     * Maneja el botón modificar - abre el formulario para modificar la fila seleccionada
     */
    @FXML
    private void handleModificar() {
        if (selectedTable == null) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay tabla seleccionada");
            return;
        }
        
        // Verificar que hay una fila seleccionada
        ObservableList<String> filaSeleccionada = tableViewData.getSelectionModel().getSelectedItem();
        if (filaSeleccionada == null) {
            MetodosFrecuentes.mostrarAlertaError("Error", "Seleccione una fila para modificar");
            return;
        }
        
        // Verificar que es una tabla modificable
        if (!esTablaModificable(selectedTable)) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No se puede modificar vistas o tablas de solo lectura");
            return;
        }
        
        // Obtener claves primarias para validar que se puede modificar
        List<String> clavesPrimarias = obtenerClavesPrimarias(selectedTable);
        if (clavesPrimarias.isEmpty()) {
            MetodosFrecuentes.mostrarAlertaError("Error", 
                "Esta tabla no tiene clave primaria definida. No se puede modificar de forma segura.");
            return;
        }
        
        // Preparar datos para el formulario
        ControllerDataManager.getInstance().setSelectedTableName(selectedTable);
        ControllerDataManager.getInstance().setOperacion("UPDATE");
        ControllerDataManager.getInstance().setDatosOriginales(filaSeleccionada);
        ControllerDataManager.getInstance().setOrigenContexto("MASTER"); // Marcar origen como MASTER
        
        // Cambiar a formulario de actualización
        MetodosFrecuentes.cambiarVentana((Stage) btnModificar.getScene().getWindow(), "/views/Update-view.fxml");
    }
    
    /**
     * Maneja el botón eliminar - elimina la fila seleccionada
     */
    @FXML
    private void handleEliminar() {
        if (selectedTable == null) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay tabla seleccionada");
            return;
        }
        
        // Verificar que hay una fila seleccionada
        ObservableList<String> filaSeleccionada = tableViewData.getSelectionModel().getSelectedItem();
        if (filaSeleccionada == null) {
            MetodosFrecuentes.mostrarAlertaError("Error", "Seleccione una fila para eliminar");
            return;
        }
        
        // Verificar que es una tabla modificable
        if (!esTablaModificable(selectedTable)) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No se puede eliminar de vistas o tablas de solo lectura");
            return;
        }
        
        // Obtener claves primarias
        List<String> clavesPrimarias = obtenerClavesPrimarias(selectedTable);
        if (clavesPrimarias.isEmpty()) {
            MetodosFrecuentes.mostrarAlertaError("Error", 
                "Esta tabla no tiene clave primaria definida. No se puede eliminar de forma segura.");
            return;
        }
        
        // Confirmar eliminación
        boolean confirmado = MetodosFrecuentes.mostrarConfirmacion("Confirmar Eliminación", 
            "¿Está seguro que desea eliminar la fila seleccionada?\n\nEsta acción no se puede deshacer.");
        
        if (!confirmado) {
            return;
        }
        
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            
            // Construir consulta DELETE usando claves primarias
            StringBuilder sqlBuilder = new StringBuilder("DELETE FROM ");
            sqlBuilder.append(selectedTable).append(" WHERE ");
            
            List<String> condiciones = new ArrayList<>();
            List<Object> valores = new ArrayList<>();
            
            // Obtener nombres de columnas
            List<String> nombreColumnas = obtenerNombresColumnas(selectedTable);
            
            for (String clavePrimaria : clavesPrimarias) {
                int indiceColumna = nombreColumnas.indexOf(clavePrimaria);
                if (indiceColumna >= 0 && indiceColumna < filaSeleccionada.size()) {
                    String valor = filaSeleccionada.get(indiceColumna);
                    if (!"NULL".equals(valor)) {
                        condiciones.add(clavePrimaria + " = ?");
                        valores.add(valor);
                    }
                }
            }
            
            if (condiciones.isEmpty()) {
                MetodosFrecuentes.mostrarAlertaError("Error", "No se pueden identificar las claves primarias para eliminar");
                return;
            }
            
            sqlBuilder.append(String.join(" AND ", condiciones));
            
            PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < valores.size(); i++) {
                pstmt.setObject(i + 1, valores.get(i));
            }
            
            int filasAfectadas = pstmt.executeUpdate();
            pstmt.close();
            
            if (filasAfectadas > 0) {
                // Realizar commit automático y refrescar vistas materializadas
                MetodosFrecuentes.realizarCommitYRefrescarVistas();
                MetodosFrecuentes.mostrarAlerta("Éxito", "Fila eliminada correctamente");
                loadTableContent(); // Recargar datos
            } else {
                MetodosFrecuentes.mostrarAlertaError("Error", "No se pudo eliminar la fila");
            }
            
        } catch (SQLException e) {
            // Realizar rollback automático en caso de error
            MetodosFrecuentes.realizarRollbackAutomatico();
            MetodosFrecuentes.mostrarAlertaError("Error", "Error al eliminar la fila: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Verifica si una tabla es modificable (no es vista)
     */
    private boolean esTablaModificable(String tableName) {
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            
            // Verificar si es una vista
            String sql = "SELECT COUNT(*) FROM USER_VIEWS WHERE VIEW_NAME = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            
            rs.next();
            boolean esVista = rs.getInt(1) > 0;
            
            rs.close();
            pstmt.close();
            
            return !esVista; // Es modificable si NO es vista
            
        } catch (SQLException e) {
            System.err.println("Error verificando si es tabla modificable: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene las columnas de clave primaria de una tabla
     */
    private List<String> obtenerClavesPrimarias(String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        
        try {
            String sql = "SELECT COLUMN_NAME " +
                        "FROM USER_CONS_COLUMNS ucc " +
                        "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                        "WHERE uc.TABLE_NAME = ? AND uc.CONSTRAINT_TYPE = 'P' " +
                        "ORDER BY ucc.POSITION";
            
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tableName.toUpperCase());
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
     * Obtiene los nombres de las columnas de una tabla
     */
    private List<String> obtenerNombresColumnas(String tableName) {
        List<String> columnNames = new ArrayList<>();
        
        try {
            String sql = "SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID";
            
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                columnNames.add(rs.getString("COLUMN_NAME"));
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo nombres de columnas: " + e.getMessage());
        }
        
        return columnNames;
    }
}
