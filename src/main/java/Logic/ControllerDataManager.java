package Logic;

/**
 * Clase para compartir datos entre controladores
 */
public class ControllerDataManager {
    private static ControllerDataManager instance;
    private String selectedTableName;
    private String selectedVistaName;
    
    private ControllerDataManager() {}
    
    public static ControllerDataManager getInstance() {
        if (instance == null) {
            instance = new ControllerDataManager();
        }
        return instance;
    }
    
    public void setSelectedTableName(String tableName) {
        this.selectedTableName = tableName;
    }
    
    public String getSelectedTableName() {
        return selectedTableName;
    }
    
    public void setSelectedVistaName(String vistaName) {
        this.selectedVistaName = vistaName;
    }
    
    public String getSelectedVistaName() {
        return selectedVistaName;
    }
    
    public void clearData() {
        selectedTableName = null;
        selectedVistaName = null;
    }
}
