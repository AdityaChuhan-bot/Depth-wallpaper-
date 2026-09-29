MediaPipe / TFLite On-Device Models Directory

Place downloaded .tflite models here for automatic detection by ImageSegmentationHelper:

1. DeepLabV3 (Object & Animal Multi-class Segmentation):
   Filename: deeplabv3.tflite
   Download URL: https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite

2. Selfie Multiclass (Face, Hair, Body, Clothes Segmentation):
   Filename: selfie_multiclass_256x256.tflite
   Download URL: https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_multiclass_256x256/float32/latest/selfie_multiclass_256x256.tflite

When a model is placed in this directory, ImageSegmentationHelper will automatically load it.
If not present, ImageSegmentationHelper seamlessly uses the bundled ML Kit neural segmenter
and Adaptive Saliency engine so the app works 100% offline out-of-the-box!
