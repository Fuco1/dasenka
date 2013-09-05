package league.main;

import league.exceptions.GameManagerException;
import league.managers.UserManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import league.entities.Game;
import league.entities.User;
import league.events.ChatEventListener;
import league.exceptions.UserManagerException;
import league.managers.BetManager;
import league.managers.GameManager;
import league.managers.TopicManager;
import static league.main.Utility.*;
import league.wrappers.ClientWrapper;
import org.apache.log4j.Logger;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class League implements ChatEventListener {

    private EntityManagerFactory emf = null;
    private EntityManager em = null;
    private Set<ClientWrapper> apps = new HashSet<ClientWrapper>();
    private Config config;
    private UserManager um;
    private GameManager gm;
    private TopicManager tm;
    private MessageHandler mh;
    private BetManager bm;
    private static Logger log = Logger.getLogger(League.class);
    private Format dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yy");
    private List<String> authRequests = new ArrayList<String>();
    private Map<User, Integer> kickWarn = new HashMap<User, Integer>();

    public League(Properties config) {
        this.config = new Config(config);
        this.um = new UserManager(this);
        this.mh = new MessageHandler(this);
        this.gm = new GameManager(this);
        this.tm = new TopicManager(this);
        this.bm = new BetManager(this);

        kickWarn.clear();

        emf = Persistence.createEntityManagerFactory(config.getProperty("league.persistence"));
        em = emf.createEntityManager();


        /*try {
        gm.startNewGame(config.getProperty("league.defaultGameType"), config.getProperty("league.defaultGameMode"));

        } catch (GameManagerException ex) {
        java.util.logging.Logger.getLogger(League.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
        java.util.logging.Logger.getLogger(League.class.getName()).log(Level.SEVERE, null, ex);
        }*/

        /*try {
        //um.addUser("5790631", "Fuco");
        um.addUser("Fucko", "Fuco");
        //User u = um.getUser("5790631");
        User u = um.getUser("Fucko");
        u.setPromotedBy(u);
        u.setAccessLevel(100);
        um.persistUser(u);
        List<User> users = new ArrayList<User>();
        users.add(um.addUser("test1", "test1", u));
        users.add(um.addUser("test2", "test2", u));
        users.add(um.addUser("test3", "test3", u));
        users.add(um.addUser("test4", "test4", u));
        users.add(um.addUser("test5", "test5", u));
        users.add(um.addUser("test6", "test6", u));
        users.add(um.addUser("test7", "test7", u));
        users.add(um.addUser("test8", "test8", u));
        users.add(um.addUser("test9", "test9", u));
        users.add(um.addUser("test10", "test10", u));
        for (User us : users) {
        us.setAccessLevel(20);
        }
        getUm().persistUsers(users);
        } catch (UserManagerException ex) {
        //just temp
        } catch (IllegalArgumentException ex) {
        //just temp
        }*/
    }

    public Config getConfig() {
        return config;
    }

    public UserManager getUm() {
        return um;
    }

    public GameManager getGm() {
        return gm;
    }

    public MessageHandler getMh() {
        return mh;
    }

    public EntityManager getEm() {
        return em;
    }

    public TopicManager getTm() {
        return tm;
    }

    public BetManager getBm() {
        return bm;
    }

    public void addApplication(ClientWrapper wrapper) {
        apps.add(wrapper);
    }

    public void handlePublicMsgEvent(String channel, String userNick, String text, ClientWrapper app) {
        System.out.println("[" + dateFormat.format(new Date()) + "] " + channel + " <" + userNick + "> " + text);

        String[] tokens = tokenize(text);

        if (app.getMask(userNick).equalsIgnoreCase(getConfig().getProperty("league.masteraccess"))) {
            if (tokens[0].equals("@say")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < tokens.length; i++) {
                    sb.append(tokens[i]).append(" ");
                }
                app.sendDefaultPublicMsg(new String(sb));
            }
            else if (tokens[0].equals("@fill")) {
                log.info("FILLING THE GAME");
                getMh().sign(channel, "test1", tokens, app);
                getMh().sign(channel, "test2", tokens, app);
                getMh().sign(channel, "test3", tokens, app);
                getMh().sign(channel, "test4", tokens, app);
                getMh().sign(channel, "test5", tokens, app);
                getMh().sign(channel, "test6", tokens, app);
                getMh().sign(channel, "test7", tokens, app);
                getMh().sign(channel, "test8", tokens, app);
                getMh().sign(channel, "test9", tokens, app);
                log.info("FILLING: DONE");
            }
            else if (tokens[0].equals("@addadmin")) {
                try {
                    User u = um.addUser(tokens[1], tokens[2]);
                    u.setPromotedBy(u);
                    u.setAccessLevel(100);
                    um.persistUser(u);
                    app.sendActionMsg(channel, "User " + tokens[1] + " has been added as superadmin!");
                } catch (UserManagerException ex) {
                    //dun care
                } catch (IllegalArgumentException ex) {
                    //dun care
                }
            }
            else if (tokens[0].equals("@reload")) {
                if (tokens.length == 1) {
                    app.sendActionMsg(channel, "Reloading config...");
                    if (getConfig().reload()) {
                        app.sendActionMsg(channel, "Config file reloaded!");
                    }
                    else {
                        app.sendActionMsg(channel, "Error while reloading config file!");
                    }
                }
            }
            else if (tokens[0].equals("@resetstats")) {
                app.sendActionMsg(channel, "Resetting stats...");
                getUm().resetStats();
                app.sendActionMsg(channel, "Stats reset!");
            }
        }

        if (tokens[0].startsWith(".")) {
            User u = getMh().getUserFromNick(userNick, app);
            if ((!(Boolean.parseBoolean(getConfig().getProperty("league.locked"))) ||
              (app.getMask(userNick).equalsIgnoreCase(getConfig().getProperty("league.masteraccess")))) ||
              ((u != null) && (u.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.opAccessLevel"))))) {

                tokens[0] = tokens[0].substring(1);

                String command = getCommand(tokens);
                if (command == null) {
                    return;
                }
                System.out.println("  Command found: " + command);

                Method method = mh.getCommandHandle(command);

                try {
                    method.invoke(mh, channel, userNick, tokens, app);
                } catch (IllegalAccessException ex) {
                    //Logger.getLogger(League.class.getName()).log(Level.SEVERE, null, ex);
                    log.fatal("Call to method " + command + " failed", ex);
                } catch (IllegalArgumentException ex) {
                    log.fatal("Call to method " + command + " failed", ex);
                } catch (InvocationTargetException ex) {
                    log.fatal("Call to method " + command + " failed", ex);
                }
            }
            else {
                log.info("League is locked, can't invoke command: " + userNick);
                Integer warn = kickWarn.get(u);
                if (warn != null) {
                    warn++;
                    kickWarn.put(u, warn);
                }
                else {
                    warn = new Integer(1);
                    kickWarn.put(u, warn);
                    app.sendPrivateMsg(userNick, "This is your last warning, one more .command and you're out of here!");
                }

                if (kickWarn.get(u) == 2) {
                    app.sendKickMsg(channel, userNick, "Bye bye");
                    kickWarn.remove(u);
                }
            }
        }
    }

    public void handlePrivateMsgEvent(String userNick, String text, ClientWrapper app) {
        System.out.println("[" + dateFormat.format(new Date()) + "] " + "<" + userNick + "> whispers to you: " + text);

        String[] tokens = tokenize(text);

        if ((!(Boolean.parseBoolean(getConfig().getProperty("league.locked"))) ||
          (app.getMask(userNick).equalsIgnoreCase(getConfig().getProperty("league.masteraccess"))))) {
            if (tokens[0].equals(".i")) {
                getMh().i(userNick, userNick, tokens, app);
            }
            else if (tokens[0].startsWith(".")) {
                tokens[0] = tokens[0].substring(1);

                String command = getCommand(tokens);
                if (command == null) {
                    return;
                }
                System.out.println("  Command found: " + command);

                Method method = mh.getCommandHandle(command);

                try {
                    method.invoke(mh, userNick, userNick, tokens, app);
                } catch (IllegalAccessException ex) {
                    //Logger.getLogger(League.class.getName()).log(Level.SEVERE, null, ex);
                    log.fatal("Call to method " + command + " failed", ex);
                } catch (IllegalArgumentException ex) {
                    log.fatal("Call to method " + command + " failed", ex);
                } catch (InvocationTargetException ex) {
                    log.fatal("Call to method " + command + " failed", ex);
                }
            }
        }
        else {
            log.info("League is locked, can't invoke command: " + userNick);
        }
    }

    public void handleChannelJoinEvent(String channel, String userNick, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        if (getConfig().getProperty("league.flags.autovouch") != null) {
            User u = getMh().getUserFromNick(userNick, app);
            if (u == null) {
                log.info("User not found, autovouching...");
                getMh().vouch(channel, getConfig().getProperty(app.getType() + ".nick"),
                  new String[]{".vouch", app.getAuth(userNick), userNick}, app);
                log.info("Autovouch done!");
            }
        }
        else {
            log.info("Autovouch disabled");
        }

        handleUserAuthEvent(userNick, app);
    }

    public void handleChannelPartEvent(String channel, String userNick, ClientWrapper app) {
        //make this dynamic from config
        getMh().out(channel, userNick, new String[]{".out"}, app);
    }

    public void handleUserAuthEvent(String userNick, ClientWrapper app) {
        User u = getMh().getUserFromNick(userNick, app);
        if (u == null) {
            log.info("User not found!");
            return;
        }

        if (u.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.opAccessLevel"))) {
            app.sendModeMsg(getConfig().getProperty("irc.mainChannelName"), "+o " + userNick);
        }

        try {
            Game g = getGm().getGame(0);
            if (g.getPlayers().contains(u)) {
                app.sendModeMsg(getConfig().getProperty("irc.mainChannelName"), "+v " + userNick);
            }
        } catch (GameManagerException ex) {
            log.info("No game found! Nothing to do.");
        }
    }

    void addAuthRequest(String userNick) {
        authRequests.add(userNick);
        log.info("Authorisation requested from usernick: " + userNick);
    }

    public void handleUserAuthorisation(String userNick, ClientWrapper app) {
        User u = getMh().getUserFromNick(userNick, app);
        if (u == null) {
            log.info("User not found!");
            log.info("Authorisation failed for user: " + userNick);
            return;
        }

        if (authRequests.contains(userNick)) {
            if (u.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.startingAccessLevel"))) {
                String chan = getConfig().getProperty(app.getType() + ".mainChannelName");
                app.sendInviteMsg(chan, userNick);
                app.sendPrivateMsg(userNick, "Auth verification successful, you are free to join the channel " + chan);
                log.info("Authorisation successful for user: " + userNick);
            }
            authRequests.remove(userNick);
        }
    }

    public void broadCastPublicMsg(String text) {
        for (ClientWrapper w : apps) {
            w.sendBroadcastPublicMsg(text);
        }
    }

    public String getCommand(String[] input) {
        //Set<String> list = config.getCommands();
        Set<String> list = config.getFullCommandList();
        List<String> output = new ArrayList<String>();
        String start = "";
        int inToken = 0;
        int longest = 0;


        do {
            output.clear();
            for (String s : list) {
                //System.out.println("LIST ITEM: " + s);
                String[] sublist = s.split("_");
                if (sublist.length >= inToken + 1) {
                    //System.out.println("  WE CAN COMPARE: " + sublist[inToken] + " to " + input[inToken]);
                    if (sublist[inToken].startsWith(input[inToken])) {
                        //System.out.println("    GOOD");
                        if (sublist.length > longest) {
                            longest = sublist.length;
                        }
                        output.add(s);
                    }
                }
            }

            if (output.size() == 0) {
                return null; //"No such command";
            }
            //start = start + input[inToken];
            inToken++;
        } while (inToken < longest);

        if (output.size() > 1) {
            return null; // "You have to be more specific";
        }
        else {
            String command = output.get(0);
            //first, test if there is a valid remap
            if (config.isValidMapping(command)) {
                command = config.getCommandFromMapping(command);
            }
            else {
                /* if this is an original command, and we have at least one mapping to it,
                check if we shouldn't disable it */
                if (config.hasMapping(command)) {
                    if (config.getProperty("league.command.disable." + command) != null) {
                        log.info("Command " + command + " is disabled!");
                        return null; //command is disabled
                    }
                }
            }
            return command;
        }
    }
}
