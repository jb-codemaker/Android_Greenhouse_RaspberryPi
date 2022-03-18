"""
Raspberry Pi and Android paho MQTT App
ECE 558 | Winter 2022 | Professor Kravitz | Final Project

File  : client.py
Author: David Craft and Joshua Blazek
Date  : 16 Mar 2022

Overview:
Turns the LEDs on the Explorer Pro Hat on and off w/ the MQTT topics
Raspberry/liteon and Raspberry/liteoff.  Returns status (LED on or off)
in the topic android/status. Reads temperature and humitity data from
an AHT20 sensor and publishes the readings to topic android/sensor, 
gauge1 and gauge2. Publishes the button status to android/button and 
sets the sensor mode. 

Code for AHT20 addapted from 2021 ladyada for Adafruit Industries
# SPDX-License-Identifier: MIT
"""

## imports
from config import *
import sensor_helper as s_h
import paho.mqtt.client as mqtt
import time
import board
import adafruit_ahtx0
import RPi.GPIO as GPIO
import smbus   
import schedule

# global variables
led_on = "LED is OFF"
button_press = "Button Not Pressed"
client = mqtt.Client()
# Create sensor object, communicating over the board's default I2C bus
i2c = board.I2C() # uses board.SCL and board.SDA
sensor = adafruit_ahtx0.AHTx0(i2c)
mode = 4
# sensDelay = 5

# MQTT callback methods
def on_connect(client, userdata, flags, rc):    
    print("Tried to connect to MQTT server: {}:{}...result: {}".format(
        mqtt_server_host,
        mqtt_server_port,
        mqtt.connack_string(rc)))  # rc, stands for return code

    # Check whether the result from connect is the CONNACK_ACCEPTED connack code
    # If conection was successful subscribe to the command topic
    if rc == mqtt.CONNACK_ACCEPTED:
         # Subscribe to the commands topic filter
        client.subscribe ([
            ("craft_blazek_ece558/raspberry/lights",  1), 
            ("craft_blazek_ece558/raspberry/fan",  1), 
            ("craft_blazek_ece558/raspberry/pump",  1),
            ("craft_blazek_ece558/raspberry/schedule", 1), 
            ("craft_blazek_ece558/raspberry/interval", 1)
            ])

        time.sleep(1)

def on_subscribe(client, userdata, mid, granted_qos):
    print("Subscribed with QoS: {}".format(granted_qos[0]))


def on_message(client, userdata, msg):
    # checks message from android and processes based on content
    global led_on
    s_h.sensDelay

    s = str(msg.payload, encoding="UTF_8")
    print("retrieved message: " + s)
    if s == "liteon":
        s_h.lightFlag = 1
        s_h.lights()
        publish_sensor_data()
    elif s == "liteoff":
        s_h.lightFlag = 0
        s_h.lights()
        publish_sensor_data()
    elif s == "fanon":
        s_h.fanFlag = 1
        s_h.aht20Sensor()
        publish_sensor_data()   
    elif s == "fanoff":
        s_h.fanFlag = 0
        s_h.aht20Sensor()
        publish_sensor_data()
    elif s == "pumpon":
        s_h.pumpFlag = 1
        s_h.checkAnalog()
        publish_sensor_data()
    elif s == "pumpoff":
        s_h.pumpFlag = 0
        s_h.checkAnalog()
        publish_sensor_data()
    elif (s_h.isTimeFormat(s)):
        s_h.LightStartSchedule(s)
    elif s.isnumeric():
    # if s.isnumeric():    
        s_h.sensDelay = int(s)
        print(f"Sensor delay set: {s_h.sensDelay} seconds")
    else:
        print("Invalid payload received")

        
def publish_sensor_data():
    # publish data from the sensors
    c, h = s_h.aht20Sensor()
    lit, soil, wat, s1, s2, s4 = s_h.checkAnalog()

    lights = (f"Lights/mat: {'ON' if s4 == 1 else 'OFF'}")
    fan = (f"Fan: {'ON' if s1 == 1 else 'OFF'}")
    pump = (f"Pump: {'ON' if s2 == 1 else 'OFF'}")
    luminosity = ("Luminosity: %0.0f" %(lit))
    soilSens = ("Soil: %1.0f" %(soil))
    water = ("Water level: %1.0f" %(wat))

    print(c)
    print(h)
    print(luminosity) # print light data
    print(soilSens)   # print soil data
    print(water)   # print water level data
    print(lights)
    print(fan)
    print(pump)
    print(f"Sensor Read Delay: {s_h.sensDelay} secs")
    print(f"|fanFlag: {s_h.fanFlag}|pumpFlag: {s_h.pumpFlag}|lightFlag: {s_h.lightFlag}|\n")

    client.publish(topic = "craft_blazek_ece558/android/celcius", payload = (c))
    client.publish(topic = "craft_blazek_ece558/android/humidity", payload = (h))
    client.publish(topic = "craft_blazek_ece558/android/lights", payload = (luminosity))
    client.publish(topic = "craft_blazek_ece558/android/soil", payload = (soilSens))
    client.publish(topic = "craft_blazek_ece558/android/waterLevel", payload = (water))
    client.publish(topic = "craft_blazek_ece558/android/fan", payload = (fan))
    client.publish(topic = "craft_blazek_ece558/android/pump", payload = (pump))
    client.publish(topic = "craft_blazek_ece558/android/lights_mat", payload = (lights))
    # publish for graph bars
    client.publish(topic = "craft_blazek_ece558/android/fahr_graph", payload = ("%0.0f" % s_h.aht20.temperature))
    client.publish(topic = "craft_blazek_ece558/android/humidity_graph", payload = ("%0.0f" % s_h.aht20.relative_humidity))
    client.publish(topic = "craft_blazek_ece558/android/lums_graph", payload = (lit))
    client.publish(topic = "craft_blazek_ece558/android/soil_graph", payload = (soil))
    client.publish(topic = "craft_blazek_ece558/android/water_graph", payload = (wat))

# ------------------------------------------------------------    
# main program
# ------------------------------------------------------------
print("start program")
for i in [21,20,16,12]:             # make sure Relays are off
    GPIO.output(i, GPIO.LOW)
## connect to the MQTT server (hiveMQ)
## subscribing to the command topic is handled in on_connect()
client.on_connect = on_connect
client.connect (host=mqtt_server_host, port=mqtt_server_port, keepalive=mqtt_keepalive)
client.on_subscribe = on_subscribe
client.loop_start()
while True:
    client.on_message = on_message
    
    schedule.run_pending()
    publish_sensor_data()
    time.sleep(s_h.sensDelay)
    
client.loop_stop()
