/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.debuguj.tcp.server.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import pl.debuguj.tcp.server.ServerTcp;
import pl.debuguj.tcp.server.ServerTcpListener;


/**
 *
 * @author Grzesiek
 */
public class MainPaneServerController implements Initializable, ServerTcpListener {
    
    @FXML
    private TextArea textArea;
    @FXML
    private Button btnRestart;
    @FXML
    private Label lblStatus;   
    @FXML
    private TextField tfdMessage;   
    
    private static final int SERVER_TCP_PORT = 9000;
    private static final int SERVER_TCP_CLIENT_LIMIT = 1;
    
    private ServerTcp serverTcp = null;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serverTcp = new ServerTcp(SERVER_TCP_PORT);
        serverTcp.setClientLimit(SERVER_TCP_CLIENT_LIMIT);
        serverTcp.addServerListener(this);
    }    
    
    @FXML
    private void handleRefreshAction(ActionEvent event) {
        textArea.appendText("Refreshing....");
        if (serverTcp != null) {

            //serverTcp.shutDown();
            
            serverTcp.start();

            if (serverTcp.isAlive()) {
                textArea.appendText("Server TCP alive :) \n");
            } else {
                textArea.appendText("Server TCP dead :( \n");
            }
        }
    }    

    @FXML
    private void handleSendDisconnect(ActionEvent event) {
        textArea.appendText("Refresh....\n"); 
    }
    
    @FXML
    private void handleSendStatus(ActionEvent event) {
        textArea.appendText("EVENT: handleSendStatus\n");

    }     
    @FXML
    private void handleSendError(ActionEvent event) {
        textArea.appendText("EVENT: handleSendError\n");
 
    } 
    
    @FXML
    private void handleSend(ActionEvent event) {
        textArea.appendText("Sending message: "+tfdMessage.getText()+ "\n");
        serverTcp.send(tfdMessage.getText(), 1);
    }     

    @Override
    public void onClientConnected(ServerTcp server, ServerTcp.ClientConnection client) {
        textArea.appendText("EVENT: onClientConnected\n");
    }

    @Override
    public void onClientDisconnected(ServerTcp server, ServerTcp.ClientConnection client) {
        textArea.appendText("EVENT: onClientDisconnected\n");
    }

    @Override
    public void onMessageReceived(String msg) {
        textArea.appendText("EVENT: onMessageReceived \n" + msg + "\n" );
    }

    @Override
    public void onMessageSent(ServerTcp server, ServerTcp.ClientConnection toClient, String msg) {
        textArea.appendText("EVENT: onMessageSent \n" + msg + "\n" );  
    }


    @Override
    public void onInternalError(ServerTcp server, ServerTcp.ClientConnection toClient, String error) {
        textArea.appendText("Event: onInternalError \n");
    }
    
}
