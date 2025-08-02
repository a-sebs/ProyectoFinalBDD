package Logic;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona operaciones en MODO MASTER
 * Muestra y permite manejar las tablas propias del usuario conectado
 */
public class MasterModeManager {
    private ConnectionUser connectionUser;
    private ConfigurationManager config;

    public MasterModeManager(ConnectionUser connectionUser) {
        this.connectionUser = connectionUser;
        this.config = connectionUser.getConfig();
    }

    /**
     * Activa el modo Master
     */
    public boolean activateMasterMode() {
        if (!connectionUser.estaConectado()) {
            return false;
        }
        
        config.setCurrentMode("MASTER");
        System.out.println("🏆 MODO MASTER ACTIVADO");
        System.out.println("Gestión completa de tablas propias del usuario");
        return true;
    }

    /**
     * Obtiene todas las tablas que el usuario puede manejar (tablas propias)
     */
    public List<TablaInfo> getMyTables() {
        List<TablaInfo> tablas = new ArrayList<>();
        
        if (!connectionUser.estaConectado()) {
            return tablas;
        }

        try {
            String sql = "SELECT TABLE_NAME, NUM_ROWS, TABLESPACE_NAME, " +
                        "TO_CHAR(LAST_ANALYZED, 'DD/MM/YYYY HH24:MI:SS') as LAST_ANALYZED " +
                        "FROM USER_TABLES ORDER BY TABLE_NAME";
            
            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                TablaInfo tabla = new TablaInfo(
                    rs.getString("TABLE_NAME"),
                    rs.getLong("NUM_ROWS"),
                    rs.getString("TABLESPACE_NAME"),
                    rs.getString("LAST_ANALYZED"),
                    "TABLE",
                    "OWNED" // Tabla propia
                );
                tablas.add(tabla);
            }

            rs.close();
            stmt.close();
            
            System.out.println("📊 Encontradas " + tablas.size() + " tablas propias");
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo tablas propias: " + e.getMessage());
        }

