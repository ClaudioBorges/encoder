package encoder;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.xuggle.ferry.IBuffer;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import static com.xuggle.xuggler.Global.DEFAULT_TIME_UNIT;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;


public class Encoder {

    private static final int AUDIO_STREAM_ID = 1;
    private static final int VIDEO_STREAM_ID = 0;
    
    private int vFrameRate;
    private Dimension vSize;
    private final IMediaWriter writer;
    
    private ScheduledFuture<Runnable> scheduleWithFixedDelay;
    private ScheduledExecutorService timeWorker;
    private final Runnable runnable;
    private long startNanoSec;
    
    private Webcam webcam;
    private TargetDataLine line;
    
    private final int id;
    
    public static void dbgMsg(String msg) {
        System.out.println("[DEBUG] " + msg);
    }
         
    public Encoder(String filename, int identifier) {
        this.id = identifier;
        this.writer = ToolFactory.makeWriter(filename); 
 
        this.runnable = new Runnable() {
            private long vFrameCount = 0;
            
            @Override
            public void run() {
                try {
                    if (webcam != null) {
                        BufferedImage capture = webcam.getImage();

                        BufferedImage image = 
                                ConverterFactory.convertToType(
                                        capture, 
                                        BufferedImage.TYPE_3BYTE_BGR);
                        IConverter converter = 
                                ConverterFactory.createConverter(
                                        image, 
                                        IPixelFormat.Type.YUV420P);

                        IVideoPicture frame = 
                                converter.toPicture(
                                        image, 
                                        (System.nanoTime() - startNanoSec) 
                                                / 1000);
                        frame.setKeyFrame(this.vFrameCount == 0);
                        frame.setQuality(0);


                        writer.encodeVideo(VIDEO_STREAM_ID, frame);

                        this.vFrameCount++;
                    }
                    if (line != null) {
                        byte[] audioBuf = getTargetDataLineBytes();
                        if (audioBuf != null) {

                            IBuffer iBuf 
                                    = IBuffer.make(null,
                                                   audioBuf,
                                                   0,
                                                   audioBuf.length);

                            IAudioSamples smp  
                                   = IAudioSamples.make(iBuf,
                                                        line.getFormat().getChannels(),
                                                        IAudioSamples.Format.FMT_S16);

                            if (smp != null) {
                                long numSample = audioBuf.length / smp.getSampleSize();

                                // System.err.println("NUM SAMPLE " + numSample + " SMP size " + smp.getSampleSize());
                                smp.setComplete(true,
                                                numSample,
                                                (int)line.getFormat().getSampleRate(),
                                                line.getFormat().getChannels(),
                                                IAudioSamples.Format.FMT_S16,
                                                (System.nanoTime() - startNanoSec) / 1000);

                                // encode audio to audio stream
                                writer.encodeAudio(AUDIO_STREAM_ID, smp);
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(
                            Encoder.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        };
    }
    
    public int getID() {
        return this.id;
    }
    
    public void addVideo(
            int videoFrameRate, 
            Dimension size, 
            Webcam webcam) {
        
        this.webcam = webcam;
        this.vFrameRate = videoFrameRate;
        this.vSize = size;
        
        writer.addVideoStream(
                VIDEO_STREAM_ID, 0, 
                ICodec.ID.CODEC_ID_H264, 
                this.vSize.width, this.vSize.height);
    }
    
    public void addAudio(AudioFormat audioFormat, TargetDataLine line) {
        
        this.line = line;
        
        writer.addAudioStream(
                AUDIO_STREAM_ID, 0, 
                ICodec.ID.CODEC_ID_AAC, 
                audioFormat.getChannels(), (int) audioFormat.getSampleRate());
        
    }
    
    private byte[] getTargetDataLineBytes() {
        if (line != null) {

            // audio buffer
            int    numBytesToRead = line.available();
            byte[] audioBuf       = new byte[numBytesToRead];

            // Encode the audio into audio stream. */
            int nBytesRead = line.read(audioBuf, 0, numBytesToRead);

            if (nBytesRead == 0) {
                return null;
            }

            return audioBuf;
        }

        return null;
    }
    
    public void rec() {
        this.startNanoSec = System.nanoTime();
        
        timeWorker = new ScheduledThreadPoolExecutor(1);  
        scheduleWithFixedDelay 
                = (ScheduledFuture<Runnable>) 
                    timeWorker.scheduleWithFixedDelay(this.runnable,
                    0,
                    1000/this.vFrameRate,
                    TimeUnit.MILLISECONDS);

    }
    
    public void stop() throws InterruptedException {
        if (scheduleWithFixedDelay != null) {
            scheduleWithFixedDelay.cancel(false);
        }
        
        if (timeWorker != null) {
            timeWorker.shutdown();
            timeWorker.awaitTermination(2000, MILLISECONDS);
            timeWorker.shutdownNow();
        }
    }
    
    public void close() {
        if (this.writer.isOpen())
            this.writer.close();
    }
    
    public static void main(String[] args) throws Throwable {

        // video parameters
        final int cameraAudioID = 1;
        final int frameRate = 15;
        final long frameTime 
                = DEFAULT_TIME_UNIT.convert(
                        1000/frameRate, MILLISECONDS);
        final Dimension dimension = WebcamResolution.QVGA.getSize();
     
        
        List<Webcam> webcams = Webcam.getWebcams();
        String[] names = new String[webcams.size()];
        for (int i = 0; i < names.length; ++i) {
            Webcam webcam = webcams.get(i);
            names[i] = webcam.getName();
            
            webcam.setViewSize(dimension);
            webcam.open(true);
            
            dbgMsg("Camera name: " + names[i]);
        }
                
        // Pick a format. Need 16 bits, the rest can be set to anything
        // It is better to enumerate the formats that the system supports,
        //because getLine() can error out with any particular format
        AudioFormat audioFormat 
                = new AudioFormat(44100.0F, 16, 1, true, false);

         // Get default TargetDataLine with that format
        DataLine.Info dataLineInfo
                 = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine line 
                = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
        // Open and start capturing audio
        line.open(audioFormat, line.getBufferSize());
        line.start();
        
        long start = System.currentTimeMillis();
        Encoder encoder[] = new Encoder[] {
            new Encoder("C:\\Teste\\2015-10-24_15_44_0.mp4", 1),
            new Encoder("C:\\Teste\\2015-10-24_15_44_1.mp4", 2),
            new Encoder("C:\\Teste\\2015-10-24_15_44_2.mp4", 3),
            new Encoder("C:\\Teste\\2015-10-24_15_44_3.mp4", 4),
            new Encoder("C:\\Teste\\2015-10-24_15_44_4.mp4", 5)
        };
        
        for (Encoder enc : encoder) {
            enc.addVideo(frameRate, dimension, webcams.get(0));
            if (enc.getID() == cameraAudioID) {
                enc.addAudio(audioFormat, line);
            }
            enc.rec();
        }
        

        final int totalFrames = 
                (int) (DEFAULT_TIME_UNIT.convert(
                        1, TimeUnit.MINUTES) / frameTime);
        long threadStart = System.currentTimeMillis();
        for (int i = 0; i < totalFrames; i++) {
            System.out.println("Capture frame " + i);
            
            long next = threadStart + (frameTime * (i + 1) / 1000);            
            long delta = next - System.currentTimeMillis();
            if (delta > 0)
                Thread.sleep(delta);
        }

        for (Encoder enc : encoder) {
            enc.stop();
            enc.close();
        }
                
        System.out.println("Video recorded in file. ");
    }
}
