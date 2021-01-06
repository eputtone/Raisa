DEVICE = '/dev/ttyACM0'

import serial
import socket
import time
import sys
import json

HOST = '127.0.0.1'
PORT = 20042

# These are arbitrary
MESSAGE_SIZE_BYTES = 100
MAX_SCANDATA_BYTES = 10000
INF_DISTANCE_MM = 10000

# Serial-port timeout
TIMEOUT_SEC  = 0.1

G_MS = 9.81

def docommand(port, command):
  str = command + '\n'
  port.write(str.encode())

def get_uptime():
  with open('/proc/uptime', 'r') as f:
    uptime_seconds = str(int(float(f.readline().split()[0]) * 1000))
  return uptime_seconds

robot = None
client = None
sock = None   

def shutdown():
  if (client):
    client.close()
  print('Shutting down ...')
  if (robot):
    docommand(robot, 'SetLDSRotation off')
    docommand(robot, 'TestMode off')
    robot.close()   
    time.sleep(1)
  sys.exit(1)

try:
  robot = serial.Serial(DEVICE, 115200, \
    serial.EIGHTBITS, serial.PARITY_NONE, serial.STOPBITS_ONE, TIMEOUT_SEC)
except serial.SerialException:
  print('Unable to connect to ' + DEVICE)
  sys.exit(1)
    
docommand(robot, 'TestMode on')
docommand(robot, 'SetLDSRotation on')

while True:
  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  try:
    sock.bind((HOST, PORT))
    break
  except socket.error as err:
    print('Bind failed: ' + str(err))
    shutdown()
  time.sleep(1)

sock.listen(1)

while True:

  print('Waiting for client to connect ...')
  try:
    client, address = sock.accept()
    print('Accepted connection')
  except Exception as e:
    print(e)
  except KeyboardInterrupt:
    print('Interrupted listening')
    shutdown()

  counter = 0
  lastLeftWheelPosition = 0
  lastRightWheelPosition = 0
  while True:
    try:
      counter += 1
      scanmsg = 'STA;NO' + str(counter) + ';'

      # lidar
      docommand(robot, 'GetLDSScan')
      scandata = robot.read(MAX_SCANDATA_BYTES).decode('utf-8').split('\n')
      for scan in scandata:
        vals = scan.split(',')
        if len(vals) > 2 and vals[1].isnumeric():
          scanmsg += 'LI' + vals[0] + '_' + str(int(int(vals[1]) / 10)) + '_' + vals[2] + ';'
        else:
          print('Invalid scan: ' + scan)

      # acceleration
      docommand(robot, 'GetAccel')
      scandata = robot.read(MAX_SCANDATA_BYTES).decode('utf-8').split('\n')
      for scan in scandata:
        if scan.startswith('XInG') or scan.startswith('YInG') or scan.startswith('ZInG'):
          vals = scan.split(',')
          scanmsg += 'A' + scan[0].lower() + str(round(G_MS * float(vals[1]),3)) + ';'

      # odometer
      docommand(robot, 'GetMotors')
      scandata = robot.read(MAX_SCANDATA_BYTES).decode('utf-8').split('\n')
      for scan in scandata:
        if scan.startswith('LeftWheel_PositionInMM'):
          tmp = int(scan.split(',')[1])
          scanmsg += 'OL' + str(tmp - lastLeftWheelPosition) + ';'
          lastLeftWheelPosition = tmp
        elif scan.startswith('RightWheel_PositionInMM'):
          tmp = int(scan.split(',')[1])
          scanmsg += 'OR' + str(tmp - lastRightWheelPosition) + ';'
          lastRightWheelPosition = tmp

      scanmsg += 'TI' + get_uptime() + ';'

      scanmsg += 'END;'
      #print(scanmsg)
      client.send(bytes(scanmsg + '\n', 'utf-8'))

      try:
        received = client.recv(MAX_SCANDATA_BYTES).decode('utf-8')
        if received != 'X':
            command = json.loads(received)
            
            # motor
            leftSpeed = int(command['leftSpeed'])
            rightSpeed = int(command['rightSpeed'])
            throttle = (abs(leftSpeed) + abs(rightSpeed)) * 30            
            if leftSpeed == 0 and rightSpeed == 0:
              docommand(robot, 'SetMotor 1 1 1')
            else:
              docommand(robot, ' '.join(['SetMotor',str(leftSpeed * INF_DISTANCE_MM),str(rightSpeed * INF_DISTANCE_MM),str(throttle)]))

            # lights
            if command['lights'] is True:
              docommand(robot, 'SetLED LEDRed')
              docommand(robot, 'SetLCD BGWhite')
              docommand(robot, 'SetLED BacklightOn')
            else:
              docommand(robot, 'SetLED ButtonOff')
              docommand(robot, 'SetLED BacklightOff')

            if command['servos'] is True:
              docommand(robot, 'SetLDSRotation on')
            else:
              docommand(robot, 'SetLDSRotation off')

      except:
        print('received nothing')

    except Exception as e:
      print(e)
      if (client):
        client.close()
      break
    except KeyboardInterrupt:
      print('Interrupted listening')
      shutdown()
