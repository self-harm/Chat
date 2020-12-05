package client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected=false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    //main mathod run()
    public void run() {
       SocketThread socket = getSocketThread();
       socket.setDaemon(true);
       socket.start();

        try {
            synchronized (this){
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            ConsoleHelper.writeMessage("Произошла ошибка с главным потоком исполения.");
            return;
        }


        if(clientConnected==true){
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        }
        else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }

        while (clientConnected) {
            String text = ConsoleHelper.readString();
            if(text.equalsIgnoreCase("exit"))break;

            if(shouldSendTextFromConsole()==true){
                sendTextMessage(text);
            }
        }
    }

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адресс сервера: ");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя пользователя: ");
        return ConsoleHelper.readString();
    }

    //client always returns true as text always was sent from console
    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    //create new object of class SocketThread
    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        }
        catch (IOException e){
            e.printStackTrace();
            ConsoleHelper.writeMessage("Произошла ошибка " + connection.getRemoteSocketAddress());
            clientConnected = false;
        }
    }

    public class SocketThread extends Thread{
        //print text in the chat
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        //print information about new user in the chat
        protected void informAboutAddingNewUser(String userName){
            System.out.println("Участник " + userName + " присоединился к чату!");
        }

        //print information about quitting the chat
        protected void informAboutDeletingNewUser(String userName){
            System.out.println("Участник " + userName + " покинул чат. :(");
        }
        
        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
//            while (!clientConnected){
//                Message message = connection.receive();
//
//                if(message.getType() == null){
//                    throw new IOException("Unexpected MessageType"); //check null
//                }
//
//                switch (message.getType()){
//                    //require the name
//                    case NAME_REQUEST:
//                        //require to type the name
//                        String name = getUserName();
//                        //send the name on the server
//                        connection.send(new Message(MessageType.USER_NAME, name));
//
//                    //server accepted the name
//                    case NAME_ACCEPTED:
//                        //notify the main thread that it can work
//                        notifyConnectionStatusChanged(true);
//                        return;
//
//                    default:
//                        throw new IOException("Unexpected MessageType");
//                }
//            }

            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.NAME_REQUEST) { // Сервер запросил имя пользователя
                    // Запрашиваем ввод имени с консоли
                    String name = getUserName();
                    // Отправляем имя на сервер
                    connection.send(new Message(MessageType.USER_NAME, name));

                } else if (message.getType() == MessageType.NAME_ACCEPTED) { // Сервер принял имя пользователя
                    // Сообщаем главному потоку, что он может продолжить работу
                    notifyConnectionStatusChanged(true);
                    return;

                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            //the main loop of text messages
//            while (true){
//                Message message = connection.receive();
//
//                if(message.getType() == null){
//                    throw new IOException("Unexpected MessageType"); //check null
//                }
//
//                switch (message.getType()){
//                    case TEXT:
//                        processIncomingMessage(message.getData());
//                    case USER_ADDED:
//                        informAboutAddingNewUser(message.getData());
//                    case USER_REMOVED:
//                        informAboutAddingNewUser(message.getData());
//                    default:
//                        throw new IOException("Unexpected MessageType");
//                }
//            }

            // Цикл обработки сообщений сервера
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) { // Сервер прислал сообщение с текстом
                    processIncomingMessage(message.getData());
                } else if (MessageType.USER_ADDED == message.getType()) {
                    informAboutAddingNewUser(message.getData());
                } else if (MessageType.USER_REMOVED == message.getType()) {
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run() {
            try {
                connection = new Connection(new Socket(getServerAddress(), getServerPort()));
                
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                notifyConnectionStatusChanged(false);
            }




        }
    }
}



