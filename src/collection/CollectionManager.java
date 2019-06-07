package collection;

import filesystem.EmptyFileException;
import org.json.JSONArray;
import org.json.JSONObject;
import shared.Troll;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Класс, с помощью которого организуется управление коллекцией
 */
public class CollectionManager {
    /**
     * Метод, получающий коллекцию из строки в формате json
     *
     * @return Collection\<Troll\>
     */
    public static Collection<Troll> getCollectionFromJson(String json) throws EmptyFileException {

        Deque<Troll> trolls = new ArrayDeque<>();
        if (json.length() == 0) {
            throw new EmptyFileException();
        } else {
            JSONArray array = new JSONArray(json);
            array.forEach(o -> trolls.add(new Troll((JSONObject) o)));
        }

        return trolls;
    }


    public static byte[] getBytesFromCollection(Collection<Troll> costumes) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(costumes);
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Ошибка: не удалось сериализовать коллекцию");
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public static Deque<Troll> getCollectionFromBytes(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Deque<Troll>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка: не удалось сериализовать коллекцию");
        }
        return null;
    }

    public static String getJsonFromCollection(Deque<Troll> collection) {
        JSONArray array = new JSONArray();
        collection.forEach(p -> array.put(p.getJSON()));
        return array.toString();
    }


}

