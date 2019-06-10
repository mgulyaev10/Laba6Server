package main;

import database.DBController;
import database.JDBCConnector;
import database.Pair;
import network.ServerThreadHandler;
import network.mail.MailSender;
import network.mail.MailService;
import shared.Troll;

import java.io.*;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Main {

    public static final String DEFAULT_CHAR_SET = "UTF-8";
    public static Collection<Pair<Troll,String>> objectsLinkedDeque = new ConcurrentLinkedDeque<>();
    public static MailSender sender =
            new MailSender(MailService.MAIL,"collectionmanagerserver@mail.ru","itsmorethana");

    public static boolean writeCollection(Collection<Pair<Troll,String>> collection) {
        try (FileOutputStream writer = new FileOutputStream("Trolls.json");
             ObjectOutputStream oos = new ObjectOutputStream(writer)) {
            oos.writeObject(collection);
        } catch (IOException e) {
            System.err.println("Что-то пошло не так при сохраненнии коллекции!");
            return false;
        }
        System.out.println("Запись успешно завершена.");
        return true;
    }

    @SuppressWarnings("unchecked")
    public static Collection<Troll> getCollectionFromFile() {
        try (FileInputStream reader = new FileInputStream("Trolls.json");
             ObjectInputStream ois = new ObjectInputStream(reader)) {
            return (Collection<Troll>) ois.readObject();
        } catch (FileNotFoundException e) {
            System.err.println("Файл с коллекцией не найден!");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Файл с коллекцией либо был повреждён, либо пустой!");
        }
        return null;
    }

    public static Collection<Pair<Troll,String>> getobjectsLinkedDeque() {
        return objectsLinkedDeque;
    }

    public static JDBCConnector jdbcConnector;
    public static DBController controller;

    static {
        jdbcConnector = new JDBCConnector();
        controller = new DBController(jdbcConnector);
    }

    public static void main(String[] args) {

        try {
            if (args.length == 0) {
                System.out.println("Введите порт!");
                System.exit(0);
            }

            int port = Integer.parseInt(args[0]);

            objectsLinkedDeque.clear();
            objectsLinkedDeque.addAll(controller.getTrollsFromDB());

            DatagramSocket socket = new DatagramSocket(port);
            byte[] buff = new byte[65536];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while (true) {
                socket.receive(packet);
                byte[] data = packet.getData();
                int port1 = packet.getPort();
                Thread t = new Thread(new ServerThreadHandler(data, port1));
                t.start();
            }

        } catch (BindException e) {
            System.err.println("Ошибка: Порт уже занят!");
        } catch (IOException e) {
            System.err.println("Что-то пошло не так!"+e.getMessage());
        } catch (SQLException e){
            System.err.println("Произошла ошибка при восстановлении коллекции");
        }

    }
}
