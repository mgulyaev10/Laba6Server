package main;

import network.ServerThreadHandler;
import network.mail.MailSender;
import network.mail.MailService;
import shared.Troll;

import java.io.*;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Main {

    public static final String DEFAULT_CHAR_SET = "UTF-8";
    public static Collection<Troll> objectsLinkedDeque = new ConcurrentLinkedDeque<>();
    public static MailSender sender =
            new MailSender(MailService.MAIL,"collectionmanagerserver@mail.ru","itsmorethana");

    public static boolean writeCollection(Collection<Troll> collection) {
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

    public static Collection<Troll> getobjectsLinkedDeque() {
        return objectsLinkedDeque;
    }

    public static void main(String[] args) {
        Collection<Troll> collectionFromFile = getCollectionFromFile();
        if (collectionFromFile != null)
            objectsLinkedDeque.addAll(collectionFromFile);
        try {
            if (args.length == 0) {
                System.out.println("Введите порт!");
                System.exit(0);
            }

            int port = Integer.parseInt(args[0]);

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
            System.out.println("Что-то пошло не так!");
        }

    }
}
