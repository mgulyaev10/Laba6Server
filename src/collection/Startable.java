package collection;

import network.TransferPackage;

import java.io.UnsupportedEncodingException;

interface Startable {
    void start(Command command, TransferPackage transferPackage) throws UnsupportedEncodingException;
}
