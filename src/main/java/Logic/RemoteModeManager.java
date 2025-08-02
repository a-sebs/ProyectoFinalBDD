package Logic;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona operaciones en MODO REMOTE
 * Solo permite VER las vistas de usuarios master previamente definidas en SQL*Plus
 */
public class RemoteModeManager {
    private ConnectionUser connectionUser;
    private ConfigurationManager config;

    public RemoteModeManager(ConnectionUser connectionUser) {
        this.connectionUser = connectionUser;
        this.config = connectionUser.getConfig();
    }

    /**
     * Activa el modo Remote
     */
    public boolean activateRemoteMode() {
        if (!connectionUser.estaConectado()) {
            return false;
        }
        
        config.setCurrentMode("REMOTE");
        System.out.println("👁️ MODO REMOTE ACTIVADO");
        System.out.println("Visualización de vistas de usuarios master");
        return true;
    }

    /**
     * Obtiene todas las vistas disponibles (creadas previamente por usuarios master)
     */
    public List<VistaInfo> getAvailableViews() {
        List<VistaInfo> vistas = new ArrayList<>();
        
        if (!connectionUser.estaConectado()) {
            return vistas;
        }

        try {
            // Obtener vistas propias
            String sql = "SELECT VIEW_NAME, TEXT_LENGTH, READ_ONLY " +
                        "FROM USER_VIEWS ORDER BY VIEW_NAME";
            
            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                VistaInfo vista = new VistaInfo(
                    rs.getString("VIEW_NAME"),
                    "USER", // Schema actual
                    rs.getInt("TEXT_LENGTH"),
                    "Y".equals(rs.getString("READ_ONLY")),
                    "OWNED"
                );
                vistas.add(vista);
            }

            rs.close();
            stmt.close();
            
            // Obtener vistas accesibles de otros schemas (grants)
            sql = "SELECT OWNER, TABLE_NAME as VIEW_NAME, PRIVILEGE " +
                  "FROM USER_TAB_PRIVS " +
                  "WHERE TABLE_NAME IN (SELECT VIEW_NAME FROM ALL_VIEWS WHERE OWNER = USER_TAB_PRIVS.OWNER) " +
                  "AND PRIVILEGE IN ('SELECT', 'READ') " +
                  "ORDER BY OWNER, TABLE_NAME";
            
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                VistaInfo vista = new VistaInfo(
                    rs.getString("OWNER") + "." + rs.getString("VIEW_NAME"),
                    rs.getString("OWNER"),
                    0, // No podemos saber text_length de vistas ajenas
                    true, // Asumir read-only para vistas ajenas
                    "GRANTED: " + rs.getString("PRIVILEGE")
                );
                vistas.add(vista);
            }

            rs.close();
            stmt.close();
            
