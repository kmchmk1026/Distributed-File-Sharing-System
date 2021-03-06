/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uom.chord;

import com.uom.communication.Connector;
import com.uom.communication.RestConnector;
import com.uom.communication.SocketConnector;
import com.uom.view.GUI;

import javax.swing.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static java.lang.System.exit;

/**
 * @author charith munasinghe
 */
public final class Node
{

    public static final int MAX_FINGERS = 4;
    private final String ip;
    private final int port;
    private final String username;
    private final int id;
    private GUI gui;
    private String BSip;
    private int BSport;
    private Connector connector;

    private ArrayList<String> files;

    private boolean successorExists;
    private boolean predecessorExists;
    private Node successor;
    private Node greatSuccessor;

    private Node predecessor;
    private Node greatPredecessor;
    private FingerFixer fingerFixer;
    private Stabilizer stabilizer;

    public Node( String ip, int port, String username )
    {
        this( ip, port, username, null, 55555, null, false, true );
    }

    public Node( String ip, int port, String username, String BSip, int BSport, GUI gui, boolean MainOrDummy, boolean SocketOrRest )
    {

        this.username = username;
        this.ip = ip;
        this.port = port;
        this.id = getHash( this.ip + this.port );

        if( MainOrDummy )
        {
            this.BSip = BSip;
            this.BSport = BSport;
            files = new ArrayList<>();

            if( SocketOrRest )
            {
                this.connector = new SocketConnector( this );
            }
            else
            {
                this.connector = new RestConnector( this );
            }

            //            this.successors = new Node[4];
            //            this.predeccessors = new Node[4];
            this.gui = gui;
            this.fingerFixer = new FingerFixer( this );
            this.stabilizer = new Stabilizer( this );
            initialize();
        }
    }

    public void initialize()
    {
        System.out.println( "I am " + this.id );
        populateWithFiles();
        this.connector.listen( port );
    }

    public void populateWithFiles()
    {
        ArrayList<String> filelist = new ArrayList<>(
                Arrays.asList( "Adventures of Tintin", "Jack and Jill", "Glee", "The Vampire Diarie", "King Arthur", "Windows XP", "Harry Potter", "Kung Fu Panda", "Lady Gaga", "Twilight",
                               "Windows 8", "Mission Impossible", "Turn Up The Music", "Super Mario", "American Pickers", "Microsoft Office 2010", "Happy Feet", "Modern Family", "American Idol",
                               "Hacking for Dummies" ) );
        String text = "<html>";
        for( int i = 0; i < 3; i++ )
        {
            int rand = new Random().nextInt( filelist.size() );
            String file = filelist.get( rand );
            files.add( file.toLowerCase() );
            text += "My file " + getHash( file ) + " is: " + file + "<br>";
            filelist.remove( rand );
        }
        text += "</html>";
        if( gui != null )
        {
            gui.updateFilesDisplay( text );
        }
    }

    public static int getHash( String text )
    {
        return Math.abs( text.hashCode() ) % 1000;
    }

    public Node( String ip, String port, String username )
    {
        this( ip, Integer.parseInt( port ), username, null, 55555, null, false, true );
    }

    public static String getMyIP()
    {
        try
        {
            final DatagramSocket socket = new DatagramSocket();
            socket.connect( InetAddress.getByName( "8.8.8.8" ), 10002 );
            return socket.getLocalAddress().getHostAddress();
        }
        catch( SocketException | UnknownHostException ex )
        {
            System.err.println( ex );
            return null;
        }
    }

    public boolean isSuccessorExists()
    {
        return successorExists;
    }

    public void setSuccessorExists( boolean successorExists )
    {
        this.successorExists = successorExists;
    }

    public boolean isPredecessorExists()
    {
        return predecessorExists;
    }

    public void setPredecessorExists( boolean predecessorExists )
    {
        this.predecessorExists = predecessorExists;
    }

    public String getBSip()
    {
        return BSip;
    }

    public int getBSport()
    {
        return BSport;
    }

    public GUI getGUI()
    {
        return this.gui;
    }

    public void setGUI( GUI gui )
    {
        this.gui = gui;
    }

    public Connector getConnector()
    {
        return connector;
    }

    public Node getGreatSuccessor()
    {
        return greatSuccessor;
    }

    public void setGreatSuccessor( Node greatSuccessor )
    {
        this.greatSuccessor = greatSuccessor;
    }

    public Node getGreatPredecessor()
    {
        return greatPredecessor;
    }

    public void setGreatPredecessor( Node greatPredecessor )
    {
        this.greatPredecessor = greatPredecessor;
    }

