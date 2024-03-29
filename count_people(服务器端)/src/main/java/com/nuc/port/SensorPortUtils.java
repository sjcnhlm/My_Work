package com.nuc.port;
import com.nuc.dto.Sensor;


import com.nuc.utils.ArrayUtils;
import com.nuc.utils.ByteUtils;
import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;



/**
 * 串口管理
 *
 */
@SuppressWarnings("all")
public class SensorPortUtils {
    public static SerialPort mSerialport;
    static Sensor sensor_sql;
    static String time;
    static float c;
    static float h;
    static int l;
    public Map<String, String> dataAll = new HashMap<>(); // �������� <��ַ ������>
    public Map<String, String> sensor = new HashMap<>(); // <���������ͣ���������ַ>
    private static OutputStream outputStream = null;
    private static InputStream inputStream = null;

    private SensorPortUtils() {
    }

    private static final SensorPortUtils isSerialPort = new SensorPortUtils();

    public static SensorPortUtils getIsSerialPort() {
        return isSerialPort;
    }

    /**
     * 查找所有可用端口
     *
     * @return 可用端口名称列表
     */
    public static final ArrayList<String> findPorts() {

    	
        Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
       
        System.out.println("mee3");
        ArrayList<String> portNameList = new ArrayList<String>();
        // 将可用串口名添加到List并返回该List
        while (portList.hasMoreElements()) {
            String portName = portList.nextElement().getName();
            portNameList.add(portName);
        }
        return portNameList;
    }

