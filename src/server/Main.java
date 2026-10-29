package server;

import com.google.gson.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Main {

    public static void main(String[] args) throws IOException {
        //Variables
        JsonObject database = new JsonObject();
        AtomicBoolean isRunning = new AtomicBoolean(true);
        int poolSize = Runtime.getRuntime().availableProcessors();
        ReadWriteLock lock = new ReentrantReadWriteLock(); //Create read and write locks
        Lock readLock = lock.readLock();
        Lock writeLock = lock.writeLock();

        ExecutorService executor = Executors.newFixedThreadPool(poolSize); //Creates Executors

        //Creates a server with an address and port number
        String address = "127.0.0.1";
        int port = 23456;
        ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address)); //Creates the server socket with the port and address initialized
        System.out.println("Server started!");

        //Loop to accept clients
        while(isRunning.get()){
            Socket socket = server.accept(); //Server connects to the client via socket

            //Executors takes the connection from each client and runs through the methods
            executor.execute(() -> {
                try {
                    DataInputStream input = new DataInputStream(socket.getInputStream()); //Creates input stream and gets information from the socket
                    DataOutput output = new DataOutputStream(socket.getOutputStream()); //Creates an output stream and gets information from the output socket

                    String request = input.readUTF();  //Puts input from the stream into the String
                    Gson gson = new Gson(); //Creates a Gson
                    JsonObject jsonRequest = gson.fromJson(request, JsonObject.class); //Takes the string rest and turns it from a Json to a JSonObject
                    String action = jsonRequest.get("type").getAsString();
                    JsonElement key = jsonRequest.get("key");
                    JsonElement value = jsonRequest.get("value");

                    if(action.equals("get")){
                        output.writeUTF(get(readLock, key, database));
                    } else if(action.equals("set")){
                        output.writeUTF(set(writeLock, key, value, database));
                    } else if(action.equals("delete")){
                        output.writeUTF(delete(writeLock, key, database));
                    } else if(action.equals("exit")){
                        String outputVariable = """
                            {"response":"OK"}
                            """;
                        output.writeUTF(outputVariable);
                        isRunning.set(false);
                        server.close();
                        executor.shutdown();
                    } else {
                        System.out.println("Wrong Request Type");
                    }

                    //Closes the input and output when the connection is done
                    try {
                        input.close(); //Closes the input stream
                        output.writeInt(67); //Closes the output stream when it writes this
                    } catch (SocketException e){
                        e.printStackTrace();
                    }

                } catch (IOException e){
                    try {
                        socket.close(); //Close the socket
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
    }

    //Method that writes to the database
    public static void writeFile(JsonObject database){
        try {
            String filePath = "C:\\Users\\alexk\\Desktop\\JSON Database with Java\\JSON Database with Java\\task\\src\\server\\data\\db.json";
            FileWriter writer = new FileWriter(filePath);

            //Creates a GSON to build in JSON format
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(database, writer); //Writes what is in JSON onto the file

            writer.flush(); //Flushes the writer stream to put it into the JSON file
        } catch (IOException e){
            System.out.println("Error could not write to file");
            e.printStackTrace();
        }
    }

    //Method to set information in the database
    public static String set(Lock writeLock, JsonElement key, JsonElement value, JsonObject database){
        JsonElement current = database; //Creates a JSonElement from the database object
        Gson gson = new Gson(); //Create a Gson
        JsonObject response = new JsonObject(); //Creates a response Json Object

        //Gets a writer lock for each connection
        writeLock.lock();
        try {
            //Checks if the key is a JSON Array
            if (key.isJsonArray()) {
                JsonArray array = key.getAsJsonArray(); //Creates a Json Array out of the keys

                //Iterate through the keys and gets the name of each value
                for (int i = 0; i < array.size() - 1; i++) {
                    JsonElement name = array.get(i);

                    //If the current Json Element is not an object then initialize it to null
                    if(!current.isJsonObject()){
                        current = null;
                    } else {
                        JsonElement next = current.getAsJsonObject().get(name.getAsString()); //Gets the next JsonElement by name
                        if(next == null){
                            //If it is null then create it as an object and put it into the database
                            JsonObject createObject = new JsonObject();
                            current.getAsJsonObject().add(name.getAsString(), createObject);
                            current = createObject;
                            writeFile(database);
                            response.addProperty("response", "OK");
                        } else {
                            //If it not null set the current to the next and overwrite it
                            current = next;
                            writeFile(database);
                            response.addProperty("response", "OK");
                        }
                    }
                }
                if (current == null) {
                    //If the element is not found then throw an error
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                } else {
                    current.getAsJsonObject().add(array.get(array.size() -1).getAsString(), value); //If it's not a Json Array then get the last one as a string and overwrite the value
                    writeFile(database);
                    response.addProperty("response", "OK");
                }
            } else {
                if(database.has(key.getAsString())){
                    database.add(key.getAsString(), value); //Sets the element in the database to the text
                    writeFile(database);
                    response.addProperty("response", "OK");
                } else {
                    database.add(key.getAsString(), value); //Sets the element in the database to the text
                    response.addProperty("response", "OK");
                }
            }
        } catch (Exception e){ //Out of bound exception print ERROR
            System.out.println("ERROR");
            response.addProperty("response", "ERROR"); //If String is still empty
        } finally {
            //Unlocks after each connection finishes
            writeLock.unlock();
        }

        return gson.toJson(response); //Write the response as a JSON back to the client
    }

    //Method to get information from the database
    public static String get(Lock readLock, JsonElement key, JsonObject database) {
        JsonElement current = database; //Creates a JSON element from the database
        Gson gson = new Gson(); //Creates a Gson
        JsonObject response = new JsonObject();
        //Grabs a read lock
        readLock.lock();

        try {
            //If the key is a JSON array then iterate through it and get its name
            if (key.isJsonArray()) {
                JsonArray array = key.getAsJsonArray();

                for (int i = 0; i < array.size(); i++) {
                    JsonElement name = array.get(i);

                    //If the current is a Json Object get its name as a String
                    if (current.isJsonObject()) {
                        current = current.getAsJsonObject().get(name.getAsString());
                    } else {
                        current = null; //If it is not an object then make it null
                    }
                }
                if (current == null) {
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                } else {
                    response.addProperty("response", "OK");
                    response.add("value", current);
                }
            } else {
                if (database.get(key.getAsString()) == null) {
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                } else {
                    response.addProperty("response", "OK");
                    response.add("value", database.get(key.getAsString()));
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR");
            response.addProperty("response", "ERROR");
            response.addProperty("reason", "No such key");
        } finally {
            //Unlocks when done reading
            readLock.unlock();
        }
        return gson.toJson(response);
    }

    //Method to delete information from the database
    public static String delete(Lock writeLock, JsonElement key, JsonObject database){
        JsonElement current = database; //Create a JSON Element from database
        Gson gson = new Gson();
        JsonObject response = new JsonObject(); //Create a JSON Object named response

        //Grabs a write like basically changing the database in some way
        writeLock.lock();

        try {

            //Check if the key is an Array to iterate through it to find the one to delete
            if (key.isJsonArray()) {
                JsonArray array = key.getAsJsonArray();

                for (int i = 0; i < array.size() - 1; i++) {
                    JsonElement name = array.get(i); //Gets the name of each element

                    //If the current is an JSON Object then get its name else init to null
                    if(current.isJsonObject()){
                        current = current.getAsJsonObject().get(name.getAsString());
                    } else {
                        current = null;
                    }
                }

                //If current is null send it as an error message to the client
                if (current == null) {
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                } else {
                    String finalKey = array.get(array.size() - 1).getAsString(); //If Store it as a final key and get its name

                    //Throw an error is the current object is not found
                    if(!current.getAsJsonObject().has(finalKey)){
                        response.addProperty("response", "ERROR");
                        response.addProperty("reason", "No such key");
                    } else {
                        //Remove the current object using the name of the finalKey
                        current.getAsJsonObject().remove(finalKey);
                        writeFile(database); //Write it back to the database
                        response.addProperty("response", "OK"); //Send this repsonse to the Client
                    }
                }

            } else {
                //If the key is null send error message to the client
                if (database.get(key.getAsString()) == null) {
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                } else {
                    //If the key is found in the database  remove it from the database and write to the database
                    if (database.has(key.getAsString())) {
                        database.remove(key.getAsString()); //Sets the key of the element to an empty string
                        writeFile(database);
                        response.addProperty("response", "OK");
                    } else {
                        response.addProperty("response", "OK");
                    }
                }
            }
        } catch (Exception e){
            System.out.println("ERROR");
            response.addProperty("response", "ERROR");
            response.addProperty("reason", "No such key");
        } finally {
            //Unlocks when finished
            writeLock.unlock();
        }
        return gson.toJson(response); //Send it back to the Client as a string
    }
}