from thrift.protocol import TBinaryProtocol
from thrift.server import TServer
from thrift.transport import TTransport
from thrift.transport import TSocket

from gen_py import ImageProcessor
from gen_py.ttypes import CroppedData, ProcessedImage

import torch
import cv2
import os
import numpy as np

import argparse
import csv
import platform
import sys
from pathlib import Path


FILE = Path(__file__).resolve()
ROOT = FILE.parents[0]  # Project root directory
if str(ROOT) not in sys.path:
    sys.path.append(str(ROOT))  # Add ROOT to PATH

# Ensure the 'ultralytics/export' directory is in the path if needed
export_dir = ROOT / 'ultralytics' / 'export'
if str(export_dir) not in sys.path:
    sys.path.append(str(export_dir))

from ultralytics.utils.plotting import Annotator, colors, save_one_box

from models.common import DetectMultiBackend
from utils.dataloaders import IMG_FORMATS, VID_FORMATS, LoadImages, LoadScreenshots, LoadStreams
from utils.general import (
    Profile,
    check_file,
    check_img_size,
    cv2,
    increment_path,
    non_max_suppression,
    scale_boxes,
)
from utils.torch_utils import select_device, smart_inference_mode


class ImageHandler(ImageProcessor.Iface):
    def __init__(self):
        self.weights = './best20.pt'  # model path or triton URL
        self.data = './data.yaml'  # dataset.yaml path
        self.device = ''  # cuda device, i.e. 0 or 0,1,2,3 or cpu
        self.half = False  # use FP16 half-precision inference
        self.dnn = False  # use OpenCV DNN for ONNX inference
        self.imgsz = (640, 640)  # inference size (height, width)
        self.conf_thres = 0.5  # confidence threshold
        self.iou_thres = 0.45  # NMS IOU threshold
        self.max_det = 1000  # maximum detections per image
        self.line_thickness = 2  # bounding box thickness (pixels)
        self.hide_labels = False  # hide labels
        self.hide_conf = False  # hide confidences
        self.vid_stride = 1  # video frame-rate stride
        self.nosave=False  # do not save images/videos
        self.project='runs/detect'  # save results to project/name
        self.name='exp'  # save results to project/name
        self.exist_ok=False  # existing project/name ok, do not increment
        self.nosave=False,  # do not save images/videos
        self.view_img=False  # show results
        self.save_txt=False  # save results to *.txt
        self.save_csv=False  # save results in CSV format
        self.save_conf=False  # save confidences in --save-txt labels
        self.save_crop=False  # save cropped prediction boxes
        self.classes=None  # filter by class: --class 0, or --class 0 2 3
        self.agnostic_nms=False  # class-agnostic NMS
        self.augment=False  # augmented inference
        self.visualize=False  # visualize features
        self.update=False  # update all models
        
        # Load model
        self.device = select_device(self.device)
        self.model = DetectMultiBackend(self.weights, device=self.device, dnn=self.dnn, data=self.data, fp16=self.half)
        self.stride, self.names, self.pt = self.model.stride, self.model.names, self.model.pt
        self.imgsz = check_img_size(self.imgsz, s=self.stride)  # check image size
        

    @smart_inference_mode()
    def processImage(self, croppedData):
        status = 'default'  # 처리 상태
        save_img = True
        save_dir = 'runs/detect'
        cropped_data_list = []

        # Byte array를 OpenCV 이미지로 변환
        nparr = np.frombuffer(croppedData.imageData, np.uint8)
        source = cv2.imdecode(nparr, cv2.IMREAD_COLOR)  # file/dir/URL/glob/screen/0(webcam)

       # 이미지를 올바른 형태로 변환
        im = cv2.resize(source, self.imgsz)  # 이미지 크기 조정
        im = im.transpose(2, 0, 1)  # HWC에서 CHW로 변환
        im = np.expand_dims(im, axis=0)  # 배치 차원 추가
        im = torch.from_numpy(im).to(self.model.device)  # Tensor로 변환 및 디바이스로 이동
        im = im.half() if self.model.fp16 else im.float()  # uint8에서 fp16/32로 변환
        im /= 255.0  # 0 - 255에서 0.0 - 1.0으로 변환

        self.model.warmup(imgsz=(1 if self.pt or self.model.triton else 1, 3, *self.imgsz))

        pred = self.model(im, augment=self.augment, visualize=self.visualize)
        pred = non_max_suppression(pred, self.conf_thres, self.iou_thres, self.classes, self.agnostic_nms, max_det=self.max_det)

        for det in pred:
            annotator = Annotator(source, line_width=self.line_thickness, example=str(self.names))
            if len(det):
                status = "alert"
                det[:, :4] = scale_boxes(im.shape[2:], det[:, :4], source.shape).round()

                for *xyxy, conf, cls in reversed(det):
                    c = int(cls)
                    label = self.names[c] if self.hide_conf else f"{self.names[c]} {conf:.2f}"
                    annotator.box_label(xyxy, label, color=colors(c, True))
            else:
                status = 'default'

            im0 = annotator.result()

            if save_img:
                save_path = 'runs/detect/image.jpg'
                cv2.imwrite(save_path, im0)
                _, buffer = cv2.imencode('.jpg', im0)
                detect_img = buffer.tobytes()
                cropped_data = CroppedData(imageData=detect_img, width=0, height=0)
                cropped_data_list.append(cropped_data)
            else:
                detect_img = None
                cropped_data = CroppedData(imageData=detect_img, width=0, height=0)
                cropped_data_list.append(cropped_data)

        return ProcessedImage(status=status, croppedData=cropped_data_list)

if __name__ == '__main__':
    handler = ImageHandler()
    processor = ImageProcessor.Processor(handler)
    transport = TSocket.TServerSocket(port=9090)
    tfactory = TTransport.TBufferedTransportFactory()
    pfactory = TBinaryProtocol.TBinaryProtocolFactory()

    server = TServer.TSimpleServer(processor, transport, tfactory, pfactory)

    print('Starting the server...')
    server.serve()
    print('Server stopped.')
