import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Handler;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>(); //thread-safe map

    public static void sendBroadcastMessage(Message message){
        try {
            for (Map.Entry<String, Connection> connectionEntry : connectionMap.entrySet()) { //send a message to all connections from the map
                connectionEntry.getValue().send(message);
            }
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("Сообщение не было отправлено!");
        }
    }


    private static class Handler extends Thread { //class Handler conducts the protocol of interactions with the client
        private Socket socket;

        public void run(){ //main method run which implements all following methods
           ConsoleHelper.writeMessage("Соединение с сервером было установлено пользователем " + socket.getRemoteSocketAddress());

           String userName = null;
            try(Connection connection = new Connection(this.socket)) {
                //the result is String(userName)
                userName = serverHandshake(connection);

                //send to all users the message about new user555
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));

                //notify user about all current users
                notifyUsers(connection,userName);

                //invoke main loop
                serverMainLoop(connection, userName);

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом " + socket.getRemoteSocketAddress());
            }
            finally {
                if(userName!=null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                }
            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом " + socket.getRemoteSocketAddress() + " закрыто");
        }


        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection conncetion) throws IOException, ClassNotFoundException {
         while (true){
             conncetion.send(new Message(MessageType.NAME_REQUEST));

             Message message = conncetion.receive();
             String userName = message.getData();

             if(message.getType() != MessageType.USER_NAME){
                 ConsoleHelper.writeMessage("Получено сообщение от " + socket.getRemoteSocketAddress() + ". Формат сообщение не верен"); // wrong format of the name
                 continue;
             }

             if(userName.isEmpty()){
                 ConsoleHelper.writeMessage("Пустое имя было введено " + socket.getRemoteSocketAddress()); //empty name
                 continue;
             }

             if(connectionMap.containsKey(userName)){
                 ConsoleHelper.writeMessage("Попытка подключения к серверу с уже использованным именем от " + socket.getRemoteSocketAddress()); //name already exists
                 continue;
             }
             connectionMap.put(userName, conncetion);
             conncetion.send(new Message(MessageType.NAME_ACCEPTED));
             return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException{ //notify all users that a person has just connected
            for(Map.Entry<String, Connection> connectionEntry: connectionMap.entrySet()){
                if(connectionEntry.getKey()!=userName){ //don't send information to user who just connected
                    connection.send(new Message(MessageType.USER_ADDED, connectionEntry.getKey())); //notify all users
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{  //Main method

            while (true){
                Message message = connection.receive();
                if(message.getType()==MessageType.TEXT){
                    sendBroadcastMessage(new Message(MessageType.TEXT,userName + ": " + message.getData()));
                }
                else{
                    ConsoleHelper.writeMessage("Получено сообщение от " + socket.getRemoteSocketAddress() + ". Тип сообщения не соответсвует протоколу.");
                }
            }
        }
    }



    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        int port = ConsoleHelper.readInt();

        try(ServerSocket serverSocket = new ServerSocket(port)){
            ConsoleHelper.writeMessage("Чат сервера запущен.");
            while(true){
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
}
