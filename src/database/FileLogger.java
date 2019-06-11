package database;

import java.io.FileWriter;
import java.io.IOException;

class FileLogger {

    private FileWriter writer;

    public FileLogger(){
        try {
            writer = new FileWriter("logs.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void log(String message){
        try {
            writer.append(message);
            writer.close();
            writer = new FileWriter("logs.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
