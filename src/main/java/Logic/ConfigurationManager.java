package Logic;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Gestiona la configuración de la aplicación distribuida
 * Configurado para trabajo con Angel (Master) y Bell (Remote)
 */
public class ConfigurationManager {
    private static final String CONFIG_FILE = "database_config.properties";
    private Properties properties;

    public ConfigurationManager() {
        this.properties = new Properties();
        loadConfiguration();
    }

    /**
     * Carga configuración desde archivo
     */
    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                // Crear configuración por defecto
                setDefaultConfiguration();
            }
        } catch (IOException e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
            setDefaultConfiguration();
        }
    }

    /**
     * Establece configuración por defecto basada en flexibilidad Angel/Bell
     */
    private void setDefaultConfiguration() {
        // Connection Strings - Cualquier computadora puede conectar a cualquiera
        properties.setProperty("connection.angel.tns", "jdbc:oracle:thin:@PROYECTO_BDD_ANGEL");
        properties.setProperty("connection.bell.tns", "jdbc:oracle:thin:@PROYECTO_BDD");
        
        // Easy Connect para ambas computadoras
        properties.setProperty("connection.angel.easy", "jdbc:oracle:thin:@Angel:1521:orcl");
        properties.setProperty("connection.bell.easy", "jdbc:oracle:thin:@Bell:1521:orcl");
        properties.setProperty("connection.local.easy", "jdbc:oracle:thin:@localhost:1521:orcl");
        
        // Service Names (RECOMENDADO) - Flexibles
        properties.setProperty("connection.angel.service", "jdbc:oracle:thin:@//Angel:1521/orcl");
        properties.setProperty("connection.bell.service", "jdbc:oracle:thin:@//Bell:1521/orcl");
        properties.setProperty("connection.local.service", "jdbc:oracle:thin:@//localhost:1521/orcl");
        
        // Conexiones TNS Names para compatibilidad
        properties.setProperty("connection.local.tns", "jdbc:oracle:thin:@PROYECTO_BDD");
        properties.setProperty("connection.localhost.tns", "jdbc:oracle:thin:@localhost:1521:orcl");
        
        // Configuración de aplicación - NO determina automáticamente el modo
        properties.setProperty("local.hostname", getComputerName());
        properties.setProperty("app.current.mode", "NONE"); // Usuario debe elegir en menú
        properties.setProperty("sync.interval", "300");
        properties.setProperty("connection.timeout", "15");
        properties.setProperty("connection.type", "service");
        
        // Network configuration
        properties.setProperty("network.angel.ip", "Angel");
        properties.setProperty("network.bell.ip", "Bell");
        properties.setProperty("network.auto.detect", "true");
        
        // Database links - Usuario configurará según necesidad
        properties.setProperty("dblink.angel.name", "LINK_TO_ANGEL");
        properties.setProperty("dblink.bell.name", "LINK_TO_BELL");
        
        // Configuración de roles flexibles
        properties.setProperty("role.master.description", "Gestión completa de tablas propias");
        properties.setProperty("role.remote.description", "Visualización de vistas de usuarios master");
        properties.setProperty("last.master.connection", ""); // Última conexión como master
        properties.setProperty("last.remote.connection", ""); // Última conexión como remote
    }

    /**
     * DEPRECATED - Ahora el modo se elige en el menú, no se determina automáticamente
     */
    @Deprecated
    private String determineAppMode() {
        return "USER_CHOICE"; // El usuario elige en Menu-View
    }

    // === MÉTODOS PARA MODO FLEXIBLE ===
    
    /**
     * Establece el modo actual (MASTER o REMOTE) según elección del usuario
     */
    public void setCurrentMode(String mode) {
        properties.setProperty("app.current.mode", mode);
        if ("MASTER".equals(mode)) {
            properties.setProperty("last.master.connection", java.time.LocalDateTime.now().toString());
        } else if ("REMOTE".equals(mode)) {
            properties.setProperty("last.remote.connection", java.time.LocalDateTime.now().toString());
        }
    }
    
    /**
     * Obtiene el modo actual seleccionado por el usuario
     */
    public String getCurrentMode() {
        return properties.getProperty("app.current.mode", "NONE");
    }
    
    /**
     * Verifica si el usuario ha seleccionado modo Master
     */
    public boolean isInMasterMode() {
        return "MASTER".equals(getCurrentMode());
    }
    
    /**
     * Verifica si el usuario ha seleccionado modo Remote
     */
    public boolean isInRemoteMode() {
        return "REMOTE".equals(getCurrentMode());
    }
    
    /**
     * Resetea el modo para que el usuario vuelva a elegir
     */
    public void resetMode() {
        properties.setProperty("app.current.mode", "NONE");
    }

    /**
     * Obtiene el nombre de la computadora actual
     */
    public String getComputerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // Fallback usando variable de entorno
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName != null) {
                return computerName;
            }
            return "unknown";
        }
    }

    /**
     * Guarda configuración a archivo
     */
    public void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Database Distributed Configuration - Angel/Bell Setup");
        } catch (IOException e) {
            System.err.println("Error guardando configuración: " + e.getMessage());
        }
    }

    // === MÉTODOS DE CONNECTION STRINGS FLEXIBLES ===
    
    /**
     * Obtiene connection string para Angel (desde cualquier computadora)
     */
    public String getAngelConnectionString() {
        String type = properties.getProperty("connection.type", "service");
        return properties.getProperty("connection.angel." + type, 
                                     "jdbc:oracle:thin:@//Angel:1521/orcl");
    }

    /**
     * Obtiene connection string para Bell (desde cualquier computadora)
     */
    public String getBellConnectionString() {
        String type = properties.getProperty("connection.type", "service");
        return properties.getProperty("connection.bell." + type, 
                                     "jdbc:oracle:thin:@//Bell:1521/orcl");
    }
    
    /**
     * Obtiene connection string para localhost - prueba múltiples formatos
     */
    public String getLocalConnectionString() {
        String type = properties.getProperty("connection.type", "tns");
        String connectionString = properties.getProperty("connection.local." + type);
        
        if (connectionString != null) {
            return connectionString;
        }
        
        // Fallback - probar diferentes formatos
        if ("tns".equals(type)) {
            return "jdbc:oracle:thin:@PROYECTO_BDD";
        } else if ("service".equals(type)) {
            return "jdbc:oracle:thin:@//localhost:1521/orcl";
        } else {
            return "jdbc:oracle:thin:@localhost:1521:orcl";
        }
    }
    
    /**
     * Lista todas las conexiones disponibles para que el usuario elija
     */
    public java.util.Map<String, String> getAvailableConnections() {
        java.util.Map<String, String> connections = new java.util.HashMap<>();
        connections.put("Local (Esta máquina)", getLocalConnectionString());
        connections.put("Angel", getAngelConnectionString());
        connections.put("Bell", getBellConnectionString());
        return connections;
    }
    
    /**
     * Obtiene connection string por nombre de servidor
     */
    public String getConnectionStringByServer(String serverName) {
        switch (serverName.toUpperCase()) {
            case "ANGEL":
                return getAngelConnectionString();
            case "BELL":
                return getBellConnectionString();
            case "LOCAL":
            case "LOCALHOST":
                return getLocalConnectionString();
            default:
                return getLocalConnectionString();
        }
    }

    public String getAppMode() { 
        return getCurrentMode(); // Usa el modo actual seleccionado por usuario
    }
    
    public void setAppMode(String mode) { 
        setCurrentMode(mode); // Delegado al nuevo método
    }

    @Deprecated
    public boolean isMasterMode() { 
        return isInMasterMode(); // Usa el nuevo método
    }
    
    @Deprecated
    public boolean isRemoteMode() { 
        return isInRemoteMode(); // Usa el nuevo método
    }

    public boolean isAngelComputer() {
        return getComputerName().toUpperCase().contains("ANGEL");
    }

    public boolean isBellComputer() {
        return getComputerName().toUpperCase().contains("BELL");
    }

    public int getSyncInterval() { 
        return Integer.parseInt(properties.getProperty("sync.interval", "300")); 
    }
    
    public void setSyncInterval(int seconds) { 
        properties.setProperty("sync.interval", String.valueOf(seconds)); 
    }

    public int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("connection.timeout", "10"));
    }
    
    public void setConnectionTimeout(int seconds) {
        properties.setProperty("connection.timeout", String.valueOf(seconds));
    }

    public String getConnectionType() { 
        return properties.getProperty("connection.type", "service"); 
    }
    
    public void setConnectionType(String type) { 
        properties.setProperty("connection.type", type); 
    }

    /**
     * Obtiene información de debug de la configuración actual
     */
    public String getConfigurationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== CONFIGURACIÓN ACTUAL ===\n");
        info.append("Computadora: ").append(getComputerName()).append("\n");
        info.append("Modo seleccionado: ").append(getCurrentMode()).append("\n");
        info.append("Es Angel: ").append(isAngelComputer()).append("\n");
        info.append("Es Bell: ").append(isBellComputer()).append("\n");
        info.append("Conexión Local: ").append(getLocalConnectionString()).append("\n");
        info.append("Conexión Angel: ").append(getAngelConnectionString()).append("\n");
        info.append("Conexión Bell: ").append(getBellConnectionString()).append("\n");
        info.append("Tipo Conexión: ").append(getConnectionType()).append("\n");
        info.append("Conexiones disponibles: ").append(getAvailableConnections().size()).append("\n");
        return info.toString();
    }

    /**
     * Obtiene el database link name según el modo actual
     */
    public String getDatabaseLinkName() {
        if (isInMasterMode()) {
            return properties.getProperty("dblink.bell.name", "LINK_TO_BELL");
        } else if (isInRemoteMode()) {
            return properties.getProperty("dblink.angel.name", "LINK_TO_ANGEL");
        } else {
            return "NO_LINK_SELECTED"; // Usuario debe elegir modo primero
        }
    }
}
