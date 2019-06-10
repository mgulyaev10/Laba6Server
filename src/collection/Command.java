package collection;

import main.Main;
import network.TransferPackage;
import org.json.JSONException;
import org.json.JSONObject;
import shared.Troll;

import java.io.*;
import java.net.SocketAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс, предназначенный для парсинга и выполнения команд.
 */
public enum Command {

    @SuppressWarnings("unchecked")
    REMOVE((command, transferPackage) -> {
        try {
            StringBuilder builder = new StringBuilder();
            for (Object s : command.data.toArray()) builder.append(s.toString());
            String strData = builder.toString();
            if (strData.length() == 0) {
                command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                        "Отсутствует аргумент команды!".getBytes(Main.DEFAULT_CHAR_SET))));
                return;
            }
            JSONObject jsonObject = new JSONObject(strData);
            Troll troll = new Troll(jsonObject);

            command.setData(null);
            Stream<Troll> stream = command.getObjectsArrayDeque().stream().filter(p -> !p.equals(troll));
            ArrayDeque<Troll> trolls = new ArrayDeque<>();
            stream.sequential().collect(Collectors.toCollection(() -> trolls));

            command.getObjectsArrayDeque().clear();
            command.getObjectsArrayDeque().addAll(trolls);

            Main.writeCollection(Main.getobjectsLinkedDeque());

