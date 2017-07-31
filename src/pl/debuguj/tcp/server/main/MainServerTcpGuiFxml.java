/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.debuguj.tcp.server.main;

import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Grzesiek
 */
public class MainServerTcpGuiFxml extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/pl/debuguj/tcp/server/gui/MainPaneServer.fxml"));
        
        ResourceBundle rb = ResourceBundle.getBundle("pl.debuguj.tcp.server.gui.Bundle", new Locale("pl", "PL"));
        loader.setResources(rb);

        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Server TCP");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
