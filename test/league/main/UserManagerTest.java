/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.main;

import league.managers.UserManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import league.entities.User;
import league.exceptions.UserManagerException;
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
public class UserManagerTest {

    private League league;
    private UserManager um;

    public UserManagerTest() {
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
            um = league.getUm();
        } catch (IOException ex) {
            Logger.getLogger(UserManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of addUser method, of class UserManager.
     */
    @Test
    public void testVouchUser_3args() throws Exception {
        System.out.println("vouchUser");

        try {
            um.addUser(null, null);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            um.addUser("root", null);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            um.addUser(null, "root");
            fail();
        } catch (IllegalArgumentException ex) {
        }

        User root = um.addUser("root", "root");
        assertEquals(root, um.getUser("root"));
        assertEquals(root.getVouchedBy(), um.getUser("root"));

        //let's add the same user again
        try {
            um.addUser("root", "root");
            fail();
        } catch (UserManagerException ex) {
        }

        try {
            um.addUser("root", "franta");
        } catch (UserManagerException ex) {
        }

        try {
            um.addUser("franta", "root");
        } catch (UserManagerException ex) {
        }

        User player = um.addUser("player", "player1", root);
        assertEquals(player, um.getUser("player"));
        assertEquals(player.getVouchedBy(), um.getUser("root"));
    }

    @Test
    public void testRemoveUser_Long() throws Exception {
        User root = um.getUser("root");
        assertEquals(root, um.getUser("root"));

        um.removeUser(um.getId("root"));

        try {
            um.getUser("root");
            fail();
        } catch (UserManagerException ex) {
        }
    }

    @Test
    public void testRemoveUser_Auth() throws UserManagerException {
        User player = um.getUser("player");
        assertEquals(player, um.getUser("player"));

        um.removeUser("player");

        try {
            um.getUser("player");
            fail();
        } catch (UserManagerException ex) {
        }
    }

    /**
     * Test of getUser method, of class UserManager.
     */
    @Test
    public void testGetUser() {
        System.out.println("getUser");
    }

    /**
     * Test of getVouchees method, of class UserManager.
     */
    @Test
    public void testGetVouchees() {
        System.out.println("getVouchees");
    }

    /**
     * Test of getFriendList method, of class UserManager.
     */
    @Test
    public void testGetFriendList() {
        System.out.println("getFriendList");
    }

    /**
     * Test of getFriendOf method, of class UserManager.
     */
    @Test
    public void testGetFriendOf() {
        System.out.println("getFriendOf");
    }

    @Test
    public void testWhois() throws UserManagerException {
        User root = um.addUser("root", "root");
        User pepa = um.addUser("pepa", "pepa", root);
        User franta = um.addUser("franta", "franta", root);
        User jozin = um.addUser("jozin", "jozin", root);

        root.setAccessLevel(100);
        root.setPromotedBy(root);

        pepa.setAccessLevel(10);
        pepa.setPromotedBy(root);

        franta.setAccessLevel(30);
        franta.setPromotedBy(root);

        jozin.setAccessLevel(70);
        jozin.setPromotedBy(root);

        Set<User> usersToPersist = new HashSet<User>();

        usersToPersist.add(root);
        usersToPersist.add(franta);
        usersToPersist.add(pepa);
        usersToPersist.add(jozin);

        um.persistUsers(usersToPersist);

        System.out.println(um.whois("root"));

        root.setAccessLevel(70);
        System.out.println(um.whois("root"));

        root.setAccessLevel(30);
        System.out.println(um.whois("root"));

        root.setAccessLevel(10);
        System.out.println(um.whois("root"));
    }
}
