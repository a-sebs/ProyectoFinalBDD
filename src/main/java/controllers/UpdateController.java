package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import Logic.SessionManager;
import Logic.ControllerDataManager;
import MetodosFrecuentes.MetodosFrecuentes;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class UpdateController implements Initializable {
    
    @FXML
    private Button btnGuardar;
    
    @FXML
    private Button btnCancelar;
    
    @FXML
    private Button btnLimpiar;
    
    @FXML
    private Label lblTitulo;
    
    @FXML
    private Label lblTablaName;
    
    @FXML
    private VBox vboxFormulario;
    
    private String tableName;
    private String operacion; // "INSERT", "UPDATE"
    private ObservableList<String> datosOriginales; // Para UPDATE, contiene los datos de la fila seleccionada
    private List<String> columnNames;
    private List<String> columnTypes;
    private List<String> primaryKeyColumns;
    private Map<String, TextField> camposFormulario;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        camposFormulario = new HashMap<>();
        
        // Obtener datos del ControllerDataManager
        tableName = ControllerDataManager.getInstance().getSelectedTableName();
        operacion = ControllerDataManager.getInstance().getOperacion();
        datosOriginales = ControllerDataManager.getInstance().getDatosOriginales();
        
        if (tableName != null) {
            String baseTableName = tableName.contains("@PROYECTO_REAL") 
                                 ? tableName.replace("@PROYECTO_REAL", "") 
                                 : tableName;
            lblTablaName.setText("Tabla: " + baseTableName);
            
            if ("INSERT".equals(operacion)) {
                lblTitulo.setText("INSERTAR NUEVO REGISTRO");
            } else if ("UPDATE".equals(operacion)) {
                lblTitulo.setText("ACTUALIZAR REGISTRO");
            }
            
            cargarEstructuraTabla();
            crearFormulario();
        } else {
            MetodosFrecuentes.mostrarAlertaError("Error", "No se pudo obtener información de la tabla");
        }
    }
    
    /**
     * Carga la estructura de la tabla (columnas, tipos, claves primarias)
     */
    private void cargarEstructuraTabla() {
        columnNames = new ArrayList<>();
        columnTypes = new ArrayList<>();
        primaryKeyColumns = new ArrayList<>();
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            return;
        }
        
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            
            // Verificar si es tabla remota
            String sql;
            String baseTableName;
            
            if (tableName.contains("@PROYECTO_REAL")) {
                baseTableName = tableName.replace("@PROYECTO_REAL", "");
                sql = "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE " +
                      "FROM USER_TAB_COLUMNS@PROYECTO_REAL " +
                      "WHERE TABLE_NAME = ? " +
                      "ORDER BY COLUMN_ID";
            } else {
                baseTableName = tableName;
                sql = "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE " +
                      "FROM USER_TAB_COLUMNS " +
                      "WHERE TABLE_NAME = ? " +
                      "ORDER BY COLUMN_ID";
            }
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, baseTableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                columnNames.add(rs.getString("COLUMN_NAME"));
                String dataType = rs.getString("DATA_TYPE");
                int dataLength = rs.getInt("DATA_LENGTH");
                columnTypes.add(dataType + "(" + dataLength + ")");
            }
            
            rs.close();
            pstmt.close();
            
            // Obtener claves primarias
            if (tableName.contains("@PROYECTO_REAL")) {
                sql = "SELECT COLUMN_NAME " +
                      "FROM USER_CONS_COLUMNS@PROYECTO_REAL ucc " +
                      "JOIN USER_CONSTRAINTS@PROYECTO_REAL uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                      "WHERE uc.TABLE_NAME = ? AND uc.CONSTRAINT_TYPE = 'P' " +
                      "ORDER BY ucc.POSITION";
            } else {
                sql = "SELECT COLUMN_NAME " +
                      "FROM USER_CONS_COLUMNS ucc " +
                      "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                      "WHERE uc.TABLE_NAME = ? AND uc.CONSTRAINT_TYPE = 'P' " +
                      "ORDER BY ucc.POSITION";
            }
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, baseTableName.toUpperCase());
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                primaryKeyColumns.add(rs.getString("COLUMN_NAME"));
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlertaError("Error", "Error al cargar estructura de la tabla: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea el formulario dinámico basado en las columnas de la tabla
     */
    private void crearFormulario() {
        vboxFormulario.getChildren().clear();
        camposFormulario.clear();
        
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            String columnType = columnTypes.get(i);
            boolean isPrimaryKey = primaryKeyColumns.contains(columnName);
            
            // Crear HBox para cada campo
            HBox hboxCampo = new HBox(10);
            hboxCampo.setPrefHeight(30);
            
            // Label para el nombre de la columna
            Label lblCampo = new Label(columnName + ":");
            lblCampo.setPrefWidth(150);
            if (isPrimaryKey) {
                lblCampo.setText(columnName + " (PK):");
                lblCampo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E8B57;");
            }
            
            // TextField para el valor
            TextField txtCampo = new TextField();
            txtCampo.setPrefWidth(300);
            
            // Configurar placeholder según el tipo de dato
            if (isDateType(columnType)) {
                txtCampo.setPromptText("Formato: DD/MM/YYYY (ej: 25/12/2024)");
            } else if (isTimestampType(columnType)) {
                txtCampo.setPromptText("Formato: DD/MM/YYYY HH:MM:SS (ej: 25/12/2024 14:30:00)");
            } else {
                txtCampo.setPromptText(columnType);
            }
            
            // Si es UPDATE, cargar datos originales
            if ("UPDATE".equals(operacion) && datosOriginales != null && i < datosOriginales.size()) {
                String valor = datosOriginales.get(i);
                if (valor != null && !"NULL".equals(valor)) {
                    txtCampo.setText(valor);
                }
            }
            
            // Si es clave primaria en modo UPDATE, deshabilitar edición
            if ("UPDATE".equals(operacion) && isPrimaryKey) {
                txtCampo.setEditable(false);
                txtCampo.setStyle("-fx-background-color: #f0f0f0;");
            }
            
            // Label para el tipo de dato
            Label lblTipo = new Label("(" + columnType + ")");
            lblTipo.setPrefWidth(150);
            lblTipo.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px;");
            
            hboxCampo.getChildren().addAll(lblCampo, txtCampo, lblTipo);
            vboxFormulario.getChildren().add(hboxCampo);
            
            // Guardar referencia al TextField
            camposFormulario.put(columnName, txtCampo);
        }
    }
    
    @FXML
    private void handleGuardar() {
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay conexión a la base de datos");
            return;
        }
        
        try {
            if ("INSERT".equals(operacion)) {
                realizarInsert();
            } else if ("UPDATE".equals(operacion)) {
                realizarUpdate();
            }
        } catch (SQLException e) {
            MetodosFrecuentes.mostrarAlertaError("Error en Base de Datos", 
                "Error al " + (operacion.equals("INSERT") ? "insertar" : "actualizar") + " el registro:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Realiza la operación INSERT
     */
    private void realizarInsert() throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder values = new StringBuilder(" VALUES (");
        
        List<String> valoresCampos = new ArrayList<>();
        
        // Construir la consulta INSERT
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            TextField campo = camposFormulario.get(columnName);
            String valor = campo.getText().trim();
            
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            
            sql.append(columnName);
            values.append("?");
            valoresCampos.add(valor.isEmpty() ? null : valor);
        }
        
        sql.append(")");
        values.append(")");
        sql.append(values);
        
        Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql.toString());
        
        // Establecer parámetros
        for (int i = 0; i < valoresCampos.size(); i++) {
            String valor = valoresCampos.get(i);
            String columnName = columnNames.get(i);
            String columnType = columnTypes.get(i);
            
            if (valor == null) {
                // Determinar el tipo correcto para NULL
                if (isDateType(columnType)) {
                    pstmt.setNull(i + 1, Types.DATE);
                } else if (isTimestampType(columnType)) {
                    pstmt.setNull(i + 1, Types.TIMESTAMP);
                } else {
                    pstmt.setNull(i + 1, Types.VARCHAR);
                }
            } else {
                // Manejar diferentes tipos de datos
                if (isDateType(columnType)) {
                    try {
                        // Intentar parsear como fecha
                        java.sql.Date sqlDate = parseDate(valor);
                        pstmt.setDate(i + 1, sqlDate);
                    } catch (Exception e) {
                        // Si falla el parsing, intentar con TO_DATE
                        pstmt.setString(i + 1, "TO_DATE('" + valor + "', 'DD/MM/YYYY')");
                    }
                } else if (isTimestampType(columnType)) {
                    try {
                        // Intentar parsear como timestamp
                        java.sql.Timestamp sqlTimestamp = parseTimestamp(valor);
                        pstmt.setTimestamp(i + 1, sqlTimestamp);
                    } catch (Exception e) {
                        // Si falla el parsing, intentar con TO_TIMESTAMP
                        pstmt.setString(i + 1, "TO_TIMESTAMP('" + valor + "', 'DD/MM/YYYY HH24:MI:SS')");
                    }
                } else {
                    pstmt.setString(i + 1, valor);
                }
            }
        }
        
        int filasAfectadas = pstmt.executeUpdate();
        pstmt.close();
        
        if (filasAfectadas > 0) {
            MetodosFrecuentes.mostrarAlerta("Éxito", "Registro insertado correctamente");
            // Regresar a la vista correcta
            regresarAVistaOrigen();
        } else {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Advertencia", "No se insertó ningún registro");
        }
    }
    
    /**
     * Realiza la operación UPDATE
     */
    private void realizarUpdate() throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        StringBuilder whereClause = new StringBuilder(" WHERE ");
        
        List<String> valoresSet = new ArrayList<>();
        List<String> valoresWhere = new ArrayList<>();
        
        // Construir la parte SET
        boolean firstSet = true;
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            
            if (!primaryKeyColumns.contains(columnName)) {
                TextField campo = camposFormulario.get(columnName);
                String valor = campo.getText().trim();
                
                if (!firstSet) {
                    sql.append(", ");
                }
                
                sql.append(columnName).append(" = ?");
                valoresSet.add(valor.isEmpty() ? null : valor);
                firstSet = false;
            }
        }
        
        // Construir la parte WHERE (usando claves primarias)
        boolean firstWhere = true;
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            
            if (primaryKeyColumns.contains(columnName)) {
                TextField campo = camposFormulario.get(columnName);
                String valor = campo.getText().trim();
                
                if (!firstWhere) {
                    whereClause.append(" AND ");
                }
                
                whereClause.append(columnName).append(" = ?");
                valoresWhere.add(valor);
                firstWhere = false;
            }
        }
        
        sql.append(whereClause);
        
        Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql.toString());
        
        // Establecer parámetros SET
        int paramIndex = 1;
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            String columnType = columnTypes.get(i);
            
            if (!primaryKeyColumns.contains(columnName)) {
                String valor = valoresSet.get(paramIndex - 1);
                if (valor == null) {
                    // Determinar el tipo correcto para NULL
                    if (isDateType(columnType)) {
                        pstmt.setNull(paramIndex, Types.DATE);
                    } else if (isTimestampType(columnType)) {
                        pstmt.setNull(paramIndex, Types.TIMESTAMP);
                    } else {
                        pstmt.setNull(paramIndex, Types.VARCHAR);
                    }
                } else {
                    // Manejar diferentes tipos de datos
                    if (isDateType(columnType)) {
                        try {
                            // Intentar parsear como fecha
                            java.sql.Date sqlDate = parseDate(valor);
                            pstmt.setDate(paramIndex, sqlDate);
                        } catch (Exception e) {
                            // Si falla el parsing, intentar con TO_DATE
                            pstmt.setString(paramIndex, "TO_DATE('" + valor + "', 'DD/MM/YYYY')");
                        }
                    } else if (isTimestampType(columnType)) {
                        try {
                            // Intentar parsear como timestamp
                            java.sql.Timestamp sqlTimestamp = parseTimestamp(valor);
                            pstmt.setTimestamp(paramIndex, sqlTimestamp);
                        } catch (Exception e) {
                            // Si falla el parsing, intentar con TO_TIMESTAMP
                            pstmt.setString(paramIndex, "TO_TIMESTAMP('" + valor + "', 'DD/MM/YYYY HH24:MI:SS')");
                        }
                    } else {
                        pstmt.setString(paramIndex, valor);
                    }
                }
                paramIndex++;
            }
        }
        
        // Establecer parámetros WHERE
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            String columnType = columnTypes.get(i);
            
            if (primaryKeyColumns.contains(columnName)) {
                String valor = valoresWhere.get(primaryKeyColumns.indexOf(columnName));
                if (valor == null) {
                    // Determinar el tipo correcto para NULL
                    if (isDateType(columnType)) {
                        pstmt.setNull(paramIndex, Types.DATE);
                    } else if (isTimestampType(columnType)) {
                        pstmt.setNull(paramIndex, Types.TIMESTAMP);
                    } else {
                        pstmt.setNull(paramIndex, Types.VARCHAR);
                    }
                } else {
                    // Manejar diferentes tipos de datos
                    if (isDateType(columnType)) {
                        try {
                            // Intentar parsear como fecha
                            java.sql.Date sqlDate = parseDate(valor);
                            pstmt.setDate(paramIndex, sqlDate);
                        } catch (Exception e) {
                            // Si falla el parsing, intentar con TO_DATE
                            pstmt.setString(paramIndex, "TO_DATE('" + valor + "', 'DD/MM/YYYY')");
                        }
                    } else if (isTimestampType(columnType)) {
                        try {
                            // Intentar parsear como timestamp
                            java.sql.Timestamp sqlTimestamp = parseTimestamp(valor);
                            pstmt.setTimestamp(paramIndex, sqlTimestamp);
                        } catch (Exception e) {
                            // Si falla el parsing, intentar con TO_TIMESTAMP
                            pstmt.setString(paramIndex, "TO_TIMESTAMP('" + valor + "', 'DD/MM/YYYY HH24:MI:SS')");
                        }
                    } else {
                        pstmt.setString(paramIndex, valor);
                    }
                }
                paramIndex++;
            }
        }
        
        int filasAfectadas = pstmt.executeUpdate();
        pstmt.close();
        
        if (filasAfectadas > 0) {
            MetodosFrecuentes.mostrarAlerta("Éxito", "Registro actualizado correctamente");
            // Regresar a la vista correcta
            regresarAVistaOrigen();
        } else {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Advertencia", "No se actualizó ningún registro");
        }
    }
    
    @FXML
    private void handleLimpiar() {
        for (TextField campo : camposFormulario.values()) {
            if (campo.isEditable()) {
                campo.clear();
            }
        }
    }
    
    @FXML
    private void handleCancelar() {
        // Obtener el contexto de origen para saber a dónde regresar
        String origen = ControllerDataManager.getInstance().getOrigenContexto();
        String currentTable = ControllerDataManager.getInstance().getSelectedTableName();
        
        // Limpiar datos de operación pero mantener información de la tabla
        ControllerDataManager.getInstance().clearData();
        ControllerDataManager.getInstance().setSelectedTableName(currentTable);
        
        // Regresar a la vista correcta según el origen
        if ("MASTER".equals(origen)) {
            // Volver al modo Master (Table-view)
            MetodosFrecuentes.cambiarVentana((Stage) btnCancelar.getScene().getWindow(), "/views/Table-view.fxml");
        } else {
            // Volver al modo Remoto (Vista-view) - comportamiento por defecto
            MetodosFrecuentes.cambiarVentana((Stage) btnCancelar.getScene().getWindow(), "/views/Vista-view.fxml");
        }
    }
    
    /**
     * Método auxiliar para regresar a la vista de origen correcta
     */
    private void regresarAVistaOrigen() {
        String origen = ControllerDataManager.getInstance().getOrigenContexto();
        String currentTable = ControllerDataManager.getInstance().getSelectedTableName();
        
        // Limpiar datos de operación pero mantener información de la tabla
        ControllerDataManager.getInstance().clearData();
        ControllerDataManager.getInstance().setSelectedTableName(currentTable);
        
        // Regresar a la vista correcta según el origen
        if ("MASTER".equals(origen)) {
            // Volver al modo Master (Table-view)
            MetodosFrecuentes.cambiarVentana((Stage) btnGuardar.getScene().getWindow(), "/views/Table-view.fxml");
        } else {
            // Volver al modo Remoto (Vista-view) - comportamiento por defecto
            MetodosFrecuentes.cambiarVentana((Stage) btnGuardar.getScene().getWindow(), "/views/Vista-view.fxml");
        }
    }
    
    /**
     * Verifica si el tipo de columna es DATE
     */
    private boolean isDateType(String columnType) {
        return columnType != null && columnType.toUpperCase().contains("DATE");
    }
    
    /**
     * Verifica si el tipo de columna es TIMESTAMP
     */
    private boolean isTimestampType(String columnType) {
        return columnType != null && columnType.toUpperCase().contains("TIMESTAMP");
    }
    
    /**
     * Parsea una cadena de fecha a java.sql.Date
     */
    private java.sql.Date parseDate(String dateStr) throws Exception {
        // Intentar diferentes formatos de fecha
        String[] formats = {
            "dd/MM/yyyy",
            "dd-MM-yyyy", 
            "yyyy-MM-dd",
            "dd/MM/yy",
            "dd-MM-yy",
            "yyyy/MM/dd"
        };
        
        for (String format : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(dateStr);
                return new java.sql.Date(date.getTime());
            } catch (Exception e) {
                // Continuar con el siguiente formato
            }
        }
        
        throw new Exception("No se pudo parsear la fecha: " + dateStr);
    }
    
    /**
     * Parsea una cadena de timestamp a java.sql.Timestamp
     */
    private java.sql.Timestamp parseTimestamp(String timestampStr) throws Exception {
        // Intentar diferentes formatos de timestamp
        String[] formats = {
            "dd/MM/yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "yyyy-MM-dd HH:mm"
        };
        
        for (String format : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(timestampStr);
                return new java.sql.Timestamp(date.getTime());
            } catch (Exception e) {
                // Continuar con el siguiente formato
            }
        }
        
        throw new Exception("No se pudo parsear el timestamp: " + timestampStr);
    }
}
