# DepthLock On-Device Vision Segmentation Models

## 1. Default On-Device Segmentation
DepthLock comes pre-configured with on-device local neural segmentation using **Google ML Kit's Selfie & Subject Segmenter** (`com.google.mlkit:segmentation-selfie`). It uses an optimized TensorFlow Lite neural backbone accelerated via GPU/NNAPI that runs **100% offline** on the user's Android smartphone with zero cloud dependencies or external network requests.

## 2. Bundling Custom TensorFlow Lite / MediaPipe Models
If you wish to bundle a custom mobile segmentation model:

### Recommended Efficient Models for Mid-Range Devices (e.g. Motorola Moto G85 5G):
1. **MediaPipe Selfie Multiclass Segmenter**:
   - Model file: `selfie_multiclass_256x256.tflite` (approx. 2.1 MB)
   - Input: 256 × 256 RGB float/int8
   - Output: 6 classes (Background, Hair, Body Skin, Face Skin, Clothes, Others)
   - Source: [MediaPipe Models](https://developers.google.com/mediapipe/solutions/vision/image_segmenter#models)

2. **DeepLabV3 MobileNetV2**:
   - Model file: `deeplabv3_257_mv_gpu.tflite` (approx. 2.7 MB)
   - Input: 257 × 257 RGB normalized
   - Output: 21 classes (PASCAL VOC including person, dog, cat, car, bird, etc.)
   - Source: [TensorFlow Lite Models](https://tfhub.dev/tensorflow/lite-model/deeplabv3/1/default/1)

### Installation Instructions:
1. Download or convert your `.tflite` model.
2. Place the file inside this directory:
   ```
   app/src/main/assets/models/<your_model>.tflite
   ```
3. Load the model using `ImageSegmentationHelper.loadModelFile("models/<your_model>.tflite")` which memory-maps the model into memory with zero copy overhead.
4. If no custom file is present, `ImageSegmentationHelper` automatically falls back to the built-in ML Kit Neural Segmenter and Adaptive Saliency Segmenter to ensure zero crashes and universal offline compatibility!