    public void registerToNetwork( Node newNode )
    {
        if( newNode != null )
        {
            String registerMessage = " " + "REG" + " " + newNode.getIp() + " " + Integer.toString( newNode.getPort() ) + " " + newNode.getUserName();
            int length = registerMessage.length() + 4;
            registerMessage = String.join( "", Collections.nCopies( 4 - ( Integer.toString( length ).length() ), "0" ) ) + length + registerMessage;

            connector.sendToBS( registerMessage );
        }
    }

    public String getIp()
    {
        return this.ip;
    }

    public int getPort()
    {
        return this.port;
    }

    public String getUserName()
    {
        return this.username;
    }

    public void unregisterFromNetwork( Node newNode )
    {
        if( newNode != null )
        {
            String unregisterMessage = " " + "UNREG" + " " + newNode.getIp() + " " + newNode.getPort() + " " + newNode.getUserName();
            int length = unregisterMessage.length() + 4;
            unregisterMessage = String.join( "", Collections.nCopies( 4 - ( Integer.toString( length ).length() ), "0" ) ) + length + unregisterMessage;

            connector.sendToBS( unregisterMessage );
        }
    }

    /*
    BS Messages:

    length REG IP_address port_no username
    length REGOK no_nodes IP_1 port_1 IP_2 port_2
    length UNREG IP_address port_no username
    length UNROK value

    Node Messages:

    length JOIN IP_address port_no
    length JOINOK value
    length LEAVE IP_address port_no
    length LEAVEOK value
    length SER IP port file_name hops
    length SEROK no_files IP port hops filename1 filename2 ... ...

    length ERROR
     */
    public void handleMessage( String message )
    {
        String[] messageList = message.split( " " );
        if( null != messageList[0] && messageList.length > 1 )
        {

            switch( messageList[0] )
            {
                case "JOIN"://find successor
                    Node sender = new Node( messageList[1], messageList[2], messageList[3] );
                    joinMessageReceived( message, sender );
                    break;

                case "UPDATE_SUCC":
                    Node newSuccessor = new Node( messageList[1], messageList[2], messageList[3] );
                    this.setSuccessor( newSuccessor );
                    break;

                case "UPDATE_PRED":
                    Node newPredecessor = new Node( messageList[1], messageList[2], messageList[3] );
                    this.setPredecessor( newPredecessor );
                    break;

                case "SEARCH_UP":
                    String fileUp = message.split( "@" )[1];
                    int hopsUp = Integer.parseInt( messageList[4] ) + 1;

                    for( String eachResult : this.contains( fileUp ) )
                    {
                        Node searcherUp = new Node( messageList[1], messageList[2], messageList[3] );
                        String found = "FOUND_FILE " + this.getIp() + " " + this.getPort() + " " + this.getUserName() + " " + hopsUp + " @" + eachResult;
                        connector.send( found, searcherUp );
                    }

                    String searchUp = "SEARCH_UP " + messageList[1] + " " + messageList[2] + " " + messageList[3] + " " + hopsUp + " @" + fileUp;
                    connector.send( searchUp, this.getSuccessor() );
                    break;

                case "SEARCH_DOWN":
                    String fileDown = message.split( "@" )[1];
                    int hopsDown = Integer.parseInt( messageList[4] ) + 1;

                    for( String eachResult : this.contains( fileDown ) )
                    {
                        Node searcherDown = new Node( messageList[1], messageList[2], messageList[3] );
                        String found = "FOUND_FILE " + this.getIp() + " " + this.getPort() + " " + this.getUserName() + " " + hopsDown + " @" + eachResult;
                        connector.send( found, searcherDown );
                    }

                    String searchDown = "SEARCH_DOWN " + messageList[1] + " " + messageList[2] + " " + messageList[3] + " " + hopsDown + " @" + fileDown;
                    connector.send( searchDown, this.getPredecessor() );
                    break;

                case "FOUND_FILE":
                    gui.updateResultsDisplay( message.split( "@" )[1], new Node( messageList[1], messageList[2], messageList[3] ), messageList[4] );
                    break;

                case "HEARTBEAT_UP":
                    int indexUp = Integer.parseInt( messageList[4] );
                    String presentUp = "PRESENT_UP " + this.getIp() + " " + this.getPort() + " " + this.getUserName() + " " + indexUp;
                    connector.send( presentUp, messageList[1], messageList[2] );

                    if( indexUp < 3 )
                    {
                        String nextHeartBeat = messageList[0] + " " + messageList[1] + " " + messageList[2] + " " + messageList[3] + " " + ( indexUp + 1 );
                        connector.send( nextHeartBeat, this.getSuccessor() );
                    }
                    break;

                case "HEARTBEAT_DOWN":
                    int indexDown = Integer.parseInt( messageList[4] );

                    String presentDown = "PRESENT_DOWN " + this.getIp() + " " + this.getPort() + " " + this.getUserName() + " " + indexDown;
                    connector.send( presentDown, messageList[1], messageList[2] );

                    if( indexDown < 3 )
                    {
                        String nextHeartBeat = messageList[0] + " " + messageList[1] + " " + messageList[2] + " " + messageList[3] + " " + ( indexDown + 1 );
                        connector.send( nextHeartBeat, this.getPredecessor() );
                    }
                    break;

                case "PRESENT_UP":
                    Node existingUp = new Node( messageList[1], messageList[2], messageList[3] );
                    int existingIndexUp = Integer.parseInt( messageList[4] );
                    updateExistingSuccessor( existingIndexUp, existingUp );
                    if( existingIndexUp == 0 )
                    {
                        this.setSuccessor( existingUp );
                    }
                    else if( existingIndexUp == 1 )
                    {
                        this.setGreatSuccessor( existingUp );
                        System.err.println( "Great successor: " + existingUp.getID() );
                    }
                    break;

                case "PRESENT_DOWN":
                    Node existingDown = new Node( messageList[1], messageList[2], messageList[3] );
                    int existingIndexDown = Integer.parseInt( messageList[4] );
                    updateExistingPredecessor( existingIndexDown, existingDown );
                    if( existingIndexDown == 0 )
                    {
                        this.setPredecessor( existingDown );
                    }
                    else if( existingIndexDown == 1 )
                    {
                        this.setGreatPredecessor( existingDown );
                        System.err.println( "Great predecessor: " + existingDown.getID() );
                    }
                    break;

                case "PING_UP":
                    String answerUp = "ANSWER_UP " + this.getIp() + " " + this.getPort() + " " + this.getID() + " " + this.getUserName();
                    connector.send( answerUp, messageList[1], messageList[2] );
                    break;

                case "PING_DOWN":
                    String answerDown = "ANSWER_DOWN " + this.getIp() + " " + this.getPort() + " " + this.getID() + " " + this.getUserName();
                    connector.send( answerDown, messageList[1], messageList[2] );
                    break;

                case "ANSWER_UP":
                    this.setSuccessorExists( true );
                    break;

                case "ANSWER_DOWN":
                    this.setPredecessorExists( true );
                    break;

                default:
                    break;
            }
            //<editor-fold defaultstate="collapsed" desc="comment">

            if( "REGOK".equals( messageList[1] ) )
            {
                switch( messageList[2] )
                {
                    case "0":
                        echo( "This is the first node." );
                        fingerFixer.start();
                        stabilizer.start();
                        break;

                    case "1":
                        echo( "This is the second node." );
                        Node firstNeighbor = new Node( messageList[3], messageList[4], "" );
                        joinNetwork( firstNeighbor );
                        fingerFixer.start();
                        stabilizer.start();
                        break;

                    case "2":
                        echo( "This is the third or a later node." );
                        Node firstNeighbor2 = new Node( messageList[3], messageList[4], "" );
                        Node secondNeighbor = new Node( messageList[5], messageList[6], "" );

                        //get the closest neigbour
                        if( Math.abs( this.id - firstNeighbor2.getID() ) < Math.abs( this.id - secondNeighbor.getID() ) )
                        {
                            joinNetwork( firstNeighbor2 );
                        }
                        else
                        {
                            joinNetwork( secondNeighbor );
                        }
                        fingerFixer.start();
                        stabilizer.start();
                        break;

                    case "9999":
                        notifyAndClose( " Failed, there is some error in the command." );
                        break;

                    case "9998":
                        notifyAndClose( "Failed, already registered to you, unregister first." );
                        break;

                    case "9997":
                        notifyAndClose( "Failed, registered to another user, try a different IP and port." );
                        break;

                    case "9996":
                        notifyAndClose( "Failed, can’t register. Bootstrap Server full." );
                        break;

                    default:
                        break;
                }
                //</editor-fold>
                //<editor-fold defaultstate="collapsed" desc="comment">

            }
            else if( "UNROK".equals( messageList[1] ) )
            {
                switch( messageList[2] )
                {
                    case "0":
                        //                        this.connector.kill(); //stop listning, equivelent to leave the network
                        //                        this.fingerFixer.kill();
                        //                        notifyAndClose("Successfully unregistered");
                        echo( "Successfully unregistered" );
                        break;
                    case "9999":
                        //                        notifyAndClose("Error while unregistering. IP and port may not be in the registry or command is incorrect.");
                        echo( "Error while unregistering. IP and port may not be in the registry or command is incorrect." );
                        break;
                    default:
                        //                        notifyAndClose("Some error while unregistering.");
                        echo( "Some error while unregistering." );
                        break;
                }
            }
            //</editor-fold>
        }
    }

