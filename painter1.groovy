#include <Arduino.h>
#include <Servo.h>

int PressSensor = A0; //压力传感器从A0输入
int LightSensor = 22; //光敏电阻从数字引脚22输入，0是亮
int LEDSwitch = 23;   //LED由数字引脚23控制
int SpraySwitch = 24; //喷雾器由数字24控制
int CeilSwitch = 25;  //用一个继电器，给顶门舵机断电
int FrontPWM = 2;     //前门由数字引脚2控制
int CeilPWM = 3;      //顶门由数字引脚3控制
int BuzzPWM = 4;      //蜂鸣器由数字引脚4控制

int PressThreshold = 500;        //超过这一压力，视为有田鼠进入笼子
int CameraTime = 3000;           //给摄像机3s拍照，之后熄灯
int SprayTime = 3000;            //染色三秒钟
int FleeTime = 5000;             //给田鼠5s逃出笼子
int FrontOpen = 0;               //开始时前门关闭
int CeilOpen = 0;                //开始时顶门关闭
const long Buzz_frequency = 300; //蜂鸣器频率

Servo front_servo, ceil_servo;

void setup()
{
  pinMode(LightSensor, INPUT_PULLUP);
  pinMode(SpraySwitch, OUTPUT);
  pinMode(LEDSwitch, OUTPUT);
  pinMode(CeilSwitch, OUTPUT);
  pinMode(BuzzPWM, PUTPUT);

  digitalWrite(LEDSwitch, LOW);
  digitalWrite(CeilSwitch, LOW);
  digitalWrite(BuzzPWM, LOW);

  front_servo.attach(FrontPWM);
  ceil_servo.attach(CeilPWM);
}

void loop()
{

  int pressure = GetPressValue(PressSensor);
  int light = digitalRead(LightSensor);
  if (pressure > PressThreshold)
  {
    /*判断是否需要亮灯*/
    LED_CNTRL(light); //自动熄灭
    /*摄像头拍照*/

    /*喷雾*/
    Spray_CNTRL() ；
        /*开前门*/
        Front_CNTRL(1);
    delay(FleeTime);
    /*压力持续存在，则打开蜂鸣器*/
    pressure = GetPressValue(PressSensor);
    while (pressure > PressThreshold)
    {
      Buzz_CNTRL();
      pressure = GetPressValue(PressSensor);
    }
    /*关顶门，前门*/
    Ceil_CNTRL(0);
    delay(10);
    Front_CNTRL(0);
    delay(1000);
    digitalWrite(CeilSwitch, LOW);

    /*向mega内存中写入捕捉记录*/
  }
}

long GetPressValue(int pin)
{
  long PRESS_AO = 0;
  int VOLTAGE_AO = 0;
  int value = analogRead(pin);
  int VOLTAGE_MIN = 100;
  int VOLTAGE_MAX = 3300;
  int PRESS_MIN = 30;
  int PRESS_MAX = 1000;

  VOLTAGE_AO = map(value, 0, 1023, 0, 5000);

  if (VOLTAGE_AO < VOLTAGE_MIN)
  {
    PRESS_AO = 0;
  }
  else if (VOLTAGE_AO > VOLTAGE_MAX)
  {
    PRESS_AO = PRESS_MAX;
  }
  else
  {
    PRESS_AO = map(VOLTAGE_AO, VOLTAGE_MIN, VOLTAGE_MAX, PRESS_MIN, PRESS_MAX);
  }
  return PRESS_AO;
}

void LED_CNTRL(light, order)
{
  if (light)
  {
    digitalWrite(LEDSwitch, HIGH);
    delay(CameraTime);
    digitalWrite(LEDSwitch, LOW);
  }
  else
  {
    digitalWrite(LEDSwitch, LOW);
  }
}

void Spray_CNTRL()
{
  digitalWrite(SpraySwitch, HIGH);
  delay(SprayTime);
  digitalWrite(SpraySwitch, LOW);
}

void Front_CNTRL(order)
{

  int pos = 0;
  int finalpos = 250;
  int beginpos = 110;
  int interval = 5;

  if (FrontOpen == 0 && order == 1)
  {
    for (pos = beginpos; pos <= finalpos; pos += 1) // 两个for循环可能要交换位置，我忘了哪个是开门，哪个是关门
    {
      front_servo.write(pos);
      delay(interval);
    }
    FrontOpen = 1;
  }
  if (FrontOpen == 1 && order == 0)
  {
    for (pos = finalpos; pos >= beginpos; pos -= 1)
    {
      front_servo.write(pos);
      delay(interval);
    }
    FrontOpen = 0;
  }
}

void Ceil_CNTRL(order)
{

  int pos = 0;
  int finalpos = 110;
  int beginpos = 0;
  int interval = 5;

  digitalWrite(CeilSwitch, HIGH);
  delay(10);

  if (order == 1 && CeilOpen == 0)
  {
    for (pos = beginpos; pos <= finalpos; pos += 1)
    {
      ceil_servo.write(pos);
      delay(interval);
    }
    CeilOpen = 1;
  }
  if (order == 0 && CeilOpen == 1)
  {
    for (pos = finalpos; pos >= beginpos; pos -= 1)
    {
      myservo.write(pos);
      delay(interval);
    }
    CeilOpen = 0;
  }
}

void Buzz_CNTRL()
{
  tone(BuzzPWM, frequency); // start making noise
  delay(1000);
  noTone(BuzzPWM); // stop making noise
  delay(1000);
}
