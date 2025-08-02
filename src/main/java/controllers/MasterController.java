package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import Logic.SessionManager;
import Logic.MasterModeManager;
import Logic.MasterModeManager.TablaInfo;
import Logic.ControllerDataManager;
import MetodosFrecuentes.MetodosFrecuentes;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MasterController implements Initializable {
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnSearch;
    
    @FXML
    private Button btnAdministrar;
    
    @FXML
    private ListView<String> listTable;
    
    @FXML
    private TextField txtTableName;
    
    private MasterModeManager masterManager;
    private ObservableList<String> allTables;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicializar el manager de modo Master
        if (SessionManager.getInstance().getConnectionUser() != null) {
            masterManager = new MasterModeManager(SessionManager.getInstance().getConnectionUser());
            loadAllTables();
        }
    }
    
    /**
     * Carga todas las tablas del usuario al inicializar la ventana
     */
    private void loadAllTables() {
        try {
            List<TablaInfo> tableInfos = masterManager.getMyTables();
            ObservableList<String> tableNames = FXCollections.observableArrayList();
            
            for (TablaInfo tableInfo : tableInfos) {
                tableNames.add(tableInfo.getNombre());
            }
            
            allTables = tableNames;
            listTable.setItems(allTables);
        } catch (Exception e) {
            MetodosFrecuentes.mostrarAlerta("Error", "Error al cargar las tablas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchText = txtTableName.getText().trim();
        
        if (searchText.isEmpty()) {
            // Si está vacío, mostrar todas las tablas
            listTable.setItems(allTables);
            return;
        }
        
        try {
            // Filtrar tablas que contengan el texto buscado (al principio del nombre)
            ObservableList<String> filteredTables = FXCollections.observableArrayList();
            
            for (String table : allTables) {
                if (table.toLowerCase().startsWith(searchText.toLowerCase())) {
                    filteredTables.add(table);
                }
            }
            
            if (filteredTables.isEmpty()) {
                MetodosFrecuentes.mostrarAlerta("Sin Resultados", "No se encontraron tablas que inicien con: " + searchText);
                // Restaurar todas las tablas
                listTable.setItems(allTables);
            } else {
                listTable.setItems(filteredTables);
            }
            
        } catch (Exception e) {
            MetodosFrecuentes.mostrarAlerta("Error", "Error durante la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAdministrar() {
        String selectedTable = listTable.getSelectionModel().getSelectedItem();
        
        if (selectedTable == null) {
            MetodosFrecuentes.mostrarAlerta("Error de Selección", "Selecciona primero un elemento de la lista");
            return;
        }
        
        // Guardar la tabla seleccionada para el TableController
        ControllerDataManager.getInstance().setSelectedTableName(selectedTable);
        
        // Cambiar a la vista de tabla
        MetodosFrecuentes.cambiarVentana((Stage) btnAdministrar.getScene().getWindow(), "/views/Table-view.fxml");
    }
    
    @FXML
    private void handleBack() {
        MetodosFrecuentes.cambiarVentana((Stage) btnBack.getScene().getWindow(), "/views/Menu-view.fxml");
    }
}
