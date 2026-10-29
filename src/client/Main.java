package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Main {
    //Global Variables Parameters
    @Parameter(names={"-t"})
    String requestType;

    @Parameter(names={"-k"})
    String key;

    @Parameter(names={"-v"})
    String valueMessage;

    @Parameter(names={"-in"})
    String json;

    //Boolean to check if JSON is not null
    public boolean hasJson(){
        return json != null;
    }

    public static void main(String[] args) throws IOException {

        //Create main that parses arguments for JCommander
        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();
    }

    //Method that runs using the arguments
    public void run() throws IOException {
        String message = "";
        JsonObject objects = new JsonObject();

        //Creates a Client with ip and port number
        String address = "127.0.0.1";
        int port = 23456;
        Socket socket = new Socket(InetAddress.getByName(address), port); //Creates a socket using the address and port number
        System.out.println("Client started!");

        DataOutputStream output = new DataOutputStream(socket.getOutputStream()); //Creates an output stream to write to the server
        Gson gson = new Gson(); //Create a Gson to turn string into JSON


        //If statement that reads and parses the JSON file as a message
        if(hasJson()){
            Map<Object, Object> importFile = readFile(json);
            message = gson.toJson(importFile);
            System.out.println("Sent: " + message);
        } else {

            //If statement that checks which type is sent
            if(requestType.equals("get")){
                objects.addProperty("type", requestType);
                objects.addProperty("key", key);

                message = gson.toJson(objects);
                System.out.println("Sent: " + objects);
            } else if(requestType.equals("set")){
                objects.addProperty("type", requestType);
                objects.addProperty("key", key);
                objects.addProperty("value", valueMessage);

                message = gson.toJson(objects);
                System.out.println("Sent: " + objects);
            } else if(requestType.equals("delete")){
                objects.addProperty("type", requestType);
                objects.addProperty("key", key);

                message = gson.toJson(objects);
                System.out.println("Sent: " + objects);
            } else if(requestType.equals("exit")){
                objects.addProperty("type", requestType);

                message = gson.toJson(objects);
                System.out.println("Sent: " + objects);
            } else {
                System.out.println("Wrong Request Type");
            }
        }
        output.writeUTF(message); //Writes it to the server

        DataInputStream input = new DataInputStream(socket.getInputStream()); //Creates and gets the input stream from the socket
        System.out.println("Received: " + input.readUTF()); //Prints out what it gets from the input stream
    }

    //Method that Reads JSON from a file
    public static Map<Object, Object> readFile(String data) {
        try {
            //Creates path to the JSON File
            Path path = Paths.get("C:\\Users\\alexk\\Desktop\\JSON Database with Java\\JSON Database with Java\\task\\src\\client\\data\\", data);
            String content = Files.readString(path); //Reads the whole file and puts it into a string

            Gson gson = new Gson(); //Creates a GSON to manipulate the JSON
            Type type = new TypeToken<Map<Object, Object>>(){}.getType(); //Gets the type from a Map
            Map<Object, Object> importFile = gson.fromJson(content, type); //Parses the content from the JSON into the Map
            return importFile;
        } catch (IOException e){
            e.printStackTrace();
        }
        return Map.of();
    }
}