package client.client;

import client.bean.Dir;
import client.bean.Explode;
import client.bean.Bubble;
import client.bean.Game;
import client.protocol.BubbleDeadMsg;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Client extends Frame {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    private Image offScreenImage = null;

    private Game myUser;  // client端的user
    private NetClient nc = new NetClient(this);
    private ConDialog dialog = new ConDialog();
    private GameOverDialog gameOverDialog = new GameOverDialog();
    private UDPPortWrongDialog udpPortWrongDialog = new UDPPortWrongDialog();
    private ServerNotStartDialog serverNotStartDialog = new ServerNotStartDialog();

    private List<Bubble> bubbles = new ArrayList<>();  // Bubble集合
    private List<Explode> explodes = new ArrayList<>();  // 爆炸集合
    private List<Game> users = new ArrayList<>();  // users集合
    BufferedImage img1, img2, img3, img4, img5, img6, img7;
    String path = "D:\\網路_期末專題2\\network_programming\\src\\client\\images\\entity";
//    String path = "H:\\網路_期末專題2\\network_programming\\src\\client\\images\\entity";

    {
        try {
            img1 = ImageIO.read(new File(path, "p1.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        try {
            img2 = ImageIO.read(new File(path, "p2.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        try {
            img3 = ImageIO.read(new File(path, "p3.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        try {
            img4 = ImageIO.read(new File(path, "p4.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        try {
            img5 = ImageIO.read(new File(path, "p5.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        try {
            img6 = ImageIO.read(new File(path, "p6.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        try {
            img7 = ImageIO.read(new File(path, "p7.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void paint(Graphics g) {
//        800 * 600

        for (int i = 0; i <= 800; i += 40) {
            for (int j = 0; j <= 800; j += 40) {
                g.drawImage(img7, i, j, null);
            }
        }
        for (int i = 0;i <= 280;i+=40){
            g.drawImage(img1, 600, i, null);
            g.drawImage(img4, 360, i, null);
            g.drawImage(img3, 680, Math.abs(i-800), null);
            g.drawImage(img4, 680, Math.abs(i-720), null);
            g.drawImage(img5, 520, Math.abs(i-720), null);
        }
        for (int i = 0; i <= 800; i += 40) {
            g.drawImage(img3, i, 400, null);
            g.drawImage(img5, i, 320, null);
            g.drawImage(img2, i, 120, null);
            g.drawImage(img2, 200, 200, null);
        }
        for (int j = 0; j <= 600; j += 40) {
            g.drawImage(img1, 120, j, null);
            g.drawImage(img6, 240, j, null);
        }
//        g.drawRect(0, 0, 40, 40);
//        g.drawRect(40, 40, 40, 40);
        g.drawString("bubbles count:" + bubbles.size(), 10, 70);  // 有bug
//        g.drawString("explodes count:" + explodes.size(), 10, 90);  // 有bug

        for (int i = 0; i < bubbles.size(); i++) {
            Bubble m = bubbles.get(i);
            if (m.hitUser(myUser)) {
                BubbleDeadMsg mmsg = new BubbleDeadMsg(m.getUserId(), m.getId());
                nc.send(mmsg);
//                nc.sendClientDisconnectMsg();
//                gameOverDialog.setVisible(true);
            }
            m.draw(g);
        }

        for (int i = 0; i < explodes.size(); i++) {
            Explode e = explodes.get(i);
            e.draw(g);
        }
        for (int i = 0; i < users.size(); i++) {
            Game t = users.get(i);
            t.draw(g);
        }
        if (null != myUser) {
            myUser.draw(g);
        }
    }

    @Override
    public void update(Graphics g) {
        if (offScreenImage == null) {
            offScreenImage = this.createImage(800, 600);
        }
        Graphics gOffScreen = offScreenImage.getGraphics();
        Color c = gOffScreen.getColor();
        gOffScreen.setColor(Color.lightGray);
        gOffScreen.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        gOffScreen.setColor(c);
        paint(gOffScreen);

        g.drawImage(offScreenImage, 0, 0, null);
    }

    public void launchFrame() {
//        this.setLocation(400, 300);
        this.setSize(GAME_WIDTH, GAME_HEIGHT);
        this.setTitle("Client");
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                nc.sendClientDisconnectMsg();  // 註銷訊息
                System.exit(0);
            }
        });
        this.setResizable(false);
        this.setBackground(Color.black);
        this.addKeyListener(new KeyMonitor());
        this.setVisible(true);
        new Thread(new PaintThread()).start();
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        Client tc = new Client();
        tc.launchFrame();
    }


    class PaintThread implements Runnable {
        public void run() {
            while (true) {
                repaint();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class KeyMonitor extends KeyAdapter {

        @Override
        public void keyReleased(KeyEvent e) {
            myUser.keyReleased(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            myUser.keyPressed(e);
        }
    }

    /**
     * 遊戲開始前的對話框
     */
    class ConDialog extends Dialog {
        Button b = new Button("connect to server");
        TextField tfIP = new TextField("127.0.0.1", 15);  // IP address
        TextField tfUserName = new TextField("player1", 8);  // player name

        public ConDialog() {
            super(Client.this, true);
            this.setLayout(new FlowLayout());
            this.add(new Label("server IP:"));
            this.add(tfIP);
            this.add(new Label("name:"));
            this.add(tfUserName);
            this.add(b);
            this.setLocation(500, 400);
            this.pack();
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    setVisible(false);
                    System.exit(0);
                }
            });
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String IP = tfIP.getText().trim();
                    String userName = tfUserName.getText().trim();
                    myUser = new Game(userName, 50 + (int) (Math.random() * (GAME_WIDTH - 100)),
                            50 + (int) (Math.random() * (GAME_HEIGHT - 100)), true, Dir.STOP, Client.this);
                    nc.connect(IP);
                    setVisible(false);
                }
            });
        }
    }

    /**
     * User死亡後退出的對話框
     */
    class GameOverDialog extends Dialog {
        Button b = new Button("exit");

        public GameOverDialog() {
            super(Client.this, true);
            this.setLayout(new FlowLayout());
            this.add(new Label("Game Over"));
            this.add(b);
            this.setLocation(500, 400);
            this.pack();
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
        }
    }

    /**
     * UDP端分配失敗後的對話框
     */
    class UDPPortWrongDialog extends Dialog {
        Button b = new Button("ok");

        public UDPPortWrongDialog() {
            super(Client.this, true);
            this.setLayout(new FlowLayout());
            this.add(new Label("something wrong, please connect again"));
            this.add(b);
            this.setLocation(500, 400);
            this.pack();
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
        }
    }

    /**
     * 連接Server失敗後的對話框
     */
    class ServerNotStartDialog extends Dialog {
        Button b = new Button("ok");

        public ServerNotStartDialog() {
            super(Client.this, true);
            this.setLayout(new FlowLayout());
            this.add(new Label("The server has not been opened yet..."));
            this.add(b);
            this.setLocation(500, 400);
            this.pack();
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
        }
    }

    public void gameOver() {
        this.gameOverDialog.setVisible(true);
    }

    public List<Bubble> getBubbles() {
        return bubbles;
    }

    public void setBubbles(List<Bubble> bubbles) {
        this.bubbles = bubbles;
    }

    public List<Explode> getExplodes() {
        return explodes;
    }

    public void setExplodes(List<Explode> explodes) {
        this.explodes = explodes;
    }

    public List<Game> getUsers() {
        return users;
    }

    public void setUsers(List<Game> users) {
        this.users = users;
    }

    public Game getMyUser() {
        return myUser;
    }

    public void setMyUser(Game myUser) {
        this.myUser = myUser;
    }

    public NetClient getNc() {
        return nc;
    }

    public void setNc(NetClient nc) {
        this.nc = nc;
    }

    public UDPPortWrongDialog getUdpPortWrongDialog() {
        return udpPortWrongDialog;
    }

    public ServerNotStartDialog getServerNotStartDialog() {
        return serverNotStartDialog;
    }
}