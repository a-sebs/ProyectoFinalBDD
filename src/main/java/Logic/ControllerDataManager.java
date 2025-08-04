package Logic;

import javafx.collections.ObservableList;

/**
 * Clase para compartir datos entre controladores
 */
public class ControllerDataManager {
    private static ControllerDataManager instance;
    private String selectedTableName;
    private String selectedVistaName;
    private String operacion; // "INSERT", "UPDATE", "DELETE"
    private ObservableList<String> datosOriginales; // Para UPDATE/DELETE
    private String origenContexto; // "MASTER", "REMOTO" - para saber a dónde regresar
    
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
    
    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }
    
    public String getOperacion() {
        return operacion;
    }
    
    public void setDatosOriginales(ObservableList<String> datosOriginales) {
        this.datosOriginales = datosOriginales;
    }
    
    public ObservableList<String> getDatosOriginales() {
        return datosOriginales;
    }
    
    public void setOrigenContexto(String origenContexto) {
        this.origenContexto = origenContexto;
    }
    
    public String getOrigenContexto() {
        return origenContexto;
    }
    
    public void clearData() {
        selectedTableName = null;
        selectedVistaName = null;
        operacion = null;
        datosOriginales = null;
        origenContexto = null;
    }
}
