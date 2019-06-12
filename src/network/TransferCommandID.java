package network;

/**
 * Класс, который содержит ID доступных команд
 */
public enum TransferCommandID {

    REMOVE(1),
    SHOW_MY(2),
    CLEAR(3),
    LOAD(4),
    INFO(5),
    IMPORT(6),
    ADD(7),
    EXIT(9),
    REMOVE_LOWER(10),
    HELP(11),
    SHOW_ALL(14),
    EMPTY_TP(666),
    OK(0),
    ERROR(-1),
    ADI_FE(601),
    CheckingConnectionTP(101);

    private int id;

    TransferCommandID(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
