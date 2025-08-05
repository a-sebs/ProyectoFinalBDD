package MetodosFrecuentes;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Connection;
import java.sql.Statement;

public class MetodosFrecuentes {

    public static void cambiarVentana(Stage currentStage, String rutaFXML) {
        try {
            // Cargar el archivo FXML
            FXMLLoader loader = new FXMLLoader(MetodosFrecuentes.class.getResource(rutaFXML));
            Parent root = loader.load();

            // Cambiar la escena del Stage actual
            currentStage.setScene(new Scene(root));

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la interfaz de usuario.");
            e.printStackTrace();
        }
    }

    public static void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    public static void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    public static void mostrarAlertaAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        
        return alert.showAndWait().orElse(null) == alert.getButtonTypes().get(0);
    }

    public static void mostrarVentana(String rutaFXML, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(MetodosFrecuentes.class.getResource(rutaFXML));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el valor de una columna del ResultSet manejando diferentes tipos de datos
     * Incluye soporte para BLOB, CLOB, DATE, TIMESTAMP y otros tipos especiales
     */
    public static String getColumnValue(ResultSet rs, int columnIndex, ResultSetMetaData metaData) {
        try {
            int columnType = metaData.getColumnType(columnIndex);
            String columnTypeName = metaData.getColumnTypeName(columnIndex);
            
            // Verificar si el valor es NULL
            if (rs.getObject(columnIndex) == null) {
                return "NULL";
            }
            
            // Manejar diferentes tipos de datos
            switch (columnType) {
                case Types.BLOB:
                    return "[BLOB]";
                case Types.CLOB:
                    return "[CLOB]";
                case Types.LONGVARBINARY:
                    return "[BINARY]";
                case Types.VARBINARY:
                    return "[BINARY]";
                case Types.BINARY:
                    return "[BINARY]";
                case Types.LONGVARCHAR:
                    return "[LONG TEXT]";
                case Types.DATE:
                    java.sql.Date date = rs.getDate(columnIndex);
                    return date != null ? date.toString() : "NULL";
                case Types.TIMESTAMP:
                    java.sql.Timestamp timestamp = rs.getTimestamp(columnIndex);
                    return timestamp != null ? timestamp.toString() : "NULL";
                case Types.TIME:
                    java.sql.Time time = rs.getTime(columnIndex);
                    return time != null ? time.toString() : "NULL";
                case Types.NUMERIC:
                case Types.DECIMAL:
                    java.math.BigDecimal decimal = rs.getBigDecimal(columnIndex);
                    return decimal != null ? decimal.toString() : "NULL";
                case Types.DOUBLE:
                    double doubleVal = rs.getDouble(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(doubleVal);
                case Types.FLOAT:
                    float floatVal = rs.getFloat(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(floatVal);
                case Types.INTEGER:
                    int intVal = rs.getInt(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(intVal);
                case Types.BIGINT:
                    long longVal = rs.getLong(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(longVal);
                case Types.SMALLINT:
                    short shortVal = rs.getShort(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(shortVal);
                case Types.TINYINT:
                    byte byteVal = rs.getByte(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(byteVal);
                case Types.BOOLEAN:
                    boolean boolVal = rs.getBoolean(columnIndex);
                    return rs.wasNull() ? "NULL" : String.valueOf(boolVal);
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.NCHAR:
                case Types.LONGNVARCHAR:
                default:
                    // Para tipos de texto y otros tipos no específicos
                    try {
                        return rs.getString(columnIndex);
                    } catch (SQLException e) {
                        // Si getString falla, intentar con getObject
                        Object obj = rs.getObject(columnIndex);
                        return obj != null ? obj.toString() : "NULL";
                    }
            }
        } catch (SQLException e) {
            return "[ERROR: " + e.getMessage() + "]";
        }
    }

    /**
     * Realiza commit automático después de una operación de modificación
     * Esto asegura que los cambios se reflejen inmediatamente en las vistas materializadas
     */
    public static void realizarCommitAutomatico() {
        try {
            Connection conn = Logic.SessionManager.getInstance().getConnectionUser().getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.commit();
                System.out.println("✅ Commit automático realizado exitosamente");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al realizar commit automático: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Realiza rollback automático en caso de error
     */
    public static void realizarRollbackAutomatico() {
        try {
            Connection conn = Logic.SessionManager.getInstance().getConnectionUser().getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                System.out.println("🔄 Rollback automático realizado");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al realizar rollback automático: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresca las vistas materializadas después de una operación de modificación
     * Esto asegura que las vistas materializadas se actualicen con los cambios recientes
     */
    public static void refrescarVistasMaterializadas() {
        try {
            Connection conn = Logic.SessionManager.getInstance().getConnectionUser().getConnection();
            if (conn != null && !conn.isClosed()) {
                // Obtener todas las vistas materializadas del usuario actual
                String sql = "SELECT MVIEW_NAME FROM USER_MVIEWS";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                
                while (rs.next()) {
                    String mviewName = rs.getString("MVIEW_NAME");
                    try {
                        // Refrescar cada vista materializada
                        String refreshSql = "BEGIN DBMS_MVIEW.REFRESH('" + mviewName + "', 'F'); END;";
                        Statement refreshStmt = conn.createStatement();
                        refreshStmt.execute(refreshSql);
                        refreshStmt.close();
                        System.out.println("✅ Vista materializada '" + mviewName + "' refrescada exitosamente");
                    } catch (SQLException e) {
                        System.err.println("⚠️ Error al refrescar vista materializada '" + mviewName + "': " + e.getMessage());
                    }
                }
                
                rs.close();
                stmt.close();
                System.out.println("✅ Proceso de refresco de vistas materializadas completado");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al refrescar vistas materializadas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Realiza commit automático y refresca vistas materializadas
     * Este método combina ambas operaciones para asegurar que los cambios se reflejen
     */
    public static void realizarCommitYRefrescarVistas() {
        realizarCommitAutomatico();
        refrescarVistasMaterializadas();
    }
}