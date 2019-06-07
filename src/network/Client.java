package network;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Client {

    private SocketAddress address;
    private TransferPackage aPackage;
    private int port;

    public Client(int p){
        port = p;
    }

    public Client(InetSocketAddress address, TransferPackage aPackage){
        this.address = address;
        this.aPackage = aPackage;
        port = address.getPort();
    }

    public SocketAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public TransferPackage getPackage() {
        return aPackage;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    public void setPackage(TransferPackage aPackage) {
        this.aPackage = aPackage;
    }
}
