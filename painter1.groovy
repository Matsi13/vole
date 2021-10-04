#include <Arduino.h>
#include <Servo.h>
#include <EEPROM.h>
#include <DS1302.h>

#define DEBUGSerial Serial
#define ClockLen 24                    //DS1302返回的字符串长度

const int PressSensor = A0;            //压力传感器从A0输入
const int LightSensor = 22;            //光敏电阻从数字引脚22输入，0是亮
const int LEDSwitch = 23;              //LED由数字引脚23控制
const int SpraySwitch = 24;            //喷雾器由数字24控制
const int CeilSwitch = 25;             //用一个继电器，给顶门舵机断电
const int FrontPWM = 5;                //前门由数字引脚5控制
const int CeilPWM = 6;                 //顶门由数字引脚6控制
const int BuzzPWM = 7;                 //蜂鸣器由数字引脚7控制
const int DSRST = 2;                   //时钟控制
const int DSDAT = 3;                   //时钟控制
const int DSCLK = 4;                   //时钟控制


const int PressThreshold = 50;         //超过这一压力，视为有田鼠进入笼子
const int CameraTime = 5000;           //给摄像机3s拍照，之后熄灯
const int SprayInterval = 1000;        //两次喷雾的间隔,针对机械喷雾器
const int Spraycount = 2;              //喷雾器次 c数，针对机械喷雾器
const int SprayTime = 3000;            //喷雾时间，针对电子喷雾器  
const int FleeTime = 5000;             //给田鼠5s逃出笼子
const long Buzz_frequency = 300;       //蜂鸣器频率

int FrontOpen = 0;                     //开始时前门关闭
int CeilOpen = 0;                      //开始时顶门关闭
int VoleCount = 0;                     //记录抓到的田鼠的数量
int WriteAddress = 0;                  //EEPROM的起始地址

struct Record{
  int count;
  char time[ClockLen];
};

Servo front_servo, ceil_servo;
DS1302 rtc(DSRST, DSDAT, DSCLK);       //对应DS1302的RST,DAT,CLK

void setup()
{
  pinMode(LightSensor, INPUT_PULLUP);
  pinMode(SpraySwitch, OUTPUT);
  pinMode(LEDSwitch, OUTPUT);
  pinMode(CeilSwitch, OUTPUT);
  pinMode(BuzzPWM, OUTPUT);

  digitalWrite(LEDSwitch, LOW);
  digitalWrite(CeilSwitch, LOW);
  digitalWrite(BuzzPWM, LOW);

  front_servo.attach(FrontPWM);
  ceil_servo.attach(CeilPWM);
  digitalWrite(CeilSwitch, LOW);
  DEBUGSerial.begin(9600);

  EEClear();                     // 打印并清理上一次的记录
}

