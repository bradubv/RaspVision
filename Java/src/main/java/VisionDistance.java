import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import java.text.DecimalFormat;
import java.text.Format;


public class VisionDistance {

private final static Format DF22 = new DecimalFormat("#0.00");
  private final static Format DF_N = new DecimalFormat("#.##########################");

  private final static double SOUND_SPEED = 34300;  // 34300;         // in cm, 340.29 m/s
  private final static double DIST_FACT   = SOUND_SPEED / 2; // round trip
 
  
  private static boolean verbose = false;
  private final static long BILLION      = (long)10E9;
  private final static int TEN_MICRO_SEC = 10 * 1000; // In Nano secs

  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    // Connect NetworkTables, and get access to the publishing table
    NetworkTable.setClientMode();
    // Set your team number here
    NetworkTable.setTeam(1635);
    NetworkTable.initialize();

    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;

    // This stores our reference to our mjpeg server for streaming the input 
    // image
    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);

    // On windows, http must be used since USB is not supported
    // On the pi we're using USB cameras. 

    // USB Camera
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = setUsbCamera(0, inputStream);

    // Set the resolution for our camera, since this is over USB
    //camera.setResolution(640,480); //getting ~8 FPS on the PI at this res
    camera.setResolution(320,240); //still getting 8 FPS
    camera.setFPS(24);

    // This creates a CvSink for us to use. This grabs images from our 
    // selected camera and will allow us to use those images in opencv
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    // This creates a CvSource to use. This will take in a Mat image that 
    // has had OpenCV operations 
    CvSource imageSource = new CvSource("CV Image Source", 
    //    VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
        VideoMode.PixelFormat.kMJPEG, 320, 240, 24);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);

    // All Mats and Lists should be stored outside the loop to avoid allocations
    // as they are expensive to create
    Mat inputImage = new Mat();
    Mat hsv = new Mat();

    // Infinitely process image
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // Just skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      // Below is where you would do your OpenCV operations 
      // on the provided image
      // The sample below just changes color source to HSV
      // Bogdan: TODO: this is where we put our processing code
      //Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);

      // Here is where you would write a processed image that you want 
      // to restreams
      // This will most likely be a marked up image of what the camera sees
      // For now, we are just going to stream the HSV image
      //imageSource.putFrame(hsv);
      imageSource.putFrame(inputImage);

      try {
        double distance = getDistance(); 
        System.out.println("Distance = " + distance);
      } catch (InterruptedException ex) {
        System.out.println("Error: InterruptedException from getDistance()");
      }
    }
    //TODO: figure out how to release the GPIO pins
    // gpio.shutdown();
    
  }

  private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
    server.setSource(camera);
    return camera;
  }

  public static double getDistance() throws InterruptedException {
  
    System.out.println("GPIO Control Starting");
    verbose = "true".equals(System.getProperty("verbose", "false"));
    
    // create gpio controller
    final GpioController gpio = GpioFactory.getInstance();
    final GpioPinDigitalOutput trigPin = 
      gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Trig", PinState.LOW);
    final GpioPinDigitalInput  echoPin = 
      gpio.provisionDigitalInputPin(RaspiPin.GPIO_05,  "Echo");
    
    Thread.sleep(10);
   
    trigPin.low();
    
    // Just to check...
    if (echoPin.isHigh())
      System.out.println(">>> !! Before sending signal, echo PIN is " 
        + (echoPin.isHigh() ? "High" : "Low"));
          
    trigPin.high();
    // 10 microsec to trigger the module  (8 ultrasound bursts at 40 kHz) 
    // https://www.dropbox.com/s/615w1321sg9epjj/hc-sr04-ultrasound-timing-diagram.png
    try { Thread.sleep(0, TEN_MICRO_SEC); } 
    catch (Exception ex) { ex.printStackTrace(); } 
    trigPin.low();

    // Wait for the signal to return
    while (echoPin.isLow()); 
    long start = System.nanoTime();
    // There it is, the echo comes back.
    while (echoPin.isHigh());
    long end   = System.nanoTime();
    double distance = 0;

    if (end > start) {
      double pulseDuration = (double)(end - start)*10 / (double)BILLION; // in seconds
      distance = pulseDuration * DIST_FACT * .393701;
      if (distance < 1000) // Less than 10 meters
        System.out.println("Distance: " + DF22.format(distance) + 
          " inch. (" + distance + "), Duration:" + (end - start) + 
          " nanoS"); 
      else
        System.out.println("   >>> Too far:" + DF22.format(distance) + " inch.");
    }
    
    trigPin.low(); // Off

    return distance;
  }
}
