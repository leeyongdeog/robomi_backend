import tensorflow as tf
import tf2onnx
import onnx

# TensorFlow 모델 로드
model_path = '/home/ubuntu2004/ROBOMI/robomi_backend/src/main/resources/model'
model = tf.saved_model.load(model_path)

# ONNX로 변환
spec = (tf.TensorSpec((None, 480, 640, 3), tf.float32, name="input"),)
output_path = "EDSR_x4.onnx"
model_proto, _ = tf2onnx.convert.from_function(
    model.signatures['serving_default'], input_signature=spec, opset=13, output_path=output_path)

onnx.save(model_proto, output_path)