    public void echo( String output )
    {
        //        System.out.println(output);
    }

    private void joinNetwork( Node neighbour )
    {
        String message = "JOIN " + this.getIp() + " " + this.getPort() + " " + this.getUserName();
        connector.send( message, neighbour.getIp(), neighbour.getPort() );
    }

    private void joinMessageReceived( String message, Node sender )
    {
        if( this.getID() < sender.getID() )
        {
            if( this.getSuccessor() == null )
            {
                this.setSuccessor( sender );
                askToUpdatePredecessor( sender, this );
            }
            else if( this.getSuccessor().getID() < sender.getID() )
            {
                connector.send( message, this.getSuccessor().getIp(), this.getSuccessor().getPort() );
            }
            else
            {
                //mysuccessor is his succerssor
                askToUpdateSuccessor( sender, this.getSuccessor() );
                askToUpdatePredecessor( sender, this );

                askToUpdatePredecessor( this.getSuccessor(), sender );
                this.setSuccessor( sender );
            }
        }
        else if( this.getPredecessor() == null )
        {
            this.setPredecessor( sender );
            askToUpdateSuccessor( sender, this );
        }
        else if( this.getPredecessor().getID() > sender.getID() )
        {
            connector.send( message, this.getPredecessor().getIp(), this.getPredecessor().getPort() );
        }
        else
        {
            //he is my predecessor
            askToUpdateSuccessor( sender, this );
            askToUpdatePredecessor( sender, this.getPredecessor() );

            askToUpdateSuccessor( this.getPredecessor(), sender );
            this.setPredecessor( sender );
        }
    }

