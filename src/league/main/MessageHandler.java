package league.main;

import java.lang.reflect.Method;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import league.entities.Bet;
import league.entities.Game;
import league.entities.GameExp;
import league.entities.GameGroup;
import league.entities.GameUserOrder;
import league.entities.Results;
import league.entities.User;
import league.entities.nodb.Vote;
import league.exceptions.GameManagerException;
import league.exceptions.UserManagerException;
import league.managers.BetManager;
import league.managers.GameManager;
import league.managers.TopicManager;
import league.managers.UserManager;
import league.wrappers.ClientWrapper;
import org.apache.log4j.Logger;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class MessageHandler {

    private static final String REPLACE_VOTE_ID_PREFIX = "rv";

    private class MsgStack {

        private List<String> list = new ArrayList<String>();
        private List<String> nickList = new ArrayList<String>();

        public void add(String nick, String str) {
            nickList.add(nick);
            list.add(str);
        }

        public int size() {
            return list.size();
        }

        public List<String> getList() {
            return list;
        }

        public List<String> getNickList() {
            return nickList;
        }
    }
    private League league;
    private static Logger log = Logger.getLogger(MessageHandler.class);
    private Map<ClientWrapper, MsgStack> signOutStack = new HashMap<ClientWrapper, MsgStack>();
    private Timer msgFlush = new Timer("msgFlush", true);
    private Map<String, Date> lastInvoke = new HashMap<String, Date>();

    public MessageHandler(League league) {
        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                //log.info("Timer executed!");
                for (ClientWrapper cw : signOutStack.keySet()) {
                    StringBuilder sb = new StringBuilder();
                    if (signOutStack.get(cw).size() > 0) {

                        Game g = null;
                        try {
                            g = getGm().getGame(0);
                        } catch (GameManagerException ex) {
                            signOutStack.clear();
                            log.error("No game found!");
                            return;
                        }
                        int len = 0;

                        StringBuilder sbvn = new StringBuilder();
                        StringBuilder sbdn = new StringBuilder();
                        StringBuilder sbv = new StringBuilder();
                        StringBuilder sbd = new StringBuilder();

                        //for (String s : signOutStack.get(cw).getList()) {
                        for (int i = 0; i < signOutStack.get(cw).size(); i++) {
                            String s = signOutStack.get(cw).getList().get(i);
                            sb.append(s).append(" ");

                            if (!g.getType().equals("challenge")) {
                                //TODO: ugly hack, we should introduce an enum to distinct in/out.
                                if ((s.endsWith("in;")) || (s.endsWith("group;"))) {
                                    sbv.append("v");
                                    len++;
                                    sbvn.append(signOutStack.get(cw).getNickList().get(i)).append(" ");
                                }
                                else if (s.endsWith("out;")) {
                                    sbd.append("v");
                                    len++;
                                    sbdn.append(signOutStack.get(cw).getNickList().get(i)).append(" ");
                                }

                                if (len >= 6) {
                                    cw.sendModeMsg(getConfig().getProperty(cw.getType() + ".mainChannelName"),
                                      new String(
                                      ((sbv.length() > 0) ? ("+" + sbv) : ("")) +
                                      ((sbd.length() > 0) ? ("-" + sbd) : ("")) + " " + sbvn + sbdn));
                                    len = 0;

                                    sbvn = new StringBuilder();
                                    sbdn = new StringBuilder();
                                    sbv = new StringBuilder();
                                    sbd = new StringBuilder();
                                }
                            }
                        }

                        if (!g.getType().equals("challenge")) {
                            cw.sendModeMsg(getConfig().getProperty(cw.getType() + ".mainChannelName"),
                              new String(
                              ((sbv.length() > 0) ? ("+" + sbv) : ("")) +
                              ((sbd.length() > 0) ? ("-" + sbd) : ("")) + " " + sbvn + sbdn));
                        }

                        sb.append(" ");
                        /*Formatter f = new Formatter();
                        sb.append(f.format(getConfig().getProperty("league.gameType.slots.format." + g.getType()),
                        g.getPlayers().size()));*/
                        sb.append(getPlayerSlots(g.getType(), g.getPlayers().size()));

                        //sb.append(sb);
                        cw.sendActionMsg(getConfig().getProperty(cw.getType() + ".mainChannelName"),
                          new String(sb));
                    }
                }
                signOutStack.clear();
            }
        };

        this.league = league;
        msgFlush.schedule(timerTask, 7000, 7000);

    }

    public void i(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(userNick)) {
            log.info("Not a pm");
            return;
        }
        league.addAuthRequest(userNick);
        app.requestAuthAndInvite(userNick);
    }

    /***************************************************************************
     *  USER MANAGEMENT
     *  VOUCH/UNVOUCH/INVITE/EVICT/PROMOTE/DEMOTE
     **************************************************************************/

    /*
     * token[1] = auth
     * token[2] = nick
     */
    public void vouch(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 3) {
            app.sendNoticeMsg(userNick, "Usage: .vouch auth nick");
            return;
        }


        /*User u = getUserFromNick(userNick, app);
        if (u == null) {
        log.error("Voucher not found");
        return;
        }

        if (u.getAccessLevel() < getConfig().getCommandAccess("vouch")) {
        log.error("Your access level is too low for this operation!");
        return;
        }*/

        User u = checkAccess(userNick, app, "vouch");
        if (u == null) {
            return;
        }

        if (u.getVouchedUsers().size() >= getConfig().getRankPromoteLimitByAccess(
          u.getAccessLevel(),
          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel")))) {
            app.sendNoticeMsg(userNick, "You can't vouch more players!");
            return;
        }

        try {
            User v = league.getUm().addUser(token[1], token[2], u);
            v.setAccessLevel(Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel")));
            v.setExp(Integer.parseInt(getConfig().getProperty("league.exp.startingExp.vouch")));
            league.getUm().persistUser(v);
        } catch (UserManagerException ex) {
            app.sendNoticeMsg(userNick, ex.getMessage());
            return;
        } catch (IllegalArgumentException ex) {
            app.sendNoticeMsg(userNick, ex.getMessage());
            return;
        }

        //Fuco, garena/autovouch compatibility added
        //2009-10-09
        if (!((getConfig().getProperty("league.flags.autovouch") != null) &&
          (userNick.equalsIgnoreCase(getConfig().getProperty(app.getType() + ".nick"))))) {
            app.sendActionMsg(actionTarget, "User " + token[1] + " has been vouched with username " + token[2]);
        }
        else {
            log.info("User " + token[1] + " has been vouched with username " + token[2]);
        }

    }

    /*
     * token[1] = auth/nick
     */
    public void unvouch(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .unvouch auth/nick");
            return;
        }

        /*User u = getUserFromNick(userNick, app);
        if (u == null) {
        log.error("Voucher not found");
        return;
        }

        if (u.getAccessLevel() < getConfig().getCommandAccess("unvouch")) {
        log.error("Your access level is too low for this operation!");
        return;
        }*/

        User u = checkAccess(userNick, app, "unvouch");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        if (u.getAccessLevel() < v.getVouchedBy().getAccessLevel()) {
            app.sendNoticeMsg(userNick, "You can't remove another voucher's users unless you are higher-level then (s)he!");
            return;
        }

        if (v.getAccessLevel() > Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
            app.sendNoticeMsg(userNick, "You have to demote user to the default rank before unvouching him!");
            return;
        }

        for (User r : v.getFriendOf()) {
            r.getFriendList().remove(v);
        }

        v.getFriendList().clear();
        league.getUm().suspendUser(v);
        app.sendActionMsg(actionTarget, "User " + v.getName() + " has been unvouched!");
    }

    /*
     * token[1] = auth
     * token[2] = nick
     */
    public void invite(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 3) {
            app.sendNoticeMsg(userNick, "Usage: .invite auth nick");
            return;
        }

        /*User u = getUserFromNick(userNick, app);
        if (u == null) {
        log.error("Inviter not found");
        return;
        }

        if (u.getAccessLevel() < getConfig().getCommandAccess("invite")) {
        log.error("Your access level is too low for this operation!");
        return;
        }*/

        User u = checkAccess(userNick, app, "invite");
        if (u == null) {
            return;
        }

        /*if (u.getVouchedUsers().size() >= getConfig().getRankPromoteLimitByAccess(
        u.getAccessLevel(),
        Integer.parseInt(getConfig().getProperty("league.startingAccessLevel")))) {
        app.sendNoticeMsg(userNick, "You can't invite more players!");
        return;
        }*/
        //FIX?
        if ((getUm().getPromotedNum(u, 0)) >= getConfig().getRankPromoteLimitByAccess(
          u.getAccessLevel(),
          Integer.parseInt(getConfig().getProperty("league.startingAccessLevel")))) {
            app.sendNoticeMsg(userNick, "You can't invite more players!");
            return;
        }

        try {
            User v = league.getUm().addUser(token[1], token[2], u);
            v.setAccessLevel(Integer.parseInt(getConfig().getProperty("league.startingAccessLevel")));
            v.setExp(Integer.parseInt(getConfig().getProperty("league.exp.startingExp.invite")));
            u.getFriendList().add(v);
            league.getUm().persistUser(v);
        } catch (UserManagerException ex) {
            app.sendNoticeMsg(userNick, ex.getMessage());
            return;
        } catch (IllegalArgumentException ex) {
            app.sendNoticeMsg(userNick, ex.getMessage());
            return;
        }

        app.sendActionMsg(actionTarget, "User " + token[1] + " has been invited with username " + token[2]);
    }

    /*
     * token[1] = auth/nick
     */
    public void evict(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .evict auth/nick");
            return;
        }

        /*User u = getUserFromNick(userNick, app);
        if (u == null) {
        log.error("Inviter not found");
        return;
        }

        if (u.getAccessLevel() < getConfig().getCommandAccess("evict")) {
        log.error("Your access level is too low for this operation!");
        return;
        }*/

        User u = checkAccess(userNick, app, "evict");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        if (!u.equals(v.getVouchedBy())) {
            app.sendNoticeMsg(userNick, "You can only evict players invited by yourself!");
            return;
        }

        if (v.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
            app.sendNoticeMsg(userNick, "You can't evict regular players! Use [.unvouch auth/nick] to do so!");
            return;
        }

        for (User r : v.getFriendOf()) {
            r.getFriendList().remove(v);
        }

        v.getFriendList().clear();
        league.getUm().suspendUser(v);
        app.sendActionMsg(actionTarget, "User " + v.getName() + " has been evicted!");
    }

    /*
     * token[1] = auth/nick
     */
    public void promote(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .promote auth/nick");
            return;
        }

        /*User u = getUserFromNick(userNick, app);
        if (u == null) {
        log.error("Promoter not found");
        return;
        }

        if (u.getAccessLevel() < getConfig().getCommandAccess("promote")) {
        log.error("Your access level is too low for this operation!");
        return;
        }*/

        User u = checkAccess(userNick, app, "promote");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendNoticeMsg(userNick, "User not found!");
            return;
        }

        if (v.getPromotedBy() != null) {
            if (v.getPromotedBy().getAccessLevel() > u.getAccessLevel()) {
                app.sendNoticeMsg(userNick, "You can't promote another voucher's users unless you are higher-level then (s)he!");
                return;
            }
        }


        int currentRankNum = getConfig().getRankNumByAccess(v.getAccessLevel());
        if (currentRankNum >= getConfig().getRankNum()) {
            log.error("There is no higher rank!");
            return;
        }
        int nextRankNum = currentRankNum + 1;

        if (v.getVouchedBy() != null) {
            if (getConfig().getRankPromoteMode(currentRankNum) == RankPromoteMode.INVITE) {
                if (v.getVouchedBy().getAccessLevel() > u.getAccessLevel()) {
                    app.sendNoticeMsg(userNick, "You can't promote another voucher's users unless you are higher-level then (s)he!");
                    return;
                }
            }
        }

        //TODO: if you promote your own invited user to user, you need 1 free slot.
        if (league.getUm().getPromotedNum(u, nextRankNum) >= getConfig().getRankPromoteLimitByAccess(
          u.getAccessLevel(), getConfig().getRankAccessByNum(nextRankNum))) {
            app.sendNoticeMsg(userNick,
              "You can't promote more " + getConfig().getRankStringByNum(currentRankNum) +
              " to " + getConfig().getRankStringByNum(nextRankNum) + "!");
            return;
        }

        switch (getConfig().getRankPromoteMode(currentRankNum)) {
            case INVITE:
                v.setAccessLevel(getConfig().getRankAccessByNum(nextRankNum));
                v.setVouchedBy(u);
                league.getUm().persistUser(v);
                break;
            case DEFAULT:
            case PROMOTE:
                v.setAccessLevel(getConfig().getRankAccessByNum(nextRankNum));
                v.setPromotedBy(u);
                league.getUm().persistUser(v);
                break;
        }
        app.sendActionMsg(actionTarget, "User " + v.getName() + " promoted from [" +
          getConfig().getRankStringByNum(currentRankNum) + "] to [" + getConfig().getRankStringByNum(nextRankNum) + "]");
    }

    /*
     * token[1] = auth/nick
     */
    public void demote(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .demote auth/nick");
            return;
        }

        User u = checkAccess(userNick, app, "demote");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        if (v.getPromotedBy() != null) {
            if (v.getPromotedBy().getAccessLevel() > u.getAccessLevel()) {
                app.sendNoticeMsg(userNick, "You can't demote another voucher's users unless you are higher-level then (s)he!");
                return;
            }
        }

        int currentRankNum = getConfig().getRankNumByAccess(v.getAccessLevel());
        if (currentRankNum <= 0) {
            log.error("There is no lower rank!");
            return;
        }
        int prevRankNum = currentRankNum - 1;

        //TODO: if you promote your own invited user to user, you need 1 free slot.
        while (league.getUm().getPromotedNum(u, prevRankNum) >= getConfig().getRankPromoteLimitByAccess(
          u.getAccessLevel(), getConfig().getRankAccessByNum(prevRankNum))) {
            /*app.sendNoticeMsg(userNick,
            "You can't promote more " + getConfig().getRankStringByNum(currentRankNum) +
            " to " + getConfig().getRankStringByNum(prevRankNum) + "!");
            return;*/
            prevRankNum--;
            if (prevRankNum <= 0) {
                app.sendNoticeMsg(userNick, "You can't demote this member. No free slots available!");
                return;
            }
        }

        if (v.getVouchedBy() != null) {
            if (getConfig().getRankPromoteMode(prevRankNum) == RankPromoteMode.INVITE) {
                if (v.getVouchedBy().getAccessLevel() > u.getAccessLevel()) {
                    app.sendNoticeMsg(userNick, "You can't demote another voucher's users unless you are higher-level then (s)he!");
                    return;
                }
            }
        }

        switch (getConfig().getRankPromoteMode(prevRankNum)) {
            case INVITE:
            case DEFAULT:
                v.setAccessLevel(getConfig().getRankAccessByNum(prevRankNum));
                v.setPromotedBy(null);
                league.getUm().persistUser(v);
                break;
            case PROMOTE:
                v.setAccessLevel(getConfig().getRankAccessByNum(prevRankNum));
                //u.setPromotedBy(u);
                league.getUm().persistUser(v);
                break;
        }
        app.sendActionMsg(actionTarget, "User " + v.getName() + " demoted from [" +
          getConfig().getRankStringByNum(currentRankNum) + "] to [" + getConfig().getRankStringByNum(prevRankNum) + "]");
    }

    public void voiduser(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .voiduser auth/nick");
            return;
        }

        User u = checkAccess(userNick, app, "voiduser");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        String user = v.getName();
        try {
            getUm().removeUser(v.getId());
        } catch (UserManagerException ex) {
            log.info(ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            log.info(ex.getMessage(), ex);
        }

        app.sendActionMsg(actionTarget, "User " + user + " completely removed from database");
    }

    public void changename(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 3) {
            app.sendNoticeMsg(userNick, "Usage: .changename old new");
            return;
        }

        User u = checkAccess(userNick, app, "changename");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        String oldname = v.getName();
        v.setName(token[2]);
        getUm().persistUser(v);
        app.sendActionMsg(actionTarget, "Username " + oldname + " changed to " + v.getName());
    }


    /*
     * token[1] = auth/nick or blank for whoami
     */
    public void whois(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        /*User u = null;
        if (token.length == 2) {
        u = getUserFromParam(token[1], app);
        }
        else {
        u = getUserFromNick(userNick, app);
        }

        if (u == null) {
        app.sendActionMsg(actionTarget, "User not found!");
        return;
        }*/

        User u = checkAccess(userNick, app, "whois");
        if (u == null) {
            return;
        }

        if (token.length == 2) {
            u = getUserFromParam(token[1], app);
        }

        if (u == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        app.sendActionMsg(actionTarget, league.getUm().whois(u));
    }

    /***************************************************************************
     *  FRIEND MANAGEMENT
     *  LIST/OF/ADD/REMOVE
     **************************************************************************/

    /*
     * token[2] = auth/nick or blank for my own list
     */
    public void friend_list(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "friend_list");
        if (u == null) {
            return;
        }

        if (token.length == 3) {
            u = getUserFromParam(token[2], app);
        }

        if (u == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        app.sendActionMsg(actionTarget, league.getUm().friendlist(u));
    }

    /*
     * token[2] = auth/nick or blank for my own list
     */
    public void friend_of(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "friend_of");
        if (u == null) {
            return;
        }

        if (token.length == 3) {
            u = getUserFromParam(token[2], app);
        }

        if (u == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        app.sendActionMsg(actionTarget, league.getUm().friendof(u));
    }

    /*
     * token[2] = auth/nick of user you want to add as a friend
     */
    public void friend_add(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 3) {
            //app.sendNoticeMsg(userNick, "Usage: .friend add auth/nick");
            return;
        }

        User u = checkAccess(userNick, app, "friend_add");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[2], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        if (!u.getFriendList().contains(v)) {
            u.getFriendList().add(v);
            league.getUm().persistUser(u);
            app.sendActionMsg(actionTarget, "User " + v.getName() + " has been added to friendlist!");
        }
    }

    /*
     * token[2] = auth/nick of user you want to remove from friendlist
     */
    public void friend_remove(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        if (token.length != 3) {
            //app.sendNoticeMsg(userNick, "Usage: .friend remove auth/nick");
            return;
        }

        User u = checkAccess(userNick, app, "friend_remove");
        if (u == null) {
            return;
        }

        User v = getUserFromParam(token[2], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        if (u.getFriendList().remove(v)) {
            league.getUm().persistUser(u);
            app.sendActionMsg(actionTarget, "User " + v.getName() + " has been removed from friendlist!");
        }
    }

    /***************************************************************************
     *  GAME MANAGEMENT
     *  STARTGAME/SIGN/JOIN/OUT/LEAVE
     **************************************************************************/
    public void startgame(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "startgame");
        if (u == null) {
            return;
        }

        String type = getConfig().getProperty("league.defaultGameType.startgame");
        String mode = getConfig().getProperty("league.defaultGameMode");

        if (token.length >= 2) {
            if (getConfig().getGameTypeFromSwitch(token[token.length - 1]) != null) {
                type = getConfig().getGameTypeFromSwitch(token[token.length - 1]);
            }
            if (getGm().checkMode(token[1], type)) {
                mode = token[1];
            }
        }

        boolean onlyStart = false;
        if (token[token.length - 1].equals("-l")) {
            log.info("Startgame: only start mode enabled!");
            onlyStart = true;
        }

        if (u.getCurrentGame() != null) {
            log.info("User is already in game! Can't start another game " + u.getName());
            return;
        }

        try {
            Game g = getGm().startNewGame(type, mode);
            if (!onlyStart) {
                g.getPlayers().add(u);
            }
            g.setOwner(u);

            if (!onlyStart) {
                if (type.equals("startgame")) {
                    GameGroup group = new GameGroup(g, u);
                    group.getGroupMembers().add(u);
                    g.getGroups().add(group);
                }
            }

            getGm().persistGame(g);
            getTm().setTopic(app);
            app.sendActionMsg(channel, getConfig().getProperty("league.channelName") + " game starts in 450 seconds. Type .sign " +
              ((type.equals("startgame")) ? "or .join <friend's name> " : "") + "to enter");
            app.sendModeMsg(getConfig().getProperty(app.getType() + ".mainChannelName"),
              new String("+v " + userNick));
        } catch (GameManagerException ex) {
            app.sendActionMsg(channel, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Failed to start game!");
        }
    }

    public void challenge(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "challenge");
        if (u == null) {
            return;
        }

        String type = getConfig().getProperty("league.defaultGameType.challenge");
        String mode = getConfig().getProperty("league.defaultGameMode");

        if (token.length != 2) {
            log.error("No opponent specified! User:" + u.getName());
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            return;
        }

        if (v.getAccessLevel() < getConfig().getCommandAccess("challenge")) {
            log.info("Oponent's access too low!" + u.getName() + " " + v.getName());
            return;
        }

        if (u.getCurrentGame() != null) {
            log.info("User is already in game! Can't start another game " + u.getName());
            return;
        }

        if (v.getCurrentGame() != null) {
            log.info("User is already in game! Can't start another game " + u.getName());
            return;
        }

        try {
            Game g = getGm().startNewGame(type, mode);
            g.getPlayers().add(u);
            g.getPlayers().add(v);
            g.setOwner(u);

            getGm().persistGame(g);
            getGm().initNewChall(u, v, g);
            getTm().setTopic(app);
            app.sendActionMsg(channel, getConfig().getProperty("league.channelName") + " game starts in 450 seconds. Type .sign " +
              ((type.equals("startgame")) ? "or .join <friend's name> " : "") + "to enter");
            app.sendModeMsg(getConfig().getProperty(app.getType() + ".mainChannelName"),
              new String("+vv " + userNick + " " + app.getNick(v.getAuth())));
        } catch (GameManagerException ex) {
            app.sendActionMsg(channel, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Failed to start game!");
        }
    }

    public void sign(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "sign");
        if (u == null) {
            return;
        }

        if (u.getCurrentGame() != null) {
            log.info("User is already in game! Can't sign to another game " + u.getName());
            return;
        }

        try {
            Game g = getGm().getGame(0);
            if ((g.getType().equals("startgame")) || (g.getType().equals("regular"))) {
                if (g.getPlayers().size() >= getConfig().getPlayerNum()) {
                    log.info("Game is full, no more players can join! " + g.getType() + " " + u.getName());
                    return;
                }
            }
            else if (g.getType().equals("challenge")) {
                if (g.getPlayers().size() >= Integer.parseInt(getConfig().getProperty("league.challenge.poolSizeLimit"))) {
                    log.info("Game is full, no more players can join! " + g.getType() + " " + u.getName());
                    return;
                }
            }


            if (g.getMatchResult().equals("picking")) {
                log.info("Picking is in progress, can't sign into game! " + u.getName());
                return;
            }
            if (!g.getPlayers().contains(u)) {
                g.getPlayers().add(u);

                if (g.getType().equals("startgame")) {
                    GameGroup group = new GameGroup(g, u);
                    group.getGroupMembers().add(u);
                    g.getGroups().add(group);
                }

                getGm().persistGame(g);
                //app.sendActionMsg(channel, "User " + u.getName() + " signed in;");
                addMsgToStack(app, userNick, getUm().getNameWithRank(u) + " signed in;");
                if (getConfig().getProperty("league.signout.inPrivate") != null) {
                    app.sendPrivateMsg(userNick, getUm().getNameWithRank(u) + " signed in; " + getPlayerSlots(g.getType(), g.getPlayers().size()));
                }

                //Added autostart functionality //Fuco
                //2009-10-09
                if (getConfig().getProperty("league.flags.autostartgame") != null) {
                    if (g.getPlayers().size() >= getConfig().getPlayerNum()) {
                        log.info("Autoconfirming game: " + g.getNum());
                        confirmstart(getConfig().getProperty(app.getType() + ".mainChannelName"),
                          getConfig().getProperty(app.getType() + ".nick"),
                          new String[]{".confirmstart"}, app);
                    }
                }

            }
        } catch (GameManagerException ex) {
            //app.sendNoticeMsg(channel, ex.getMessage());
            log.error("Unable to sign game! User: " + u.getName(), ex);
        }
    }

    public void join(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "join");
        if (u == null) {
            return;
        }

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .join <friend's auth/nick>");
            return;
        }

        if (u.getCurrentGame() != null) {
            log.info("User is already in game! Can't join to another game " + u.getName());
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            return;
        }

        try {
            Game g = getGm().getGame(0);

            if (!g.getType().equals("startgame")) {
                log.info("Game type is not startgame, can't join player groups! " + u.getName());
                return;
            }

            if (g.getPlayers().size() >= getConfig().getPlayerNum()) {
                log.info("Game is full, no more players can join! " + g.getType() + " " + u.getName());
                return;
            }

            if (!g.getPlayers().contains(u)) {
                if (!g.getPlayers().contains(v)) {
                    log.info("Player " + v.getName() + " is not signed in");
                    return;
                }

                if (!u.getFriendOf().contains(v)) {
                    log.info("Player " + v.getName() + " is not friend of " + u.getName());
                    return;
                }

                GameGroup group = g.getUserGroup(v);
                if (group.getGroupMembers().size() >= getConfig().getPlayerNumTeam()) {
                    log.info("Unable to join game! User: " + u.getName() + " Group is full!");
                    return;
                }

                int i = 0;
                for (User gu : group.getGroupMembers()) {
                    if (gu.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
                        i++;
                    }
                }

                if ((i >= Integer.parseInt(getConfig().getProperty("league.groups.regularLimit"))) &&
                  (u.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel")))) {
                    log.info("Too many regulars in this group! Can't join group! " + u.getName());
                    return;
                }

                group.getGroupMembers().add(u);

                int[] groups = new int[g.getGroups().size()];
                i = 0;
                for (GameGroup gg : g.getGroups()) {
                    groups[i++] = gg.getGroupMembers().size();
                }

                if (!getGm().verifyGroups(groups)) {
                    group.getGroupMembers().remove(u);
                    app.sendActionMsg(channel, "Unable to join game: Cannot create teams from current groups!");
                    return;
                }

                g.getPlayers().add(u);

                getGm().persistGame(g);
                //app.sendActionMsg(channel, "User " + u.getName() + " signed out;");
                addMsgToStack(app, userNick, getUm().getNameWithRank(u) + " joined " + v.getName() + "\'s group;");
            }
            //}
        } catch (GameManagerException ex) {
            //app.sendNoticeMsg(channel, ex.getMessage());
            log.error("Unable to join game! User: " + u.getName(), ex);
        }

    }

    public void out(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "out");
        if (u == null) {
            return;
        }

        //Fuco, garena/autostartgame compatibility added
        //2009-10-09
        boolean addMsg = true;
        if ((getConfig().getProperty("league.flags.autostartgame") != null) &&
          (userNick.equalsIgnoreCase(getConfig().getProperty(app.getType() + ".nick")))) {
            addMsg = false;
        }

        try {
            Game g = getGm().getGame(0);
            if (g.getMatchResult().equals("picking")) {
                log.info("Picking is in progress, can't leave game! " + u.getName());
                return;
            }
            if (g.getPlayers().remove(u)) {

                GameGroup group = g.getUserGroup(u);
                if (group != null) {
                    if (group.getGroupOwner().equals(u)) {
                        for (User gu : group.getGroupMembers()) {
                            g.getPlayers().remove(gu);
                            if (addMsg) {
                                addMsgToStack(app, app.getNick(gu.getAuth()), getUm().getNameWithRank(gu) + " signed out;");
                            }
                        }
                        group.getGroupMembers().clear();
                    }
                    else {
                        group.getGroupMembers().remove(u);
                        if (addMsg) {
                            addMsgToStack(app, userNick, getUm().getNameWithRank(u) + " signed out;");
                        }
                    }

                    if (group.getGroupMembers().size() == 0) {
                        g.getGroups().remove(group);
                    }
                }
                else {
                    if (addMsg) {
                        addMsgToStack(app, userNick, getUm().getNameWithRank(u) + " signed out;");
                        if (getConfig().getProperty("league.signout.inPrivate") != null) {
                            app.sendPrivateMsg(userNick, getUm().getNameWithRank(u) + " signed out; " + getPlayerSlots(g.getType(), g.getPlayers().size()));
                        }
                    }
                }
                getGm().persistGame(g);
                //app.sendActionMsg(channel, "User " + u.getName() + " signed out;");
                //addMsgToStack(app, userNick, getUm().getNameWithRank(u) + " signed out;");
            }
            else {
                log.error("Unable to leave game! User: " + u.getName() + " not in game!");
            }
        } catch (GameManagerException ex) {
            //app.sendNoticeMsg(channel, ex.getMessage());
            log.error("Unable to leave game! User: " + u.getName() + " game not found");
        }
    }

    public void leave(String channel, String userNick, String[] token, ClientWrapper app) {
        out(channel, userNick, token, app);
    }

    public void abort(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "abort");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }


        if (u.getAccessLevel() < Integer.parseInt(getConfig().getProperty("league.opAccessLevel"))) {
            if (!g.getType().equals("challenge")) {
                if (!g.getOwner().equals(u)) {
                    log.info("You can't abort this game! User " + u.getName());
                    return;
                }
            }
            else {
                /*if ((!g.getPlayers().get(0).equals(u)) && (!g.getPlayers().get(1).equals(u))) {
                log.info("You can't abort this game! User " + u.getName());
                return;
                }*/
                if ((!getGm().getPick().getPicker1().equals(u)) && (!getGm().getPick().getPicker2().equals(u))) {
                    log.info("Player is not captain in this game! Can't use ready! " + u.getName());
                    return;
                }
            }
        }

        try {
            if (Boolean.parseBoolean(getConfig().getProperty("league.avail.devoice." + app.getType()))) {
                devoiceUsers(g, app);
            }
            getGm().removeGame(0);
        } catch (GameManagerException ex) {
            log.error(ex.getMessage());
            return;
        }
        getTm().setTopic(app);
        app.sendActionMsg(channel, "Game aborted!");
    }

    public void confirmstart(String channel, String userNick, String[] token, final ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "confirmstart");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }

        if ((!g.getType().equals("startgame")) && (!g.getType().equals("regular"))) {
            log.info("Game mode is not startgame/regular! Can't confirm game " + u.getName());
            return;
        }

        if (g.getPlayers().size() < getConfig().getPlayerNum()) {
            log.info("Game is not full! Can't confirm game " + u.getName());
            return;
        }

        if (!g.getOwner().equals(u)) {
            if (u.getAccessLevel() < Integer.parseInt(getConfig().getProperty("league.opAccessLevel"))) {
                log.info("Player is not captain (or has admin access) in this game! Can't confirm game " + u.getName());
                return;
            }
        }

        g.setNum(getGm().getGameNum());
        g.setMatchDate(new Date());
        g.setMatchResult("progress");

        for (User gu : g.getPlayers()) {
            gu.setCurrentGame(g);
        }

        getUm().persistUsers(g.getPlayers());

        List<User> sentTeam = new ArrayList<User>();
        List<User> scrgTeam = new ArrayList<User>();

        if (g.getType().equals("startgame")) {
            Collections.shuffle(g.getGroups()); //shuffle so we doesn't get the same order as when signed in.
            getGm().createTeamsFromGroups(g.getGroups(), sentTeam, scrgTeam);
        }
        else if (g.getType().equals("regular")) {
            getGm().createTeamsFromPlayers(g.getPlayers(), sentTeam, scrgTeam);
        }

        g.getPlayers().clear();
        g.getPlayers().addAll(sentTeam);
        g.getPlayers().addAll(scrgTeam);
        getGm().setUserListOrder(g);
        g.clearGameGroups();
        getGm().persistGame(g);


        getTm().setTopic(app);
        app.sendActionMsg(channel, new String("Team Sentinel: " + getUserListWithExpRank(sentTeam)));
        app.sendActionMsg(channel, new String("Team Scourge: " + getUserListWithExpRank(scrgTeam)));

        StringBuilder sb = new StringBuilder();
        /*describe %ch Game mode is - $+ $get.gamemode(%c.gamemode) $+ ;
        Game name is $get.gamename(%c.gamemode,%gamenum) $+ ;
        Map version is %c.gameversion $+
        Use .teams and .heroes to get a reminder on the line-ups*/
        sb.append("Game mode is " + g.getMode() +
          "; Game name is " + getGm().getGameName(g.getMode(), g.getNum()) +
          "; Map version is " + getConfig().getMapVersion() + ". Use .teams to get a reminder on the line-ups");
        app.sendActionMsg(channel, new String(sb));

        if (getConfig().getProperty("league.flags.autostartgame") != null) {
            TimerTask startNewGame = new TimerTask() {

                @Override
                public void run() {
                    startgame(getConfig().getProperty(app.getType() + ".mainChannelName"),
                      getConfig().getProperty(app.getType() + ".nick"),
                      new String[]{".startgame", "-l"}, app);
                    /*out(getConfig().getProperty(app.getType() + ".mainChannelName"),
                    getConfig().getProperty(app.getType() + ".nick"),
                    new String[]{".out"}, app);*/
                }
            };
            Timer startGame = new Timer();
            startGame.schedule(startNewGame, 30000);
            app.sendActionMsg(getConfig().getProperty(app.getType() + ".mainChannelName"),
              "New game starting in 30 seconds...");
        }
    }

    public void ready(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "ready");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }

        if (!g.getType().equals("challenge")) {
            log.info("Game mode is not challenge! Can't confirm game " + u.getName());
            return;
        }

        if (g.getPlayers().size() < getConfig().getPlayerNum()) {
            log.info("Game is not full! Can't confirm game " + u.getName());
            return;
        }

        if (getGm().getPick().getPicker1().equals(u)) {
            getGm().getPick().setPicker1ready(true);
        }
        else if (getGm().getPick().getPicker2().equals(u)) {
            getGm().getPick().setPicker2ready(true);
        }
        else {
            log.info("Player is not captain in this game! Can't use ready! " + u.getName());
            return;
        }

        if (g.getMatchResult().equals("picking")) {
            log.info("Picking is in progress, can't re-ready the game! " + u.getName());
            return;
        }

        if (getGm().getPick().canPick()) {
            g.setMatchResult("picking");
            getGm().persistGame(g);
            app.sendActionMsg(channel, "Sign-up closed. It's " + getGm().getPick().getPicker1().getName() + "'s turn to pick.");
            app.sendActionMsg(channel, "Player pool: " + getUserListWithExpRank(getGm().getPick().getPool()));
        }
    }

    public void pick(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "pick");
        if (u == null) {
            return;
        }

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .pick <user's auth/nick>");
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }

        if (!g.getType().equals("challenge")) {
            log.info("Game mode is not challenge! Can't pick players " + u.getName());
            return;
        }

        if (!getGm().getPick().canPick()) {
            log.info("Captains are not ready, can't pick! " + u.getName());
            return;
        }

        if (!getGm().getPick().whopick().equals(u)) {
            log.info("It's not your turn! " + u.getName());
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            return;
        }

        if (!getGm().getPick().getPool().contains(v)) {
            log.error("Can't pick player " + v.getName() + "! " + u.getName());
            return;
        }

        if (u.equals(getGm().getPick().getPicker1())) {
            getGm().getPick().getSent().add(v);
        }
        else {
            getGm().getPick().getScrg().add(v);
        }

        getGm().getPick().incRound(1);

        if (getGm().getPick().getRound() < (getConfig().getPlayerNum() - 2)) {
            app.sendActionMsg(channel, u.getName() + " picked " + v.getName() + "; " + getGm().getPick().whopick().getName() + "'s turn to pick");
            app.sendModeMsg(channel, "+v " + app.getNick(v.getAuth()));
        }
        else {
            g.setNum(getGm().getGameNum());
            g.setMatchDate(new Date());
            g.setMatchResult("progress");

            g.getPlayers().clear();
            g.getPlayers().addAll(getGm().getPick().getSent());
            g.getPlayers().addAll(getGm().getPick().getScrg());
            getGm().setUserListOrder(g);
            g.clearGameGroups();
            getGm().persistGame(g);

            for (User gu : g.getPlayers()) {
                gu.setCurrentGame(g);
            }

            getUm().persistUsers(g.getPlayers());


            getTm().setTopic(app);
            app.sendActionMsg(channel, new String("Team Sentinel: " + getUserListWithExpRank(getGm().getPick().getSent())));
            app.sendActionMsg(channel, new String("Team Scourge: " + getUserListWithExpRank(getGm().getPick().getScrg())));

            StringBuilder sb = new StringBuilder();
            /*describe %ch Game mode is - $+ $get.gamemode(%c.gamemode) $+ ;
            Game name is $get.gamename(%c.gamemode,%gamenum) $+ ;
            Map version is %c.gameversion $+
            Use .teams and .heroes to get a reminder on the line-ups*/
            sb.append("Game mode is " + g.getMode() +
              "; Game name is " + getGm().getGameName(g.getMode(), g.getNum()) +
              "; Map version is " + getConfig().getMapVersion() + ". Use .teams to get a reminder on the line-ups");
            app.sendActionMsg(channel, new String(sb));
        }

    }

    public void mode(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "mode");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }

        if (!g.getOwner().equals(u)) {
            log.info("Player is not captain in this game! Can't change mode " + u.getName());
            return;
        }

        if (g.getMatchResult().equals("picking")) {
            log.info("Picking is in progress, can't change game mode! " + u.getName());
            return;
        }

        if (getGm().checkMode(token[1], g.getType())) {
            g.setMode(token[1]);
            getTm().setTopic(app);
            app.sendActionMsg(channel, "Mode is [" + g.getMode() + "]");
        }
    }

    public void listplayers(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "listplayers");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }

        StringBuilder sb = new StringBuilder();

        if (g.getPlayers().size() == 0) {
            sb.append("Game is empty!");
        }
        else {
            if (g.getType().equals("startgame")) {
                for (GameGroup gr : g.getGroups()) {
                    if (gr.getGroupMembers().size() > 0) {
                        sb.append("<");
                        sb.append(getUserListWithExpRank(gr.getGroupMembers()));
                        sb.append(">; ");
                    }
                }
            }
            else {
                sb.append(getUserListWithExpRank(g.getPlayers())).append(" ");
            }

            sb.append(getPlayerSlots(g.getType(), g.getPlayers().size()));
        }

        app.sendActionMsg(channel, new String(sb));
        setLastInvoke("listplayers");
    }

    public void pool(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "pool");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
        } catch (GameManagerException ex) {
            log.info("No active game found!");
            return;
        }

        app.sendActionMsg(channel, "Player pool: " + getUserListWithExpRank(getGm().getPick().getPool()));
        setLastInvoke("pool");
    }

    public void teams(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "teams");
        if (u == null) {
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(0);
            if (g.getPlayers().contains(u)) {
                if (g.getMatchResult().equals("picking")) {
                    if (g.getType().equals("challenge")) {
                        app.sendActionMsg(channel, "Team Sentinel: " + getUserListWithExpRank(getGm().getPick().getSent()));
                        app.sendActionMsg(channel, "Team Scourge: " + getUserListWithExpRank(getGm().getPick().getScrg()));
                    }
                }
            }
            else {
                g = u.getCurrentGame();
                if (g != null) {
                    if (g.getMatchResult().equals("progress")) {
                        app.sendActionMsg(channel, "Team Sentinel: " + getUserListWithExpRank(g.getSent()));
                        app.sendActionMsg(channel, "Team Scourge: " + getUserListWithExpRank(g.getScrg()));
                    }
                }
            }
        } catch (GameManagerException ex) {
            g = u.getCurrentGame();
            if (g != null) {
                if (g.getMatchResult().equals("progress")) {
                    app.sendActionMsg(channel, "Team Sentinel: " + getUserListWithExpRank(g.getSent()));
                    app.sendActionMsg(channel, "Team Scourge: " + getUserListWithExpRank(g.getScrg()));
                }
            }
            else {
                log.info("No active game found!");
            }
        }
        setLastInvoke("teams");
    }

    public void selfbet(String channel, String userNick, String[] token, ClientWrapper app) {
        if (getConfig().getProperty("league.selfbet.enable") == null) {
            //log.info("Betting is disabled!");
            app.sendNoticeMsg(userNick, "Betting is disabled!");
            return;
        }

        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "selfbet");
        if (u == null) {
            return;
        }

        if (token.length != 2) {
            app.sendNoticeMsg(userNick, "Usage: .selfbet <stake>");
            return;
        }

        Game g = u.getCurrentGame();
        if (g == null) {
            log.error("User is not in game! " + u.getName());
            return;
        }

        Bet b = getBm().getSelfBet(g, u);
        if (b != null) {
            app.sendNoticeMsg(userNick, "You can't bet again on this game!");
            return;
        }

        if (Utility.getDateDiffInSeconds(new Date(), g.getMatchDate()) > Integer.parseInt(getConfig().getProperty("league.selfbet.timeTreshold"))) {
            app.sendNoticeMsg(userNick, "You can't bet now (betting is closed - time treshold reached)");
            return;
        }

        if (Integer.parseInt(token[1]) > Integer.parseInt(getConfig().getProperty("league.selfbet.maxValue"))) {
            app.sendNoticeMsg(userNick, "You can only bet " + Integer.parseInt(getConfig().getProperty("league.selfbet.maxValue")) + " or less exp on this game.");
            return;
        }

        Bet bet = new Bet();
        bet.setUserBetRef(u);
        bet.setGameBetRef(g);
        bet.setBetValue(Integer.parseInt(token[1]));
        u.incExp(-bet.getBetValue());
        bet.setBetType("selfbet");

        //get ratio
        List<User> enemyTeam = g.getSent();
        String team = "Scourge";
        if (g.getSent().contains(u)) {
            enemyTeam = g.getScrg();
            team = "Sentinel";
        }

        //get winner exp and count
        double winnerExp = 1;
        int winnerCount = 0;
        for (User ue : enemyTeam) {
            winnerCount++;
            winnerExp *= ue.getExp();
        }
        winnerExp = Math.pow(10, Math.log10(winnerExp) / winnerCount);
        double ratio = getGm().e(u.getExp(), winnerExp);
        double base = Double.parseDouble(getConfig().getProperty("league.selfbet.winRatio.base"));
        double modifier = Double.parseDouble(getConfig().getProperty("league.selfbet.winRatio.modifier"));
        ratio = base + ((1 - ratio) * modifier);
        bet.setBetWinRatio(ratio);
        getBm().persistBet(bet);
        getUm().persistUser(u);
        app.sendActionMsg(channel, "User " + u.getName() + " has bet " + bet.getBetValue() + " exp on game " +
          getGm().getGameName(g) + "/" + team + ". Current exp is: " + u.getExp() + ". His/Her payout is: " + (int) (bet.getBetValue() * bet.getBetWinRatio()) + " exp");
    }

    /***************************************************************************
     *  GAME MANAGEMENT - ADMIN FUNCTIONS
     *  REPORT
     **************************************************************************/
    public void report(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "report");
        if (u == null) {
            return;
        }

        if (token.length != 3) {
            log.info("Bad report format. " + u.getName());
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(Integer.parseInt(token[1]));
        } catch (GameManagerException ex) {
            log.info("No game found! Gamenum: " + token[1]);
            return;
        }

        if (!g.getMatchResult().equals("progress")) {
            log.info("Game is already closed or hasn't started yet! " + u.getName());
            return;
        }

        if (!getGm().checkResult(token[2])) {
            log.info("Not valid result string: " + token[2] + " " + u.getName());
            return;
        }

        closegame(g, token[2], channel, app);

        //calc exp
    }

    public void result(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "result");
        if (u == null) {
            return;
        }

        if (token.length != 2) {
            log.info("Bad result format. " + u.getName());
            return;
        }

        Game g = u.getCurrentGame();
        if (g == null) {
            log.info("No game found! Result " + u.getName());
            return;
        }

        if (!g.getMatchResult().equals("progress")) {
            log.info("Game is already closed or hasn't started yet! " + u.getName());
            return;
        }

        if (!getGm().checkResult(token[1])) {
            log.info("Not valid result string: " + token[1] + " " + u.getName());
            return;
        }

        //<editor-fold desc="Result calc">
        if (g.getResults().getVotes().size() >= 2 * Integer.parseInt(getConfig().getProperty("league.resultsNeeded"))) {
            log.info("No more results neccessary! " + u.getName());
            return;
        }

        Results r = g.getResults();
        if (!r.getVoted().contains(u)) {
            if (g.getSent().contains(u)) {
                if (r.getSent() < Integer.parseInt(getConfig().getProperty("league.resultsNeeded"))) {
                    r.getVoted().add(u);
                    r.incSent(1);
                    //r.getVotes().add(token[1]);
                    r.add(token[1]);
                }
            }
            else {
                if (r.getScrg() < Integer.parseInt(getConfig().getProperty("league.resultsNeeded"))) {
                    r.getVoted().add(u);
                    r.incScrg(1);
                    r.add(token[1]);
                }
            }
            app.sendNoticeMsg(userNick, "Vote accepted!");
        }

        getGm().persistGame(g);

        int sent = 0;
        int scrg = 0;
        int draw = 0;
        if (g.getResults().getVotes().size() >= 2 * Integer.parseInt(getConfig().getProperty("league.resultsNeeded"))) {
            String resultSet[] = getConfig().getProperty("league.results").split("\\s");
            for (String re : g.getResults().getVotesStr()) {
                if (re.equals(resultSet[0])) {
                    sent++;
                }
                else if (re.equals(resultSet[1])) {
                    scrg++;
                }
                else if (re.equals(resultSet[2])) {
                    draw++;
                }
            }

            String re = null;
            if ((sent > scrg) && (sent > draw)) {
                re = resultSet[0];
            }
            if ((scrg > sent) && (scrg > draw)) {
                re = resultSet[1];
            }
            if ((draw > sent) && (draw > scrg)) {
                re = resultSet[2];
            }
            if (re != null) {
                closegame(g, re, channel, app);
            }
            else {
                app.sendActionMsg(channel, "Can't close game " +
                  getGm().getGameName(g.getMode(), g.getNum()) + "! Call admin bitches");
            }
        }
        //</editor-fold>
    }

    private void closegame(Game g, String result, String channel, ClientWrapper app) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        List<String> closeGame = getGm().closeGame(g, result);

        if (Boolean.parseBoolean(getConfig().getProperty("league.avail.devoice." + app.getType()))) {
            devoiceUsers(g, app);
        }
        getTm().setTopic(app);
        app.sendActionMsg(channel, "Game " + getGm().getGameName(g.getMode(), g.getNum()) +
          ": Result confirmed and submitted; " + ((!result.equals(resultSet[2])) ? "The " + result.toLowerCase() + " have won" : "Game is canceled"));

        if (!g.getMatchResult().equals(resultSet[2])) {
            app.sendActionMsg(channel, "Experience changes: " + getUserListWithGameExp(g.getSent(), g, true) +
              ", " + getUserListWithGameExp(g.getScrg(), g, false));
            if (getConfig().getProperty("league.selfbet.enable") != null) {
                if ((closeGame.get(0) != null) && (closeGame.get(0).length() > 0)) {
                    app.sendActionMsg(channel, "These players have won additional bonus exp through betting: " + closeGame.get(0));
                }
            }
        }
    }

    //TODO: nefunguje spravne pri zmene z draw na result, neco s betmi
    public void submit(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "submit");
        if (u == null) {
            return;
        }

        if (token.length != 3) { //submit #id result
            log.info("Bad submit format. " + u.getName());
            return;
        }

        Game g = null;
        try {
            g = getGm().getGame(Integer.parseInt(token[1]));
        } catch (GameManagerException ex) {
            log.info("No game found! Gamenum: " + token[1]);
            return;
        }

        if (!getGm().checkResult(g.getMatchResult())) {
            log.info("Game is not closed: " + token[2] + " " + u.getName());
            return;
        }

        if (!getGm().checkResult(token[2])) {
            log.info("Not valid result string: " + token[2] + " " + u.getName());
            return;
        }

        if (g.getMatchResult().equals(token[2])) {
            log.info("No change in result! " + u.getName());
            return;
        }

        Map<User, Integer> expChange = new LinkedHashMap<User, Integer>();
        for (User gp : g.getOrderedPlayers()) {
            expChange.put(gp, gp.getExp());
        }

        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        if (token[2].equals(resultSet[2])) {
            voidGame(g);
        }
        else {
            voidGame(g);
            changeResult(g, token[2]);
        }

        StringBuilder sb = new StringBuilder("Game " + getGm().getGameName(g.getMode(), g.getNum()) +
          " result changed to " + token[2] + "! Experience changes: ");
        for (User gp : expChange.keySet()) {
            int exp = gp.getExp() - expChange.get(gp);
            String expstring = (exp > 0) ? ("+" + Integer.toString(exp)) : Integer.toString(exp);
            sb.append(gp.getName()).append("[").append(expstring).append("], ");
        }
        sb.delete(sb.length() - 2, sb.length());

        app.sendActionMsg(channel, new String(sb));
    }

    //TODO: nefunguje spravne pri zmene z draw na result, neco s betmi
    private void voidGame(Game g) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        //g.getMatchResult().equals(resultSet[0])) ? g.getUserExp(u).getExpStr() : g.getUserExp(u).getAltexpStr()) + "]";
        for (GameExp ge : g.getExp()) {
            if (g.getMatchResult().equals(resultSet[0])) {
                ge.getUserRef().incExp(-ge.getExp());
            }
            else if (g.getMatchResult().equals(resultSet[1])) {
                ge.getUserRef().incExp(-ge.getAltexp());
            }
        }

        if (getConfig().getProperty("league.selfbet.enable") != null) {
            if (!g.getMatchResult().equals(resultSet[2])) {
                List<Bet> bets = getBm().getSelfBetsFromGame(g);
                for (Bet b : bets) {
                    if (g.getWinner(resultSet).contains(b.getUserBetRef())) {
                        double win = b.getBetValue() * b.getBetWinRatio();
                        b.getUserBetRef().incExp(-(int) win);
                    }
                }
            }
        }

        g.setMatchResult(resultSet[2]);
        getGm().persistGame(g);
        getUm().persistUsers(g.getPlayers());
    }

    private void changeResult(Game g, String result) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        for (GameExp ge : g.getExp()) {
            if (result.equals(resultSet[0])) {
                ge.getUserRef().incExp(ge.getExp());
            }
            else if (result.equals(resultSet[1])) {
                ge.getUserRef().incExp(ge.getAltexp());
            }
        }
        g.setMatchResult(result);

        //we have new result now, so we can recalc bets
        if (getConfig().getProperty("league.selfbet.enable") != null) {
            if (!g.getMatchResult().equals(resultSet[2])) {
                List<Bet> bets = getBm().getSelfBetsFromGame(g);
                for (Bet b : bets) {
                    if (g.getWinner(resultSet).contains(b.getUserBetRef())) {
                        double win = b.getBetValue() * b.getBetWinRatio();
                        b.getUserBetRef().incExp((int) win);

                    }
                }
            }
        }

        getGm().persistGame(g);
        getUm().persistUsers(g.getPlayers());
    }

    /*
     * vote for replace has ID rv + gamenum (as string)
     */
    public void replace(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "replace");
        if (u == null) {
            return;
        }

        Game g = u.getCurrentGame();
        if (g == null) {
            log.info("No game found! Result " + u.getName());
            return;
        }

        User[] re = canReplace(u, g, token, app);
        if (re == null) {
            return;
        }

        User r = re[0];
        User n = re[1];

        if (!g.equals(r.getCurrentGame())) {
            log.info("Voter and user to be replaced are not in the same game! " + u.getName() + " " + r.getName());
            return;
        }

        Map<String, Vote> votes = getGm().getVotes();
        if (!votes.containsKey(REPLACE_VOTE_ID_PREFIX + g.getNum())) {
            Vote v = new Vote(g, REPLACE_VOTE_ID_PREFIX + g.getNum());
            votes.put(REPLACE_VOTE_ID_PREFIX + g.getNum(), v);
        }

        Vote v = votes.get(REPLACE_VOTE_ID_PREFIX + g.getNum());
        if (!v.getVoted().contains(u)) {
            v.addVote(token[1] + "_" + token[2]);
            v.addVoter(u);
        }
        else {
            app.sendNoticeMsg(userNick, "You've already voted!");
            return;
        }

        if (v.getVotes().get(token[1] + "_" + token[2]) >= Integer.parseInt(getConfig().getProperty("league.replaceVotesNeeded"))) {
            for (GameUserOrder guo : g.getUserOrder()) {
                if (guo.getUserOrderRef().equals(r)) {
                    guo.setUserOrderRef(n);
                }
            }
            g.getPlayers().remove(r);
            g.getPlayers().add(n);

            GameGroup gg = g.getUserGroup(r);
            if (gg != null) {
                gg.getGroupMembers().remove(r);
                gg.getGroupMembers().add(n);
                if (gg.getGroupOwner().equals(r)) {
                    gg.setGroupOwner(n);
                }
            }
            getGm().persistGame(g);
            r.setCurrentGame(null);
            getUm().persistUser(r);
            n.setCurrentGame(g);
            getUm().persistUser(n);
            votes.remove(REPLACE_VOTE_ID_PREFIX + g.getNum());
            app.sendActionMsg(channel, "Player " + r.getName() + " replaced by " + n.getName() + " in " + getGm().getGameName(g.getMode(), g.getNum()) + "!");
        }
    }

    public void adminreplace(String channel, String userNick, String[] token, ClientWrapper app) {
        if (!channel.equals(getConfig().getProperty(app.getType() + ".mainChannelName"))) {
            return;
        }

        User u = checkAccess(userNick, app, "adminreplace");
        if (u == null) {
            return;
        }

        User[] re = canReplace(u, null, token, app);
        if (re == null) {
            return;
        }

        User r = re[0];
        User n = re[1];
        Game g = r.getCurrentGame();

        //replace
        for (GameUserOrder guo : g.getUserOrder()) {
            if (guo.getUserOrderRef().equals(r)) {
                guo.setUserOrderRef(n);
            }
        }
        g.getPlayers().remove(r);
        g.getPlayers().add(n);

        GameGroup gg = g.getUserGroup(r);
        if (gg != null) {
            gg.getGroupMembers().remove(r);
            gg.getGroupMembers().add(n);
            if (gg.getGroupOwner().equals(r)) {
                gg.setGroupOwner(n);
            }
        }
        getGm().persistGame(g);
        r.setCurrentGame(null);
        getUm().persistUser(r);
        n.setCurrentGame(g);
        getUm().persistUser(n);
        //replace ends here

        //remove vote if there's one going on
        getGm().getVotes().remove(REPLACE_VOTE_ID_PREFIX + g.getNum());

        app.sendActionMsg(channel, "Player " + r.getName() + " replaced by " + n.getName() + " in " + getGm().getGameName(g.getMode(), g.getNum()) + "!");
    }

    private User[] canReplace(User invoker, Game g, String[] token, ClientWrapper app) {
        if (token.length != 3) {
            log.info("Bad (admin)replace format. " + invoker.getName() + " [(admin)replace old_user new_user]");
            return null;
        }

        User r = getUserFromParam(token[1], app);
        if (r == null) {
            log.info("Replaced user not found");
            return null;
        }

        User n = getUserFromParam(token[2], app);
        if (n == null) {
            log.info("New user not found");
            return null;
        }

        if (g == null) {
            g = r.getCurrentGame();
            if (g == null) {
                return null;
            }
        }
        if (!g.getMatchResult().equals("progress")) {
            log.info("Game is already closed or hasn't started yet! " + invoker.getName());
            return null;
        }

        if (n.getCurrentGame() != null) {
            log.info("New user is in the game! Can't replace " + n.getName());
            return null;
        }
        User[] re = new User[2];
        re[0] = r;
        re[1] = n;
        return re;
    }

    /***************************************************************************
     *  GAME MANAGEMENT - ADMIN FUNCTIONS
     *  INFO/LASTGAME/GAMES
     **************************************************************************/
    public void info(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "info");
        if (u == null) {
            return;
        }

        int gamenum = 0;
        if (token.length == 2) {
            gamenum = Integer.parseInt(token[1]);
        }
        else {
            gamenum = getGm().getGameNum();
            if (getGm().gameIsOpened()) {
                gamenum--;
            }

        }

        Game g = null;
        try {
            g = getGm().getGame(gamenum);
        } catch (GameManagerException ex) {
            log.info("No game found! Gamenum: " + token[1]);
            return;

        }

        Format dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yy");

        StringBuilder sb = new StringBuilder();
        sb.append("Game " + getGm().getGameName(g.getMode(), g.getNum()));
        sb.append("(").append(dateFormat.format(g.getMatchDate())).append("); ");
        sb.append("Game type: " + g.getType() + "; ");
        sb.append("Team Sentinel <" + getUserListWithGameExp(g.getSent(), g, true));
        sb.append("> Team Scourge <" + getUserListWithGameExp(g.getScrg(), g, false) + "> ");
        sb.append("Game result: " + g.getMatchResult());
        if (g.getMatchResult().equals("progress")) {
            sb.append("[").append(Utility.getDateDiffInMinutes(new Date(), g.getMatchDate())).append("]");
        }

        app.sendActionMsg(actionTarget, new String(sb));
    }

    public void lastgame(String channel, String userNick, String[] token, ClientWrapper app) {
        User u = checkAccess(userNick, app, "lastgame");
        if (u == null) {
            return;
        }

        if (token.length == 1) {
            info(channel, userNick, new String[]{".info"}, app);
        }
        else if (token.length == 2) {
            User v = getUserFromParam(token[1], app);
            if (v == null) {
                log.info("User not found! lastgame " + u.getName());
                return;

            }

            info(channel, userNick, new String[]{".info", Integer.toString(v.getLastGame().getNum())}, app);
        }
        setLastInvoke("lastgame");
    }

    public void games(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);
        User u = checkAccess(userNick, app, "games");
        if (u == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        List<Game> gamesInProgress = getGm().getGamesInProgress();
        if (gamesInProgress.size() > 0) {
            sb.append("Games currently in progress: ");
            for (Game gp : gamesInProgress) {
                sb.append(getGm().getGameName(gp.getMode(), gp.getNum()));
                sb.append("[").append(Utility.getDateDiffInMinutes(new Date(), gp.getMatchDate())).append("], ");
            }
            sb.delete(sb.length() - 2, sb.length());
        }
        else {
            sb.append("No games in progress");
        }

        app.sendActionMsg(actionTarget, new String(sb));
        setLastInvoke("games");
    }

    public void rank(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "rank");
        if (u == null) {
            return;
        }

        User v = null;
        if (token.length == 1) {
            v = u;
        }
        else {
            v = getUserFromParam(token[1], app);
        }

        if (v == null) {
            app.sendActionMsg(channel, "User not found!");
            return;

        }

        /*var %line = $getname(%u) has $user(%u).win wins, $user(%u).lost losses, $user(%u).exp experience, $iif($user(%u).inactive != $true,ranked $user(%u).rank $+ $chr(44) Rated $enclose($get.exprank(%u)) $+ ;,Inactive;)
        var %line = %line $iif($get.streakrank($user(%u).spree),$ifmatch)
         */


        StringBuilder sb = new StringBuilder();
        sb.append(v.getName() + " has " + v.getWin() + " wins, " + v.getLoss() + " losses, ");
        sb.append(v.getExp() + " experience, ranked " + v.getRank() + "/" + getUm().getUserCount() + "; ");
        sb.append("Rated [" + getConfig().getExpRank(v.getExp()) + "]; ");
        String streakRank = getConfig().getStreakString(v.getStreak());
        if (streakRank != null) {
            sb.append("Current " + ((v.getStreak() > 0) ? "winning" : "losing") +
              " streak: " + v.getStreak() + "[" + streakRank + "]");
        }

        app.sendActionMsg(actionTarget, new String(sb));
    }

    public void recent(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "recent");
        if (u == null) {
            return;
        }

        User v = null;
        if (token.length == 1) {
            v = u;
        }
        else {
            v = getUserFromParam(token[1], app);
        }

        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;

        }

        app.sendActionMsg(actionTarget, getUm().getRecentGames(v));
    }

    public void top(String channel, String userNick, String[] token, ClientWrapper app) {
        User u = checkAccess(userNick, app, "top");
        if (u == null) {
            return;
        }

        int end = 10;
        if (token.length == 2) {
            end = Integer.parseInt(token[1]);
        }

        app.sendActionMsg(channel, "Top " + end + ": " + getUm().getTopPlayersList(end));
        setLastInvoke("top");
    }

    public void bottom(String channel, String userNick, String[] token, ClientWrapper app) {
        User u = checkAccess(userNick, app, "bottom");
        if (u == null) {
            return;
        }

        top(channel, userNick, new String[]{".top", Integer.toString(getUm().getUserCount())}, app);
        setLastInvoke("bottom");
    }

    public void compare(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "compare");
        if (u == null) {
            log.info("Command invoker not found! (compare)");
            return;
        }

        if ((token.length < 2) || (token.length > 3)) {
            log.info("Bad compare format. " + u.getName());
            return;
        }

        User c1 = getUserFromParam(token[1], app);
        if (c1 == null) {
            //log.info("User 1 not found!");
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        User c2;
        if (token.length == 3) {
            c2 = getUserFromParam(token[2], app);
            if (c2 == null) {
                //log.info("User 2 not found!");
                app.sendActionMsg(actionTarget, "User not found!");
                return;
            }
        }
        else {
            c2 = c1;
            c1 = u;
        }

        StringBuilder sb = new StringBuilder(getUm().getNameWithRank(c1) + " compared to " + getUm().getNameWithRank(c2) + ": ");

        int gamediff = Math.abs(c1.getGames() - c2.getGames());
        if (c1.getGames() > c2.getGames()) {
            sb.append(gamediff + " games more for " + c1.getName() + "; ");
            int windiff = c1.getWin() - c2.getWin();
            int lossdiff = c1.getLoss() - c2.getLoss();
            if (windiff != 0) {
                sb.append(Math.abs(windiff) + " wins " + ((windiff > 0) ? "more" : "less") + "; ");
            }
            if (lossdiff != 0) {
                sb.append(Math.abs(lossdiff) + " losses " + ((lossdiff > 0) ? "more" : "less") + "; ");
            }
        }
        else if (c1.getGames() < c2.getGames()) {
            sb.append(gamediff + " games more for " + c2.getName() + "; ");
            int windiff = c2.getWin() - c1.getWin();
            int lossdiff = c2.getLoss() - c1.getLoss();
            if (windiff != 0) {
                sb.append(Math.abs(windiff) + " wins " + ((windiff > 0) ? "more" : "less") + "; ");
            }
            if (lossdiff != 0) {
                sb.append(Math.abs(lossdiff) + " losses " + ((lossdiff > 0) ? "more" : "less") + "; ");
            }
        }

        if (c1.getRank() < c2.getRank()) {
            sb.append("Rank difference: +" + Math.abs(c1.getRank() - c2.getRank()) + " for " + c1.getName()).append(" (" + c1.getRank() + " against " + c2.getRank() + "); ");
        }
        else {
            sb.append("Rank difference: +" + Math.abs(c2.getRank() - c1.getRank()) + " for " + c2.getName()).append(" (" + c2.getRank() + " against " + c1.getRank() + "); ");
        }

        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        List<Game> commonTogether = getGm().getCommonGames(c1, c2);
        List<Game> commonAgainst = getGm().getCommonGamesAgainst(c1, c2);
        //calc together stats
        int togetherWins = 0;
        int togetherLosses = 0;

        for (Game g : commonTogether) {
            if (g.getSent().contains(c1)) {
                if (g.getMatchResult().equals(resultSet[0])) {
                    togetherWins++;
                }
                else {
                    togetherLosses++;
                }
            }
            else {
                if (g.getMatchResult().equals(resultSet[0])) {
                    togetherLosses++;
                }
                else {
                    togetherWins++;
                }
            }
        }

        //calc against stats
        int againstWins = 0;
        int againstLosses = 0;
        for (Game g : commonAgainst) {
            if (g.getSent().contains(c1)) {
                if (g.getMatchResult().equals(resultSet[0])) {
                    againstWins++;
                }
                else {
                    againstLosses++;
                }
            }
            else {
                if (g.getMatchResult().equals(resultSet[0])) {
                    againstLosses++;
                }
                else {
                    againstWins++;
                }
            }
        }

        sb.append("Score togehter: " + togetherWins + "/" + togetherLosses + "[" +
          ((togetherWins == togetherLosses) ? "Neutral" : ((togetherWins > togetherLosses) ? ("+" + (togetherWins - togetherLosses)) : ("-" + (togetherLosses - togetherWins)))) + "]; ");
//$enclose($iif( %stw == %stl ,Neutral,$iif(%stw > %stl,+ $+ $calc(%stw - %stl),- $+ $calc(%stl - %stw)))
        sb.append("Score against: " + againstWins + "/" + againstLosses + "[" +
          ((againstWins == againstLosses) ? "Tied" : ((againstWins > againstLosses) ? (c1.getName() + " leads: +" + (againstWins - againstLosses)) : (c2.getName() + " leads: +" + (againstLosses - againstWins)))) + "]");

//Score against: %saw $+ / $+ %sal $+ $enclose($iif( %saw == %sal ,Tied,$iif(%saw > %sal,%nu leads: + $+ $calc(%saw - %sal),%ncu leads: + $+ $calc(%sal - %saw))))
        app.sendActionMsg(actionTarget, new String(sb));
    }

    /***************************************************************************
     *  GROUP/RANK MANAGEMENT
     *
     **************************************************************************/
    public void group(String channel, String userNick, String[] token, ClientWrapper app) {
        User u = checkAccess(userNick, app, "group");
        if (u == null) {
            return;
        }

        if (token.length < 2) {
            return;
        }

        List<String> ranks = getConfig().getAccessRanks();
        for (String r : ranks) {
            r.toLowerCase();
        }

        StringBuilder r = new StringBuilder();
        for (int i = 1; i < token.length; i++) {
            r.append(token[i]).append(" ");
        }

        r.delete(r.length() - 1, r.length());

        boolean valid = false;
        for (String s : ranks) {
            if (s.equalsIgnoreCase(new String(r))) {
                valid = true;
                break;
            }
        }

        if (valid) {
            List<User> group = getUm().getGroup(token[1]);
            StringBuilder sb = new StringBuilder("Group " + token[1] + "(" + group.size() + "): ");
            if (group.size() > 0) {
                for (User gu : group) {
                    sb.append(gu.getName()).append(", ");
                }

                sb.delete(sb.length() - 2, sb.length());
            }
            else {
                log.info("Group is empty!");
            }

            app.sendActionMsg(channel, new String(sb));
        }
        else {
            log.info("Group " + token[1] + " doesnt exist!");
        }

    }

    /***************************************************************************
     *  USER MANAGEMENT
     *
     **************************************************************************/
    public void reward(String channel, String userNick, String[] token, ClientWrapper app) {
        String actionTarget = getTarget(channel, userNick);

        User u = checkAccess(userNick, app, "reward");
        if (u == null) {
            return;
        }

        if (token.length != 3) {
            log.info("Bad reward format. " + u.getName() + " [reward user exp_change]");
            return;
        }

        User v = getUserFromParam(token[1], app);
        if (v == null) {
            app.sendActionMsg(actionTarget, "User not found!");
            return;
        }

        int reward = 0;
        try {
            reward = Integer.parseInt(token[2]);
        } catch (NumberFormatException ex) {
            log.info("Bad reward format. " + u.getName() + " [reward user exp_change] (NumberFormatException)");
            return;
        }

        v.incExp(reward);
        getUm().persistUser(v);
        app.sendActionMsg(actionTarget, "User " + v.getName() + ": Experience points changed from " + (v.getExp() - reward) + " to " + v.getExp() + "[" + ((reward > 0) ? "+" : "") + reward + "]");
    }

    public void penalty(String channel, String userNick, String[] token, ClientWrapper app) {
        User u = checkAccess(userNick, app, "penalty");
        if (u == null) {
            return;
        }

        if (token.length != 3) {
            log.info("Bad penalty format. " + u.getName() + " [penalty user exp_change]");
            return;
        }
        log.info("Calling reward handler...");
        reward(channel, userNick, new String[]{".reward", token[1], Integer.toString(-Integer.parseInt(token[2]))}, app);
        log.info("Done!");
    }

    public void setrank(String channel, String userNick, String[] token, ClientWrapper app) {
    }

    /***************************************************************************
     *  USELESS SHIT HELPER FUNCTIONS
     *  
     **************************************************************************/
    public User getUserFromParam(String param, ClientWrapper app) {
        String auth = app.getAuth(param);
        User u = null;

        try {
            u = league.getUm().getUser(auth);
        } catch (UserManagerException ex) {
            try {
                u = league.getUm().getUser(param);
            } catch (UserManagerException ex1) {
                //app.sendActionMsg(actionTarget, "User not found!");
                //return;
            }
        }
        return u;
    }

    public User getUserFromNick(String userNick, ClientWrapper app) {
        String auth = app.getAuth(userNick);
        User u = null;
        try {
            u = league.getUm().getUser(auth);
        } catch (UserManagerException ex) {
            //System.out.println("Voucher not found!");
            //return;
        }
        return u;
    }

    private void setLastInvoke(String command) {
        lastInvoke.put(command, new Date());
    }

    public User checkAccess(String userNick, ClientWrapper app, String command) {
        User u = getUserFromNick(userNick, app);
        if (u == null) {
            log.error("Caller of method " + command + " not found");
            return null;
        }

        if (!app.getMask(userNick).equalsIgnoreCase(getConfig().getProperty("league.masteraccess"))) {
            if (u.getAccessLevel() < getConfig().getCommandAccess(command)) {
                log.error("Your access level is too low for this operation!");
                return null;
            }

            if (u.getAccessLevel() > getConfig().getCommandAccessLimit(command)) {
                log.error("Your access level is high low for this operation!");
                return null;
            }
        }

        if (u.getAccessLevel() < Integer.parseInt(getConfig().getProperty("league.opAccessLevel"))) {
            int timeout = Integer.parseInt(getConfig().getProperty("league.command.timeout." + command, "0"));
            if (lastInvoke.get(command) == null) {
                lastInvoke.put(command, new Date());
            }
            else {
                Date d = new Date();
                if ((d.getTime() - lastInvoke.get(command).getTime()) < (timeout * 1000)) {
                    return null;
                }

            }
        }
        return u;
    }

    public String getTarget(String channel, String userNick) {
        //changed to work with garena, hopefuly it will work with IRC as well.
        //2009-10-08
        //@{
        //return (channel != null) ? channel : userNick;
        //@}
        return (!channel.equals(userNick)) ? channel : userNick;
    }

    private Config getConfig() {
        return league.getConfig();
    }

    private GameManager getGm() {
        return league.getGm();
    }

    private UserManager getUm() {
        return league.getUm();
    }

    private TopicManager getTm() {
        return league.getTm();
    }

    private BetManager getBm() {
        return league.getBm();
    }

    public Method getCommandHandle(String methodName) {
        Method tempMethod = null;
        try {
            //(String channel, String userNick, String[] token, ClientWrapper app)
            tempMethod = getClass().getMethod(methodName, String.class, String.class, String[].class, ClientWrapper.class);
        } catch (NoSuchMethodException e) {
            log.fatal("No methods acquired", e);
        }
        return tempMethod;
    }

    private void addMsgToStack(ClientWrapper app, String nick, String msg) {
        if (!signOutStack.containsKey(app)) {
            signOutStack.put(app, new MsgStack());
        }
        signOutStack.get(app).add(nick, msg);
    }

    private String getPlayerSlots(String type, int players) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");

        Formatter f = new Formatter();
        String mode = getConfig().getProperty("league.gameType.slots.number." + type);
        int p = 0;

        if (mode.equalsIgnoreCase("free")) {
            p = getConfig().getPlayerNum() - players;
        }
        else if (mode.equalsIgnoreCase("signed")) {
            p = players;
        }

        if (p != 0) {
            sb.append(f.format(getConfig().getProperty("league.gameType.slots.format." + type), p));
        }
        else {
            sb.append("Game is full. Host can start the game with .confirmstart");
        }

        return new String(sb);
    }

    public StringBuilder getUserListWithExpRank(List<User> users) {
        StringBuilder sb = new StringBuilder();

        for (User p : users) {
            sb.append(getUm().getNameWithRank(p)).append(", ");
        }

        if (sb.length() > 2) {
            sb.delete(sb.length() - 2, sb.length());
        }

        return sb;
    }

    public StringBuilder getUserListWithExp(List<User> users) {
        StringBuilder sb = new StringBuilder();

        for (User p : users) {
            sb.append(p.getName() + "[" + p.getExp() + "], ");
        }
        sb.delete(sb.length() - 2, sb.length());

        return sb;
    }

    public StringBuilder getUserListWithGameExp(List<User> users, Game g, boolean teamOne) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        StringBuilder sb = new StringBuilder();

        int i = (teamOne) ? 0 : 5;
        for (User p : users) {
            if ((!g.getMatchResult().equals("progress")) && (!g.getMatchResult().equals(resultSet[2]))) {
                sb.append(getUm().getNameWithGameExp(p, g)).append(", ");
            }
            else {
                sb.append(p.getName()).append(", ");
            }
            i++;
        }
        sb.delete(sb.length() - 2, sb.length());

        return sb;
    }

    private void devoiceUsers(Game g, ClientWrapper app) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (User gu : g.getPlayers()) {
            sb.append(app.getNick(gu.getAuth())).append(" ");
            i++;
            if (i >= 5) {
                app.sendModeMsg(getConfig().getProperty(app.getType() + ".mainChannelName"),
                  new String("-vvvvv " + sb));
                i = 0;
                sb = new StringBuilder();
            }
        }
        if (i != 0) {
            StringBuilder sbv = new StringBuilder();
            for (int j = 0; j < i; j++) {
                sbv.append("v");
            }
            app.sendModeMsg(getConfig().getProperty(app.getType() + ".mainChannelName"),
              new String("-" + sbv + " " + sb));
        }
    }
}
