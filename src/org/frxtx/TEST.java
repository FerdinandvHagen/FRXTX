/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frxtx;

import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author ferdinand
 */
public class TEST {

    private InputStream inputStream;

    public static void main(String[] args) {
        new TEST();
    }

    public TEST() {
        System.out.println("Test!!");

        FRXTX rxtx = new FRXTX();

        /**
         * available Ports are stored in a java.util.List
         *
         * List<String> ports = rxtx.getAvailablePorts();
         */
        for (String port : rxtx.getAvailablePorts()) {
            System.out.println(port);
        }

        //Open the COM-PORT: name, baudrate, Databits, Stopbits, parity
        rxtx.openPort("COM3", 9600, FRXTX.DATABITS_8, FRXTX.STOPBITS_1, FRXTX.PARITY_NONE);

        /**
         * Alternative with Callback: rxtx.openPort("COM3", 9600, FRXTX.DATABITS_8, FRXTX.STOPBITS_1, FRXTX.PARITY_NONE, new mySerialListener());
         *
         * The InputStream is needed to read data in callback
         * inputStream = rxtx.getInputStream();
         */
        
        //Sending message, nothing Special
        rxtx.sendMessage("Hallo, dies ist ein Test");

        /**
         * Of course you can send a byte Array, too:
         * 
         * byte[] b = {'H','A','L','L','O'};
         * rxtx.sendMessage(b);
         */

        while (true) {
            String rs = rxtx.receiveMessage();
            if (!rs.isEmpty()) {
                System.out.println("Received: " + rs);
            }
        }
    }

    private class mySerialListener implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent e) {
            if (e.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {
                    byte[] data = new byte[150];
                    int num;
                    while (inputStream.available() > 0) {
                        num = inputStream.read(data, 0, data.length);
                        System.out.println("Received: " + new String(data, 0, num));
                    }
                } catch (IOException ex) {
                    System.err.println("Error reading received message");
                }
            }
        }
    }
}
