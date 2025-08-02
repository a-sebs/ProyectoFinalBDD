module bdd.database {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens application to javafx.fxml;
    opens controllers to javafx.fxml;
    opens MetodosFrecuentes to javafx.fxml;
    opens Logic to javafx.fxml;

    exports application;
    exports controllers;
    exports MetodosFrecuentes;
    exports Logic;
}