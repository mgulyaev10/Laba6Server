package network;

import collection.CollectionManager;
import collection.Command;
import database.Pair;
import filesystem.EmptyFileException;
import main.Main;
import shared.Troll;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.stream.Stream;

public class ServerThreadHandler implements Runnable {

    private static Command previousCmd = null;
    private Client client;
    private DatagramSocket socket;
    private byte[] recievedata;
    private byte[] sendData;
    private int port;

    public ServerThreadHandler(byte[] data, int port) {
        recievedata = data;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            client = new Client(new InetSocketAddress("localhost", port), null);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            socket = new DatagramSocket();
        } catch (IOException e) {
            System.err.println("Ошибка при создании потока для получения и отправки сообщения.");
        }

        read(recievedata);
        sendData = new byte[65536];
        write(client);
    }

    private void read(byte[] recievedata) {
        try {
            TransferPackage recieved = TransferPackage.restoreObject(new ByteArrayInputStream(recievedata));

            if (recieved.getId() == TransferCommandID.CheckingConnectionTP.getId()) {
                client.setPackage(recieved);
                return;
            }

            if (recieved.getAdditionalData() != null && recieved.getCmdData().equals("load"))
                recieved.setData(CollectionManager.getCollectionFromJson(new String(recieved.getAdditionalData(), Main.DEFAULT_CHAR_SET)).stream());

            User user = recieved.getUser();

            if (user!=null && !recieved.getCmdData().equals("load")){
                Stream<Pair<Troll,String>> userStream = Main.getobjectsLinkedDeque().stream().filter(p->p.getValue().equals(user.getLogin()));
                if (userStream.count()==0){
                    client.setPackage(new TransferPackage(-1, "Прежде чем работать с вашей коллекцией, загрузите её с помощью комманды 'load' ", null));
                    return;
                }
            }
            if (recieved.getCmdData().trim().equals("help")) {
                Command command = Command.parseCmd(recieved.getCmdData().trim());
                command.setAddress(client.getAddress());
                client.setPackage(command.start(command, recieved));
                return;
            }

            Command command = Command.parseCmd(recieved.getCmdData().trim());
            if (previousCmd != null)
                System.out.println("Previous CMD : " + previousCmd.toString());
            if (command == null)
                client.setPackage(new TransferPackage(-1, "Неверная команда!", null));
            else {
                System.out.println("Current CMD : " + command.toString());
                if (command == Command.SET_PATH_IMPORT && previousCmd != Command.IMPORT) {
                    client.setPackage(new TransferPackage(-1, "Неверная команда!", null));
                } else {
                    command.setAddress(client.getAddress());
                    client.setPackage(command.start(command, recieved));
                    System.out.println("Collection size: " + Main.getobjectsLinkedDeque().size());
                }
            }

            if (client.getAddress() != null) {
                previousCmd = command;
            }
        } catch (IOException e) {
            System.err.println("Ошибка при получении!");
        } catch (IllegalArgumentException e) {
            try {
                client.setPackage(new TransferPackage(-1, "Команда не выполнена.", null,
                        ("Ошибка: Неверная команда.").getBytes(Main.DEFAULT_CHAR_SET)));
            } catch (UnsupportedEncodingException e1) {
                System.err.println(e1.getMessage());
            }
        } catch (EmptyFileException e) {
            try {
                client.setPackage(new TransferPackage(-1, "Команда не выполнена.", null,
                        ("Файл с коллекцией пуст!").getBytes(Main.DEFAULT_CHAR_SET)));
            } catch (UnsupportedEncodingException e1) {
                System.err.println(e1.getMessage());
            }
        } catch (NullPointerException e) {
            client.setPackage(new TransferPackage(-1, "Команда не выполнена. Попробуйте ещё раз." , null));
            return;
        }
    }

    private void write(Client client) {
        try {
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), client.getPort());
            if (client.getPackage() != null) {
                packet.setData(client.getPackage().getBytes());
                socket.send(packet);
            } else {
                packet.setData(new TransferPackage(TransferCommandID.ERROR.getId(), "Ошибка: команда не найдена",
                        null).getBytes());
                socket.send(packet);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при отправке!");
        }
    }


}
