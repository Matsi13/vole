# coding:utf-8

# 加入摄像头模块，让小车实现自动循迹行驶
# 思路为：摄像头读取图像，进行二值化，将白色的赛道凸显出来
# 选择下方的一行像素，黑色为0，白色为255
# 找到白色值的中点
# 目标中点与标准中点（320）进行比较得出偏移量
# 根据偏移量来控制小车左右轮的转速
# 考虑了偏移过多失控->停止;偏移量在一定范围内->高速直行(这样会速度不稳定，已删)

import RPi.GPIO as gpio
import time
import cv2
import numpy as np
from keras.models import load_model
import time

img_size = 200
model = load_model('./model/ars_cnn_model.h5')

animal = {
            0 : 'cat',
            1 : 'dog',
            2 : 'monkey',
            3 : 'cow',
            4 : 'elephant',
            5 : 'horse' ,
            6 : 'vole',
            7 : 'chicken' ,
            8 : 'spider' ,
            9 : 'sheep'
         }

def animalrecognition(img):

    
    img = cv2.resize(img, (int(img_size),int(img_size)))
    img = img.reshape(-1, img_size, img_size , 1).astype('float32')
    img /= 255 
    x = model.recognize(img)
    y = x > 0.5 # image must match more than 50% with any of the 10 animals 
    if y.any():  
        x = np.argmax(x,axis = 1)
        recognize_animal = animal[x[0]] + '\n'
    else:
        recognize_animal = "Not Found\n"
        
    return  recognize_animal

# 定义引脚
PressSensor = 11      
Reset = 12

# 设置GPIO口为BOARD编号规范
gpio.setmode(gpio.BOARD)


# 树莓派只有数字输入，建议使用mega处理压力传感器，
# 再用另一个引脚给树莓派发高低电平信号
# 设置GPIO口为输入
gpio.setup(PressSensor, gpio.IN)
gpio.setup(Reset, gpio.IN)        #复位信号
# 打开摄像头，图像尺寸640*480（长*高），opencv存储值为480*640（行*列）
cap = cv2.VideoCapture(0)

VoleFile = open('VoleFile.txt', 'w')
while (GPIO.input(Reset) == gpio.LOW):            #如果没收到复位信号
    if(GPIO.input(PressSensor) == gpio.HIGH):     #而且压力传感器读到了数值
        ret, frame = cap.read()                   #拍照               
        img = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        result = animalrecognition(img)           #识别
        localtime = time.asctime( time.localtime(time.time()) )
        VoleFile.write(localtime + result)        #记录识别结果和识别时间
        
        

        
# 释放清理
VoleFile.close()
cap.release()
cv2.destroyAllWindows()
gpio.cleanup()
