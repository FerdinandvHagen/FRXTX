/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frxtx;

/**
 *
 * @author ferdinand
 */
public class DEBUG {

    private String suffix;
    private boolean printDebug;

    public DEBUG(String suffix, boolean printDebug) {
        this.printDebug = printDebug;
        this.suffix = suffix;
    }

    public DEBUG(String suffix) {
        this.printDebug = true;
        this.suffix = suffix;
    }

    public void setDebugOutput(boolean printDebug) {
        this.printDebug = printDebug;
    }

    public void error(String message) {
        if (this.printDebug) {
            System.err.println(suffix + ": " + message);
        }
    }

    public void print(String message) {
        if (this.printDebug) {
            System.out.println(suffix + ": " + message);
        }
    }
}
