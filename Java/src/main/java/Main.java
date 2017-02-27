import java.util.ArrayList;
import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Main {

  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    // Connect NetworkTables, and get access to the publishing table
    NetworkTable.setClientMode();
    // Set your team number here
    NetworkTable.setTeam(1635);
    NetworkTable.initialize();
    NetworkTable nt = NetworkTable.getTable("SmartDashboard");

    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;
    int climbCameraPort = 1187;

    // This stores our reference to our mjpeg server for streaming the input 
    // image
    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);
    MjpegServer climbStream = new MjpegServer("Climb Server", climbCameraPort);

    // On windows, http must be used since USB is not supported
    // On the pi we're using USB cameras. 

    // USB Camera
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = setUsbCamera(0, "frontCamera", inputStream);
    UsbCamera climbCamera = setUsbCamera(1, "climbCamera", climbStream);

    // Set the resolution for our camera, since this is over USB
    //camera.setResolution(640,480); //getting ~8 FPS on the PI at this res
    camera.setResolution(320,240); 
    camera.setFPS(24);
    climbCamera.setResolution(320,240); 
    climbCamera.setFPS(24);

    // This creates a CvSink for us to use. This grabs images from our 
    // selected camera and will allow us to use those images in opencv
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);
    CvSink imageSinkClimb = new CvSink("ClimbCamera Image Grabber");
    imageSinkClimb.setSource(climbCamera);

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
    
    Sonar sonar = new Sonar();
    Thread distThread = new Thread(sonar);
    distThread.start();
    
    // Infinitely process image
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // Just skip and continue
      //long frameTime = imageSink.grabFrame(inputImage);
      long frameTime = imageSinkClimb.grabFrame(inputImage);
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

      double distance = sonar.getDistance();  
      nt.putNumber("Sonar Distance", distance);
    }
    //TODO: figure out how to release the GPIO pins
    // gpio.shutdown();
    
  }

  private static UsbCamera setUsbCamera(int cameraId, 
    String cameraName, MjpegServer server) {
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = new UsbCamera(cameraName, cameraId);
    server.setSource(camera);
    return camera;
  }

}
