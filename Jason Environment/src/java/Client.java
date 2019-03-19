import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.*;

/**
 * Client class abstraction used for networking with the jason environment.
 * @author group 16
 */
public class Client {
    private Logger logger;
    private String serverIp;
    private int serverPort;
    
    private ServerSocket server;
    private Socket clientSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public Client(String serverIp, int serverPort, Logger logger) throws IOException {
        this.logger     = logger;
        this.serverIp   = serverIp;
        this.serverPort = serverPort;
    }
    
    /**
     * This method will attempt to connect with the server socket.
     * Will continuously try to connect.
     */
    public void connect() {
        int i = 0;
        connector: while (true) {
            this.clientSocket = new Socket();
            System.out.println("Attempt: " + (i++));
            try {
                clientSocket.connect(new InetSocketAddress(serverIp, serverPort), 2000);
                this.outputStream = clientSocket.getOutputStream();
                this.inputStream = clientSocket.getInputStream();
                this.ois = new ObjectInputStream(inputStream);
                this.oos = new ObjectOutputStream(outputStream);
                break connector; // break out of the while loop
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            try {
                clientSocket.close();
            } catch (Throwable x) {
                // ignore
            }

            // System.out.println("Unable to connect to server ");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                // ignore
            }
        }
    }
    
    /**
     * Sends an instruction to the robot.
     * IS BLOCKING - WILL RETURN VALUES
     * @param txt
     * @return Return value (precepts) by the Ev3 Brick
     */
    public String sendInstruction(String txt) {
        logger.info("Sending Instruction: " + txt);
        return ((String) send(txt));
    }
    
    /**
     * Sends a list of instructions to the robot.
     * IS BLOCKING - WILL RETURN VALUES
     * @param set
     * @return Return value (precepts) by the Ev3 Brick
     */
    public String sendInstructions(ArrayList<String> set) {
        logger.info("Sending Instructions set! Size: " + set.size());
        return ((String) send(set));
    }
    
    /**
     * Wrapper for write object.
     * @param obj
     * @return
     */
    public Object send(Object obj) {
        Object returnValue = null;
        try {
            oos.writeObject(obj);
            logger.info("Waiting for completed flag.");
            returnValue = read();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        logger.info("Action completed by Ev3 Brick - Return value: " + ((String) returnValue));
        return returnValue;
    }
    
    /** 
     * Blocking method that will wait until it reads something.
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public Object read() throws ClassNotFoundException, IOException {
        Object o = ois.readObject();
        return o;
    }

    /**
     * Clean up method.
     */
    public void close() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
