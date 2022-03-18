"""
Raspberry Pi and Android paho MQTT App
ECE 558 | Winter 2022 | Professor Kravitz | Final Project

Author: David Craft and Joshua Blazek
Date  : 16 Mar 2022

Overview:
Helper functions for various sensors attached to the RPi.
Sets pins in 'BCM' mode to use the 'board' import, if there
are issues running on a different RPi check the pin name using
'pinout' in the terminal. 
"""
import RPi.GPIO as GPIO
import smbus   
import time
import adafruit_ahtx0
import board
import schedule
import datetime

GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

# variables for relay switches
Relay1_GPIO = 21                  # fan
Relay2_GPIO = 20                  # water pump
Relay3_GPIO = 16
Relay4_GPIO = 12                  # lights and heat mat
GPIO.setup(Relay1_GPIO, GPIO.OUT) # GPIO Assign mode
GPIO.setup(Relay2_GPIO, GPIO.OUT)
GPIO.setup(Relay3_GPIO, GPIO.OUT)
GPIO.setup(Relay4_GPIO, GPIO.OUT)

# variable for LED
led = 5                         # GPIO5 ---> pin 29
GPIO.setup(led,GPIO.OUT)
# Create sensor object, communicating over the board's default I2C bus
i2c = board.I2C()               # uses board.SCL and board.SDA
aht20 = adafruit_ahtx0.AHTx0(i2c)
 
address = 0x48                  # address  ---> device address
cmd = 0x40                      # DA converter command
A0 = 0x40                       # A0  ---> soil humidity
A1 = 0x41                       # A1  ---> light sensor
A2 = 0x42                       # A2  ---> water level
A3 = 0x43
bus = smbus.SMBus(1)            # start the bus
# Other variables
sensDelay = 10                  # delay for sensor read
bakDelay = 0                    # variable to remember delay after water pump
fanFlag = 0
pumpFlag = 0
lightFlag = 0

"""
Small function used to read from the PCF8591 A/D converter.
"""
def analogRead(count):          # function to read analog data
    read_val = bus.read_byte_data(address,cmd+count)
    return read_val

"""
Code tests analog input from a soil moisture sensor and turns on
GPIO 18 which can be connected to a relay to pump.
"""
def soil_pump(soilValue,waterLevel):
    global sensDelay
    global bakDelay
    global pumpFlag
    state2 = GPIO.input(Relay2_GPIO)

    if(waterLevel>2):
        sensDelay = bakDelay
        GPIO.output(Relay2_GPIO,GPIO.LOW)
        pumpFlag = 0
        print("Container is full - pump stopped")
        
    elif(pumpFlag == 1 ):
        if(state2 == 0):
            bakDelay = sensDelay    # if we are changing states, backup the delay
        sensDelay = 0
        GPIO.output(Relay2_GPIO,GPIO.HIGH)
        print("Manually pumping ON")
    elif(soilValue<50):                  # When the soil moisture value is less than 50, 
                                   # turn on the relay to start the water pump
        if(state2 == 0):
            bakDelay = sensDelay    # if we are changing states, backup the delay
        sensDelay = 0
        GPIO.output(Relay2_GPIO,GPIO.HIGH)
    elif(pumpFlag == 0 ):
        sensDelay = bakDelay
        GPIO.output(Relay2_GPIO,GPIO.LOW)
        print("Manually pumping OFF")
    else:
        sensDelay = bakDelay
        GPIO.output(Relay2_GPIO,GPIO.LOW)
        pumpFlag = 0
    print("soil moisture:%1.0f" %(soilValue))   # print soil data


"""
Code tests analog input from a photoresistor and turns an LED
on and off.
"""
def photoresistor(lightValue):
    if(lightValue<80):               # When the ambient brightness is less than 
                                # 80, the LED light will be on
        GPIO.output(led,GPIO.HIGH)
        print("LED on")
    else:
        GPIO.output(led,GPIO.LOW)
        print("LED off")


"""
Code read input from the adh20 temperature and humidity
sensor and prints to the terminal.
"""
def aht20Sensor():
    global fanFlag
    # calculate fahrenheit
    fahrenheit = (aht20.temperature * 1.8) + 32
    c = ("Temp: %0.0fF (%0.0fC)" %(fahrenheit, aht20.temperature))
    h = ("Humidity: %0.2f" % aht20.relative_humidity)

    if(fanFlag == 1):
        GPIO.output(Relay1_GPIO, GPIO.HIGH)
    else:
        if(aht20.relative_humidity>70):
            GPIO.output(Relay1_GPIO, GPIO.HIGH) # on
            # print("turn on relay 1")
        else:
            GPIO.output(Relay1_GPIO, GPIO.LOW) # off
            fanFlag == 0
            # print("turn off relay 1")

    return c, h


"""
Function to turn on lights and heat mat
"""
def lights():
    global lightFlag
    # check the state of the gpio for the light relay switch
    state = GPIO.input(Relay4_GPIO)
    # toggle the relay
    if(lightFlag == 1):
        GPIO.output(Relay4_GPIO, GPIO.HIGH) # on
        # print("turn on relay 4")
    else:
        GPIO.output(Relay4_GPIO, GPIO.LOW) # out
        lightFlag == 0
        # print("turn off relay 4")

def checkAnalog():
    global pumpFlag
    # check the state of the gpio for the light relay switch
    state4 = GPIO.input(Relay4_GPIO)
    # check the state of the gpio for the pump relay switch
    state2 = GPIO.input(Relay2_GPIO)
    # check the state of the gpio for the fan relay switch
    state1 = GPIO.input(Relay1_GPIO)
    lightValue = analogRead(2)       # read A2 data
    waterLevel = analogRead(1)       # read A1 data
    soilValue = analogRead(0)          # read A0 data

    if(state2 == 1):
        soil_pump(soilValue, waterLevel)
    elif(pumpFlag == 1):
        soil_pump(soilValue, waterLevel)
    elif(soilValue<50):                  # When the soil moisture value is less than 50, 
        soil_pump(soilValue, waterLevel)
    elif(lightValue < 80 and state4 == 0):               # When the ambient brightness is less than 80
        photoresistor(lightValue)
    elif(lightValue > 80 and state4 == 1):
        photoresistor(lightValue)

    # else:
    #     for i in [21,20,16,12]:
    #         GPIO.output(i, GPIO.LOW) # off
    return lightValue, soilValue, waterLevel, state1, state2, state4

def ledOn():
    print("LED on")
    GPIO.output(5,GPIO.HIGH)

def ledOff():
    print("LED off")
    GPIO.output(5,GPIO.LOW)

def LightStartSchedule(startTime):
    schedule.every().day.at(startTime).do(lightOnTime)
    print("Lights On scheduled")
    d = datetime.datetime.strptime(startTime, '%H:%M')
    stopTime = (d + datetime.timedelta(minutes=2)).strftime("%H:%M")
    schedule.every().day.at(stopTime).do(lightOffTime)

def lightOnTime():
    global lightFlag 
    lightFlag = 1
    lights()

def lightOffTime():
    global lightFlag 
    lightFlag = 0
    lights()

def isTimeFormat(input):
    try:
        time.strptime(input, '%H:%M')
        return True
    except ValueError:
        return False
