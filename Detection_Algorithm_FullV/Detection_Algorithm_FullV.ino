#include "Wire.h"
// I2Cdev and MPU6050 must be installed as libraries, or else the .cpp/.h files
// for both classes must be in the include path of your project
#include "I2Cdev.h"
#include "MPU6050.h"
#include <SPI.h>
#include <SD.h>
#include <SoftwareSerial.h>

#define MPU6050_AFS_SEL3
#define PIN_GATE_IN 2
#define IRQ_GATE_IN  0
#define PIN_LED_OUT 13
#define PIN_ANALOG_IN0 A0
#define PIN_ANALOG_IN1 A1
#define LED_PIN 13
#define PIN_SHUTDOWN A4

//const int MPU = 0x69; // I2C address of the sensor
// class default I2C address is 0x68
// specific I2C addresses may be passed as a parameter here
// AD0 low = 0x68 (default for InvenSense evaluation board)
// AD0 high = 0x69

MPU6050 accelgyro;
int16_t ax, ay, az, gx, gy, gz;
int16_t Yaccel, Xgyro, Ygyro,Yaccel_t, Xgyro_t, Ygyro_t,audio;
int16_t data[3];
//bool blinkState = false;
File myFile;
uint32_t period = 10 * 60000L;       // 30 minutes
bool IsDetected = false;
SoftwareSerial indication(5, 6); // RX, TX
uint32_t countTime=0;
void soundISR();

void setup()
{ 
  Serial.begin(1000000);
   indication.begin(9600);
  if (!SD.begin(4)) {
    Serial.println("SD initialization failed!");
    return;
  }
  myFile = SD.open("newfile.txt", FILE_WRITE);
  //////////////////////////// Sound Sensor setup //////////////////////////////////

  //  Configure LED pin as output
  pinMode(PIN_LED_OUT, OUTPUT);

  // configure input to interrupt
  pinMode(PIN_GATE_IN, INPUT);
  attachInterrupt(IRQ_GATE_IN, soundISR, CHANGE);

  // Display status
  Serial.println("Sound Sensor Initialized");

  ////////////////////////////// MPU6050 setup ////////////////////////////////////

  // join I2C bus (I2Cdev library doesn't do this automatically)
  Wire.begin();
  // initialize serial communication
  // (38400 chosen because it works as well at 8MHz as it does at 16MHz, but
  // it's really up to you depending on your project)

  // initialize device
  Serial.println("Initializing I2C devices...");
  accelgyro.initialize();
  // accelgyro.setClockSource(MPU6050_CLOCK_PLL_XGYRO);
  accelgyro.setFullScaleGyroRange(MPU6050_GYRO_FS_250);
  accelgyro.setFullScaleAccelRange(MPU6050_ACCEL_FS_16);
  // verify connection
  Serial.println("Testing device connections...");
  Serial.println(accelgyro.testConnection() ? "MPU6050 connection successful" : "MPU6050 connection failed");
  accelgyro.setStandbyXGyroEnabled(true);
  accelgyro.setStandbyYGyroEnabled(true);
  accelgyro.setStandbyZGyroEnabled(true);
  
  //////////////////////////// SD Card Write setup //////////////////////////////////

  //while (!Serial) {
  ; // wait for serial port to connect. Needed for native USB port only
  //}

  // open the file. note that only one file can be open at a time,
    myFile = SD.open("newfile.txt", FILE_WRITE);
  if (myFile)
    myFile.println("\n--- New Sensors Values 6050 ---");
  else
    Serial.println("error opening file");
}


void loop() {
  for( uint32_t tStart = millis();  (millis()-tStart) < period;  ){
  uint32_t accelTime = millis();
  boolean SleepMode = true;
   IsDetected=false;
  
 // do {
    do {
      do {
      accelgyro.getAcceleration(&ax, &ay, &az);
      Xgyro = ax * 9.8 / 2048.0;
      Yaccel = ay * 9.8 / 2048.0;
      if (accelTime >= 900000)
      accelTime=0;
    } while (Yaccel < 1.0 && Xgyro < 1.0);

    accelgyro.setStandbyXGyroEnabled(false);
    accelgyro.setStandbyYGyroEnabled(false);
    accelgyro.setStandbyZGyroEnabled(false);

    
    accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
    audio = analogRead(PIN_ANALOG_IN0);
 
    myFile.print(ax * 9.8 / 2048.0, DEC);   myFile.print(",");
    myFile.print(ay * 9.8 / 2048.0, DEC);   myFile.print(",");
    myFile.print(az * 9.8 / 2048.0, DEC);   myFile.print(",");
    myFile.print(gx / 131.0, DEC);        myFile.print(",");
    myFile.print(gy / 131.0, DEC);        myFile.print(",");
    myFile.print(gz / 131.0, DEC);       myFile.print(",");
    myFile.print(audio);              myFile.print(",");
   
    Yaccel = abs(ay* 9.8 / 2048.0);
    Xgyro =abs( gx / 131.0);
    Ygyro = abs(gy / 131.0);
    
      
 uint32_t t = millis();  
 //Detect Accident
    if (Yaccel >= 15 &&  ( Xgyro >= 15  || Ygyro >= 15 )&&   audio >= 2)
        {
           data[0]=Yaccel;
           data[1]=Xgyro;
            data[2]=Ygyro;
          do{
          accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
          Yaccel_t=abs(ay* 9.8 / 2048.0);
          Xgyro_t=abs( gx / 131.0);
          Ygyro_t=abs(gy / 131.0);
         

     if ( Yaccel_t >=data[1]*0.005|| Xgyro_t >=data[2]*0.005|| Ygyro >=data[3]*0.005){
      countTime = millis();  
      //myFile.print("Accident Detected! -> 1.Turn on BT 2.Send indication to the app 3. Turn on GPS & get location 4.Send SMS with location link");
      indication.write("1",1);
      indication.println();
      IsDetected=true;
      break;
     }
         }while(t<=250 && !IsDetected);
        }
      }
     while (!IsDetected);

   /* //after accident
    uint32_t tAfterAccident = millis();
  //while(tAfterAccident < 0.25 * 60000L){}
  audio = analogRead(PIN_ANALOG_IN0);
   if (audio <= 30 )
   {
    myFile.close();
    break;
    }
    else{
       myFile.print("1.Send a quiet indication to the app 2.Send SMS to cancel 3.Turn off BT");
      }*/
 // } while ( PIN_SHUTDOWN == HIGH  );
 myFile.print("Time: ");
myFile.println(millis()-countTime);
myFile.close();
//exit(0);

}
//myFile.close();
}

void soundISR()
{
  int pin_val;
  pin_val = digitalRead(PIN_GATE_IN);
  digitalWrite(PIN_LED_OUT, pin_val);
}



// accelgyro.setSleepEnabled(true);
   // accelgyro.setStandbyXAccelEnabled(false);
   // accelgyro.setStandbyYAccelEnabled(false);
   // accelgyro.setStandbyZAccelEnabled(false);
