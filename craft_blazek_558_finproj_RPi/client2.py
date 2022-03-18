"""
Student: Joshua Blazek
Class: ECE558 - Winter 2022

Raspberry Pi and Android paho MQTT example
Turns the Green LED on the Explorer Pro Hat on and off w/ the MQTT topics
Raspberry/liteon and Raspberry/liteoff.  Returns status (LED on or off)
in the topic Raspberry/status

Raspberry Pi code based on example from the Institute of Geomatics Engineering
Android code loosly based on HiveMQ and patho examples
"""

## imports
from config2 import *
import paho.mqtt.client as mqtt
import time
import os.path
from threading import Thread

# global variables

client = mqtt.Client()



# MQTT callback methods
def on_connect(client, userdata, flags, rc):    
    print("Tried to connect to MQTT server: {}:{}...result: {}".format(
        mqtt_server_host,
        mqtt_server_port,
        mqtt.connack_string(rc)))

    # Check whether the result from connect is the CONNACK_ACCEPTED connack code
    # If conection was successful subscribe to the command topic
    if rc == mqtt.CONNACK_ACCEPTED:
        # Subscribe to the commands topic filter
        client.subscribe("jblazek/image", qos=1)
        time.sleep(1)

def on_subscribe(client, userdata, mid, granted_qos):
    print("Subscribed with QoS: {}".format(granted_qos[0]))


def on_message(client, userdata, msg):
    global led_on
    global ledState
    global sensorInterval
    f = open('output.jpg', "wb")
    f.write(msg.payload)
    print("Image Received")
    f.close()


def publish_status(detected_payload):
    print("Payload: " + detected_payload)
    client.publish(
        topic = "jblazek/detections",
        payload = detected_payload
    )
    #time.sleep(2)
    
def publish_image():
    f=open("current.jpg", "rb") #3.7kiB in same folder
    fileContent = f.read()
    byteArr = bytearray(fileContent)
    client.publish(
        topic = "jblazek/image",
        payload = byteArr
    )

#MQTT client
client.on_connect = on_connect
client.on_subscribe = on_subscribe
client.on_message = on_message

# connect to the MQTT server (which runs locally on the RPI)
# subscribing to the command topic is handled in on_connect()
client.connect (
    host=mqtt_server_host,
    port=mqtt_server_port,
    keepalive=mqtt_keepalive
 )

Thread(target=client.loop_forever).start()

