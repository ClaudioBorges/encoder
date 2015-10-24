/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoderclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Claudio
 */
public class EncoderClient {
    public enum ENCODER_ERROR {

        INIT_OK,
        INIT_FAIL,
        CPF_OK,
        CPF_INVALID,
        STOP_OK,
        STOP_FAIL,
    };
  
    private static final int SERVER_PORT = 1766;
    private static final String HOSTNAME = "localhost";
    
    private static final String reqEncInit      = "REQ_encoder_init";
    private static final String reqEncSetCpf    = "REQ_encoder_set_cpf";
    private static final String reqEncStop      = "REQ_encoder_stop";
    
    
    static void dbgMsg(String s) {
        System.out.println("[DEBUG] " + s);
    }
    
    
    public static boolean initEncoder() {
        boolean state = false;
        Socket socket = null;
        
        try {
            String fromServer = null;
            
            socket = new Socket(HOSTNAME, SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket oppened...");
            dbgMsg("Client: " + reqEncInit);
            out.println(reqEncInit);
            
            fromServer = in.readLine();
            dbgMsg("Server: " + fromServer);
            if (ENCODER_ERROR.valueOf(fromServer) == ENCODER_ERROR.INIT_OK)
                state = true;
            
        } catch (Exception ex) {
            Logger.getLogger(
                    EncoderClient.class.getName())
                    .log(Level.SEVERE, null, ex);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(
                            EncoderClient.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return state;
    }        

    public static boolean setCPF(String cpf) {
        boolean state = false;
        Socket socket = null;
        
        try {
            String fromServer = null;
            
            socket = new Socket(HOSTNAME, SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket oppened...");
            dbgMsg("Client: " + reqEncStop);            
            dbgMsg("Client: " + cpf);
            out.println(reqEncSetCpf);
            out.println(cpf);
            
            fromServer = in.readLine();
            dbgMsg("Server: " + fromServer);
            ENCODER_ERROR err = ENCODER_ERROR.valueOf(fromServer);
            if (err == ENCODER_ERROR.CPF_OK) {
                state = true;
            }
            
        } catch (Exception ex) {
            Logger.getLogger(
                    EncoderClient.class.getName())
                    .log(Level.SEVERE, null, ex);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(
                            EncoderClient.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return state;
    }        

    public static boolean stopEncoder() {
        boolean state = false;
        Socket socket = null;
        
        try {
            String fromServer = null;
            
            socket = new Socket(HOSTNAME, SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket oppened...");
            dbgMsg("Client: " + reqEncStop);
            out.println(reqEncStop);
            
            fromServer = in.readLine();            
            dbgMsg("Server: " + fromServer);
            if (ENCODER_ERROR.valueOf(fromServer) == ENCODER_ERROR.STOP_OK) {
                state = true;
            }            
        } catch (Exception ex) {
            Logger.getLogger(
                    EncoderClient.class.getName())
                    .log(Level.SEVERE, null, ex);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(
                            EncoderClient.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return state;
    } 
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        while (true) {
            String fromUser;
            BufferedReader stdIn = new BufferedReader(
                            new InputStreamReader(System.in));

            System.out.println("1 - Init");
            System.out.println("2 - Set CPF");
            System.out.println("3 - Stop");
            System.out.println("or exit");
                        
            try {
                boolean state = false;
                
                fromUser = stdIn.readLine();
                if (fromUser.contains("exit")) {
                    break;
                }
                else if (fromUser.contains("1")) {
                    state = initEncoder();
                }
                else if (fromUser.contains("2")) {
                    state = setCPF(stdIn.readLine());
                }
                else if (fromUser.contains("3")) {
                    state = stopEncoder();
                }

                System.out.println("RSP: " + state);
                System.out.println("\n\n");
            } 
            catch (IOException ex) {
                Logger.getLogger(EncoderClient.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
