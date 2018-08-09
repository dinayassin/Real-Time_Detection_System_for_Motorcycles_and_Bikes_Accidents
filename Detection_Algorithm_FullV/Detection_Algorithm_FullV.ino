#include "Wire.h"
// I2Cdev and MPU6050 must be installed as libraries, or else the .cpp/.h files
// for both classes must be in the include path of your project
#include "I2Cdev.h"
#include "MPU6050.h"
#include <SD.h>
#include <SoftwareSerial.h>
#include <EEPROM.h>

#define MPU6050_AFS_SEL3
#define PIN_GATE_IN 2
#define IRQ_GATE_IN 0
#define PIN_LED_OUT 13
#define PIN_ANALOG_IN0 A0
#define DATASIZE 10
#define ACCEL_FIXEDSLOPE 30100
#define GYRO_FIXEDSLOPE 30100
#define ACCEL_THRESHOLD 150
#define GYRO_THRESHOLD 220

//const int MPU = 0x69; // I2C address of the sensor
// class default I2C address is 0x68
// specific I2C addresses may be passed as a parameter here
// AD0 low = 0x68 (default for InvenSense evaluation board)
// AD0 high = 0x69

MPU6050 accelgyro;
int16_t ax, ay, az, gx, gy, gz;
uint16_t audio, AUDIOdata[DATASIZE], ACCEL_fixedVar = 0,SOUND_fixedVar = 0, dataIndex = 0;
double Xaccel, Yaccel, Xgyro = 0, Ygyro = 0, Ax_lstSmpl, Ay_lstSmpl, Gx_lstSmpl, Gy_lstSmpl;
double Xdata[DATASIZE], Ydata[DATASIZE], average = 0, Xvariance, Yvariance, AUDIOvariance, sum = 0;
bool IsDetected = false;
char dataRecieved = -1;
//bool blinkState = false;

//File myFile;
//uint32_t period = 0.5 * 60000L;       // 30 minutes

SoftwareSerial indication(5, 6); // RX, TX
void soundISR();

void setup()
{
  Serial.begin(1000000);
  indication.begin(9600);
  if (!SD.begin(4)) {
    Serial.println("SD initialization failed!");
  }
  //myFile = SD.open("newfile.txt", FILE_WRITE);
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
  accelgyro.setFullScaleGyroRange(MPU6050_GYRO_FS_2000);
  accelgyro.setFullScaleAccelRange(MPU6050_ACCEL_FS_16);
  // verify connection
  Serial.println("Testing device connections...");
  Serial.println(accelgyro.testConnection() ? "MPU6050 connection successful" : "MPU6050 connection failed");
  accelgyro.setStandbyXGyroEnabled(true);
  accelgyro.setStandbyYGyroEnabled(true);
  accelgyro.setStandbyZGyroEnabled(true);

  //////////////////////////// SD Card Write setup //////////////////////////////////
/*
  //while (!Serial) {
  ; // wait for serial port to connect. Needed for native USB port only
    //}

    // open the file. note that only one file can be open at a time,
 // myFile = SD.open("newfile.txt", FILE_WRITE);
  if (myFile)
    myFile.println("\n--- New Sensors Values 6050 ---");
  else
    Serial.println("error opening file"); */

      /* if EEPROM empty, then wait until recieve indication from app*/
    dataRecieved == -1;
    if(EEPROM.read(0) == 255){
    while (dataRecieved == -1){
     dataRecieved = indication.read();
      Serial.print("dataRecieved:");
     Serial.println(dataRecieved);
      } 

 /* Compute ACCEL_fixedVar & SOUND_fixedVar & store them in EEPROM  */   
 sum =0 ;
 average = 0;
 dataIndex =0;
   for (unsigned int i = 0; i < 3; i++){
            for (unsigned int i = 0; i < DATASIZE; i++){
           accelgyro.getAcceleration(&ax, &ay, &az);
          Xdata[dataIndex] = fabs(ax* 9.8 / 2048.0);
          Ydata[dataIndex] = fabs(ay* 9.8 / 2048.0);
          AUDIOdata[dataIndex] = analogRead(PIN_ANALOG_IN0);
            }
      
        // Compute the sum of all elements of Xdata & Compute Xvarianc
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + Xdata[i];
        average = sum / (double)DATASIZE;
        sum = 0;
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + pow((Xdata[i] - average), 2);
        Xvariance = sum / (double)DATASIZE;
        if(Xvariance > ACCEL_fixedVar)
        ACCEL_fixedVar = Xvariance;

          sum = 0;
          average = 0;

        // Compute the sum of all elements of Ydata & Compute  Yvariance
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + Ydata[i];
        average = sum / (double)DATASIZE;
        sum = 0;
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + pow((Ydata[i] - average), 2);
        Yvariance = sum / (double)DATASIZE;
         if(Yvariance > ACCEL_fixedVar)
        ACCEL_fixedVar = Yvariance;
        
         sum = 0;
        average = 0;

        // Compute the sum of all elements of AUDIOdata & Compute  AUDIOvariance
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + AUDIOdata[i];
        average = sum / (double)DATASIZE;
        sum = 0;
        for (unsigned int i = 0; i < DATASIZE; i++)
          AUDIOvariance = sum / (double)DATASIZE;
           if(AUDIOvariance > SOUND_fixedVar)
            SOUND_fixedVar = AUDIOvariance;
          }

           Serial.println("real values");
           Serial.println(ACCEL_fixedVar);
           Serial.println(SOUND_fixedVar);
           
          EEPROM.put(0, ACCEL_fixedVar);
          EEPROM.put(1, SOUND_fixedVar);
          Serial.println("var computed!!!!!!");
          Serial.println(EEPROM.read(0));
          Serial.println(EEPROM.read(1));
    }
}


