/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoder;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Claudio
 */
public class EncConfig {    
    private JLabel webcamLabel;
    private JComboBox webcamComboBox;
    private WebcamPanel webcamPanel;
    private Webcam webcam;
    
    private JFrame frame;
    
    public void dispose() {
        if (webcamPanel != null) webcamPanel.stop();
        if (webcam != null) webcam.close();
        if (frame != null) frame.dispose();
    
        webcamPanel = null;
        webcam = null;
        frame = null;
    }
    
    public String[] getWebcams() {
        List<Webcam> webcams = Webcam.getWebcams();
        String[] names = new String[webcams.size()];
        
        int i = 0;
        for (Webcam webcam : webcams) {
            names[i] = webcam.getName();
            ++i;
        }
        
        return names;
    }
    
    public void fillCombobox(JComboBox cb, String[] names) {
        cb.removeAll();
        
        for (String name : names) {
            cb.addItem(name);
        }
    }
    
    public void setWebcam(String name) throws IOException {
        List<Webcam> webcams = Webcam.getWebcams();
        for (Webcam webcam : webcams) {
            if (webcam.getName().equals(name)) {
                if (webcam == this.webcam)
                    throw new IOException("Webcam + \"" + webcam.getName() + "\" already opened.");
                
                if (this.webcam != null) this.webcam.close();
                
                this.webcam = webcam;
                return;
            }
        }
        
        throw new IOException("Unknown webcam: " + name);
    }
    
    public void changeWebcam(String name) throws IOException {
        setWebcam(name);
        
        Container container = frame.getContentPane();
        
        container.removeAll();
        container.add(createPanel()); 
        frame.pack();
        
        frame.revalidate();
        frame.repaint();
        
        frame.setVisible(true);
    }
    
    public JPanel createPanel() throws IOException {        
        JPanel panel = new JPanel(new GridBagLayout());        
        GridBagConstraints c = new GridBagConstraints();
        
        webcamLabel =  new JLabel("Camera: ");
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(2, 2, 2, 2);
        panel.add(webcamLabel, c);              
        
        
        webcamComboBox =  new JComboBox();
        fillCombobox(webcamComboBox, getWebcams());
        webcamComboBox.addActionListener((java.awt.event.ActionEvent evt) -> {
            webcamComboBoxActionPerformed(evt);
        });        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.1;
        c.weighty = 0.1;
        c.insets = new Insets(5, 5, 5, 5);
        panel.add(webcamComboBox, c);   
        
        if (webcam == null)
            webcam = Webcam.getDefault();
        if (webcamPanel != null) webcamPanel.stop();
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(false);
        webcamPanel.setDisplayDebugInfo(false);
        webcamPanel.setImageSizeDisplayed(false);
        webcamPanel.setFPSLimited(true);
        webcamPanel.setFPSLimit(15);
        webcamPanel.setMirrored(false);        
        webcamPanel.setMinimumSize(new Dimension(320, 240));
        webcamPanel.setPreferredSize(new Dimension(640, 480));
        webcamPanel.start();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(10, 10, 10, 10);
        panel.add(webcamPanel, c);   
        
        return panel;                
    }
    
    public JFrame createFrame(JPanel panel) throws IOException {
        if (frame != null)
            throw new IOException("Frame already opened.");
        
        frame = new JFrame();
        frame.getContentPane().add(panel);
        frame.pack();
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        
        return frame;
    }
    
    private void webcamComboBoxActionPerformed(ActionEvent evt) {
         Object obj = webcamComboBox.getSelectedItem();
         if (obj instanceof String) {
             String name = (String)obj;
             try {             
                 this.changeWebcam(name);
             } catch (IOException ex) {
                 Logger.getLogger(EncConfig.class.getName()).log(Level.SEVERE, null, ex);
             }
         }
         else {
             throw new UnsupportedOperationException("Not implemented for object != String.");
         }
    }
    
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) {
        try {
            EncConfig encConfig = new EncConfig();
            
            JFrame frame = encConfig.createFrame(encConfig.createPanel());        
            frame.setVisible(true);
        } catch (IOException ex) {
            Logger.getLogger(EncConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
