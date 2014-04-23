/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frxtx;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

/**
 *
 * @author ferdinand
 */
public class FRXTX {

    private List<String> ports;
    String receivedMessage = "";
    private boolean portOpen = false;
    private CommPortIdentifier serialPortId;
    private Enumeration enumComm;
    private SerialPort serialPort;
    private OutputStream outputStream;
    InputStream inputStream;

    public FRXTX() {
        //First neccessary: Loading the important natives
        System.out.println("System detected: " + getOsName() + "; Using " + getDataModel() + "-bit Version;");

        String nativelib = "";

        if (getOsName() == "windows") {
            nativelib = createTemp("/libs/rxtxSerialx" + getDataModel() + ".dll", "rxtxSerial.dll");
        } else {
            nativelib = createTemp("/libs/librxtxSerial.so", "librxtxSerial.so");
        }

        try {
            addLibraryPath(nativelib.substring(0, nativelib.lastIndexOf(File.separator)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (nativelib.length() > 1) {
            System.load(nativelib);
            System.out.println("Native load successfully");
        }

        ports = new ArrayList<String>();
        listPorts();
    }

    public List<String> getAvailablePorts() {
        return ports;
    }

    public boolean openPort(String port, int baudrate, int dataBits, int stopBits, int parity) {
        return openPort(port, baudrate, dataBits, stopBits, parity, new serialPortEventListener());
    }

    public InputStream getInputStream() {
        if (portOpen) {
            return inputStream;
        }
        System.err.println("no InputStream available");
        return null;
    }

    public boolean openPort(String port, int baudrate, int dataBits, int stopBits, int parity, SerialPortEventListener listener) {
        if (portOpen) {
            System.err.println("Port already opened");
            return false;
        }

        boolean foundPort = false;
        enumComm = CommPortIdentifier.getPortIdentifiers();
        while (enumComm.hasMoreElements()) {
            serialPortId = (CommPortIdentifier) enumComm.nextElement();
            if (port.contentEquals(serialPortId.getName())) {
                foundPort = true;
                break;
            }
        }
        if (foundPort != true) {
            System.err.println("Serialport not found: " + port);
            return false;
        }

        try {
            serialPort = (SerialPort) serialPortId.open("Öffnen und Senden", 500);
        } catch (PortInUseException e) {
            System.err.println("Port already in use");
            return false;
        }

        try {
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            System.err.println("No access to OutputStream");
            return false;
        }

        try {
            inputStream = serialPort.getInputStream();
        } catch (IOException e) {
            System.err.println("No access to InputStream");
        }

        try {
            serialPort.addEventListener(listener);
        } catch (TooManyListenersException e) {
            System.out.println("TooManyListenersException on Serialport");
        }
        serialPort.notifyOnDataAvailable(true);

        try {
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
        } catch (UnsupportedCommOperationException e) {
            System.err.println("Couldn't set parameters for serial connection");
            return false;
        }

        portOpen = true;
        return true;
    }

    public boolean sendMessage(String message) {
        if (!portOpen) {
            return false;
        }
        try {
            outputStream.write(message.getBytes());
        } catch (IOException e) {
            System.err.println("Error sending");
            return false;
        }
        return true;
    }

    public String receiveMessage() {
        String returnString = receivedMessage + "";
        receivedMessage = "";
        return returnString;
    }

    public boolean closeSerialPort() {
        if (portOpen == true) {
            System.out.println("closing Serialport");
            serialPort.close();
            portOpen = false;
            return true;
        } else {
            System.err.println("Serialport already closed");
            return false;
        }
    }

    private void listPorts() {
        ports.clear();

        CommPortIdentifier serialPortId;
        Enumeration enumComm;

        enumComm = CommPortIdentifier.getPortIdentifiers();
        while (enumComm.hasMoreElements()) {
            serialPortId = (CommPortIdentifier) enumComm.nextElement();
            if (serialPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                ports.add(serialPortId.getName());
            }
        }
    }

    public static final int DATABITS_5 = 5;
    public static final int DATABITS_6 = 6;
    public static final int DATABITS_7 = 7;
    public static final int DATABITS_8 = 8;
    public static final int PARITY_NONE = 0;
    public static final int PARITY_ODD = 1;
    public static final int PARITY_EVEN = 2;
    public static final int PARITY_MARK = 3;
    public static final int PARITY_SPACE = 4;
    public static final int STOPBITS_1 = 1;
    public static final int STOPBITS_2 = 2;
    public static final int STOPBITS_1_5 = 3;
    public static final int FLOWCONTROL_NONE = 0;
    public static final int FLOWCONTROL_RTSCTS_IN = 1;
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;
    public static final int FLOWCONTROL_XONXOFF_IN = 4;
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;

    private void readSerialPortData() {
        try {
            byte[] data = new byte[150];
            int num;
            while (inputStream.available() > 0) {
                num = inputStream.read(data, 0, data.length);
                receivedMessage += new String(data, 0, num);
            }
        } catch (IOException e) {
            System.err.println("Error reading received message");
        }
    }

    private class serialPortEventListener implements SerialPortEventListener {

        public void serialEvent(SerialPortEvent event) {
            switch (event.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
                    readSerialPortData();
                    break;
                case SerialPortEvent.BI:
                case SerialPortEvent.CD:
                case SerialPortEvent.CTS:
                case SerialPortEvent.DSR:
                case SerialPortEvent.FE:
                case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                case SerialPortEvent.PE:
                case SerialPortEvent.RI:
                default:
            }
        }
    }

    private String getDataModel() {
        if (System.getProperty("sun.arch.data.model").matches("64")) {
            return "64";
        }
        return "32";
    }

    private String getOsName() {
        String os = "";
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            os = "windows";
        } else if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
            os = "linux";
        } else if (System.getProperty("os.name").toLowerCase().indexOf("mac") > -1) {
            os = "mac";
        }

        return os;
    }

    private String createTemp(String dll, String name) {
        try {
            InputStream in = FRXTX.class.getResourceAsStream(dll);
            if (in == null) {
                System.err.println("Resource " + name + " not found");
            } else {
                File file = new File(name);
                file.deleteOnExit();
                OutputStream out = new FileOutputStream(file);

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                return file.getAbsolutePath();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static void addLibraryPath(String pathToAdd) throws Exception {
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[]) usrPathsField.get(null);

        //check if the path to add is already present
        for (String path : paths) {
            if (path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
}