    /**
     * 打开串口
     *
     * @param portName 端口名称
     * @param baudrate 波特率
     * @return 串口对象
     * @throws PortInUseException 串口已被占用
     */
    public static final SerialPort openPort(String portName, int baudrate) throws PortInUseException {
        try {
            // 通过端口名识别端口
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            // 打开端口，并给端口名字和一个timeout（打开操作的超时时间）
            CommPort commPort = portIdentifier.open(portName, 2000);
            // 判断是不是串口
            if (commPort instanceof SerialPort) {
                SerialPort serialPort = (SerialPort) commPort;
                try {
                    // 设置一下串口的波特率等参数
                    // 数据位：8
                    // 停止位：1
                    // 校验位：None
                    serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                            SerialPort.PARITY_NONE);
                } catch (UnsupportedCommOperationException e) {
                    e.printStackTrace();
                }
                return serialPort;
            }
        } catch (NoSuchPortException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    /**
     * 关闭串口
     *
     * @param serialport 待关闭的串口对象
     */
    public static void closePort(SerialPort serialPort) {
        if (serialPort != null) {
            serialPort.close();
        }
    }

    /**
     * 往串口发送数据
     *
     * @param add     发送的地址
     * @param request 待发送数据
     */
    public void sendToPort(String add, String request) {

    	
        String info = "";
        String msg = "071800F1" + add + "01" + request;//Ҫ���͵�����
        info = "02" + msg + ByteUtils.checkcode(msg);

        try {
            outputStream.write(ByteUtils.hexStr2Bytes(info));
            outputStream.flush();
            System.out.println("输出成功");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("输出失败");
        }
    }


    /**
     * 从串口读取数据
     *
     * @param serialPort 当前已建立连接的SerialPort对象
     * @return 读取到的数据
     */
    public static byte[] readFromPort(SerialPort serialPort) {
        byte[] bytes = {};
        try {
            inputStream = serialPort.getInputStream();
            // 缓冲区大小为一个字节
            byte[] readBuffer = new byte[1];
            int bytesNum = inputStream.read(readBuffer);
            int len = 0;
            while (bytesNum > 0) {
                bytes = ArrayUtils.concat(bytes, readBuffer);
                bytesNum = inputStream.read(readBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }


    /**
     * 添加监听器
     *
     * @param port     串口对象
     * @param listener 串口存在有效数据监听
     */
    public static void addListener(SerialPort serialPort, DataAvailableListener listener) {
        try {
            // 给串口添加监听器
            serialPort.addEventListener(new SerialPortListener(listener));
            // 设置当有数据到达时唤醒监听接收线程
            serialPort.notifyOnDataAvailable(true);
            // 设置当通信中断时唤醒中断线程
            serialPort.notifyOnBreakInterrupt(true);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
    }

    /**
     * 串口监听
     */
    public static class SerialPortListener implements SerialPortEventListener {

        private DataAvailableListener mDataAvailableListener;

        public SerialPortListener(DataAvailableListener mDataAvailableListener) {
            this.mDataAvailableListener = mDataAvailableListener;
        }

        public void serialEvent(SerialPortEvent serialPortEvent) {
            switch (serialPortEvent.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE: //1.串口存在有效数据
                    if (mDataAvailableListener != null) {
                        mDataAvailableListener.dataAvailable();
                    }
                    break;

                case SerialPortEvent.OUTPUT_BUFFER_EMPTY: //  2.输出缓冲区已清空
                    break;

                case SerialPortEvent.CTS: //  3.清除待发送数据
                    break;

                case SerialPortEvent.DSR: //4.待发送数据准备好了
                    break;

                case SerialPortEvent.RI:  // 5.振铃指示
                    break;

                case SerialPortEvent.CD: // 6.载波检测
                    break;

                case SerialPortEvent.OE: // 7.溢位（溢出）错误
                    break;

                case SerialPortEvent.PE: // 8.奇偶校验错误
                    break;

                case SerialPortEvent.FE: // 9.帧错误
                    break;

                case SerialPortEvent.BI: // 10.通讯中断
                    System.err.println("与串口设备通讯中断");
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * 串口存在有效数据监听
     */
    public interface DataAvailableListener {
        /**
         * 串口存在有效数据
         */
        void dataAvailable();
    }

    public void openSerialPort() {
        // 获取串口名称
    	System.out.println("mee1");
    	
    	System.out.println(findPorts());
    	
    	System.out.println("mee2");
        String commName = findPorts().toString();
        System.out.println(commName);
        //检查串口名称是否获取正确
        if (commName == null || commName.equals("")) {
            System.out.println("没有搜索到有效串口！");
        } else {
            try {
                mSerialport = openPort("COM3", 115200);
                outputStream = mSerialport.getOutputStream();
                if (mSerialport != null) {

                }
            } catch (PortInUseException e) {
                System.out.println("com3 faild！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 添加串口监听
        SensorPortUtils.addListener(mSerialport, new DataAvailableListener() {
            String data2 = null;

            @Override
            public void dataAvailable() {

                byte[] data = null;
                if (mSerialport == null) {
                    System.out.println("串口对象为空，监听失败！");
                } else {
                    // 读取串口数据
                    data = readFromPort(mSerialport);
                    data2 = ByteUtils.dispose(data, data.length);
                    String[] handler = data2.split(" ");
//                    System.err.println("����"+data.length);
//                    System.out.println("����"+ByteUtils.dispose(data,data.length));
//                    for (String i :
//                            dataAll.keySet()) {
//                        String str = dataAll.get(i);
//                        System.out.print(i+"  |"+str);
//                        System.out.println();
//                    }
//                    System.out.println("================");
//                    for (String s :
//                            sensor.keySet()) {
//                        String str = sensor.get(s);
//                        System.out.print(s+"  |"+str);
//                        System.out.println();
//                    }
//                    System.out.println("================");
                    if (data.length == 10) {//这个是执行器
                        //s传感器的网络地址，t传感器获取数据
                        String s = handler[5] + " " + handler[6];
                        String t = handler[handler.length - 2];


                        sensor.put("EXECUTEB", s);
                        dataAll.put(s, t);
                        
                    } else if (data2.split(" ").length == 11) {//这个是光照传感器
                        String s = handler[5] + " " + handler[6] + " " + handler[7];
                        String t = handler[handler.length - 3] + " " + handler[handler.length - 2];
                        sensor.put("ILLU", s);
                        dataAll.put(s, t);

                    } else if (data2.split(" ").length == 22) {//温湿度传感器

                        String s1 = handler[5] + " " + handler[6] + " " + handler[7];
                        String t1 = handler[8] + " " + handler[9];
                        c = Integer.parseInt(handler[9] + handler[8], 16) * 0.01f;
                        h = Integer.parseInt(handler[20] + handler[19], 16) * 0.01f;
                        sensor.put("TEMP", s1);
                        dataAll.put(s1, t1);
                        String s2 = handler[16] + " " + handler[17] + " " + handler[18];
                        String t2 = handler[19] + " " + handler[20];
                        sensor.put("HUMI", s2);
                        dataAll.put(s2, t2);


                    }
                }
            }
        });
    }

}