    public int getID()
    {
        return this.id;
    }

    public Node getSuccessor()
    {
        return this.successor;
    }

    public void setSuccessor( Node successor )
    {
        this.successor = successor;
        //        this.successors[0] = successor;
        gui.UpdateSuccessor( 0, successor );
    }

    public Node getPredecessor()
    {
        return this.predecessor;
    }

    public void setPredecessor( Node predecessor )
    {
        this.predecessor = predecessor;
        //        this.predeccessors[0] = predecessor;
        gui.UpdatePredecessor( 0, predecessor );
    }

    public void askToUpdateSuccessor( Node receiver, Node successor_ )
    {
        if( receiver != null && successor_ != null )
        {
            receiver.setSuccessorExists( true );//this is useful for staibilizer thread
            String message = "UPDATE_SUCC " + successor_.getIp() + " " + successor_.getPort() + " " + successor_.getUserName();
            connector.send( message, receiver.getIp(), receiver.getPort() );
        }
    }

    public void askToUpdatePredecessor( Node receiver, Node predecessor_ )
    {
        if( receiver != null && predecessor_ != null )
        {
            receiver.setPredecessorExists( true );//this is useful for staibilizer thread
            String message = "UPDATE_PRED " + predecessor_.getIp() + " " + predecessor_.getPort() + " " + predecessor_.getUserName();
            connector.send( message, receiver.getIp(), receiver.getPort() );
        }
    }

    private ArrayList<String> contains( String file )
    {
        ArrayList<String> results = new ArrayList<>();
        for( String eachFile : this.getFiles() )
        {

            if( file.equals( eachFile ) )
            {
                results.add( eachFile );
            }
            String[] words = eachFile.split( " " );
            if( words.length > 1 )
            {
                for( int i = 0; i < words.length; i++ )
                {
                    if( file.equals( words[i] ) )
                    {
                        results.add( eachFile );
                    }
                }
            }

        }
        return results;
    }

    public ArrayList<String> getFiles()
    {
        return files;
    }

    public void updateExistingSuccessor( int index, Node existingSuccessor )
    {
        gui.UpdateSuccessor( index, existingSuccessor );
    }

    public void updateExistingPredecessor( int index, Node existingPredecessor )
    {
        gui.UpdatePredecessor( index, existingPredecessor );
    }

    private void notifyAndClose( String message )
    {
        JOptionPane.showMessageDialog( gui, message );
        exit( 0 );
    }

    public void search( String file )
    {
        for( String eachResult : this.contains( file ) )
        {
            gui.updateResultsDisplay( eachResult, this, "0" );
        }

        String searchUp = "SEARCH_UP " + this.getIp() + " " + this.getPort() + " " + this.getUserName() + " " + 0 + " @" + file;
        connector.send( searchUp, this.getSuccessor() );

        String searchDown = "SEARCH_DOWN " + this.getIp() + " " + this.getPort() + " " + this.getUserName() + " " + 0 + " @" + file;
        connector.send( searchDown, this.getPredecessor() );

    }
}
