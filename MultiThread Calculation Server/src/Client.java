//Shahriar Sagor
//CIS 427
//Winter 2022
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException, InterruptedException {
        Socket client = new Socket("localhost", 6867);
        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        DataInputStream in = new DataInputStream(client.getInputStream());
        Scanner input = new Scanner(System.in);
        System.out.println("Connected to Server proceed to login");
        boolean loop = true;
        startListening(in);
        while(loop){
            String command = input.nextLine();
            command = command.trim().replaceAll("\\s+"," ");
            out.writeUTF(command);
            out.flush();
            Thread.sleep(500);
            if(command.contains("SHUTDOWN")){
                loop = false;
            }
        }

    }

   static void  startListening(DataInputStream in){
        new Thread(()->{
            try {
                while(true) {
                    System.out.println(in.readUTF());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
