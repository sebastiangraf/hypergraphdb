/* 
 * This file is part of the HyperGraphDB source distribution. This is copyrighted 
 * software. For permitted uses, licensing options and redistribution, please see  
 * the LicensingInformation file at the root level of the distribution.  
 * 
 * Copyright (c) 2005-2010 Kobrix Software, Inc.  All rights reserved. 
 */
package org.hypergraphdb.peer.xmpp;

import static org.hypergraphdb.peer.Structs.*;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Future;
import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.MessageHandler;
import org.hypergraphdb.peer.Messages;
import org.hypergraphdb.peer.NetworkPeerPresenceListener;
import org.hypergraphdb.peer.PeerFilter;
import org.hypergraphdb.peer.PeerFilterEvaluator;
import org.hypergraphdb.peer.PeerInterface;
import org.hypergraphdb.peer.PeerRelatedActivity;
import org.hypergraphdb.peer.PeerRelatedActivityFactory;
import org.hypergraphdb.peer.protocol.Protocol;
import org.hypergraphdb.util.CompletedFuture;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 * <p>
 * A peer interface implementation based upon the Smack library 
 * (see http://www.igniterealtime.org for more info). 
 * </p>
 * 
 * <p>
 * The connection is configured as a regular chat connection with
 * a server name, port, username and a password. Then peers are either
 * simply all users in this user's roster or all member of a chat room
 * or the union of both.  
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class XMPPPeerInterface implements PeerInterface
{
    // Configuration options.
    private String serverName;
    private Number port;
    private String user;
    private String password;
    private String roomId;
    private boolean ignoreRoster = false;
    private boolean anonymous;
    private boolean autoRegister;
    private int fileTransferThreshold;
    private HyperGraphPeer thisPeer;
    private ArrayList<NetworkPeerPresenceListener> presenceListeners = 
        new ArrayList<NetworkPeerPresenceListener>();
    private MessageHandler messageHandler;
     
    ConnectionConfiguration config = null;
    XMPPConnection connection;
    MultiUserChat room = null;
    FileTransferManager fileTransfer;
    
    public void configure(Map<String, Object> configuration)
    {
        serverName = getPart(configuration, "serverUrl");
        port = getOptPart(configuration, 5222, "port");
        user = getPart(configuration, "user");
        password = getPart(configuration, "password");
        roomId = getOptPart(configuration, null, "room");
        ignoreRoster = getOptPart(configuration, false, "ignoreRoster");
        autoRegister = getOptPart(configuration, false, "autoRegister");
        anonymous = getOptPart(configuration, false, "anonymous");
        fileTransferThreshold = getOptPart(configuration, 100*1024, "fileTransferThreshold");
        config = new ConnectionConfiguration(serverName, port.intValue());
        config.setRosterLoadedAtLogin(true);
        config.setReconnectionAllowed(true);
        SmackConfiguration.setPacketReplyTimeout(30000);
    }    
        
    private void reconnect()
    {
    	if (connection != null && connection.isConnected())
    		stop();
    	start();
    }

    private void processPeerJoin(String name)
    {
//    	System.out.println("peer joined: " + name);
        for (NetworkPeerPresenceListener listener : presenceListeners)
            listener.peerJoined(name);    	
    }
    
    private void processPeerLeft(String name)
    {
//    	System.out.println("peer left: " + name);
        for (NetworkPeerPresenceListener listener : presenceListeners)
            listener.peerLeft(name);    	
    }
    
    private void processMessage(Message msg)
    {
        //
        // Encapsulate message deserialization into a transaction because the HGDB might
        // be accessed during this process.
        // 
        org.hypergraphdb.peer.Message M = null;
        if (thisPeer != null)
        	thisPeer.getGraph().getTransactionManager().beginTransaction();
        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(StringUtils.decodeBase64(msg.getBody()));                        
            M = new org.hypergraphdb.peer.Message((Map<String, Object>)new Protocol().readMessage(in));                        
        }
        catch (Exception t)
        {
            throw new RuntimeException(t);
        }
        finally
        {
            try { if (thisPeer != null) 
            	thisPeer.getGraph().getTransactionManager().endTransaction(false); }
            catch (Throwable t) { t.printStackTrace(System.err); }
        }
        messageHandler.handleMessage(M);                    
    	
    }
    
    private void initPacketListener()
    {
        connection.addPacketListener(new PacketListener() 
        {
            private void handlePresence(Presence presence)
            {
                String user = presence.getFrom();
                //System.out.println("Presence: " + user);
                Roster roster = connection.getRoster();
                String n = makeRosterName(user);
                //me - don't fire
                if(connection.getUser().equals(n)) return;
                //if user is not in the roster don't fire to listeners
                //the only exception is when presence is unavailable
                //because this could be fired after the user was removed from the roster
                //so we couldn't check this 
                if(roster.getEntry(n) == null && 
                   presence.getType() != Presence.Type.unavailable) return;
                if (presence.getType() == Presence.Type.subscribe)
                {
                    Presence reply = new Presence(Presence.Type.subscribed);
                    reply.setTo(presence.getFrom());
                    connection.sendPacket(reply);
                }
                else if (presence.getType() == Presence.Type.available)
                	processPeerJoin(user);
                else if (presence.getType() == Presence.Type.unavailable)
                	processPeerLeft(user);
            }
            
            private String makeRosterName(String name)
            {
                //input could be in following form
                //bizi@kobrix.syspark.net/67ae7b71-2f50-4aaf-85af-b13fe2236acb
                //test@conference.kobrix.syspark.net/bizi
                //bizi@kobrix.syspark.net
                //output should be:  
                //bizi@kobrix.syspark.net
                if(name.indexOf('/') < 0) return name;
                String first = name.substring(0, name.indexOf('/'));
                String second = name.substring(name.indexOf('/') + 1);
                if(second.length() != 36) return second + "@" + connection.getServiceName();
                try
                {
                    thisPeer.getGraph().getHandleFactory().makeHandle(second);
                   return first;
                }
                catch(NumberFormatException ex)
                {
                    return second;
                }
            }
            
            public void processPacket(Packet packet)
            {
                if (packet instanceof Presence)
                {
                	if (!ignoreRoster)
                		handlePresence((Presence)packet);
                    return;
                }
                processMessage((Message)packet);
            }                
            },
           new PacketFilter() { public boolean accept(Packet p)               
           {
               //System.out.println("filtering " + p);
             if (p instanceof Presence) return true;
             if (! (p instanceof Message)) return false;
             Message msg = (Message)p;
             if (!msg.getType().equals(Message.Type.normal)) return false;
             Boolean hgprop = (Boolean)msg.getProperty("hypergraphdb");
             return hgprop != null && hgprop;                                         
        }});                	
    }

    private String roomJidToUser(String jid)
    {
    	String [] A = jid.split("/");
    	return A[1] + "@" + connection.getServiceName();
    }
    
    private void initRoomConnectivity()
    {
    	room = new MultiUserChat(getConnection(), roomId);
    	room.addParticipantStatusListener(new DefaultParticipantStatusListener() 
    	{	
            @Override
            public void joined(String participant)
            {
            	processPeerJoin(roomJidToUser(participant));
            }
	
            public void kicked(String participant, String actor, String reason)
            {
            	processPeerLeft(roomJidToUser(participant));
            }
	
            public void left(String participant)
            {
            	processPeerLeft(roomJidToUser(participant));
            }
        });    	
    }
    
    private void login() throws XMPPException
    {
        if (anonymous)
            connection.loginAnonymously();
        else
        {
            // maybe auto-register if login fails
            try
            {
                connection.login(user, 
                			     password, 
                			     thisPeer != null && thisPeer.getGraph() != null ? 
                			    		 thisPeer.getIdentity().getId().toString() : null);
            }
            catch (XMPPException ex)
            {
                //XMPPError error = ex.getXMPPError();
                if (/* error != null && 
                     error.getCondition().equals(XMPPError.Condition.forbidden.toString()) && */
                    ex.getMessage().indexOf("authentication failed") > -1 &&
                    autoRegister &&
                    connection.getAccountManager().supportsAccountCreation())
                {
                    connection.getAccountManager().createAccount(user, password);
                    connection.disconnect();
                    connection.connect();
                    connection.login(user, password);
                }
                else
                    throw ex;
            }
        }                	
    }
    
    public void start()
    {
        assert messageHandler != null : new NullPointerException("MessageHandler not specified.");
        connection = new XMPPConnection(config);
        try
        {                             
            connection.connect();
            connection.addConnectionListener(new MyConnectionListener());
            fileTransfer = new FileTransferManager(connection);
            fileTransfer.addFileTransferListener(new BigMessageTransferListener());
            
            // Before we login, we add all relevant listeners so that we don't miss
            // any messages.
           	initPacketListener();
           	
            login();
            
            // Now join the room (if any) and explicitly send a presence message
            // to all peer in the roster cause otherwise presence seems
            // to go unnoticed.
            if (roomId != null && roomId.trim().length() > 0)
            	initRoomConnectivity();                        
            if (room != null)
            	room.join(user);
            if (!ignoreRoster)
            {
	            final Roster roster = connection.getRoster();                                                
	            Presence presence = new Presence(Presence.Type.subscribe);
	            for (RosterEntry entry : roster.getEntries())
	            {
	                presence.setTo(entry.getUser());
	                connection.sendPacket(presence);
	            }
            }
        }
        catch (XMPPException e)
        {    
            if (connection != null && connection.isConnected())
                connection.disconnect();
            throw new RuntimeException(e);
        }                  
    }
    
    public boolean isConnected()
    {
        return connection != null && connection.isConnected();
    }
    
    public void stop()
    {
        if (connection != null)
            try { connection.disconnect(); } catch (Throwable t) { }
    }
    
    public PeerRelatedActivityFactory newSendActivityFactory()
    {
        return new PeerRelatedActivityFactory() {
            public PeerRelatedActivity createActivity()
            {
                return new PeerRelatedActivity()
                {
                    public Boolean call() throws Exception
                    {
                        
                        org.hypergraphdb.peer.Message msg = getMessage();
                        if (getPart(msg, Messages.REPLY_TO) == null)
                        {
                            combine(msg, struct(Messages.REPLY_TO, connection.getUser()));
                        }
                        
                        Protocol protocol = new Protocol();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();                        
                        //
                        // Encapsulate message serialization into a transaction because the HGDB might
                        // be accessed during this process.
                        //                   
                        // NOTE: this breaks when a large sub-graph is being serialized because
                        // there aren't enough BDB locks to cover every page being accessed. There are
                        // several possible solution to this problem until BDB implements an unlimited
                        // locks feature (which would be pretty nice): either track and break down
                        // such large messages into smaller pieces (that's a heavy burden on P2P
                        // activities, but it's the cleanest solution) or implement a facility to grab
                        // a global lock on the whole DB environment which again requires a BDB feature.
                        // So we disable the message serialization transaction for now.
//                        thisPeer.getGraph().getTransactionManager().beginTransaction();
//                        try
//                        {
                            protocol.writeMessage(out, msg);
//                        }
//                        catch (Throwable t)
//                        {
//                        	System.err.println("Failed to serialize message " + msg);
//                        	t.printStackTrace(System.err);
//                        }
//                        finally
//                        {
//                            try { thisPeer.getGraph().getTransactionManager().endTransaction(false); }
//                            catch (Throwable t) { t.printStackTrace(System.err); }
//                        }  
                        
                        byte [] data = out.toByteArray();
                        if (data.length > fileTransferThreshold)
                        {
//                            System.out.println("Sending " + data.length + " byte of data as a file.");
                            OutgoingFileTransfer outFile = 
                                fileTransfer.createOutgoingFileTransfer((String)getTarget());
                            outFile.sendStream(new ByteArrayInputStream(data), 
                                               "", 
                                               data.length, 
                                               "");
                            return true;
                        }
                        else
                        {
                            try
                            {
                                Message xmpp = new Message((String)getTarget());                            
                                xmpp.setBody(StringUtils.encodeBase64(out.toByteArray()));
                                xmpp.setProperty("hypergraphdb", Boolean.TRUE);
                                connection.sendPacket(xmpp);                            
                                return true;
                            }
                            catch (Throwable t)
                            {
                                t.printStackTrace(System.err);
                                return false;
                            }
                        }
                    }                    
                };
            }
        };
    }
    
    public Future<Boolean> send(Object networkTarget,org.hypergraphdb.peer.Message msg)
    {
        PeerRelatedActivityFactory activityFactory = newSendActivityFactory();
        PeerRelatedActivity act = activityFactory.createActivity(); 
        act.setTarget(networkTarget);
        act.setMessage(msg);
        if (thisPeer != null)
        	return thisPeer.getExecutorService().submit(act);
        else
        {
        	try
			{
				return new CompletedFuture<Boolean>(act.call());
			} 
        	catch (Exception e)
			{
        		throw new RuntimeException(e);
			}
        }
    }
    
    public void broadcast(org.hypergraphdb.peer.Message msg)
    {
        for (HGPeerIdentity peer : thisPeer.getConnectedPeers())
            send(thisPeer.getNetworkTarget(peer), msg);
    }
    
    public HyperGraphPeer getThisPeer()
    {
        return thisPeer;
    }
    
    public PeerFilter newFilterActivity(PeerFilterEvaluator evaluator)
    {
        throw new UnsupportedOperationException();
    }
    
    public void addPeerPresenceListener(NetworkPeerPresenceListener listener)
    {
        presenceListeners.add(listener);
    }
    
    public void removePeerPresenceListener(NetworkPeerPresenceListener listener)
    {
        presenceListeners.remove(listener);
    }
    
    public void setMessageHandler(MessageHandler messageHandler)
    {
        this.messageHandler = messageHandler;
    }

    
    public void setThisPeer(HyperGraphPeer thisPeer)
    {
        this.thisPeer = thisPeer;
    }
    
    public XMPPConnection getConnection()
    {
        return connection;
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    public Number getPort()
    {
        return port;
    }

    public void setPort(Number port)
    {
        this.port = port;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public boolean isAnonymous()
    {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous)
    {
        this.anonymous = anonymous;
    }

    public boolean isAutoRegister()
    {
        return autoRegister;
    }

    public void setAutoRegister(boolean autoRegister)
    {
        this.autoRegister = autoRegister;
    }

	public int getFileTransferThreshold()
	{
		return fileTransferThreshold;
	}

	public void setFileTransferThreshold(int fileTransferThreshold)
	{
		this.fileTransferThreshold = fileTransferThreshold;
	}    
	
    private class BigMessageTransferListener implements FileTransferListener
    {
//        @SuppressWarnings("unchecked")
        public void fileTransferRequest(FileTransferRequest request)
        {
            if (thisPeer.getIdentity(request.getRequestor()) != null)
            {
                IncomingFileTransfer inFile = request.accept();
                org.hypergraphdb.peer.Message M = null;
                java.io.InputStream in = null;
                thisPeer.getGraph().getTransactionManager().beginTransaction();
                try
                {
                    in = inFile.recieveFile();
                    // TODO - sometime in the presence of a firewall (happened in VISTA)
                    // the file is silently truncated. Here we can read the whole thing
                    // into a byte[] and compare the size to inFile.getFileSize() to
                    // make sure that we got everything. If the file is truncated, the 
                    // parsing of the message will fail for no apparent reason.
                    if (inFile.getFileSize() > Integer.MAX_VALUE)
                        throw new Exception("Message from " + request.getRequestor() + 
                                            " to long with " + inFile.getFileSize() + " bytes.");
                    byte [] B = new byte[(int)inFile.getFileSize()];
                    for (int count = 0; count < inFile.getFileSize(); )
                        count += in.read(B, count, (int)inFile.getFileSize() - count);
                    M = new org.hypergraphdb.peer.Message((Map<String, Object>)
                                  new Protocol().readMessage(new ByteArrayInputStream(B)));                        
                }
                catch (Throwable t)
                {
                    t.printStackTrace(System.err);
                    throw new RuntimeException(t);
                }
                finally
                {
                    try { thisPeer.getGraph().getTransactionManager().endTransaction(false); }
                    catch (Throwable t) { t.printStackTrace(System.err); }
                    try { if ( in != null) in.close(); } catch (Throwable t) { t.printStackTrace(System.err); }
                }
                messageHandler.handleMessage(M);                
            }
            else
                request.reject();
        }        
    }
 
    private class MyConnectionListener implements ConnectionListener
    {

		public void connectionClosed()
		{
//			System.out.println("XMPP connection " + user + "@" + 
//					serverName + ":" + port + " closed gracefully.");
//			reconnect();
		}

		public void connectionClosedOnError(Exception ex)
		{
//			System.out.println("XMPP connection " + user + "@" + 
//					serverName + ":" + port + " closed exceptionally.");
			ex.printStackTrace(System.err);
			reconnect();
		}

		public void reconnectingIn(int arg0)
		{
//			System.out.println("Auto-reconnecting in " + arg0 + "...");
		}

		public void reconnectionFailed(Exception ex)
		{
//			System.out.println("XMPP auto-re-connection " + 
//					serverName + ":" + port + " failed.");
			ex.printStackTrace(System.err);
			reconnect();
		}

		public void reconnectionSuccessful()
		{
//			System.out.println("Auto-reconnection successful");
		}    	
    }
    
    static
    {
        // Force going through the XMPP server for every file transfer. This is rather
        // slowish, but otherwise it breaks especially for peers behind firewalls/NATs.
//        FileTransferNegotiator.IBB_ONLY = true;        
    }
}
