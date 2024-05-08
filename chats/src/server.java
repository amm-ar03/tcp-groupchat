import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public server(){
        connections = new ArrayList<>();
        done = false;
    }
    @Override
    public void run(){
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();//
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }
    public void broadcast(String message){
        for(ConnectionHandler ch : connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        try{
            done = true;
            pool.shutdown();
            if(!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections){
                ch.shutdown();
            }
        } catch (IOException e){
            // todo
        }

    }
    class ConnectionHandler implements Runnable {
    //handles the client connection. individual connections
        private Socket client;
        private BufferedReader in; //client sends something
        private PrintWriter out; //when we want to send something
        private String name;
        public ConnectionHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run(){
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a name: ");
                name = in.readLine(); // c;ient enters name
                System.out.println(name + " connected!");
                broadcast(name + " joined the chat!");

                String message;
                while((message = in.readLine()) != null){
                    if(message.startsWith("/nick")){
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2){
                            broadcast(name + " renamed themselves to " + messageSplit[1]);
                            System.out.println(name + " renamed themselves to" + messageSplit[1]);
                            name = messageSplit[1];
                            out.println("Successfully changed nickname to: " + name);
                        }else {
                            out.println("No nickname provided");
                        }

                    } else if(message.startsWith("/quit")){
                        broadcast(name + " left the chat");
                        shutdown();
                    } else {
                        broadcast(name + ": " + message);
                    }
                }
            } catch(IOException e) {
                shutdown();

            }

        }

        public void sendMessage(String message){
            out.println(message);
        }
        public void shutdown() {
             try {
                 in.close();
                 out.close();
                 if (!client.isClosed()) {
                     client.close();
                 }
             } catch (IOException e){
                 //ignore
             }

        }
    }

    public static void main(String[] args) {
        server server1 = new server();
        server1.run();
    }
}
