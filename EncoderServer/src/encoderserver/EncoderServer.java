/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoderserver;

import encoder.EncControl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Claudio
 */
public class EncoderServer {

    private static final String HOSTNAME = "localhost";
    private static final int desiredCams = 5;
    
    private static final int SERVER_PORT = 1766;
    
    private static final String reqEncInit      = "REQ_encoder_init";
    private static final String reqEncSetCpf    = "REQ_encoder_set_cpf";
    private static final String reqEncStop      = "REQ_encoder_stop";

    public enum ENCODER_ERROR {

        INIT_OK,
        INIT_FAIL,
        CPF_OK,
        CPF_INVALID,
        STOP_OK,
        STOP_FAIL,
    };

    private void dbgMsg(String s) {
        System.out.println("[DEBUG] " + s);
    }

    
    private void encHandle_Init(
            PrintWriter out, BufferedReader in, EncControl encControl) {
        
        try {
            ENCODER_ERROR err = ENCODER_ERROR.INIT_FAIL;
            
            if (encControl.isRecording) {
                encControl.stop();
            }
            
            encControl.start();
            err = ENCODER_ERROR.INIT_OK;
            
            dbgMsg(err.toString());
            out.println(err.toString());
        } catch (IOException ex) {
            Logger.getLogger(EncoderServer.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    private void encHandle_SetCpf(
            PrintWriter out, BufferedReader in, EncControl encControl) 
            throws IOException {
        
        ENCODER_ERROR err = ENCODER_ERROR.CPF_INVALID;
        
        String fromClient = in.readLine();
        
        fromClient = cpflib.CPFLib.formatCPF_onlyNumbers(fromClient);
        
        encControl.setIdentifier(fromClient);
        
        err = ENCODER_ERROR.CPF_OK;
        
        dbgMsg(err.toString());
        out.println(err.toString());
    }
    
    private void encHandle_Stop(
            PrintWriter out, BufferedReader in, EncControl encControl) {
        
        ENCODER_ERROR err = ENCODER_ERROR.STOP_FAIL;
        
        if (encControl.isRecording) {
            encControl.stop();
        }
        
        err = ENCODER_ERROR.STOP_OK;
        
        dbgMsg(err.toString());
        out.println(err.toString());  
    }
    
    public void encHandleClient(
        Socket clientSocket, EncControl encControl) {

        try {
            PrintWriter out
                    = new PrintWriter(
                            clientSocket.getOutputStream(),
                            true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));

            String fromClient;

            if ((fromClient = in.readLine()) != null) {
                switch (fromClient) {
                    case reqEncInit:
                        dbgMsg(reqEncInit);
                        encHandle_Init(out, in, encControl);
                        break;
                        
                    case reqEncSetCpf:
                        dbgMsg(reqEncSetCpf);
                        encHandle_SetCpf(out, in, encControl);
                        break;
                        
                    case reqEncStop:
                        dbgMsg(reqEncStop);
                        encHandle_Stop(out, in, encControl);
                        break;
                }
            }
        } catch (SocketTimeoutException e) {
            dbgMsg("SocketTimeoutException");
        } catch (IOException e) {
            dbgMsg("IOException");

            Logger.getLogger(
                    EncoderServer.class.getName()).log(Level.SEVERE, null, e);
        } catch (Exception e) {
            dbgMsg("Exception");

            Logger.getLogger(
                    EncoderServer.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(
                        EncoderServer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean run() throws ClassNotFoundException, SQLException, IOException {
        ServerSocket socket = null;
        Socket clientSocket = null;
        EncControl encControl = null;

        try {
            dbgMsg("Openning Encoder...");
            encControl = new EncControl("C:\\Teste\\Videos");
            encControl.openResources();
            if ((encControl.isAudioOk() == false)
                || (encControl.getNumCams() >= desiredCams)) {

                throw new IOException("Cameras or audio not found.");
            }

            dbgMsg("Openning server port...");
            socket = new ServerSocket(SERVER_PORT);
            dbgMsg("Server listenning to: " + SERVER_PORT);

            while (true) {
                dbgMsg("Waiting for client...");

                clientSocket = socket.accept();
                dbgMsg("Client connected: "
                        + clientSocket.getInetAddress()
                        .getHostAddress());

                clientSocket.setSoTimeout(60 * 1000);
                clientSocket.setKeepAlive(true);

                encHandleClient(clientSocket, encControl);
            }
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }                
                if (encControl != null) {
                    encControl.stop();
                }
            } catch (IOException ex) {
                Logger.getLogger(
                    EncoderServer.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                socket      = null;
                encControl  = null;
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EncoderServer encServer = new EncoderServer();

        while (true) {
            try {
                encServer.dbgMsg("System running...");
                encServer.run();

            } catch (Exception ex) {
                Logger.getLogger(
                        EncoderServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            encServer.dbgMsg("Sleeping...");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(EncoderServer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
    }

}
