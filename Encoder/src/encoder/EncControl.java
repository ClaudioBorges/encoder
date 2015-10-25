/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoder;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import static encoder.Encoder.dbgMsg;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author Claudio
 */
public class EncControl {
    private final String defaultFilePath;
    
    private static final int frameRate = 15;
    private static final Dimension dimension = WebcamResolution.QVGA.getSize();
     
    private static final AudioFormat audioFormat 
                = new AudioFormat(44100.0F, 16, 1, true, false);
    
    private List<Webcam> webcams = null;
    private TargetDataLine line = null;
    private String[] filenames = null;
    private Encoder[] encs = null;
    
    private String identifier = null;
    
    public boolean isRecording = false;
    
    public EncControl(String path) {
        defaultFilePath = path;
    } 
    
    public void openResources() {
        try {
            webcams = Webcam.getWebcams();
            
            filenames = new String[webcams.size()];
            encs = new Encoder[webcams.size()];
            
            for (int i = 0; i < webcams.size(); ++i) {
                Webcam webcam = webcams.get(i);
                
                webcam.setViewSize(dimension);
                webcam.open(true);
                
                dbgMsg("Camera name: " + webcam.getName());
            }
            
            DataLine.Info dataLineInfo
                    = new DataLine.Info(TargetDataLine.class, audioFormat);
            line
                    = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
            // Open and start capturing audio
            line.open(audioFormat, line.getBufferSize());
            line.start();
        } catch (LineUnavailableException ex) {
            Logger.getLogger(EncControl.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    private static final String formatFilename(String path, int camID) {
        Date date = new Date();
        SimpleDateFormat dateFormat = 
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss") ;
        
        return (path + "\\" + dateFormat.format(date) + "_" + camID + ".mp4");
    } 
    
    public void start() {        
        for (int camID = 0; camID < webcams.size(); camID++) {
            filenames[camID] = formatFilename(defaultFilePath, camID);
            
            encs[camID] = new Encoder(filenames[camID], camID);
            
            encs[camID].addVideo(frameRate, dimension, webcams.get(camID));
            if (camID == 0 && line != null)
                encs[camID].addAudio(audioFormat, line);
            
            encs[camID].rec();
        }
        
        this.isRecording = true;
    }
    
    public void stop() {
        for (Encoder enc : encs) {
            try {
                enc.stop();
                enc.close();
                enc = null;
            } catch (InterruptedException ex) {
                Logger.getLogger(EncControl.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        for (String f : filenames) {
            try {
                int pointIdx = f.lastIndexOf(".");
                
                String name = f.subSequence(0, pointIdx).toString();
                String ext = f.subSequence(pointIdx, f.length()).toString();
                
                String filename = name + "__" + identifier + ext;
                
                File oldFile = new File(f);
                File newFile = new File(filename);
                Files.move(oldFile.toPath(), newFile.toPath());
            } catch (IOException ex) {
                Logger.getLogger(EncControl.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        this.isRecording = false;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    public int getNumCams() {
        if (webcams != null) {
            return webcams.size();
        }
        
        return 0;
    }
    
    public boolean isAudioOk() {
        return (line != null);
    }
    
    public static void main(String[] args) {
        EncControl enc = new EncControl("C:\\Teste\\Videos");
        
        enc.openResources();
        
        enc.start();
        enc.setIdentifier("38502729829");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(EncControl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        enc.stop();
        
    }
    
    
}