            Collection<Troll> trollsCollection = new ArrayDeque<>();
            trolls.forEach(p -> trollsCollection.add(p));
            command.setData(Stream.of(new TransferPackage(1, "Команда выполнена.", null)));
            System.out.println("Команда выполнена.");
        } catch (JSONException e) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                    "Аргумент команды неверный!".getBytes(Main.DEFAULT_CHAR_SET))));
        }

    }),

    REMOVE_LOWER((command, transferPackage) -> {
        StringBuilder builder = new StringBuilder();
        for (Object s : command.data.toArray()) builder.append(s.toString());
        String strData = builder.toString();
        if (strData.length() == 0) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                    "Отсутствует аргумент команды!".getBytes(Main.DEFAULT_CHAR_SET))));
            return;
        }
        JSONObject jsonObject = new JSONObject(strData);
        Troll troll = new Troll(jsonObject);

        command.setData(null);
        Stream<Troll> stream = command.getObjectsArrayDeque().stream().filter(p -> p.compareTo(troll) >= 0);

        ArrayDeque<Troll> trolls = new ArrayDeque<>();

        stream.sequential().collect(Collectors.toCollection(() -> trolls));

        command.getObjectsArrayDeque().clear();
        command.getObjectsArrayDeque().addAll(trolls);

        Main.writeCollection(Main.getobjectsLinkedDeque());

        Collection<Troll> trollsCollection = new ArrayDeque<>();
        trolls.forEach(p -> trollsCollection.add(p));
        command.setData(Stream.of(new TransferPackage(10, "Команда выполнена.", null)));
        System.out.println("Команда выполнена.");
    }),

    SHOW((command, transferPackage) -> {
        command.setData(null);
        List<Troll> trolls = command.getObjectsArrayDeque().stream().collect(Collectors.toList());
        final String[] output = {""};
        trolls.forEach(p -> output[0] += p.toString() + "\t");
        command.setData(Stream.of(new TransferPackage(2, "Команда выполнена.", null, output[0].getBytes(Main.DEFAULT_CHAR_SET))));
        System.out.println("Команда выполнена.");
    }),

    CLEAR((command, transferPackage) -> {
        command.setData(null);
        command.getObjectsArrayDeque().clear();
        command.setData(Stream.of(new TransferPackage(3, "Команда выполнена.", null)));
    }),

    LOAD((command, transferPackage) -> {
        Stream<Troll> concatStream = Stream.concat(command.getObjectsArrayDeque().stream(), transferPackage.getData());
        ArrayDeque<Troll> trolls = concatStream.distinct().collect(Collectors.toCollection(ArrayDeque::new));
        command.getObjectsArrayDeque().clear();
        command.getObjectsArrayDeque().addAll(trolls);
        command.setData(Stream.of(new TransferPackage(4, "Команда выполнена.", null, "Load collection to server".getBytes(Main.DEFAULT_CHAR_SET))));
        Main.writeCollection(Main.getobjectsLinkedDeque());
        System.out.println("Команда выполнена.");
    }),

    INFO((command, transferPackage) -> {
        Collection<Troll> collection = new ArrayDeque<>();
        Stream<Troll> stream = command.getObjectsArrayDeque().stream();
        stream.collect(Collectors.toCollection(() -> collection));
        try (ByteArrayOutputStream byteObject = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteObject)) {
            objectOutputStream.writeObject(collection);
            objectOutputStream.flush();
            objectOutputStream.close();
            byteObject.close();
            command.setData(Stream.of(new TransferPackage(5, "Команда выполнена.", null,
                    String.format(
                            "Тип коллекции: %s \nТип элементов коллекции: %s\nДата инициализации: %s\nКоличество элементов: %s\n",
                            collection.getClass().getName(),
                            "Troll", new Date().toString(), collection.size()
                    ).getBytes(Main.DEFAULT_CHAR_SET))));

        } catch (IOException e) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда выполнена.", null,
                    "Ошибка при выполнении команды info.".getBytes(Main.DEFAULT_CHAR_SET))));
        }
        System.out.println("Команда выполнена.");
    }),

    IMPORT((command, transferPackage) -> {
        String path = "";
        for (Object s : command.data.toArray()) path += s.toString();
        if (path.length() == 0) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                    "Отсутствует аргумент команды!".getBytes(Main.DEFAULT_CHAR_SET))));
            return;
        }
        command.setData(Stream.of(new TransferPackage(6, "Команда выполнена.", null, path.getBytes(Main.DEFAULT_CHAR_SET))));
        System.out.println("Первый этап импорта пройден.");
    }),

    @SuppressWarnings("unchecked")
    SET_PATH_IMPORT((command, transferPackage) -> {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(transferPackage.getAdditionalData());
             ObjectInputStream dis = new ObjectInputStream(bis)) {
            Collection<Troll> mainCollection = (Collection<Troll>) dis.readObject();
            ArrayDeque<Troll> collection = new ArrayDeque<>();
            mainCollection.forEach(p -> collection.add(p));
            command.getObjectsArrayDeque().addAll(collection);
            command.setData(Stream.of(new TransferPackage(601, "Команда выполнена.", null)));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }),

    ADD((command, transferPackage) -> {
        try {
            StringBuilder builder = new StringBuilder();
            for (Object s : command.data.toArray()) builder.append(s.toString());
            String strData = builder.toString();
            if (strData.length() == 0) {
                command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                        "Отсутствует аргумент команды!".getBytes(Main.DEFAULT_CHAR_SET))));
                return;
            }
            JSONObject jsonObject = new JSONObject(strData);
            Troll Troll = new Troll(jsonObject);

            command.setData(null);

            Stream<Troll> mainStream = command.getObjectsArrayDeque().stream();
            Stream<Troll> stream = Stream.concat(mainStream, Stream.of(Troll));

            ArrayDeque<Troll> trolls = new ArrayDeque<>();

            stream.sequential().collect(Collectors.toCollection(() -> trolls));
            command.getObjectsArrayDeque().clear();
            command.getObjectsArrayDeque().addAll(trolls);

            Main.writeCollection(Main.getobjectsLinkedDeque());

            command.setData(Stream.of(new TransferPackage(7, "Команда выполнена.", null)));
            System.out.println("Команда выполнена.");
        } catch (JSONException e) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                    "Аргумент команды неверный!".getBytes(Main.DEFAULT_CHAR_SET))));
        }
    }),

    EXIT((command, transferPackage) -> {
        command.setData(Stream.of(new TransferPackage(9, "Команда выполнена.", null, "null".getBytes(Main.DEFAULT_CHAR_SET))));
        System.out.println("Команда выполнена.");
    }),

    HELP((command, transferPackage) -> {
        command.setData(Stream.of(new TransferPackage(11, "Команда выполнена.", null,
                (
                        "\"remove_last\": удалить последний элемент из коллекции\n" +
                                "\"remove {element}\": удалить элемент из коллекции по его значению\n" +
                                "\"clear\": очистить коллекцию\n" +
                                "\"info\": вывести в стандартный поток вывода информацию о коллекции " +
                                "(тип, дата инициализации, количество элементов и т.д.)\n" +
                                "\"remove_lower {element}\": удалить из коллекции все элементы, меньшие, чем заданный\n" +
                                "\"add {element}\": добавить новый элемент в коллекцию\n" +
                                "\"show\": вывести в стандартный поток вывода все элементы коллекции в строковом представлении\n" +
                                "\"help\": получить информацию о доступных командах\n" +
                                "\"exit\": выйти из программы\n" +
                                "\"import {path}\": добавить к коллекции объекты из файла\n" +
                                "\"load\": загрузить в коллекцию объекты из файла с перезаписью\n" +
                                "\"save\": сохранить объекты коллекции в файл\n" +
                                "Пример корректного JSON-объекта:\n" +
                                "{\"isSad\":true,\"name\":\"Петя\",\"HP\":100,\"things\":[{\"condition\":\"Solid\"," +
                                "\"name\":\"Соль\",\"weight\":20}],\"isSit\":true,\"age\":10}\n"
                ).getBytes())));
    }),

    CHANGE_DEF_FILE_PATH(((command, transferPackage) -> {
        StringBuilder builder = new StringBuilder();
        for (Object s : command.data.toArray()) builder.append(s.toString());
        String strData = builder.toString();
        if (strData.length() == 0) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                    "Отсутствует аргумент команды!".getBytes(Main.DEFAULT_CHAR_SET))));
            return;
        }
        command.setData(Stream.of(new TransferPackage(10, "Команда выполнена.", null, strData.getBytes(Main.DEFAULT_CHAR_SET))));
    })),

    SAVE(((command, transferPackage) -> {
        Collection<Troll> collection = new ArrayDeque<>();
        Stream<Troll> userStream = command.getObjectsArrayDeque().stream();
        userStream.collect(Collectors.toCollection(() -> collection));
        command.setData(Stream.of(new TransferPackage(12, "Команда выполнена.", null,
                CollectionManager.getBytesFromCollection(collection))));
    }));

    /**
     * Тело выполняемой команды.
     */
    private Startable cmd;
    /**
     * Данные, с которыми оперирует команда.
     */
    private Stream data;

    private SocketAddress address;

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    Command(Startable cmd) {
        this.cmd = cmd;
    }

    public static Command parseCmd(String jsonInput) throws IllegalArgumentException {

        String jsonRegex = "\\{\"isSad\":(true|false),\".+\":\".+\",\".+\":(\\d+),\"things\":\\[(\\{\"condition\":\"(Solid|Gaseous|Liquid)\",\"name\":\".+\",\"weight\":(\\d+)})*],\"isSit\":(true|false),\"age\":(\\d+)}";
        String dataCommandRegex = "(remove|import|add|remove_lower|change_def_file_path) \\{.+}";
        String nodataCommandRegex = "show|load|info|exit|help|save|SET_PATH_IMPORT|clear";

        if (jsonInput.matches(dataCommandRegex)) {
            String cmd = findMatches("(remove_lower|remove|import|add)", jsonInput).get(0).toUpperCase();
            String data;
            if (cmd.equals("IMPORT") || cmd.equals("CHANGE_DEF_FILE_PATH")) {
                data = jsonInput.split(" ")[1].substring(1, jsonInput.split(" ")[1].length() - 1);
            } else {
                ArrayList<String> list = findMatches(jsonRegex, jsonInput);
                if (list.size() != 0)
                    data = list.get(0);
                else
                    return null;
            }
            Command command = Command.valueOf(cmd);
            command.setData(Stream.of(data));
            return command;
        } else if (jsonInput.matches(nodataCommandRegex)) {
            Command command = Command.valueOf(jsonInput.toUpperCase());
            return command;
        } else {
            return null;
        }
    }

    public static ArrayList<String> findMatches(String patterStr, String text) {
        Pattern pattern = Pattern.compile(patterStr);
        Matcher matcher = pattern.matcher(text);
        ArrayList<String> collection = new ArrayList<>();
        while (matcher.find()) {
            collection.add(text.substring(matcher.start(), matcher.end()));
        }
        return collection;
    }

    private void setData(Stream data) {
        this.data = data;
    }

    public Collection<Troll> getObjectsArrayDeque() {
        return Main.getobjectsLinkedDeque();
    }

    @SuppressWarnings("unchecked")
    public TransferPackage start(Command command, TransferPackage transferPackage) {
        try {
            this.cmd.start(command, transferPackage);
        } catch (UnsupportedEncodingException e) {
            System.err.println(e.getMessage());
        }
        Object obj = data.findFirst().orElse(null);
        return (TransferPackage) obj;
    }
}