void loop()
{

  int pressure = GetPressValue(PressSensor);
  DEBUGSerial.print("F = ");
  DEBUGSerial.print(pressure);
  DEBUGSerial.println(" g,");

  int light = digitalRead(LightSensor);
  DEBUGSerial.print("light:");
  DEBUGSerial.println(light);
  
  if (pressure > PressThreshold)
  {
    
    DEBUGSerial.println("enter if");

    /*记录一次捕捉，写入mega内存*/
    Records();
    
    CeilOpen = 1;
    /*判断是否需要亮灯*/
    LED_CNTRL(light); //自动熄灭
    if(light == 0)
    {
      delay(CameraTime);
    }
    
    /*摄像头拍照*/

    /*喷雾*/
    Spray_CNTRL();
    /*开前门*/
    Front_CNTRL(1);   // order = 1 为开门
    delay(FleeTime);
    
    /*压力持续存在，则打开蜂鸣器*/
    DEBUGSerial.println("read presssure");
    pressure = GetPressValue(PressSensor);
    DEBUGSerial.print("pressure: ");
    DEBUGSerial.println(pressure);
    
    while (pressure > PressThreshold)
    {
      DEBUGSerial.println("turn on buzz");
      Buzz_CNTRL();
      pressure = GetPressValue(PressSensor);
    }
    /*关顶门，前门*/
    Ceil_CNTRL();
    delay(10);
    Front_CNTRL(0);
    delay(1000);
    
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

void LED_CNTRL(int light)
{
  if (light)
  {
    DEBUGSerial.println("turn on LED");
    digitalWrite(LEDSwitch, HIGH);
    delay(CameraTime);
    digitalWrite(LEDSwitch, LOW);
    DEBUGSerial.println("turn off LED");
  }
  else
  { 
    DEBUGSerial.println("dont need to turn on LED");
    digitalWrite(LEDSwitch, LOW);
  }
}

void Spray_CNTRL()
{
  digitalWrite(SpraySwitch, HIGH);
  delay(SprayTime);
  digitalWrite(SpraySwitch, LOW);
}

void Front_CNTRL(int order)
{

  int pos = 0;
  int finalpos = 250;
  int beginpos = 110;
  int interval = 5;

  if (FrontOpen == 0 && order == 1)
  {
    DEBUGSerial.println("open front door");
    for (pos = beginpos; pos <= finalpos; pos += 1) // 两个for循环可能要交换位置，我忘了哪个是开门，哪个是关门
    {
      front_servo.write(pos);
      delay(interval);
    }
    FrontOpen = 1;
  }
  if (FrontOpen == 1 && order == 0)
  {
    DEBUGSerial.println("close front door");
    for (pos = finalpos; pos >= beginpos; pos -= 1)
    {
      front_servo.write(pos);
      delay(interval);
    }
    FrontOpen = 0;
  }
}

void Ceil_CNTRL()
{
  DEBUGSerial.println("close ceil door");
  int pos = 0;
  int finalpos = 110;
  int beginpos = 0;
  int interval = 5;

  digitalWrite(CeilSwitch, HIGH);
  delay(10);
  
  if (CeilOpen == 1)   // 若门开，则关门
  {
    for (pos = beginpos; pos <= finalpos; pos += 1)
    {
      ceil_servo.write(pos);
      delay(interval);
    }
    CeilOpen = 0;
  }
  if (CeilOpen == 0)   // 若关门，舵机复位
  {
    for (pos = finalpos; pos >= beginpos; pos -= +1)
    {
      ceil_servo.write(pos);
      delay(interval);
    }
  }
  digitalWrite(CeilSwitch, LOW);
}

void Buzz_CNTRL()
{
  tone(BuzzPWM, Buzz_frequency); // start making noise
  delay(1000);
  noTone(BuzzPWM); // stop making noise
  delay(1000);
}
void Records()
{
  DEBUGSerial.println("In Records");
  VoleCount += 1;
  Time tim = rtc.time(); //从DS1302获取时间数据
  Record r;
  snprintf(r.time, sizeof(r.time), "%04d-%02d-%02d %02d:%02d:%02d",
           tim.yr, tim.mon, tim.date,
           tim.hr, tim.min, tim.sec); 
  r.count = VoleCount;
  
  EEPROM.put(WriteAddress, r);
  WriteAddress += sizeof(Record);
  if (WriteAddress >= EEPROM.length())
  WriteAddress -= EEPROM.length();
}

void EEClear()
{
  int ReadAddress = 0;
  Record prerecord;
  EEPROM.get(ReadAddress, prerecord);
  while (prerecord.count > 0 && ReadAddress <= EEPROM.length())
  {
    ReadAddress += sizeof(Record);
    DEBUGSerial.println(prerecord.count);
    DEBUGSerial.println(prerecord.time);
    EEPROM.get(ReadAddress, prerecord);
  }
  
 for (int i = 0 ; i < EEPROM.length() ; i++) 
 {
    EEPROM.write(i, 0);
  }
}



