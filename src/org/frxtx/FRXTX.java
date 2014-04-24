package org.frxtx;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
 * @author Ferdinand von Hagen
 */
public class FRXTX {
    
    private List<String> ports;
    
    private String receivedMessage = "";
    
    private boolean portOpen = false;
    private CommPortIdentifier serialPortId;
    private Enumeration enumComm;
    private SerialPort serialPort;
    private OutputStream outputStream;
    InputStream inputStream;
    String nativelib = "";
    DEBUG debug;
    
    boolean flag = true;
    
    public FRXTX(boolean printDebug) {
        init(printDebug);
    }
    
    public FRXTX() {
        init(false);
    }
    
    private void init(boolean printDebug) {
        //Initialize debugging
        debug = new DEBUG("FRXTX", printDebug);

        //First neccessary: Loading the important natives
        debug.print("System detected: " + getOsName() + "; Using " + getDataModel() + "-bit Version;");
        
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
            debug.print("Native loaded");
        }
    }
    
    public List<String> getAvailablePorts() {
        ports = new ArrayList<String>();
        listPorts();
        return ports;
    }
    
    public boolean openPort(String port, int baudrate, int dataBits, int stopBits, int parity) {
        return openPort(port, baudrate, dataBits, stopBits, parity, new serialPortEventListener());
    }
    
    public InputStream getInputStream() {
        if (portOpen) {
            return inputStream;
        }
        debug.error("No InputStream available");
        return null;
    }
    
    public boolean openPort(String port, int baudrate, int dataBits, int stopBits, int parity, SerialPortEventListener listener) {
        if (portOpen) {
            debug.error("Port already opened");
            return false;
        }

        //Search the correct Port
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
            debug.error("Serialport not found: " + port);
            return false;
        }

        //Get  handle to that Port
        try {
            serialPort = (SerialPort) serialPortId.open("Öffnen und Senden", 500);
        } catch (PortInUseException e) {
            debug.error("Port already in use");
            return false;
        }

        //Get the In and OutputStreams
        try {
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            debug.error("No access to OutputStream");
            return false;
        }
        try {
            inputStream = serialPort.getInputStream();
        } catch (IOException e) {
            debug.error("No access to InputStream");
        }

        //Register an EnventListener for Receiving messages
        try {
            serialPort.addEventListener(listener);
        } catch (TooManyListenersException e) {
            debug.error("TooManyListenersException on Serialport");
        }
        serialPort.notifyOnDataAvailable(true);

        //And set the proper connection details
        try {
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
        } catch (UnsupportedCommOperationException e) {
            debug.error("Couldn't set parameters for serial connection");
            return false;
        }
        
        debug.print("Port opened");
        portOpen = true;
        return true;
    }
    
    public boolean sendMessage(byte[] message) {
        if (!portOpen) {
            return false;
        }
        try {
            outputStream.write(message);
        } catch (IOException e) {
            debug.error("Error sending: " + e.getMessage());
            return false;
        }
        debug.print("Message send");
        return true;
    }
    
    public boolean sendMessage(String message) {
        return sendMessage(message.getBytes());
    }
    
    public synchronized String receiveMessage() {
        if (flag) {
            flag = false;
        } else {
            try {
                wait();
            } catch (InterruptedException e) {
                debug.print("aufgewacht");
            }
            flag = false;
        }
        String returnString = receivedMessage + "";
        receivedMessage = "";
        flag = true;
        notify();
        return returnString;
    }
    
    public boolean closeSerialPort() {
        if (portOpen == true) {
            debug.print("closing Serialport");
            serialPort.close();
            portOpen = false;
            return true;
        } else {
            debug.error("Serialport already closed");
            return false;
        }
    }
    
    private void listPorts() {
        debug.print("Listing available Ports");
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
    
    private synchronized void readSerialPortData() {
        debug.print("Saving received Message");
        String rs = "";
        try {
            byte[] data = new byte[150];
            int num;
            
            while (inputStream.available() > 0) {
                num = inputStream.read(data, 0, data.length);
                rs += new String(data, 0, num);
            }
        } catch (IOException e) {
            debug.error("Error reading received message: " + e.getMessage());
        }
        
        debug.print("readSerialData: " + rs);
        if (flag) {
            flag = false;
        } else {
            try {
                wait();
            } catch (InterruptedException ex) {
                debug.print("read aufgewacht");
            }
            flag = false;
        }
        receivedMessage += rs;
        flag = true;
        notify();
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
                debug.error("Resource " + name + " not found");
            } else {
                File file = new File(System.getProperty("java.io.tmpdir") + name);
                if (file.exists()) {
                    if (checkLibs(file, in)) {
                        in.close();
                        debug.print("Correct Library already exists");
                        return file.getAbsolutePath();
                    }
                    in = FRXTX.class.getResourceAsStream(dll);
                }
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
    
    private boolean checkLibs(File file, InputStream in) throws FileNotFoundException, IOException {
        InputStream in2 = new FileInputStream(file);
        byte[] buf = new byte[1024];
        byte[] buf2 = new byte[1024];
        int len;
        int len2;
        while ((len2 = in2.read(buf2)) > 0 && (len = in.read(buf)) > 0) {
            if (!Arrays.equals(buf, buf2)) {
                in2.close();
                return false;
            }
        }
        return true;
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
