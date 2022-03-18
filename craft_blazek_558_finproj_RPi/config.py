"""
Defines client-related constants
Based on example in "Hands-on MQTT Programming with Python" by  Gaston C. Hillar
"""
import os.path


# Replace server host string with the IP or hostname for the Mosquitto or other MQTT server
# Make sure the IP or hostname matches the value you used for Common Name
mqtt_server_host = "broker.hivemq.com"  #Uses the HiveMQ public MQTT broker
mqtt_server_port = 1883
mqtt_keepalive = 60
