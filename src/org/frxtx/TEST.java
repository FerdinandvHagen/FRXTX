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
public class TEST implements Runnable {

    private InputStream inputStream;

    public static void main(String[] args) {
        Runnable runnable = new TEST();
        new Thread(runnable).start();
        System.out.println("main finished");
    }

    public TEST() {
        System.out.println("Test!!");
    }

    @Override
    public void run() {
        FRXTX rxtx = new FRXTX();

        List<String> ports = rxtx.getAvailablePorts();

        for (String port : ports) {
            System.out.println(port);
        }

        rxtx.openPort("COM3", 9600, FRXTX.DATABITS_8, FRXTX.STOPBITS_1, FRXTX.PARITY_NONE);

        /**
         * Alternative mit Callback: inputStream = rxtx.getInputStream();
         * rxtx.openPort("COM3", 9600, FRXTX.DATABITS_8, FRXTX.STOPBITS_1,
         * FRXTX.PARITY_NONE, new mySerialListener);
         */
        System.out.println("OpenedPort");

        rxtx.sendMessage("Hallo, dies ist ein Test");

        System.out.println("SendMessage");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.println(rxtx.receiveMessage());
        System.out.println("2: " + rxtx.receiveMessage());
        System.exit(0);
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