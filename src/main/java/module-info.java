module me.julionxn.nobaitc {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires org.controlsfx.controls;

    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;
    requires org.kordamp.ikonli.typicons;

    requires kernel;
    requires layout;
    requires io;
    requires org.slf4j;
    requires org.apache.logging.log4j;

    // Punto de entrada (JavaFX instancia la Application por reflexión)
    exports me.julionxn.nobaitc.app;
    opens me.julionxn.nobaitc.app to javafx.graphics, javafx.fxml;

    // Dominio (API pública del modelo)
    exports me.julionxn.nobaitc.doe.design;
    exports me.julionxn.nobaitc.doe.nonbpa;
    exports me.julionxn.nobaitc.doe.alias;

    // Controladores: FXMLLoader los instancia e inyecta @FXML por reflexión
    opens me.julionxn.nobaitc.ui.controllers to javafx.fxml;

    // View-model: PropertyValueFactory accede a las *Property() por reflexión
    opens me.julionxn.nobaitc.ui.viewmodel to javafx.base, javafx.fxml;
}
