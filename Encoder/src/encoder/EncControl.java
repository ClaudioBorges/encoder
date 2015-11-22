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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
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
    private static final String DEFAULT_PATH = ".\\";
    private static final String TEMP_PATH = "temp\\";
    
    private String defaultPath = DEFAULT_PATH;
    private String defaultPathTemp = DEFAULT_PATH;
    
    private static final int frameRate = 15;
     
    private static final AudioFormat audioFormat 
                = new AudioFormat(44100.0F, 16, 1, true, false);
    
    private List<Webcam> webcams = null;
    private TargetDataLine line = null;
    private String[] filenames = null;
    private Encoder[] encs = null;
    
    private String identifier = null;
    
    public boolean isRecording = false;
 
    private PropertyValues config;
 
    private class CamConfig {
         public final int ID;
         public final String name;
         public Dimension dimension;
         public int frameRate;
         public boolean flipped;
         public boolean hasAudio;

         public CamConfig(int ID, String name) {
             this.ID         = ID;

             if (name == null)
                 name = "";
             this.name       = name;

             this.dimension = WebcamResolution.QVGA.getSize();
             this.frameRate = 15;
             this.flipped   = false;
             this.hasAudio  = false;
         }

         public void setDimension(Dimension dimension) {
             this.dimension = dimension;
         }
         
         public void setFrameRate(int frameRate) {
             if ((5 < frameRate) && (60 > frameRate))
                this.frameRate = frameRate;
         }

         public void setFlipped(boolean flipped) {
             this.flipped = flipped;
         }

         public void setAudio(boolean hasAudio) {
             this.hasAudio = hasAudio;
         }

     }

    
    public EncControl(String fConfigs) {
        loadConfigs(fConfigs);
    }
    
    public EncControl(String fConfigs, String path) {
        loadConfigs(fConfigs);
        setDefaultPath(path);
    }
        
    private void loadConfigs(String filename) {
        try {
            config = new PropertyValues(filename);
        } catch (IOException e) {
            // Do nothing
        }
        
        if (config.framerate == 0) {
            config.framerate = 15;
        }
    }
     
    private final class PropertyValues {
        public int num = 0;        
        public int framerate = 0;
        public CamConfig[] camConfigs = null;
        
        public PropertyValues(String filename) throws IOException {
            updatePropValues(filename);
        }
        
        @SuppressWarnings("empty-statement")
        public void updatePropValues(String filename) throws IOException {
            InputStream is = null;
                    
            try {
                Properties prop = new Properties();
                is = new FileInputStream(filename);
                
                if (is != null) {
                    prop.load(is);
                } else {
                    throw new FileNotFoundException(
                            "property file '" + filename + "' not found");
                }
                
                try {
                    num = Integer.parseInt(prop.getProperty("CAM_IDS"));
                    if (num > 0) {
                        camConfigs = new CamConfig[num];
                        for (int i = 0; i < num; ++i) {
                            String name     = prop.getProperty("CAM_ID_" + i);;
                            String dim      = prop.getProperty("CAM_ID_" + i + "_DIMENSION");
                            String fr       = prop.getProperty("CAM_ID_" + i + "_FRAMERATE");
                            String flip     = prop.getProperty("CAM_ID_" + i + "_FLIPPED");
                            String audio    = prop.getProperty("CAM_ID_" + i + "_AUDIO");
                           
                            camConfigs[i] = new CamConfig(i, name);
                            
                            switch (dim==null?"":dim) {
                                case "VGA":
                                    camConfigs[i].setDimension(WebcamResolution.VGA.getSize());
                                    break;
                                case "QVGA":                
                                    camConfigs[i].setDimension(WebcamResolution.QVGA.getSize());
                                    break;
                                default:
                                    // Do nothing
                                    break;
                            }
                            
                            if (fr != null) {
                                try {
                                    camConfigs[i].setFrameRate(Integer.valueOf(frameRate));
                                } catch (NumberFormatException e) {
                                    // Do nothing
                                }
                            }                            
                            
                            if (flip != null) {
                                camConfigs[i].setFlipped(Boolean.valueOf(flip));
                            }
                            
                            if (audio != null) {
                                camConfigs[i].setAudio(Boolean.valueOf(flip));
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Do nothing
                }
                
                try {
                    framerate = Integer.parseInt(prop.getProperty("CAM_CONFIG_FRAMERATE"));
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            } finally {
                if (is != null) is.close();
            }
        }
    }
    
    public void createFolder(String path) throws SecurityException {
        File dir = new File(path);
                
        if (dir.exists() == false) {
            dir.mkdirs();
        }
    }
    
    public void setDefaultPath(String path) {
        
        defaultPath = path;
        defaultPathTemp = path + "\\" + TEMP_PATH;
        
        try {
            
            createFolder(path);
            createFolder(defaultPathTemp);
            
        } catch (SecurityException ex) {
           Logger.getLogger(EncControl.class.getName())
                .log(Level.SEVERE, null, ex);

           defaultPath = DEFAULT_PATH;
           defaultPathTemp = DEFAULT_PATH;
        }
    }
    
    public void openResources() {
        try {
            int cams = 0;
            webcams = Webcam.getWebcams();
            
            for (int i = 0; i < webcams.size(); ++i) {
                Webcam webcam = webcams.get(i);
                
                dbgMsg("Testing camera: " + webcam.getName());
                
                for (CamConfig camConfig : config.camConfigs) {
                    String value = camConfig.name;
                    if (value == null)
                        continue;
                    
                    dbgMsg("Camera from config: " + value);
                    
                    if (webcam.getName().equals(value)) {
                        webcam.setViewSize(camConfig.dimension);
                        webcam.close();
                        webcam.open(true);
                
                        dbgMsg("Camera accepted: " + webcam.getName());
                        
                        cams++;
                        
                        break;
                    }
                }
            }
            
            filenames = new String[config.num];
            encs = new Encoder[config.num];
            
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
    
    private static String formatFilename(String path, int camID) {
        Date date = new Date();
        SimpleDateFormat dateFormat = 
                new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss") ;
        
        return (path + "\\" + camID + "_" + dateFormat.format(date) + ".mp4");
    } 
    
    public void start() throws IOException {   
        if (this.isRecording)
            throw new IOException("Already running...");
        
        int cams = 0;
        webcams = Webcam.getWebcams();

        for (int i = 0; i < webcams.size(); ++i) {
            Webcam webcam = webcams.get(i);

            for (CamConfig camConfig : config.camConfigs) {
                String value = camConfig.name;

                if (webcam.getName().equals(value)) {
                    
                    filenames[camConfig.ID] = formatFilename(defaultPathTemp, camConfig.ID);
            
                    encs[camConfig.ID] = new Encoder(filenames[camConfig.ID], camConfig.ID);
                    encs[camConfig.ID].addVideo(
                            camConfig.frameRate, 
                            camConfig.dimension,
                            camConfig.flipped,
                            webcam);
                    
                    if (camConfig.hasAudio && line != null)
                        encs[camConfig.ID].addAudio(audioFormat, line);

                    encs[camConfig.ID].rec();

                    break;
                }                    
            }
        }
                
        this.isRecording = true;
    }
    
    public void stop() {
        if (encs != null) {
            for (Encoder enc : encs) {
                try {
                    if (enc != null) {
                        enc.stop();
                        enc.close();
                        enc = null;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(EncControl.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
        
        if (identifier != null) {
            for (String f : filenames) {
                if (f == null)
                    continue;
                
                try {
                    int pathIdx = f.lastIndexOf("\\");
                    int pointIdx = f.lastIndexOf(".");

                    String name = f.subSequence(pathIdx, pointIdx).toString();
                    String ext = f.subSequence(pointIdx, f.length()).toString();

                    String newPath = defaultPath + "\\" + identifier;
                    try {
                        createFolder(newPath);

                        String filename = newPath + "\\" + name + "___" + identifier + ext;

                        File oldFile = new File(f);
                        File newFile = new File(filename);
                        Files.move(oldFile.toPath(), newFile.toPath());
                    } catch (SecurityException ex) {
                        Logger.getLogger(EncControl.class.getName())
                            .log(Level.SEVERE, null, ex);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(EncControl.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }
        this.identifier = null;
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
        
        try {
            enc.start();
        } catch (IOException ex) {
            Logger.getLogger(EncControl.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        enc.setIdentifier("38502729829");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(EncControl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        enc.stop();
        
    }
    
    
}
