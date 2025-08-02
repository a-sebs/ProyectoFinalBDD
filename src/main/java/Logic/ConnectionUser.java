package Logic;

import java.sql.*;
import java.util.Properties;

public class ConnectionUser {
    private Connection connection;
    private ConfigurationManager config;
    private String currentUser;
    private String currentHost;
    private String connectionUrl;
    
    // Managers para diferentes modos
    private MasterModeManager masterManager;
    private RemoteModeManager remoteManager;
    
    // Constructor
    public ConnectionUser() {
        this.connection = null;
        this.config = new ConfigurationManager();
    }

    /**
     * Intenta conectar con una URL específica
     */
    private boolean conectarConURL(String usuario, String password, String url) {
        try {
            // Cerrar conexión existente si hay una
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            // Cargar el driver de Oracle
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Configurar propiedades de conexión
            Properties props = new Properties();
            props.setProperty("user", usuario);
            props.setProperty("password", password);
            props.setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(config.getConnectionTimeout() * 1000));

            // Intentar nueva conexión
            connection = DriverManager.getConnection(url, props);
            
            // Verificar que la conexión sea válida
            if (connection != null && !connection.isClosed()) {
                // Configurar autocommit en false (como en SQL*Plus)
                connection.setAutoCommit(false);
                this.currentUser = usuario;
                this.currentHost = extractHostFromURL(url);
                this.connectionUrl = url;
                
                // Inicializar managers después de conectar
                initializeManagers();
                
                System.out.println("✅ Conexión establecida exitosamente a: " + url);
                return true;
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver Oracle JDBC no encontrado");
            System.err.println("Detalle: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión SQL con " + url + ": " + e.getMessage());
            // Solo imprimir detalles para errores no relacionados con credenciales
            if (!e.getMessage().toLowerCase().contains("invalid username") && 
                !e.getMessage().toLowerCase().contains("invalid password")) {
                System.err.println("Código: " + e.getErrorCode());
                System.err.println("Estado: " + e.getSQLState());
            }
        } catch (Exception e) {
            System.err.println("❌ Error inesperado con " + url + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Conecta usando configuración automática (Angel/Bell) con múltiples intentos
     */
    public boolean conectar(String usuario, String password) {
        // Lista de URLs para probar en orden de prioridad
        String[] urlsToTry = {
            "jdbc:oracle:thin:@PROYECTO_BDD",                    // TNS Name principal
            "jdbc:oracle:thin:@localhost:1521:orcl",             // SID format
            "jdbc:oracle:thin:@//localhost:1521/orcl",           // Service Name format
            "jdbc:oracle:thin:@127.0.0.1:1521:orcl",            // IP con SID
            "jdbc:oracle:thin:@//127.0.0.1:1521/orcl"           // IP con Service Name
        };
        
        for (String url : urlsToTry) {
            System.out.println("Probando conexión con: " + url);
            if (conectarConURL(usuario, password, url)) {
                System.out.println("✅ Conexión exitosa con: " + url);
                return true;
            } else {
                System.out.println("❌ Falló conexión con: " + url);
            }
        }
        
        System.out.println("❌ Todas las conexiones fallaron");
        return false;
    }

    /**
     * Conecta a la computadora remota (Bell desde Angel o viceversa)
     */
    public boolean conectarRemoto(String usuario, String password, String serverName) {
        String url = config.getConnectionStringByServer(serverName);
        return conectarConURL(usuario, password, url);
    }

    /**
     * Conecta usando el string PROYECTO_BDD de tu tnsnames.ora
     */
    public boolean conectarProyectoBDD(String usuario, String password) {
        String url = "jdbc:oracle:thin:@PROYECTO_BDD";
        return conectarConURL(usuario, password, url);
    }

    /**
     * Extrae el hostname de una URL JDBC
     */
    private String extractHostFromURL(String url) {
        try {
            if (url.contains("@//")) {
                // Service name format: jdbc:oracle:thin:@//host:port/service
                String hostPart = url.substring(url.indexOf("@//") + 3);
                return hostPart.substring(0, hostPart.indexOf(':'));
            } else if (url.contains("@") && !url.contains("(")) {
                // Easy connect format: jdbc:oracle:thin:@host:port:sid
                String hostPart = url.substring(url.indexOf('@') + 1);
                return hostPart.substring(0, hostPart.indexOf(':'));
            } else if (url.contains("@")) {
                // TNS name format: jdbc:oracle:thin:@TNSNAME
                return url.substring(url.indexOf('@') + 1);
            }
        } catch (Exception e) {
            // Si no se puede extraer, devolver la URL completa
        }
        return url;
    }

    /**
     * Inicializa los managers después de establecer conexión
     */
    private void initializeManagers() {
        if (connection != null) {
            this.masterManager = new MasterModeManager(this);
            this.remoteManager = new RemoteModeManager(this);
        }
    }

    public boolean estaConectado() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentHost() {
        return currentHost;
    }

    public ConfigurationManager getConfig() {
        return config;
    }

    public String obtenerInfoServidor() {
        if (connection == null) {
            return "❌ Sin conexión activa";
        }

        StringBuilder info = new StringBuilder();
        info.append("=== INFORMACIÓN DE CONEXIÓN ===\n");
        info.append("Usuario: ").append(currentUser).append("\n");
        info.append("Host: ").append(currentHost).append("\n");
        info.append("URL: ").append(connectionUrl).append("\n");
        info.append("Computadora local: ").append(config.getComputerName()).append("\n");
        info.append("Modo aplicación: ").append(config.getAppMode()).append("\n");

        try {
            DatabaseMetaData meta = connection.getMetaData();
            info.append("Producto DB: ").append(meta.getDatabaseProductName()).append("\n");
            info.append("Versión DB: ").append(meta.getDatabaseProductVersion()).append("\n");
            info.append("AutoCommit: ").append(connection.getAutoCommit()).append("\n");
        } catch (SQLException e) {
            info.append("Error obteniendo metadata: ").append(e.getMessage()).append("\n");
        }

        return info.toString();
    }

    public void desconectar() {
        cerrarConexion();
    }

    public MasterModeManager getMasterManager() {
        if (masterManager == null) {
            initializeManagers();
        }
        return masterManager;
    }

    public RemoteModeManager getRemoteManager() {
        if (remoteManager == null) {
            initializeManagers();
        }
        return remoteManager;
    }

    public void cerrarConexion() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Conexión cerrada correctamente");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error cerrando conexión: " + e.getMessage());
        } finally {
            connection = null;
            masterManager = null;
            remoteManager = null;
            currentUser = null;
            currentHost = null;
            connectionUrl = null;
        }
    }
}
