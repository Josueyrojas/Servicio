package me.julionxn.nobaitc.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.julionxn.nobaitc.util.AppExecutor;

import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {

    /**
     * Raíz de recursos en el classpath. Se usa una ruta ABSOLUTA (en vez de
     * relativa a esta clase) para que la carga de FXML/imágenes siga funcionando
     * aunque esta clase haya cambiado de paquete (controllers → app).
     */
    private static final String RESOURCE_ROOT = "/me/julionxn/nobaitc/";

    @Override
    public void start(Stage stage) throws IOException {
        double[] size = { 1240, 680 };
        FXMLLoader fxmlLoader = new FXMLLoader(getResourceURL("fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), size[0], size[1]);
        stage.setTitle("Diseño Experimental | Generador de Fracciones");
        Image icon = new Image(getResourceURL("images/icons8-fraction-48.png").toString());
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        AppExecutor.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** Resuelve un recurso del classpath bajo {@link #RESOURCE_ROOT}. */
    public static URL getResourceURL(String path) {
        return MainApplication.class.getResource(RESOURCE_ROOT + path);
    }
}
