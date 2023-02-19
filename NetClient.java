package client.client;

import client.protocol.*;
import server.Server;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetClient {
    private Client tc;
    private int UDP_PORT;  // 用戶端的UDP port
    private String serverIP;  // server的IP address
    private int serverUDPPort;  // 透過server轉發的UDP port
    private int USER_DEAD_UDP_PORT;  // server監聽user死亡的UDP port
    private DatagramSocket ds = null;
    public void setUDP_PORT(int UDP_PORT) {
        this.UDP_PORT = UDP_PORT;
    }

    public NetClient(Client tc) {
        this.tc = tc;
        try {
            this.UDP_PORT = getRandomUDPPort();
        } catch (Exception e) {
            tc.getUdpPortWrongDialog().setVisible(true);//弹窗提示
            System.exit(0);//如果选择到了重复的UDP端口号就退出客户端重新选择.
        }
    }

    /**
     * 與Server進行 TCP 連接
     */

    public void connect(String ip) {
        serverIP = ip;
        Socket s = null;
        try {
            ds = new DatagramSocket(UDP_PORT);  // 創建UDP
            try {
                s = new Socket(ip, Server.socket);  // 創建TCP
            } catch (Exception e1) {
                tc.getServerNotStartDialog().setVisible(true);
            }
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeInt(UDP_PORT);  // 向server發送自己的UDP port
            DataInputStream dis = new DataInputStream(s.getInputStream());
            int id = dis.readInt();  // 取得自己的id
            this.serverUDPPort = dis.readInt();  // 取得server轉發到client端的訊息的UDP port
            this.USER_DEAD_UDP_PORT = dis.readInt();  // 取得server監聽user死亡的UDP port
            tc.getMyUser().setId(id);  // set user id
            tc.getMyUser().setAlliance((id & 1) == 0 ? true : false);  // 根據user id 來分配陣營
            System.out.println("connect to server successfully...");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (s != null) s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        new Thread(new UDPThread()).start();  // 向server發送和接收遊戲數據

        CA_NewMsg msg = new CA_NewMsg(tc.getMyUser());  // user誕生的消息
        send(msg);
    }

//    user隨機獲得UDP port
    private int getRandomUDPPort() {
        return 55558 + (int) (Math.random() * 9000);
    }

    public void send(Msg msg) {
        msg.send(ds, serverIP, serverUDPPort);
    }

    public class UDPThread implements Runnable {

        byte[] buf = new byte[1024];

        @Override
        public void run() {
            while (null != ds) {
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                try {
                    ds.receive(dp);
                    parse(dp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void parse(DatagramPacket dp) {
            ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
            DataInputStream dis = new DataInputStream(bais);
            int msgType = 0;
            try {
                msgType = dis.readInt();//获得消息类型
            } catch (IOException e) {
                e.printStackTrace();
            }
            Msg msg = null;
            switch (msgType) {//根据消息的类型调用对应消息的解析方法
                case Msg.USER_NEW_MSG:
                    msg = new CA_NewMsg(tc);
                    msg.parse(dis);
                    break;
                case Msg.USER_MOVE_MSG:
                    msg = new CA_MoveMsg(tc);
                    msg.parse(dis);
                    break;
                case Msg.BUBBLE_NEW_MSG:
                    msg = new BubbleNewMsg(tc);
                    msg.parse(dis);
                    break;

                case Msg.USER_DEAD_MSG:
                    msg = new CA_DeadMsg(tc);
                    msg.parse(dis);
                    break;
                case Msg.BUBBLE_DEAD_MSG:
                    msg = new BubbleDeadMsg(tc);
                    msg.parse(dis);
                    break;
                case Msg.USER_ALREADY_EXIST_MSG:
                    msg = new CA_AlreadyExistMsg(tc);
                    msg.parse(dis);
                    break;
                case Msg.USER_REDUCE_BLOOD_MSG:
                    msg = new CA_ReduceBloodMsg(tc);
                    msg.parse(dis);
                    break;
            }
        }
    }

    public void sendClientDisconnectMsg() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(88);
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(UDP_PORT);//发送客户端的UDP端口号, 从服务器Client集合中注销
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != dos) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != baos) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        byte[] buf = baos.toByteArray();
        try {
            DatagramPacket dp = new DatagramPacket(buf, buf.length, new InetSocketAddress(serverIP, USER_DEAD_UDP_PORT));
            ds.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
