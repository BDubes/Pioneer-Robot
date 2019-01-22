import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mobilerobots.Aria.*;
import com.rabbitmq.client.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Receiver
{
    static {
        try {
            System.loadLibrary("AriaJava");
        } catch(UnsatisfiedLinkError e) {
            System.err.println("Native code library (libAriaJava.so or AriaJava.dll) failed to load. Make sure that it is in your library path.");
            System.exit(1);
        }
    }
    
    public static void main(String[] argv) throws java.io.IOException
    {
        Aria.init();
        ArArgumentParser parser = new ArArgumentParser(argv);
        parser.loadDefaultArguments();
        ArSimpleConnector con = new ArSimpleConnector(parser);
        if(!Aria.parseArgs())
        {
            Aria.logOptions();
            System.exit(1);
        }
    
        ArRobot robot = new ArRobot();
        
        Scanner reader = null;
    
        try
        {
            reader = new Scanner(new File("auth.json"));
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Please make sure your auth.json is correctly placed");
            System.exit(1);
        }
        
        // open the connection to the robot; if this fails exit
        if(!con.connectRobot(robot))
        {
            System.err.println("Could not connect to the robot.");
            System.exit(1);
        }
        robot.runAsync(true);
        
        robot.enableMotors();
    
        
        
        StringBuilder plaintext = new StringBuilder();
        while(reader.hasNext())
        {
            plaintext.append(reader.next());
        }
    
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Auth auth = gson.fromJson(plaintext.toString(), Auth.class);
        
        ConnectionFactory factory = new ConnectionFactory();
        try
        {
            factory.setUri(auth.getURI());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("URI set");
    
        Connection connection = null;
        try
        {
            connection = factory.newConnection();
        }
        catch(TimeoutException e)
        {
            System.err.println("Connection timed out.");
            System.exit(1);
        }
    
        System.out.println("Connection successful");
    
        Channel channel = connection.createChannel();
    
        channel.queueDeclare(auth.getQueue(), false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
    
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
   
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException
            {
                robot.lock();
                String message = new String(body, "UTF-8");
                
                if(message.toLowerCase().equals("f"))
                {
                    //Code to move forward
                    robot.move(1000);
                }
                else if(message.toLowerCase().equals("b"))
                {
                    //Code to reverse
                    robot.move(-1000);
                }
                else if(message.toLowerCase().equals("l"))
                {
                    //Code to turn left
                    robot.setDeltaHeading(20);
                }
                else if(message.toLowerCase().equals("r"))
                {
                    //Code to move right
                    robot.setDeltaHeading(-20);
                }
                else if(Integer.parseInt(message) > 0)
                {
                    //Code to accelerate
                }
                else if(Integer.parseInt(message) < 0)
                {
                    //Code to decelerate
                }
                robot.unlock();
            }
        };
        channel.basicConsume(auth.getQueue(), true, consumer);
    }
}