            System.out.println("👁️ Encontradas " + vistas.size() + " vistas disponibles");
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo vistas: " + e.getMessage());
        }

        return vistas;
    }

    /**
     * Obtiene vistas accesibles através de database links (de otros servidores)
     */
    public List<VistaInfo> getRemoteViews(String databaseLink) {
        List<VistaInfo> vistas = new ArrayList<>();
        
        if (!connectionUser.estaConectado()) {
            return vistas;
        }

        try {
            String sql = "SELECT VIEW_NAME FROM USER_VIEWS@" + databaseLink + " ORDER BY VIEW_NAME";
            
            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                VistaInfo vista = new VistaInfo(
                    rs.getString("VIEW_NAME") + "@" + databaseLink,
                    "REMOTE",
                    0,
                    true,
                    "REMOTE via " + databaseLink
                );
                vistas.add(vista);
            }

            rs.close();
            stmt.close();
            
            System.out.println("🌐 Encontradas " + vistas.size() + " vistas remotas en " + databaseLink);
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo vistas remotas: " + e.getMessage());
            // Es normal que falle si no hay database link configurado
        }

        return vistas;
    }

    /**
     * Muestra el contenido de una vista (SOLO LECTURA)
     */
    public String viewData(String viewName, String whereClause, int limit) {
        if (!connectionUser.estaConectado()) {
            return "No hay conexión activa";
        }

        StringBuilder result = new StringBuilder();
        
        try {
            String sql = "SELECT * FROM " + viewName;
            
            if (whereClause != null && !whereClause.trim().isEmpty()) {
                sql += " WHERE " + whereClause;
            }
            
            if (limit > 0) {
                sql += " AND ROWNUM <= " + limit;
            } else {
                sql += " AND ROWNUM <= 100"; // Límite por defecto
            }

            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            
            result.append("=== VISTA: ").append(viewName).append(" ===\n");
            
            // Headers
            for (int i = 1; i <= columnCount; i++) {
                result.append(String.format("%-20s", metadata.getColumnName(i)));
            }
            result.append("\n");
            result.append("-".repeat(columnCount * 20)).append("\n");
            
            // Data
            int rowCount = 0;
            while (rs.next() && rowCount < limit) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    if (value == null) value = "NULL";
                    result.append(String.format("%-20s", 
                        value.length() > 19 ? value.substring(0, 16) + "..." : value));
                }
                result.append("\n");
                rowCount++;
            }
            
            result.append("\nMostradas ").append(rowCount).append(" filas");
            if (rowCount == limit) {
                result.append(" (limitado a ").append(limit).append(")");
            }

            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            result.append("Error accediendo a la vista: ").append(e.getMessage()).append("\n");
            result.append("Verifique que:\n");
            result.append("- La vista existe\n");
            result.append("- Tiene permisos de SELECT\n");
            result.append("- La sintaxis WHERE es correcta\n");
        }

        return result.toString();
    }

    /**
     * Obtiene información detallada de una vista
     */
    public String getViewInfo(String viewName) {
        if (!connectionUser.estaConectado()) {
            return "No hay conexión activa";
        }

        StringBuilder info = new StringBuilder();
        
        try {
            // Información básica de la vista
            String sql = "SELECT VIEW_NAME, TEXT, TEXT_LENGTH, READ_ONLY " +
                        "FROM USER_VIEWS WHERE VIEW_NAME = ?";
            
            Connection conn = connectionUser.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, viewName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                info.append("=== INFORMACIÓN DE VISTA ===\n");
                info.append("Nombre: ").append(rs.getString("VIEW_NAME")).append("\n");
                info.append("Solo lectura: ").append(rs.getString("READ_ONLY")).append("\n");
                info.append("Tamaño definición: ").append(rs.getInt("TEXT_LENGTH")).append(" chars\n");
                info.append("Definición SQL:\n").append(rs.getString("TEXT")).append("\n");
            } else {
                // Intentar como vista de otro schema
                sql = "SELECT OWNER, VIEW_NAME FROM ALL_VIEWS WHERE VIEW_NAME = ? AND OWNER IN " +
                      "(SELECT OWNER FROM USER_TAB_PRIVS WHERE TABLE_NAME = ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, viewName.toUpperCase());
                pstmt.setString(2, viewName.toUpperCase());
                rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    info.append("=== VISTA EXTERNA ===\n");
                    info.append("Owner: ").append(rs.getString("OWNER")).append("\n");
                    info.append("Nombre: ").append(rs.getString("VIEW_NAME")).append("\n");
                    info.append("Acceso: Solo lectura (vista de otro usuario)\n");
                } else {
                    info.append("Vista no encontrada o sin acceso: ").append(viewName);
                }
            }

            rs.close();
            pstmt.close();

            // Información de columnas
            sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE " +
                  "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, viewName.toUpperCase());
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                info.append("\n=== COLUMNAS ===\n");
                do {
                    info.append(String.format("%-30s %-15s %s\n", 
                        rs.getString("COLUMN_NAME"),
                        rs.getString("DATA_TYPE"),
                        rs.getString("NULLABLE")
                    ));
                } while (rs.next());
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            info.append("Error obteniendo información: ").append(e.getMessage());
        }

        return info.toString();
    }

    /**
     * Lista todos los database links disponibles para acceder vistas remotas
     */
    public List<String> getAvailableDatabaseLinks() {
        List<String> links = new ArrayList<>();
        
        if (!connectionUser.estaConectado()) {
            return links;
        }

        try {
            String sql = "SELECT DB_LINK, USERNAME, HOST FROM USER_DB_LINKS ORDER BY DB_LINK";
            
            Connection conn = connectionUser.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String linkInfo = String.format("%s (%s@%s)", 
                    rs.getString("DB_LINK"),
                    rs.getString("USERNAME"),
                    rs.getString("HOST")
                );
                links.add(linkInfo);
            }

            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo database links: " + e.getMessage());
        }

        return links;
    }

    /**
     * Clase interna para información de vistas
     */
    public static class VistaInfo {
        private String nombre;
        private String schema;
        private int textLength;
        private boolean readOnly;
        private String estado;

        public VistaInfo(String nombre, String schema, int textLength, 
                        boolean readOnly, String estado) {
            this.nombre = nombre;
            this.schema = schema;
            this.textLength = textLength;
            this.readOnly = readOnly;
            this.estado = estado;
        }

        // Getters
        public String getNombre() { return nombre; }
        public String getSchema() { return schema; }
        public int getTextLength() { return textLength; }
        public boolean isReadOnly() { return readOnly; }
        public String getEstado() { return estado; }

        @Override
        public String toString() {
            return String.format("%s [%s] - %s", nombre, schema, estado);
        }
    }
}
