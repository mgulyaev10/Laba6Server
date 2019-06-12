package network;

import collection.CollectionManager;
import collection.Command;
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
    private byte[] receiveData;
    private byte[] sendData;
    private int port;

    public ServerThreadHandler(byte[] data, int port) {
        receiveData = data;
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

        read(receiveData);
        sendData = new byte[65536];
        write(client);
    }

    private void read(byte[] receiveData) {
        try {
            TransferPackage received = TransferPackage.restoreObject(new ByteArrayInputStream(receiveData));

            if (received.getId() == TransferCommandID.CheckingConnectionTP.getId()) {
                client.setPackage(received);
                return;
            }

            if (received.getAdditionalData() != null && received.getCmdData().equals("load"))
                received.setData(CollectionManager.getCollectionFromJson(new String(received.getAdditionalData(), Main.DEFAULT_CHAR_SET)).stream());

            if (received.getCmdData().trim().equals("help")) {
                Command command = Command.parseCmd(received.getCmdData().trim());
                command.setAddress(client.getAddress());
                client.setPackage(command.start(command, received));
                return;
            }
            if (!received.getCmdData().equals("load")) {
                Stream<Troll> userStream = Main.getobjectsLinkedDeque().stream();
                if (userStream.count() == 0) {
                    client.setPackage(new TransferPackage(-1, "Прежде чем работать с вашей коллекцией, загрузите её с помощью комманды 'load' ", null));
                    return;
                }
            }

            Command command = Command.parseCmd(received.getCmdData().trim());
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
                    client.setPackage(command.start(command, received));
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
            client.setPackage(new TransferPackage(-1, "Команда не выполнена. Попробуйте ещё раз.\n" , null));
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