        return tablas;
    }

    /**
     * Obtiene privilegios que el usuario tiene sobre otras tablas
     */
    public List<TablaInfo> getAccessibleTables() {
        List<TablaInfo> tablas = new ArrayList<>();
        
        if (!connectionUser.estaConectado()) {
            return tablas;
        }

        try {
            String sql = "SELECT OWNER||'.'||TABLE_NAME as FULL_TABLE_NAME, " +
                        "OWNER, TABLE_NAME, PRIVILEGE " +
                        "FROM USER_TAB_PRIVS " +
                        "WHERE TABLE_NAME NOT IN (SELECT TABLE_NAME FROM USER_TABLES) " +
                        "ORDER BY OWNER, TABLE_NAME";
            
            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                TablaInfo tabla = new TablaInfo(
                    rs.getString("FULL_TABLE_NAME"),
                    0L, // No podemos conocer NUM_ROWS de tablas ajenas fácilmente
                    "N/A",
                    "N/A",
                    "TABLE",
                    "GRANTED: " + rs.getString("PRIVILEGE")
                );
                tablas.add(tabla);
            }

            rs.close();
            stmt.close();
            
            System.out.println("🔑 Encontradas " + tablas.size() + " tablas con privilegios");
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo tablas con privilegios: " + e.getMessage());
        }

        return tablas;
    }

    /**
     * Obtiene estadísticas detalladas de una tabla propia
     */
    public String getTableStatistics(String tableName) {
        if (!connectionUser.estaConectado()) {
            return "No hay conexión activa";
        }

        StringBuilder stats = new StringBuilder();
        
        try {
            // Información básica de la tabla
            String sql = "SELECT TABLE_NAME, NUM_ROWS, BLOCKS, EMPTY_BLOCKS, " +
                        "AVG_ROW_LEN, TABLESPACE_NAME, STATUS, " +
                        "TO_CHAR(LAST_ANALYZED, 'DD/MM/YYYY HH24:MI:SS') as LAST_ANALYZED " +
                        "FROM USER_TABLES WHERE TABLE_NAME = ?";
            
            Connection conn = connectionUser.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.append("=== ESTADÍSTICAS DE TABLA ===\n");
                stats.append("Tabla: ").append(rs.getString("TABLE_NAME")).append("\n");
                stats.append("Registros: ").append(rs.getLong("NUM_ROWS")).append("\n");
                stats.append("Bloques: ").append(rs.getLong("BLOCKS")).append("\n");
                stats.append("Bloques vacíos: ").append(rs.getLong("EMPTY_BLOCKS")).append("\n");
                stats.append("Tamaño promedio fila: ").append(rs.getLong("AVG_ROW_LEN")).append(" bytes\n");
                stats.append("Tablespace: ").append(rs.getString("TABLESPACE_NAME")).append("\n");
                stats.append("Estado: ").append(rs.getString("STATUS")).append("\n");
                stats.append("Último análisis: ").append(rs.getString("LAST_ANALYZED")).append("\n");
            }

            rs.close();
            pstmt.close();

            // Información de columnas
            sql = "SELECT COUNT(*) as TOTAL_COLUMNS FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tableName.toUpperCase());
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                stats.append("Total columnas: ").append(rs.getInt("TOTAL_COLUMNS")).append("\n");
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            stats.append("Error obteniendo estadísticas: ").append(e.getMessage());
        }

        return stats.toString();
    }

    /**
     * Ejecuta operaciones CRUD sobre tablas propias
     */
    public boolean executeOperation(String operation, String tableName, String whereClause) {
        if (!connectionUser.estaConectado()) {
            return false;
        }

        try {
            String sql = "";
            
            switch (operation.toUpperCase()) {
                case "SELECT":
                    sql = "SELECT * FROM " + tableName;
                    if (whereClause != null && !whereClause.trim().isEmpty()) {
                        sql += " WHERE " + whereClause;
                    }
                    sql += " AND ROWNUM <= 100"; // Limitar a 100 registros
                    break;
                    
                case "COUNT":
                    sql = "SELECT COUNT(*) as TOTAL FROM " + tableName;
                    if (whereClause != null && !whereClause.trim().isEmpty()) {
                        sql += " WHERE " + whereClause;
                    }
                    break;
                    
                case "DESCRIBE":
                    sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT " +
                          "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tableName.toUpperCase() + "' " +
                          "ORDER BY COLUMN_ID";
                    break;
                    
                default:
                    System.err.println("Operación no soportada en modo Master: " + operation);
                    return false;
            }

            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            
            if (operation.toUpperCase().equals("SELECT") || 
                operation.toUpperCase().equals("COUNT") || 
                operation.toUpperCase().equals("DESCRIBE")) {
                
                ResultSet rs = stmt.executeQuery(sql);
                
                // Mostrar resultados
                ResultSetMetaData metadata = rs.getMetaData();
                int columnCount = metadata.getColumnCount();
                
                System.out.println("=== RESULTADOS ===");
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(metadata.getColumnName(i) + "\t");
                }
                System.out.println();
                
                int rowCount = 0;
                while (rs.next() && rowCount < 50) { // Limitar output
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(rs.getString(i) + "\t");
                    }
                    System.out.println();
                    rowCount++;
                }
                
                rs.close();
            }
            
            stmt.close();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error ejecutando operación: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clase interna para información de tablas
     */
    public static class TablaInfo {
        private String nombre;
        private long numRows;
        private String tablespace;
        private String lastAnalyzed;
        private String tipo;
        private String estado;

        public TablaInfo(String nombre, long numRows, String tablespace, 
                        String lastAnalyzed, String tipo, String estado) {
            this.nombre = nombre;
            this.numRows = numRows;
            this.tablespace = tablespace;
            this.lastAnalyzed = lastAnalyzed;
            this.tipo = tipo;
            this.estado = estado;
        }

        // Getters
        public String getNombre() { return nombre; }
        public long getNumRows() { return numRows; }
        public String getTablespace() { return tablespace; }
        public String getLastAnalyzed() { return lastAnalyzed; }
        public String getTipo() { return tipo; }
        public String getEstado() { return estado; }

        @Override
        public String toString() {
            return String.format("%s (%d filas) - %s", nombre, numRows, estado);
        }
    }
}
