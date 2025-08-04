package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.application.Platform;
import Logic.SessionManager;
import Logic.ControllerDataManager;
import Logic.RemoteModeManager;
import MetodosFrecuentes.MetodosFrecuentes;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;

public class RemoteController implements Initializable {
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnAccederVista;
    
    @FXML
    private Button btnAccederTabla;
    
    @FXML
    private TableView<VistaInfo> tableViewVistas;
    
    @FXML
    private TableColumn<VistaInfo, String> colVistaName;
    
    @FXML
    private TableColumn<VistaInfo, String> colVistaSchema;
    
    @FXML
    private TableColumn<VistaInfo, String> colVistaEstado;
    
    @FXML
    private TableView<TablaInfo> tableViewTablas;
    
    @FXML
    private TableColumn<TablaInfo, String> colTablaName;
    
    @FXML
    private TableColumn<TablaInfo, String> colTablaSchema;
    
    @FXML
    private TableColumn<TablaInfo, String> colTablaEstado;
    
    private RemoteModeManager remoteModeManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar las columnas de las tablas
        colVistaName.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colVistaSchema.setCellValueFactory(new PropertyValueFactory<>("schema"));
        colVistaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        
        colTablaName.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTablaSchema.setCellValueFactory(new PropertyValueFactory<>("schema"));
        colTablaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        
        // Inicializar el RemoteModeManager
        if (SessionManager.getInstance().getConnectionUser().estaConectado()) {
            remoteModeManager = new RemoteModeManager(SessionManager.getInstance().getConnectionUser());
            remoteModeManager.activateRemoteMode();
            
            // Cargar datos en segundo plano para evitar congelar la UI
            cargarDatosEnSegundoPlano();
        } else {
            MetodosFrecuentes.mostrarAlertaError("Error", "No hay conexión activa a la base de datos");
        }
    }
    
    /**
     * Carga los datos en segundo plano para evitar congelar la interfaz
     */
    private void cargarDatosEnSegundoPlano() {
        // Deshabilitar botones mientras se cargan los datos
        Platform.runLater(() -> {
            btnAccederVista.setDisable(true);
            btnAccederTabla.setDisable(true);
        });
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Ejecutar carga de datos en hilo de fondo
                cargarVistasYTablas();
                return null;
            }
            
            @Override
            protected void succeeded() {
                // Este método se ejecuta en el hilo de JavaFX cuando la tarea termina exitosamente
                Platform.runLater(() -> {
                    btnAccederVista.setDisable(false);
                    btnAccederTabla.setDisable(false);
                });
                System.out.println("Carga de vistas y tablas completada");
            }
            
            @Override
            protected void failed() {
                // Este método se ejecuta si hay una excepción
                Platform.runLater(() -> {
                    btnAccederVista.setDisable(false);
                    btnAccederTabla.setDisable(false);
                    Throwable exception = getException();
                    MetodosFrecuentes.mostrarAlertaError("Error", 
                        "Error al cargar vistas y tablas: " + exception.getMessage());
                    exception.printStackTrace();
                });
            }
        };
        
        // Ejecutar la tarea en un hilo separado
        Thread thread = new Thread(task);
        thread.setDaemon(true); // El hilo se cerrará cuando la aplicación se cierre
        thread.start();
    }
    
    /**
     * Carga todas las vistas y tablas disponibles
     */
    private void cargarVistasYTablas() {
        cargarVistas();
        cargarTablas();
    }
    
    /**
     * Carga las vistas disponibles
     */
    private void cargarVistas() {
        ObservableList<VistaInfo> vistas = FXCollections.observableArrayList();
        Set<String> vistasYaAgregadas = new HashSet<>(); // Para evitar duplicados
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            return;
        }
        
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            
            // Consulta consolidada para vistas locales usando UNION
            String sql = 
                // Vistas normales (primera prioridad)
                "SELECT VIEW_NAME as NOMBRE, 'USER' as OWNER, " +
                "CASE WHEN READ_ONLY = 'Y' THEN 'Solo Lectura' ELSE 'Lectura/Escritura' END as ESTADO, " +
                "'VISTA' as TIPO " +
                "FROM USER_VIEWS " +
                
                "UNION ALL " +
                
                // Vistas materializadas (segunda prioridad)
                "SELECT MVIEW_NAME as NOMBRE, 'USER' as OWNER, " +
                "'Vista Materializada' as ESTADO, 'MVIEW' as TIPO " +
                "FROM USER_MVIEWS " +
                
                "UNION ALL " +
                
                // Tablas VW (tercera prioridad)
                "SELECT TABLE_NAME as NOMBRE, 'USER' as OWNER, " +
                "'Vista (VW)' as ESTADO, 'VW' as TIPO " +
                "FROM USER_TABLES " +
                "WHERE TABLE_NAME LIKE 'VW%' " +
                
                "ORDER BY TIPO, NOMBRE";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                String viewName = rs.getString("NOMBRE");
                if (!vistasYaAgregadas.contains(viewName)) {
                    VistaInfo vista = new VistaInfo(
                        viewName,
                        rs.getString("OWNER"),
                        rs.getString("ESTADO")
                    );
                    vistas.add(vista);
                    vistasYaAgregadas.add(viewName);
                    System.out.println("Vista encontrada: " + viewName + " (" + rs.getString("TIPO") + ")");
                }
            }
            
            rs.close();
            stmt.close();
            
            // Obtener vistas de otros schemas con permisos - consulta simplificada
            try {
                sql = "SELECT ut.TABLE_NAME, ut.OWNER, ut.PRIVILEGE " +
                      "FROM USER_TAB_PRIVS ut " +
                      "WHERE ut.PRIVILEGE IN ('SELECT', 'READ') " +
                      "AND (EXISTS (SELECT 1 FROM ALL_VIEWS av WHERE av.VIEW_NAME = ut.TABLE_NAME AND av.OWNER = ut.OWNER) " +
                      "     OR EXISTS (SELECT 1 FROM ALL_MVIEWS amv WHERE amv.MVIEW_NAME = ut.TABLE_NAME AND amv.OWNER = ut.OWNER) " +
                      "     OR ut.TABLE_NAME LIKE 'VW%') " +
                      "ORDER BY ut.OWNER, ut.TABLE_NAME";
                
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                
                while (rs.next()) {
                    String nombreCompleto = rs.getString("OWNER") + "." + rs.getString("TABLE_NAME");
                    if (!vistasYaAgregadas.contains(nombreCompleto)) {
                        VistaInfo vista = new VistaInfo(
                            nombreCompleto,
                            rs.getString("OWNER"),
                            "Acceso: " + rs.getString("PRIVILEGE")
                        );
                        vistas.add(vista);
                        vistasYaAgregadas.add(nombreCompleto);
                        System.out.println("Vista externa encontrada: " + nombreCompleto);
                    }
                }
                
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                // Si falla la consulta de vistas externas, continuamos solo con las propias
                System.out.println("No se pudieron cargar vistas de otros esquemas: " + e.getMessage());
            }
            
            // Obtener vistas remotas usando database link PROYECTO_REAL
            try {
                // Vistas normales remotas
                sql = "SELECT VIEW_NAME, 'REMOTO' as OWNER, 'Acceso Remoto' as ESTADO " +
                      "FROM USER_VIEWS@PROYECTO_REAL " +
                      "ORDER BY VIEW_NAME";
                
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                
                while (rs.next()) {
                    String nombreCompleto = rs.getString("VIEW_NAME") + "@PROYECTO_REAL";
                    if (!vistasYaAgregadas.contains(nombreCompleto)) {
                        VistaInfo vista = new VistaInfo(
                            nombreCompleto,
                            rs.getString("OWNER"),
                            rs.getString("ESTADO")
                        );
                        vistas.add(vista);
                        vistasYaAgregadas.add(nombreCompleto);
                        System.out.println("Vista remota encontrada: " + nombreCompleto);
                    }
                }
                
                rs.close();
                stmt.close();
                
                // Vistas materializadas remotas
                try {
                    sql = "SELECT MVIEW_NAME as VIEW_NAME, 'REMOTO' as OWNER, 'Vista Mat. Remota' as ESTADO " +
                          "FROM USER_MVIEWS@PROYECTO_REAL " +
                          "ORDER BY MVIEW_NAME";
                    
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery(sql);
                    
                    while (rs.next()) {
                        String nombreCompleto = rs.getString("VIEW_NAME") + "@PROYECTO_REAL";
                        if (!vistasYaAgregadas.contains(nombreCompleto)) {
                            VistaInfo vista = new VistaInfo(
                                nombreCompleto,
                                rs.getString("OWNER"),
                                rs.getString("ESTADO")
                            );
                            vistas.add(vista);
                            vistasYaAgregadas.add(nombreCompleto);
                            System.out.println("Vista materializada remota encontrada: " + nombreCompleto);
                        }
                    }
                    
                    rs.close();
                    stmt.close();
                    
                } catch (SQLException e) {
                    System.out.println("No se pudieron cargar vistas materializadas remotas: " + e.getMessage());
                }
                
                // Tablas VW remotas
                try {
                    sql = "SELECT TABLE_NAME as VIEW_NAME, 'REMOTO' as OWNER, 'Vista VW Remota' as ESTADO " +
                          "FROM USER_TABLES@PROYECTO_REAL " +
                          "WHERE TABLE_NAME LIKE 'VW%' " +
                          "ORDER BY TABLE_NAME";
                    
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery(sql);
                    
                    while (rs.next()) {
                        String nombreCompleto = rs.getString("VIEW_NAME") + "@PROYECTO_REAL";
                        if (!vistasYaAgregadas.contains(nombreCompleto)) {
                            VistaInfo vista = new VistaInfo(
                                nombreCompleto,
                                rs.getString("OWNER"),
                                rs.getString("ESTADO")
                            );
                            vistas.add(vista);
                            vistasYaAgregadas.add(nombreCompleto);
                            System.out.println("Vista VW remota encontrada: " + nombreCompleto);
                        }
                    }
                    
                    rs.close();
                    stmt.close();
                    
                } catch (SQLException e) {
                    System.out.println("No se pudieron cargar vistas VW remotas: " + e.getMessage());
                }
                
            } catch (SQLException e) {
                // Si falla la consulta de vistas remotas, continuamos 
                System.out.println("No se pudieron cargar vistas remotas desde PROYECTO_REAL: " + e.getMessage());
            }
            
        } catch (SQLException e) {
            Platform.runLater(() -> {
                MetodosFrecuentes.mostrarAlertaError("Error", "Error al cargar vistas: " + e.getMessage());
            });
            e.printStackTrace();
        }
        
        System.out.println("Total vistas cargadas: " + vistas.size());
        // Actualizar la UI desde el hilo de JavaFX
        Platform.runLater(() -> {
            tableViewVistas.setItems(vistas);
        });
    }
    
    /**
     * Carga las tablas disponibles
     */
    private void cargarTablas() {
        ObservableList<TablaInfo> tablas = FXCollections.observableArrayList();
        
        if (!SessionManager.getInstance().getConnectionUser().estaConectado()) {
            return;
        }
        
        try {
            Connection conn = SessionManager.getInstance().getConnectionUser().getConnection();
            
            // Obtener tablas propias (excluyendo vistas, vistas materializadas y tablas VW)
            String sql = "SELECT TABLE_NAME, 'USER' as OWNER, 'Propia' as ESTADO " +
                        "FROM USER_TABLES " +
                        "WHERE TABLE_NAME NOT IN (SELECT VIEW_NAME FROM USER_VIEWS) " +
                        "AND TABLE_NAME NOT IN (SELECT MVIEW_NAME FROM USER_MVIEWS) " +
                        "AND TABLE_NAME NOT LIKE 'VW%' " +
                        "ORDER BY TABLE_NAME";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                TablaInfo tabla = new TablaInfo(
                    tableName,
                    rs.getString("OWNER"),
                    rs.getString("ESTADO")
                );
                tablas.add(tabla);
                System.out.println("Tabla encontrada: " + tableName);
            }
            
            rs.close();
            stmt.close();
            
            // Obtener tablas de otros schemas con permisos (excluyendo vistas, vistas materializadas y VW)
            try {
                sql = "SELECT ut.TABLE_NAME, ut.OWNER, ut.PRIVILEGE " +
                      "FROM USER_TAB_PRIVS ut " +
                      "WHERE ut.PRIVILEGE IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE') " +
                      "AND EXISTS (SELECT 1 FROM ALL_TABLES at WHERE at.TABLE_NAME = ut.TABLE_NAME AND at.OWNER = ut.OWNER) " +
                      "AND NOT EXISTS (SELECT 1 FROM ALL_VIEWS av WHERE av.VIEW_NAME = ut.TABLE_NAME AND av.OWNER = ut.OWNER) " +
                      "AND NOT EXISTS (SELECT 1 FROM ALL_MVIEWS amv WHERE amv.MVIEW_NAME = ut.TABLE_NAME AND amv.OWNER = ut.OWNER) " +
                      "AND ut.TABLE_NAME NOT LIKE 'VW%' " +
                      "ORDER BY ut.OWNER, ut.TABLE_NAME";
                
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                
                while (rs.next()) {
                    String nombreCompleto = rs.getString("OWNER") + "." + rs.getString("TABLE_NAME");
                    TablaInfo tabla = new TablaInfo(
                        nombreCompleto,
                        rs.getString("OWNER"),
                        "Acceso: " + rs.getString("PRIVILEGE")
                    );
                    tablas.add(tabla);
                    System.out.println("Tabla externa encontrada: " + nombreCompleto);
                }
                
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                // Si falla la consulta de tablas externas, continuamos solo con las propias
                System.out.println("No se pudieron cargar tablas de otros esquemas: " + e.getMessage());
            }
            
            // Obtener tablas remotas usando database link PROYECTO_REAL (excluyendo vistas, vistas materializadas y VW)
            try {
                sql = "SELECT TABLE_NAME, 'REMOTO' as OWNER, 'Acceso Remoto' as ESTADO " +
                      "FROM USER_TABLES@PROYECTO_REAL " +
                      "WHERE TABLE_NAME NOT IN (SELECT VIEW_NAME FROM USER_VIEWS@PROYECTO_REAL) " +
                      "AND TABLE_NAME NOT IN (SELECT MVIEW_NAME FROM USER_MVIEWS@PROYECTO_REAL) " +
                      "AND TABLE_NAME NOT LIKE 'VW%' " +
                      "ORDER BY TABLE_NAME";
                
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                
                while (rs.next()) {
                    String nombreCompleto = rs.getString("TABLE_NAME") + "@PROYECTO_REAL";
                    TablaInfo tabla = new TablaInfo(
                        nombreCompleto,
                        rs.getString("OWNER"),
                        rs.getString("ESTADO")
                    );
                    tablas.add(tabla);
                    System.out.println("Tabla remota encontrada: " + nombreCompleto);
                }
                
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                // Si falla la consulta de tablas remotas, continuamos 
                System.out.println("No se pudieron cargar tablas remotas desde PROYECTO_REAL: " + e.getMessage());
            }
            
        } catch (SQLException e) {
            Platform.runLater(() -> {
                MetodosFrecuentes.mostrarAlertaError("Error", "Error al cargar tablas: " + e.getMessage());
            });
            e.printStackTrace();
        }
        
        System.out.println("Total tablas cargadas: " + tablas.size());
        // Actualizar la UI desde el hilo de JavaFX
        Platform.runLater(() -> {
            tableViewTablas.setItems(tablas);
        });
    }
    
    @FXML
    private void handleAccederVista() {
        VistaInfo vistaSeleccionada = tableViewVistas.getSelectionModel().getSelectedItem();
        
        if (vistaSeleccionada == null) {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error de Selección", "Seleccione una vista de la lista");
            return;
        }
        
        // Guardar la vista seleccionada en el ControllerDataManager
        ControllerDataManager.getInstance().setSelectedVistaName(vistaSeleccionada.getNombre());
        ControllerDataManager.getInstance().setSelectedTableName(null); // Limpiar tabla
        
        // Cambiar a la vista Vista-view.fxml
        MetodosFrecuentes.cambiarVentana((Stage) btnAccederVista.getScene().getWindow(), "/views/Vista-view.fxml");
    }
    
    @FXML
    private void handleAccederTabla() {
        TablaInfo tablaSeleccionada = tableViewTablas.getSelectionModel().getSelectedItem();
        
        if (tablaSeleccionada == null) {
            MetodosFrecuentes.mostrarAlertaAdvertencia("Error de Selección", "Seleccione una tabla de la lista");
            return;
        }
        
        // Guardar la tabla seleccionada en el ControllerDataManager
        ControllerDataManager.getInstance().setSelectedTableName(tablaSeleccionada.getNombre());
        ControllerDataManager.getInstance().setSelectedVistaName(null); // Limpiar vista
        
        // Cambiar a la vista Vista-view.fxml (misma vista para tablas y vistas)
        MetodosFrecuentes.cambiarVentana((Stage) btnAccederTabla.getScene().getWindow(), "/views/Vista-view.fxml");
    }
    
    @FXML
    private void handleRefresh() {
        cargarVistasYTablas();
        MetodosFrecuentes.mostrarAlerta("Información", "Lista de vistas y tablas actualizada correctamente.");
    }
    
    @FXML
    private void handleBack() {
        MetodosFrecuentes.cambiarVentana((Stage) btnBack.getScene().getWindow(), "/views/Menu-view.fxml");
    }
    
    /**
     * Clase interna para información de vistas
     */
    public static class VistaInfo {
        private String nombre;
        private String schema;
        private String estado;
        
        public VistaInfo(String nombre, String schema, String estado) {
            this.nombre = nombre;
            this.schema = schema;
            this.estado = estado;
        }
        
        public String getNombre() { return nombre; }
        public String getSchema() { return schema; }
        public String getEstado() { return estado; }
        
        public void setNombre(String nombre) { this.nombre = nombre; }
        public void setSchema(String schema) { this.schema = schema; }
        public void setEstado(String estado) { this.estado = estado; }
    }
    
    /**
     * Clase interna para información de tablas
     */
    public static class TablaInfo {
        private String nombre;
        private String schema;
        private String estado;
        
        public TablaInfo(String nombre, String schema, String estado) {
            this.nombre = nombre;
            this.schema = schema;
            this.estado = estado;
        }
        
        public String getNombre() { return nombre; }
        public String getSchema() { return schema; }
        public String getEstado() { return estado; }
        
        public void setNombre(String nombre) { this.nombre = nombre; }
        public void setSchema(String schema) { this.schema = schema; }
        public void setEstado(String estado) { this.estado = estado; }
    }
}
