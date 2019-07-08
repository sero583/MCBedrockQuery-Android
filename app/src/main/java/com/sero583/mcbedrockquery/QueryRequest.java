package com.sero583.mcbedrockquery;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryRequest extends AsyncTask<ServerInfo, Void, TreeMap<String, String>> {
    private static final byte[] QUERY_COMMAND = {(byte) 0xFE, (byte) 0xFD, (byte) 0x09, (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01};
    private static final int BUFFER_SIZE_RECEIVE = 10240;
    private static final int TIMEOUT = 3500;

    private MainActivity context;
    private ProgressDialog pgDialog;

    public QueryRequest(MainActivity context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.pgDialog = new ProgressDialog(this.context);
        this.pgDialog.setMessage("Fetching server information...");
        this.pgDialog.setIndeterminate(false);
        this.pgDialog.setCancelable(false);
        this.pgDialog.show();
    }

    @Override
    protected TreeMap<String, String> doInBackground(ServerInfo... serverInfo) {
        try {
            if(serverInfo.length>0) {
                ServerInfo info = serverInfo[0];

                InetSocketAddress local = new InetSocketAddress(info.getIp(), info.getPort());

                DatagramSocket udpSocket = new DatagramSocket();
                //this.udpSocket.setSoTimeout(5*1000);
                udpSocket.connect(InetAddress.getByName(info.getIp()), info.getPort());

                if(udpSocket.isConnected()==false) {
                    System.out.println("Server offline");
                    return null;
                }

                final byte[] receiveData = new byte[BUFFER_SIZE_RECEIVE];
                udpSocket.setSoTimeout(TIMEOUT);
                sendPacket(udpSocket, local, QUERY_COMMAND);

                final int challengeInteger;
                {
                    receivePacket(udpSocket, receiveData);
                    byte byte1 = -1;
                    int i = 0;
                    byte[] buffer = new byte[11];
                    for (int count = 5; (byte1 = receiveData[count++]) != 0; )
                        buffer[i++] = byte1;
                    challengeInteger = Integer.parseInt(new String(buffer).trim());
                }
                sendPacket(udpSocket, local, 0xFE, 0xFD, 0x00, 0x01, 0x01, 0x01, 0x01, challengeInteger >> 24, challengeInteger >> 16, challengeInteger >> 8, challengeInteger, 0x00, 0x00, 0x00, 0x00);


                final int length = receivePacket(udpSocket, receiveData).getLength();
                TreeMap<String, String> values = new TreeMap<String, String>();
                final AtomicInteger cursor = new AtomicInteger(5);
                while(cursor.get()<length) {
                    final String s = readString(receiveData, cursor);
                    if(s.length()==0)
                        break;
                    else {
                        final String v = readString(receiveData, cursor);
                        values.put(s, v);
                    }
                }

                readString(receiveData, cursor);
                final Set<String> players = new HashSet<String>();

                while(cursor.get()<length) {
                    final String name = readString(receiveData, cursor);
                    if (name.length() > 0)
                        players.add(name);
                }

                String[] onlineUsernames = players.toArray(new String[players.size()]);
                udpSocket.close();

                Thread.currentThread().interrupt();

                for(Map.Entry<String, String> entry : values.entrySet()) {
                    System.out.println(entry.getKey() + ":" + entry.getValue());
                }

                for(int i = 0; i < onlineUsernames.length; i++) {
                    System.out.println("Player " + (i + 1) + ":" + onlineUsernames[i]);
                    values.put("player." + i, onlineUsernames[i]);
                }
                return values;
            }
        } catch(Exception e){
            System.out.println("Error");
            e.printStackTrace();
        }
        System.out.println("Nothing given");
        return null;
    }

    @Override
    protected void onPostExecute(TreeMap<String, String> values) {
        super.onPostExecute(values);
        this.pgDialog.cancel();

        AlertDialog.Builder builder = new AlertDialog.Builder(this.context).setTitle("Query Result").setPositiveButton(android.R.string.ok, null);
        if(values==null) {
            builder.setMessage("Was unable to gather query information. Reasons could be as stated following:\nServer is offline.\nThis software is (maybe) outdated or bugged.");
        } else {
            String s = "";
            for(Map.Entry<String, String> entry : values.entrySet()) {
                s += entry.getKey() + ": " + entry.getValue() + "\n";
            }
            builder.setMessage(s);
        }
        builder.show();
    }

    private final static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, byte... data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort());
        socket.send(sendPacket);
    }

    private final static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, int... data) throws IOException {
        final byte[] d = new byte[data.length];
        int i = 0;
        for(int j : data)
            d[i++] = (byte)(j & 0xff);
        sendPacket(socket, targetAddress, d);

    }

    private final static DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
        final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        socket.receive(dp);
        return dp;
    }

    private final static String readString(byte[] array, AtomicInteger cursor) {
        final int startPosition = cursor.incrementAndGet();
        for(; cursor.get() < array.length && array[cursor.get()] != 0; cursor.incrementAndGet());
        return new String(Arrays.copyOfRange(array, startPosition, cursor.get()));
    }
}
