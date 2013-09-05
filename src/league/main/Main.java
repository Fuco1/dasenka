package league.main;

import league.managers.UserManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import league.entities.User;
import league.exceptions.UserManagerException;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class Main {

    public static void main(String[] args) throws FileNotFoundException, IOException, UserManagerException {
        /*EntityManagerFactory emFactory =
        Persistence.createEntityManagerFactory("PickupLeaguePU");
        EntityManager em = emFactory.createEntityManager();*/

        Properties config = new Properties();
        config.load(new FileInputStream("clientConfig.properties"));

        League league = new League(config);
        /*User u = league.getUm().getUser("Fucko");
        User v = league.getUm().getUser("L0P0");
        System.out.println(new Date().getTime());
        for (int j = 0; j < 30; j++) {
            System.out.println("Number of games: " + league.getGm().getCommonGames(u, v));
        }
        System.out.println(new Date().getTime());*/

    /*UserManager um = league.getUm();
    User fuco = um.addUser("Fucko", "Fuco");
    User lopo = um.addUser("Lopoo", "Lopo", fuco);

    User u1 = um.getUser("Fucko");
    System.out.println(u1.getName());
    System.out.println(u1.getVouchedUsers().getClass());
    for (User a : u1.getVouchedUsers()) {
    System.out.println(a.getName());
    }


    //List<User> result = em.createQuery("SELECT u.vouchedUsers FROM User u WHERE u.auth = ?1").setParameter(1, "Fucko").getResultList();

    System.out.println("VouchList");
    for (User u : um.getVouchees("Fucko")) {
    System.out.println(u.getName());
    }

    //result = em.createQuery("SELECT u.friendList FROM User u WHERE u.auth = ?1").setParameter(1, "Fucko").getResultList();

    System.out.println("FriendList");
    for (User u : um.getFriendList("Fucko")) {
    System.out.println(u.getName());
    }

    //result = em.createQuery("SELECT u.friendOf FROM User u WHERE u.auth = ?1").setParameter(1, "Lopoo").getResultList();

    System.out.println("FriendOf");
    for (User u : um.getFriendOf("Fucko")) {
    System.out.println(u.getName());
    }*/


    /*Order order = new Order();
    order.setCustomer("Pepa");
    OrderField of = new OrderField();
    of.setAmount(10);
    of.setDescription("polozka cislo 5");
    of.setOrder(order);

    OrderField of2 = new OrderField();
    of2.setAmount(4);
    of2.setDescription("polozka cislo 6");
    of2.setOrder(order);

    order.getFields().add(of);
    order.getFields().add(of2);

    em.getTransaction().begin();
    em.persist(order);
    Order order = em.find(Order.class, 1L);
    System.out.println(order);
    em.remove(order);
    em.getTransaction().commit();*/

    // emFactory.close();
    }
}
