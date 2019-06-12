package collection;

import database.Pair;
import main.Main;
import network.TransferPackage;
import org.json.JSONException;
import org.json.JSONObject;
import shared.Troll;

import java.io.*;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
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

            Main.controller.synchronizeCollection();
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

            User user = transferPackage.getUser();
            command.setData(null);

            Stream<Pair<Troll, String>> userStream = command.getObjectsArrayDeque().stream().filter(p -> p.getValue().equals(user.getLogin()) && !p.getKey().equals(troll));
            Stream<Pair<Troll, String>> otherStream = command.getObjectsArrayDeque().stream().filter(p -> !p.getValue().equals(user.getLogin()));
            Collection<Pair<Troll, String>> userTrolls = new ArrayDeque<>();
            Collection<Pair<Troll, String>> otherTrolls = new ArrayDeque<>();

            userStream.collect(Collectors.toCollection(() -> userTrolls));
            otherStream.collect(Collectors.toCollection(() -> otherTrolls));
            otherTrolls.addAll(userTrolls);
            command.getObjectsArrayDeque().clear();
            command.getObjectsArrayDeque().addAll(otherTrolls);

            Main.writeCollection(Main.getObjectsLinkedDeque());
            Main.controller.reloadCollection();

            command.setData(Stream.of(new TransferPackage(1, "Команда выполнена.", null)));
            System.out.println("Команда выполнена.");
        } catch (JSONException e) {
            command.setData(Stream.of(new TransferPackage(-1, "Команда не выполнена.", null,
                    "Аргумент команды неверный!".getBytes(Main.DEFAULT_CHAR_SET))));
        }

    }),

    REMOVE_LOWER((command, transferPackage) -> {
        Main.controller.synchronizeCollection();
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
        User user = transferPackage.getUser();
        Stream<Pair<Troll, String>> stream = command.getObjectsArrayDeque().stream().filter((p) -> p.getKey().compareTo(troll) >= 0 && p.getValue().equals(user.getLogin()));
        Collection<Pair<Troll, String>> trolls = new ArrayDeque<>();
        stream.collect(Collectors.toCollection(() -> trolls));
        command.getObjectsArrayDeque().clear();
        command.getObjectsArrayDeque().addAll(trolls);
        Main.writeCollection(Main.getObjectsLinkedDeque());
        Main.controller.reloadCollection();
        command.setData(Stream.of(new TransferPackage(10, "Команда выполнена.", null)));
        System.out.println("Команда выполнена.");
    }),

    SHOW_MY((command, transferPackage) -> {
        Main.controller.synchronizeCollection();
        command.setData(null);
        User user = transferPackage.getUser();
        List<Pair<Troll,String>> trolls = command.getObjectsArrayDeque().stream().filter((p) -> p.getValue().equals(user.getLogin())).collect(Collectors.toList());
        String[] output = new String[]{""};
        trolls.forEach(p->output[0]+=p.getKey().toString()+"; Создан: "+p.getValue()+"\n");
        command.setData(Stream.of(new TransferPackage(2, "Команда выполнена.", null, output[0].getBytes(StandardCharsets.UTF_8))));
        System.out.println("Команда выполнена.");
    }),

    SHOW_ALL((command, transferPackage) -> {
        Main.controller.synchronizeCollection();
        command.setData(null);
        User user = transferPackage.getUser();
        List<Pair<Troll,String>> trolls = command.getObjectsArrayDeque().stream().collect(Collectors.toList());
        String[] output = new String[]{""};
        trolls.forEach(p->output[0]+=p.getKey().toString()+"; Создан: "+p.getValue()+"\n");
        command.setData(Stream.of(new TransferPackage(14, "Команда выполнена.", null, output[0].getBytes(StandardCharsets.UTF_8))));
        System.out.println("Команда выполнена.");
    }),

    CLEAR((command, transferPackage) -> {
        Main.controller.synchronizeCollection();
        command.setData(null);
        User user = transferPackage.getUser();
        Stream<Pair<Troll, String>> stream = command.getObjectsArrayDeque().stream().filter((p) -> !p.getValue().equals(user.getLogin()));
        Collection<Pair<Troll, String>> trolls = new ArrayDeque<>();
        stream.collect(Collectors.toCollection(() -> trolls));
        command.getObjectsArrayDeque().clear();
        command.getObjectsArrayDeque().addAll(trolls);
        Main.writeCollection(Main.getObjectsLinkedDeque());
        Main.controller.reloadCollection();
        command.setData(Stream.of(new TransferPackage(3, "Команда выполнена.",null)));
    }),

    LOAD((command, transferPackage) -> {
        Main.controller.synchronizeCollection();
        User user = transferPackage.getUser();
        Stream<Pair<Troll,String>> stream = transferPackage.getData().map(p->new Pair<>(p,user.getLogin()));
        ArrayDeque<Pair<Troll,String>> collection = new ArrayDeque<>(stream.collect(Collectors.toCollection(ArrayDeque::new)));
        command.getObjectsArrayDeque().addAll(collection);
        Main.controller.addTrollsCollectionToDB(collection.stream().map(Pair::getKey).collect(Collectors.toCollection(ArrayDeque::new)), user);
        Main.writeCollection(Main.getObjectsLinkedDeque());
        Main.controller.reloadCollection();
        command.setData(Stream.of(new TransferPackage(4, "Команда выполнена.", null, "Load collection to server".getBytes("UTF-8"))));
        System.out.println("Команда выполнена.");
    }),

    INFO((command, transferPackage) -> {
        Main.controller.synchronizeCollection();
        Collection<Troll> collection = new ArrayDeque<>();
        User user = transferPackage.getUser();
        Stream<Pair<Troll,String>> userStream = command.getObjectsArrayDeque().stream().filter(p->p.getValue().equals(user.getLogin()));

        userStream.map(Pair::getKey).collect(Collectors.toCollection(()->collection));

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
        Main.controller.synchronizeCollection();
        User user = transferPackage.getUser();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(transferPackage.getAdditionalData());
             ObjectInputStream dis = new ObjectInputStream(bis)) {
            Collection<Troll> mainCollection = (Collection<Troll>) dis.readObject();
            Collection<Pair<Troll,String>> collection = new ArrayDeque<>();
            mainCollection.forEach(p->collection.add(new Pair<>(p,user.getLogin())));
            collection.forEach(p->Main.controller.addTrollToDb(p.getKey(),user));
            command.getObjectsArrayDeque().addAll(collection);
            command.setData(Stream.of(new TransferPackage(601, "Команда выполнена.", null)));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }),

    ADD((command, transferPackage) -> {
        try {
            Main.controller.synchronizeCollection();
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
            User user = transferPackage.getUser();

            Stream<Pair<Troll, String>> userStream = command.getObjectsArrayDeque().stream().filter(p -> p.getValue().equals(user.getLogin()));
            Stream<Pair<Troll, String>> otherStream= command.getObjectsArrayDeque().stream().filter(p -> !p.getValue().equals(user.getLogin()));
            Stream<Pair<Troll, String>> stream = Stream.concat(userStream, Stream.of(new Pair<>(Troll, user.getLogin())));

            HashSet<Pair<Troll, String>> userTrolls = new HashSet<>();
            HashSet<Pair<Troll, String>> otherTrolls = new HashSet<>();
            stream.sequential().collect(Collectors.toCollection(() -> userTrolls));
            otherStream.sequential().collect(Collectors.toCollection(()->otherTrolls));
            userTrolls.addAll(otherTrolls);
            command.getObjectsArrayDeque().clear();
            command.getObjectsArrayDeque().addAll(userTrolls);

            Main.writeCollection(Main.getObjectsLinkedDeque());
            Main.controller.reloadCollection();
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
                                "\"show_my\": вывести в стандартный поток вывода все элементы вашей коллекции в строковом представлении\n" +
                                "\"show_my\": вывести в стандартный поток вывода все элементы общей коллекции в строковом представлении\n" +
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
        Main.controller.synchronizeCollection();
        Collection<Troll> collection = new ArrayDeque<Troll>();
        User user = transferPackage.getUser();

        Stream<Pair<Troll,String>> userStream = command.getObjectsArrayDeque().stream().filter(p->p.getValue().equals(user.getLogin()));
        userStream.map(Pair::getKey).collect(Collectors.toCollection(()->collection));
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

    Command(Startable cmd) {
        this.cmd = cmd;
    }

    public static Command parseCmd(String jsonInput) throws IllegalArgumentException {

        String jsonRegex = "\\{\"isSad\":(true|false),\".+\":\".+\",\".+\":(\\d+),\"things\":\\[(\\{\"condition\":\"(Solid|Gaseous|Liquid)\",\"name\":\".+\",\"weight\":(\\d+)})*],\"isSit\":(true|false),\"age\":(\\d+)}";
        String dataCommandRegex = "(remove|import|add|remove_lower|change_def_file_path) \\{.+}";
        String nodataCommandRegex = "show_my|load|info|exit|help|save|SET_PATH_IMPORT|clear|show_all";
        String loginRegex = "login \\{.+} \\{.+}( \\{.+})?";

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
        }else if(jsonInput.matches(loginRegex)){
            String[] args = jsonInput.split(" ");
            Command command = Command.LOGIN;
            if(args.length == 3)
                command.setData(Stream.of(
                        args[1].substring(1, args[1].length() - 1) + "|" +
                                args[2].substring(1, args[2].length() - 1)
                ));
            else
                command.setData(Stream.of(
                        args[1].substring(1, args[1].length() - 1) + "|" +
                                args[2].substring(1, args[2].length() - 1) + "|" +
                                args[3].substring(1, args[3].length() - 1)
                ));
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

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    private void setData(Stream data) {
        this.data = data;
    }

    public Collection<Pair<Troll, String>> getObjectsArrayDeque() {
        return Main.getObjectsLinkedDeque();
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
