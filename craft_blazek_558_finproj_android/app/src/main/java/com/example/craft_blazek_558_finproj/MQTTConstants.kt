package com.example.craft_blazek_558_finproj

const val MQTT_SERVER_URI_KEY   = "MQTT_SERVER_URI"
const val MQTT_CLIENT_ID_KEY    = "MQTT_CLIENT_ID"
const val MQTT_USERNAME_KEY     = "MQTT_USERNAME"
const val MQTT_PWD_KEY          = "MQTT_PWD"

const val MQTT_SERVER_URI       = "tcp://broker.hivemq.com:1883"
const val MQTT_CLIENT_ID        = ""
const val MQTT_USERNAME         = ""
const val MQTT_PWD              = ""

const val LITE_CON_TOPIC        = "craft_blazek_ece558/raspberry/lights"
const val FAN_CON_TOPIC         = "craft_blazek_ece558/raspberry/fan"
const val PUMP_CON_TOPIC        = "craft_blazek_ece558/raspberry/pump"
const val INTERVAL_TOPIC        = "craft_blazek_ece558/raspberry/interval"
const val SCHEDULE_TOPIC        = "craft_blazek_ece558/raspberry/schedule"

const val CELCIUS_TOPIC         = "craft_blazek_ece558/android/celcius"
const val HUMIDITY_TOPIC        = "craft_blazek_ece558/android/humidity"
const val LIGHT_SENS_TOPIC      = "craft_blazek_ece558/android/lights"
const val SOIL_TOPIC            = "craft_blazek_ece558/android/soil"
const val WATER_LEVEL_TOPIC     = "craft_blazek_ece558/android/waterLevel"
const val FAN_TOPIC             = "craft_blazek_ece558/android/fan"
const val PUMP_TOPIC            = "craft_blazek_ece558/android/pump"
const val LIGHTS_MAT_TOPIC      = "craft_blazek_ece558/android/lights_mat"

const val FAHR_GRAPH_TOPIC      = "craft_blazek_ece558/android/fahr_graph"
const val HUMIDITY_GRAPH_TOPIC  = "craft_blazek_ece558/android/humidity_graph"
const val LUM_GRAPH_TOPIC       = "craft_blazek_ece558/android/lums_graph"
const val SOIL_GRAPH_TOPIC      = "craft_blazek_ece558/android/soil_graph"
const val WATER_GRAPH_TOPIC     = "craft_blazek_ece558/android/water_graph"

const val MILDEW_DETECT         = "jblazek/detections"
const val PLANT_IMAGE           = "jblazek/image"

const val LITEON_MESSAGE        = "liteon"
const val LITEOFF_MESSAGE       = "liteoff"
const val FANON_MESSAGE         = "fanon"
const val FANOFF_MESSAGE        = "fanoff"
const val PUMPON_MESSAGE        = "pumpon"
const val PUMPOFF_MESSAGE       = "pumpoff"
