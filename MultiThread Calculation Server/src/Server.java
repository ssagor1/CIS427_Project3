//Shahriar Sagor
//CIS 427
//Winter 2022

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    static ArrayList<String[]> users = new ArrayList<>();
    static ArrayList<ConnectionHandler> loggedInUsers = new ArrayList<>();
    static boolean shutdown = false;
    public static void main(String[] args) throws IOException {
        loadUser();
        ServerSocket server = new ServerSocket(6867);
        while(!shutdown){
        Socket client = server.accept();
        new Thread(new ConnectionHandler(client)).start();
        }


    }

  //Reads login.txt file. Username and password needs to be in the txt file to log in. 
    static void loadUser() throws IOException {
        Path filePath = new File("login.txt").toPath();
        Charset charset = Charset.defaultCharset();
        List<String> stringList = Files.readAllLines(filePath, charset);
        for(String i : stringList) {
            String user[] = i.split(" ");
            Server.users.add(user);
        }
    }

}
