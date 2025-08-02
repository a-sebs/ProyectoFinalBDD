package Logic;

/**
 * Clase singleton para manejar la sesión del usuario actual
 */
public class SessionManager {
    private static SessionManager instance;
    private String currentUser;
    private ConnectionUser connectionUser;
    
    private SessionManager() {}
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public void setConnectionUser(ConnectionUser connectionUser) {
        this.connectionUser = connectionUser;
    }
    
    public ConnectionUser getConnectionUser() {
        return connectionUser;
    }
    
    public void logout() {
        if (connectionUser != null && connectionUser.estaConectado()) {
            connectionUser.desconectar();
        }
        currentUser = null;
        connectionUser = null;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null && connectionUser != null && connectionUser.estaConectado();
    }
}
