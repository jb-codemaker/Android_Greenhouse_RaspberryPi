####

#
# Students: Josh Blazek and David Craft
# ECE558
#
# Code is edited but sourced from Tensorflow
#
#

#####


# Copyright 2021 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Utility functions to display the pose detection results."""

from typing import List

import cv2
import numpy as np
from object_detector import Detection

from client2 import *

_MARGIN = 10  # pixels
_ROW_SIZE = 10  # pixels
_FONT_SIZE = 2
_FONT_THICKNESS = 2
_TEXT_COLOR = (0, 255, 0)  # red


def visualize(
    image: np.ndarray,
    detections: List[Detection],
) -> np.ndarray:
  """Draws bounding boxes on the input image and return it.

  Args:
    image: The input RGB image.
    detections: The list of all "Detection" entities to be visualize.

  Returns:
    Image with bounding boxes.
  """
  for detection in detections:
    # Draw bounding_box
    start_point = detection.bounding_box.left, detection.bounding_box.top
    end_point = detection.bounding_box.right, detection.bounding_box.bottom
    cv2.rectangle(image, start_point, end_point, _TEXT_COLOR, 3)

    # Draw label and score
    category = detection.categories[0]
    class_name = category.label
    probability = round(category.score, 2)
    result_text = class_name + ' (' + str(probability) + ')'
    text_location = (_MARGIN + detection.bounding_box.left,
                     _MARGIN + _ROW_SIZE + detection.bounding_box.top)
    cv2.putText(image, result_text, text_location, cv2.FONT_HERSHEY_PLAIN,
                _FONT_SIZE, _TEXT_COLOR, _FONT_THICKNESS)
    if class_name == 'ostrich' and probability > 0.34:
        # Object detected.  Call client.py and publish to MQTT deteted object
        publish_status('I\'m ' + str(int(probability*100)) + '% certain that freaken ostrich is back.')
        cv2.imwrite('current.jpg', image)
        publish_image()
        time.sleep(30)
    if class_name == 'moldy' and probability > 0.28:
        # Object detected.  Call client.py and publish to MQTT deteted object
        publish_status('I\'m pretty sure I saw powdery mildew!!!!')
        cv2.imwrite('current.jpg', image)
        publish_image()
        time.sleep(30)

  return image
