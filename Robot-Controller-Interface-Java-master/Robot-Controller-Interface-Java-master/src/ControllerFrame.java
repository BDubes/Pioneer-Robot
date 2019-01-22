import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ControllerFrame extends JFrame
{
    private JButton buttonUp;
    private JButton buttonDown;
    private JButton buttonLeft;
    private JButton buttonRight;
    private Container container = new Container();
    private String currentMessage = null;
    private ActionRepeater actionRepeater = new ActionRepeater();
    private Lock lock = new ReentrantLock();
    private Condition noMessages = lock.newCondition();
    
    public static void main(String[] args)
    {
        ExecutorService threadExecutor = Executors.newCachedThreadPool();
        ControllerFrame controllerFrame = new ControllerFrame("Robot Controller");
        threadExecutor.execute(controllerFrame.actionRepeater);
    }
    
    ControllerFrame(String title)
    {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        this.buttonDown = new JButton();
        this.buttonLeft = new JButton();
        this.buttonRight = new JButton();
        this.buttonUp = new JButton();
        
        MyMouseListener mouseListener = new MyMouseListener();
        
        buttonUp.setActionCommand("f");
        buttonRight.setActionCommand("r");
        buttonLeft.setActionCommand("l");
        buttonDown.setActionCommand("b");
        
        buttonDown.addMouseListener(mouseListener);
        buttonUp.addMouseListener(mouseListener);
        buttonLeft.addMouseListener(mouseListener);
        buttonRight.addMouseListener(mouseListener);
        
        //Builds the JPanel.
        build();
        container.setLayout(new GridLayout(3, 3,0,0));
        add(container);
        setSize(250,250);
        setVisible(true);
        revalidate();
        repaint();
    }
    
    
    //Uses the container to add all elements, then revalidate.
    private void build()
    {
        container.removeAll();
        container.add(new JPanel());
        container.add(buttonUp);
        container.add(new JPanel());
        container.add(buttonLeft);
        container.add(new JPanel());
        container.add(buttonRight);
        container.add(new JPanel());
        container.add(buttonDown);
        container.add(new JPanel());
        revalidate();
    }
    
    private class ActionRepeater implements Runnable
    {
        
        
        @Override
        public void run()
        {
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
            try
            {
                Connection connection = null;
                try
                {
                    connection = factory.newConnection();
                }catch(TimeoutException e)
                {
                    System.err.println("Connection timed out.");
                    System.exit(1);
                }
    
                System.out.println("Connection successful");
    
                Channel channel = connection.createChannel();
    
                channel.queueDeclare(auth.getQueue(), false, false, false, null);
    
    
                while(true)
                {
                    try
                    {
                        move(channel, auth);
                        Thread.sleep(500);
                    }catch(InterruptedException e)
                    {
                        System.out.println(e);
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        private void move(Channel channel, Auth auth) throws InterruptedException, IOException
        {
            String newMessage = currentMessage;
            lock.lock();
            try
            {
                while(newMessage == null)
                {
                    noMessages.await();
                    newMessage = currentMessage;
                }
                channel.basicPublish("", auth.getQueue(), null, newMessage.getBytes());
                System.out.println("Moving in direction: " + newMessage);
            }
            finally
            {
                lock.unlock();
            }
        }
    }
    
    private class MyMouseListener implements MouseListener
    {
    
        @Override
        public void mouseClicked(MouseEvent e)
        {
        
        }
    
        @Override
        public void mousePressed(MouseEvent e)
        {
            lock.lock();
            JButton buttonClicked = (JButton)e.getSource();
            currentMessage = buttonClicked.getActionCommand();
            noMessages.signal();
            lock.unlock();
        }
    
        @Override
        public void mouseReleased(MouseEvent e)
        {
            currentMessage = null;
        }
    
        @Override
        public void mouseEntered(MouseEvent e)
        {
        
        }
    
        @Override
        public void mouseExited(MouseEvent e)
        {
        
        }
    }
}