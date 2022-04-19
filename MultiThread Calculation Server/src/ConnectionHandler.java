//Shahriar Sagor
//CIS 427
//Winter 2022

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class ConnectionHandler implements Runnable{
    String userLogged = "";
    boolean logged = false;
    Socket client;
    DataOutputStream out;
    DataInputStream in;
    ConnectionHandler(Socket client) throws IOException {
        this.client = client;
        out = new DataOutputStream(client.getOutputStream());
        in = new DataInputStream(client.getInputStream());
    }

    @Override
    public void run() {

        try {

            boolean loop = true;
            while (loop) {
                String request = in.readUTF();
                String[] command = request.split(" ");
                switch (command[0]) {
                    case "LOGIN": //Allows user to log in. If the username and password is not listed in login.txt it will return error
                        try{
                            if (logged) {
                                break;
                            }
                            for (String[] user : Server.users) {
                                if (user[0].equals(command[1]) & user[1].equals(command[2])) {
                                    userLogged = user[0];
                                    logged = true;
                                    Server.loggedInUsers.add(this);
                                    break;
                                }
                            }
                            if (logged)
                                out.writeUTF("Logged in as "+userLogged);
                            else {
                                out.writeUTF("FAILURE: Please provide correct username and password. Try again.");
                            }}catch (ArrayIndexOutOfBoundsException e){
                            out.writeUTF("Invalid login info");
                        }
                        break;

                    case "SOLVE": // solves for radius and sides. Returns error upon invalid entry
                        if(!logged){
                            out.writeUTF("Not Logged In");
                            break;
                        }
                        String result;
                        if (command[1].equals("-c")) {
                            if(command.length<3){
                                result = "Error: No radius found";
                            }else
                                result = circle(Double.parseDouble(command[2]));
                        } else if (command[1].equals("-r")) {
                            if(command.length<3){
                                result = "Error: No sides found";
                            }else if (command.length > 3) {
                                result = rect(Double.parseDouble(command[2]), Double.parseDouble(command[3]));
                            } else {
                                result = rect(Double.parseDouble(command[2]));
                            }
                        } else {
                            result = ("300 Invalid command");
                        }
                        out.writeUTF(result);
                        break;

                    case "LIST": //lists all the solutions requested by user
                        if(!logged){
                            out.writeUTF("Not Logged In"); //if the user if not logged in, system will display this message
                            break;
                        }
                        if (command.length == 2) {
                            if (command[1].equals("-all")) { //User has to be root to use this command, otherwise returns error
                                if (userLogged.equals("root"))
                                    out.writeUTF(readAllHistory());
                                else
                                    out.writeUTF("Error: you are not the root user");
                            }else{
                                out.writeUTF("400 Bad Request");
                            }
                        } else
                            out.writeUTF(readHistory(userLogged));
                        break;
 
                    case "SHUTDOWN": //shuts down the server
                        if(userLogged.equals("root"))
                            out.writeUTF("200 OK");
                        else{
                            out.writeUTF("403 Forbidden (Only root can shutdown server logging out)");
                            Server.loggedInUsers.remove(this);
                        }
                        out.close();
                        in.close();
                        client.close();
                        if(userLogged.equals("root"))
                            System.exit(0);
                        loop = false;
                        break;

                    case "MESSAGE": //sends messages to the user
                        if(!logged){
                            out.writeUTF("Not Logged In");
                            break;
                        }
                        String message = request.replaceFirst(command[0], "").replaceFirst(command[1], "");
                        System.out.println("Message from client:\n" +
                                message+"\nSending to "+command[1]);

                        if (command.length >= 3) {
                            if (command[1].equals("-all")) {
                                if (userLogged.equals("root")){
                                    for (ConnectionHandler loggedInUser : Server.loggedInUsers) {
                                        loggedInUser.out.writeUTF("Message from "+userLogged+":"+request.replaceFirst(command[0], "").replaceFirst(command[1], ""));
                                        loggedInUser.out.flush();
                                    }
                                    out.writeUTF("Sent");
                                    out.flush();
                                }
                                else
                                    out.writeUTF("Error: you are not the root user");

                        } else{
                                boolean found = false;
                                for (String[] loggedInUser : Server.users) {
                                    if(loggedInUser[0].equals(command[1])){
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found){
                                    System.out.println("User "+command[1]+" does not exist\n" +
                                            "informing client");
                                    out.writeUTF("User "+command[1] +" does not exist");
                                    out.flush();
                                }else{
                                    boolean isLoggedIn = false;
                                    for (ConnectionHandler loggedInUser : Server.loggedInUsers) {
                                        if(loggedInUser.userLogged.equals(command[1])){
                                            loggedInUser.out.writeUTF("Message from "+userLogged+":"+message);
                                            loggedInUser.out.flush();
                                            isLoggedIn = true;
                                            break;

                                        }
                                    }
                                    if (isLoggedIn)
                                        out.writeUTF("Sent");
                                    else {
                                        out.writeUTF("User " + command[1] + " is not logged in");
                                        System.out.println("User "+command[1] +" is not logged in\n" +
                                                "Informing client");
                                    }
                                    out.flush();
                                }
                            }
                        }
                        break;

                    case "LOGOUT": //logs out the user
                        if(!logged){
                            out.writeUTF("Not Logged In");
                            break;
                        }
                        out.writeUTF("200 OK");
                        logged =false;
                        userLogged = "";
                        Server.loggedInUsers.remove(this);
                        break;

                    default:
                        out.writeUTF("400 Bad Request");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Calculating and displaying the circle's circumference
    String circle(double radius) throws IOException {
        double area = Math.PI*Math.pow(radius,2);
        double circumference = 2*Math.PI*radius;
        String result = String.format("Circle's circumference is %.2f and area is %.2f", circumference, area);
        appendSolution("Radius "+radius+": "+result);
        return result;
    }
    
    //Calculating and displaying the parameter and area of the rectangle if provided 1 side
    String rect(double side) throws IOException {
        double parameter = 4*side;
        double area = side*side;
        String result = String.format("Rectangle's perimeter is %.2f and area is %.2f", parameter, area);
        appendSolution("Side "+side+": "+result);
        return result;
    }

    //Calculating and displaying the parameter and area of the rectangle if provided 2 sides

    String rect(double side1, double side2) throws IOException {
        double parameter = 2*(side1+side2);
        double area = side1*side2;
        String result = String.format("Rectangle's perimeter is %.2f and area is %.2f", parameter, area);
        appendSolution("Sides "+side1+" "+side2+": "+result);
        return result;
    }

    //Reads history and store them in the _solutions.txt

    String readHistory(String filename) throws IOException {
        File file =  new File(filename+"_solutions.txt");
        if(!file.exists())
            file.createNewFile();
        String result = filename+":\n";
        String fileText = Files.readString(Path.of(file.getName()));
        if(fileText.equals("")){
            result += "No interactions yet";
        }else
            result += fileText;
        return result;
    }

  //Reads history and store them in the _solutions.txt
    String readAllHistory() throws IOException {
        StringBuilder result = new StringBuilder();
        for (String[] user : Server.users) {
            result.append(readHistory(user[0])).append("\n");
        }
        return result.toString();
    }

    
    void appendSolution(String result) throws IOException {
        File file = new File(userLogged+"_solutions.txt");
        if(!file.exists())
            file.createNewFile();
        Files.write(Paths.get(file.getName()), (result+"\n").getBytes(), StandardOpenOption.APPEND);

    }

}
