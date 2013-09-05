/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.main;

import league.managers.UserManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import league.entities.User;
import league.exceptions.UserManagerException;
import league.wrappers.ClientWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matus
 */
public class MessageHandlerTest {

    private ClientWrapper wrapper = new ClientWrapper() {

        public void sendPublicMsg(String channel, String text) {
            System.out.println("Channel MSG: " + channel + " " + text);
        }

        public void sendPrivateMsg(String target, String text) {
            System.out.println("Private MSG: " + target + " " + text);
        }

        public void sendNoticeMsg(String target, String text) {
            System.out.println("Notice MSG: " + target + " " + text);
        }

        public void sendActionMsg(String target, String text) {
            System.out.println("Action MSG: " + target + " " + text);
        }

        public void sendKickMsg(String channel, String target, String text) {
            System.out.println("Kick");
        }

        public void sendBroadcastPublicMsg(String text) {
            System.out.println("Broadcast Channel MSG: " + text);
        }

        public void sendDefaultPublicMsg(String text) {
            System.out.println("Default Channel MSG: " + text);
        }

        public String getAuth(String nick) {
            if (nick.equals("koren")) {
                return "root";
            }
            else if (nick.equals("lopar")) {
                return "l0p0";
            }
            else if (nick.equals("dude")) {
                return "dude";
            }
            return null;
        }
    };
    private League league;
    private UserManager um;
    private MessageHandler mh;

    public MessageHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        try {
            Properties config = new Properties();
            config.load(new FileInputStream("clientConfig.properties"));
            league = new League(config);
            league.addApplication(wrapper);
            um = league.getUm();
            mh = league.getMh();
        } catch (IOException ex) {
            Logger.getLogger(UserManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of vouch method, of class MessageHandler.
     */
    @Test
    public void testVouch() throws UserManagerException {
        String[] token1 = {".vouch", "dude"};

        mh.vouch("123456", "koren", token1, wrapper);

        String[] token2 = {".vouch", "dude", "dude123"};
        String[] token3 = {".vouch", "l0p0", "lopo"};
        mh.vouch("123456", "koren", token2, wrapper);

        User root = um.addUser("root", "root");
        mh.vouch("123456", "koren", token2, wrapper);

        root.setAccessLevel(100);
        mh.vouch("123456", "koren", token2, wrapper);
        mh.vouch("123456", "koren", token3, wrapper);

        mh.vouch("123456", "koren", token2, wrapper);

        token2[1] = "lol";
        mh.vouch("123456", "koren", token2, wrapper);


    }

    /**
     * Test of unvouch method, of class MessageHandler.
     */
    @Test
    public void testUnvouch() throws UserManagerException {
        User root = um.getUser("root");
        User dude = um.getUser("dude");
        User lopo = um.getUser("l0p0");

        root.getFriendList().add(dude);
        root.getFriendList().add(lopo);

        dude.getFriendList().add(lopo);

        Set<User> p = new HashSet<User>();
        p.add(root);
        p.add(dude);
        p.add(lopo);
        um.persistUsers(p);

        root = um.getUser("root");
        dude = um.getUser("dude");
        lopo = um.getUser("l0p0");

        System.out.println("root: FriendList");
        for (User u : root.getFriendList()) {
            System.out.println(u.getName());
        }

        System.out.println("root: FriendOf");
        for (User u : root.getFriendOf()) {
            System.out.println(u.getName());
        }

        System.out.println("dude: FriendList");
        for (User u : dude.getFriendList()) {
            System.out.println(u.getName());
        }

        System.out.println("dude: FriendOf");
        for (User u : dude.getFriendOf()) {
            System.out.println(u.getName());
        }

        System.out.println("lopo: FriendList");
        for (User u : lopo.getFriendList()) {
            System.out.println(u.getName());
        }

        System.out.println("lopo: FriendOf");
        for (User u : lopo.getFriendOf()) {
            System.out.println(u.getName());
        }

        String[] token1 = {".unvouch", "dude"};
        mh.unvouch("123456", "koren", token1, wrapper);

        root = um.getUser("root");
        dude = um.getUser("dude");
        lopo = um.getUser("l0p0");

        System.out.println("root: FriendList");
        for (User u : root.getFriendList()) {
            System.out.println(u.getName());
        }

        System.out.println("root: FriendOf");
        for (User u : root.getFriendOf()) {
            System.out.println(u.getName());
        }

        System.out.println("dude: FriendList");
        for (User u : dude.getFriendList()) {
            System.out.println(u.getName());
        }

        System.out.println("dude: FriendOf");
        for (User u : dude.getFriendOf()) {
            System.out.println(u.getName());
        }

        System.out.println("lopo: FriendList");
        for (User u : lopo.getFriendList()) {
            System.out.println(u.getName());
        }

        System.out.println("lopo: FriendOf");
        for (User u : lopo.getFriendOf()) {
            System.out.println(u.getName());
        }

        String[] token2 = {".vouch", "dude", "dude123"};
        mh.vouch("123456", "koren", token2, wrapper);

        mh.unvouch("123456", "lopar", token1, wrapper);

        String[] token3 = {".unvouch", "asdsgwt"};
        mh.unvouch("123456", "koren", token3, wrapper);

        dude.setAccessLevel(30);
        mh.unvouch("123456", "koren", token1, wrapper);
    }

    /**
     * Test of invite method, of class MessageHandler.
     */
    @Test
    public void testInvite() throws UserManagerException {
        System.out.println("------------ INVITE ------------");
        String[] token1 = {".invite", "dude", "blabla"};
        mh.invite("123456", "koren", token1, wrapper);

        String[] token2 = {".invite", "dude"};
        mh.invite("123456", "koren", token2, wrapper);

        um.whois(um.getUser("root"));

        String[] token3 = {".invite", "pimp0", "pimpo"};
        mh.invite("123456", "koren", token3, wrapper);

        String[] token4 = {".invite", "pimp0", "pimpo"};
        mh.invite("123456", "lopar", token4, wrapper);

        String[] token5 = {".invite", "slambik", "slambik"};
        mh.invite("123456", "dude", token5, wrapper);

        System.out.println(um.whois(um.getUser("root")));
        System.out.println(um.whois(um.getUser("l0p0")));
        System.out.println(um.whois(um.getUser("dude")));
    }

    /**
     * Test of evict method, of class MessageHandler.
     */
    @Test
    public void testEvict() {
    }

    /**
     * Test of promote method, of class MessageHandler.
     */
    @Test
    public void testPromote() {
    }

    /**
     * Test of demote method, of class MessageHandler.
     */
    @Test
    public void testDemote() {
    }
}