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

/**
 * Write a description of class Sonar here.
 * 
 * @author Bogdan V. Bradu 
 * @version 0.1
 */
public class Sonar extends Thread {
  private final static double SOUND_SPEED = 34300;  // 34300; 
    // in cm, 340.29 m/s
  private final static double DIST_FACT   = SOUND_SPEED / 2; // round trip
  private final static long BILLION      = (long)10E9;
  private final static int TEN_MICRO_SEC = 10 * 1000; // In Nano secs
    
  private boolean verbose = false;
  private GpioPinDigitalOutput trigPin;
  private GpioPinDigitalInput echoPin;
  private double distance;

  /**
   * Constructor for objects of class Sonar
   */
  public Sonar() {
    System.out.println("GPIO Control Starting");

    // create gpio controller
    final GpioController gpio = GpioFactory.getInstance();
    trigPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Trig", PinState.LOW);
    echoPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05, "Echo");

    //TODO Need to try out this property for debugging at runtime
    verbose = "true".equals(System.getProperty("verbose", "false"));
    
    distance = 0.0f;
  }

  public double getMockDistance() { 
    try { Thread.sleep(100); } 
    catch (Exception ex) { ex.printStackTrace(); } 

    return 8.0f; 
  }
  
  public double getDistance() {
    return distance;
  }

  public void run() {
    while (true) {
      //TODO: is this needed?
      try { Thread.sleep(10); } 
      catch (Exception ex) { ex.printStackTrace(); } 

      trigPin.low();
    
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
      while (echoPin.isLow()) {
        this.yield(); //TODO: do we need to win
      } 

      long start = System.nanoTime();
      // There it is, the echo comes back.
      while (echoPin.isHigh());
      long end = System.nanoTime();
      double distanceCm = 0;

      if (end > start) {
        double pulseDuration = (double)(end - start)*10 / (double)BILLION; // in seconds
        distanceCm = pulseDuration * DIST_FACT;
      }
    
      trigPin.low(); // Off

      this.distance = distanceCm * .393701;
    }
  }
}