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
import java.util.logging.Level;
import java.util.logging.Logger;

import encoderserver.EncoderServer.ENCODER_ERROR;

/**
 *
 * @author Claudio
 */
public class EncoderClient {
    private static final String HOSTNAME = "localhost";
  
    static void dbgMsg(String s) {
        System.out.println("[DEBUG] " + s);
    }    
    
    public static boolean initEncoder() {
        boolean state = false;
        Socket socket = null;
        
        try {
            String fromServer;
            
            socket = new Socket(HOSTNAME, encoderserver.EncoderServer.SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket opened...");
            dbgMsg("Client: " + encoderserver.EncoderServer.reqEncInit);
            out.println(encoderserver.EncoderServer.reqEncInit);
            
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
            String fromServer;
            
            socket = new Socket(HOSTNAME, encoderserver.EncoderServer.SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket opened...");
            dbgMsg("Client: " + encoderserver.EncoderServer.reqEncSetCpf);            
            dbgMsg("Client: " + cpf);
            out.println(encoderserver.EncoderServer.reqEncSetCpf);
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

    public static boolean setPath(String path) {
        boolean state = false;
        Socket socket = null;
        
        try {
            String fromServer;
            
            socket = new Socket(HOSTNAME, encoderserver.EncoderServer.SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket opened...");
            dbgMsg("Client: " + encoderserver.EncoderServer.reqEncSetPath);            
            dbgMsg("Client: " + path);
            out.println(encoderserver.EncoderServer.reqEncSetPath);
            out.println(path);
            
            fromServer = in.readLine();
            dbgMsg("Server: " + fromServer);
            ENCODER_ERROR err = ENCODER_ERROR.valueOf(fromServer);
            if (err == ENCODER_ERROR.PATH_OK) {
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
            String fromServer;
            
            socket = new Socket(HOSTNAME, encoderserver.EncoderServer.SERVER_PORT);
            socket.setSoTimeout(25000);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        socket.getInputStream()));        
        
            dbgMsg("Socket opened...");
            dbgMsg("Client: " + encoderserver.EncoderServer.reqEncStop);
            out.println(encoderserver.EncoderServer.reqEncStop);
            
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
            System.out.println("3 - Set Path");
            System.out.println("4 - Stop");
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
                    state = setPath(stdIn.readLine());
                }
                else if (fromUser.contains("4")) {
                    state = stopEncoder();
                }

                System.out.println("RSP: " + state);
                System.out.println("\n");
            } 
            catch (IOException ex) {
                Logger.getLogger(EncoderClient.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