void loop()
{
 // for( uint32_t tStart = millis();  (millis()-tStart) < period;  ){
  do {
     IsDetected = false;      
    do {
        uint32_t accelTime = millis();
      do {
        accelgyro.getAcceleration(&ax, &ay, &az);
        Xaccel = fabs(ax* 9.8 / 2048.0);
        Yaccel = fabs(ay* 9.8 / 2048.0);
        if ((millis()-accelTime) >= 600000){
          accelgyro.setStandbyXGyroEnabled(true);
          accelgyro.setStandbyYGyroEnabled(true);
          accelgyro.setStandbyZGyroEnabled(true);
          accelTime = 0;
        }
      } while (Xaccel == 0 && Yaccel == 0);

      if(accelgyro.getStandbyXGyroEnabled()){
      accelgyro.setStandbyXGyroEnabled(false);
      accelgyro.setStandbyYGyroEnabled(false);
      accelgyro.setStandbyZGyroEnabled(false);
      }
      
      accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
      audio = analogRead(PIN_ANALOG_IN0);

      Ax_lstSmpl = Xaccel;
      Ay_lstSmpl = Yaccel;
      Gx_lstSmpl = Xgyro;
      Gy_lstSmpl = Ygyro;

      Xaccel = fabs(ax* 9.8 / 2048.0);
      Yaccel = fabs(ay* 9.8 / 2048.0);
      Xgyro = fabs(gx / 131.0);
      Ygyro = fabs(gy / 131.0);

      /****************** Detect Accident ******************/
      /* 1. FIRST CHECK: Passing threshold */
      if ((Xaccel > ACCEL_THRESHOLD || Yaccel > ACCEL_THRESHOLD) && (Xgyro > GYRO_THRESHOLD || Ygyro > GYRO_THRESHOLD))
      {
      /* 2. SECOND CHECK: Passing Fixed Slopes */
      if((((Xaccel-Ax_lstSmpl)/0.005) > ACCEL_FIXEDSLOPE || ((Yaccel-Ay_lstSmpl)/0.005) > ACCEL_FIXEDSLOPE) && (((Xgyro-Gx_lstSmpl)/0.005) > GYRO_FIXEDSLOPE || ((Ygyro-Gy_lstSmpl)/0.005) > GYRO_FIXEDSLOPE))
      {    
        sum = 0;
        average = 0;
        dataIndex = 0;
    
          for (unsigned int i = 0; i < 3; i++){
            for (unsigned int i = 0; i < DATASIZE; i++){
          accelgyro.getAcceleration(&ax, &ay, &az);
          Xdata[dataIndex] = fabs(ax* 9.8 / 2048.0);
          Ydata[dataIndex] = fabs(ay* 9.8 / 2048.0);
          AUDIOdata[dataIndex] = analogRead(PIN_ANALOG_IN0);
            }
      
        // Compute the sum of all elements of Xdata & Compute Xvarianc
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + Xdata[i];
        average = sum / (double)DATASIZE;
        sum = 0;
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + pow((Xdata[i] - average), 2);
        Xvariance = sum / (double)DATASIZE;

        /* 3. THIRD CHECK: Xvariance Less than ACCEL_fixedVar */
         if (Xvariance < ACCEL_fixedVar){
          sum = 0;
          average = 0;

        // Compute the sum of all elements of Ydata & Compute  Yvariance
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + Ydata[i];
        average = sum / (double)DATASIZE;
        sum = 0;
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + pow((Ydata[i] - average), 2);
        Yvariance = sum / (double)DATASIZE;
        
        /* 4. FOURTH CHECK: Yvariance Less than ACCEL_fixedVar */
         if (Yvariance < ACCEL_fixedVar){
         sum = 0;
        average = 0;

        // Compute the sum of all elements of AUDIOdata & Compute  AUDIOvariance
        for (unsigned int i = 0; i < DATASIZE; i++)
          sum = sum + AUDIOdata[i];
        average = sum / (double)DATASIZE;
        sum = 0;
        for (unsigned int i = 0; i < DATASIZE; i++)
          AUDIOvariance = sum / (double)DATASIZE;

       /* 5. LAST CHECK: AUDIOvariance Less than SOUND_fixedVar ==> ACCIDENT ! ! ! */
        if (AUDIOvariance < SOUND_fixedVar )
        {
          indication.write("1", 1);
          indication.println();
          IsDetected = true;
          delay(30000);
        }
          }
          }
          if(IsDetected)
          break;
          }
      }
      }
    } while (!IsDetected);
    //}
  } while (true);
}
  //myFile.print("Time: ");
  //myFile.println(millis()-countTime);
 // myFile.close();
 // exit(0);

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


/*
//After passing threshold
if ((Xaccel_t > Xaccel*0.2 || Yaccel_t > Yaccel*0.2) && Xgyro_t > Xgyro*0.2 || Ygyro > Ygyro*0.2){
//countTime = millis();
//myFile.print("Accident Detected! -> 1.Turn on BT 2.Send indication to the app 3. Turn on GPS & get location 4.Send SMS with location link");
indication.write("1",1);
indication.println();
IsDetected=true;
for( uint32_t delay = millis();  (millis()-delay) < 0.5*60000L;  ){}
break;
}*/


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
