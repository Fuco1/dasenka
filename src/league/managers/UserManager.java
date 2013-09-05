package league.managers;

import league.main.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import league.comparators.UserExpComparator;
import league.comparators.UserReversedExpComparator;
import league.entities.Game;
import league.entities.User;
import league.exceptions.UserManagerException;
import org.apache.log4j.Logger;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class UserManager {

    private League league;
    private static Logger log = Logger.getLogger(UserManager.class);

    public UserManager(League league) {
        this.league = league;
    }

    public User addUser(String auth, String name) throws UserManagerException, IllegalArgumentException {
        return addUser(auth, name, null);
    }

    public User addUser(String auth, String name, User vouchedBy) throws UserManagerException, IllegalArgumentException {
        if ((auth == null) || (auth.length() == 0)) {
            throw new IllegalArgumentException("Auth is null");
        }
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("Name is null");
        }

        try {
            User u = getUser(auth);
            if (u.getAccessLevel() == 0) {
                resetUser(u);
                u.setVouchedBy(vouchedBy);
                u.setAccessLevel(Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel")));
                persistUser(u);
                return u;
            }
        } catch (UserManagerException ex) {
        }

        try {
            canAddUser(auth, name);
        } catch (UserManagerException ex) {
            throw new UserManagerException(ex);
        }

        User re = new User(auth, name, vouchedBy);
        if (vouchedBy == null) {
            re.setVouchedBy(re);
        }
        try {
            getEm().getTransaction().begin();
            getEm().persist(re);
            getEm().getTransaction().commit();
        } finally {
            if (getEm().getTransaction().isActive()) {
                getEm().getTransaction().rollback();
            }
        }
        return re;
    }

    public User removeUser(Long id) throws UserManagerException, IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("Id not specified!");
        }
        User remove = null;
        try {
            remove = getEm().find(User.class, id);
            getEm().getTransaction().begin();
            getEm().remove(remove);
            getEm().getTransaction().commit();
        } catch (Exception ex) {
            throw new UserManagerException("Unable to remove user with id: " + id, ex);
        }
        return remove;
    }

    public User removeUser(String auth) throws UserManagerException, IllegalArgumentException {
        if ((auth == null) || (auth.length() == 0)) {
            throw new IllegalArgumentException("Auth not specified");
        }
        Long remove = null;
        try {
            remove = (Long) getEm().createNamedQuery("getIdByAuth").setParameter(1, auth).getSingleResult();
        } catch (Exception ex) {
            throw new UserManagerException("Unable to remove user with auth: " + auth, ex);
        }
        return removeUser(remove);
    }

    public User resetUser(User u) {
        u.setPromotedBy(null);

        u.setAccessLevel(Integer.parseInt(getConfig().getProperty("league.startingAccessLevel")));
        u.setWin(0);
        u.setLoss(0);
        u.setDraw(0);
        u.setExp(1000);
        u.setStreak(0);
        u.setBestStreak(0);
        u.setWorstStreak(0);
        u.setConfidence(500);
        u.setRank(0);

        u.setLastGame(null);
        u.setCurrentGame(null);

        getEm().getTransaction().begin();
        getEm().persist(u);
        getEm().getTransaction().commit();
        return u;
    }

    public User suspendUser(User u) {
        u.setAccessLevel(0);
        u.setVouchedBy(null);
        persistUser(u);
        return u;
    }

    public User getUser(Long id) throws UserManagerException {
        if (id == null) {
            throw new UserManagerException("User not found!");
        }
        User re = null;
        try {
            re = getEm().getReference(User.class, id);
            re.setVouchedUsers(getVouchees(re.getAuth()));
            re.setPromotedUsers(getPromotedUsers(re.getAuth()));
            re.setFriendList(getFriendList(re.getAuth()));
            re.setFriendOf(getFriendOf(re.getAuth()));
        } catch (EntityNotFoundException ex) {
            throw new UserManagerException("User not found!");
        }

        if (re == null) {
            throw new UserManagerException("User not found!");
        }
        return re;
    }

    public User getUser(String auth) throws UserManagerException {
        if (auth == null) {
            throw new UserManagerException("User not found!");
        }
        Long id = null;
        try {
            id = (Long) getEm().createNamedQuery("getIdByAuth").setParameter(1, auth).getSingleResult();
        } catch (NoResultException ex) {
            try {
                id = (Long) getEm().createNamedQuery("getIdByName").setParameter(1, auth).getSingleResult();
            } catch (NoResultException exx) {
                throw new UserManagerException("User not found!", exx);
            }
        }
        return getUser(id);
    }

    public List<User> getUsers() {
        return getEm().createNamedQuery("getUsers").getResultList();
    }

    public int getUserCount() {
        return ((Long) getEm().createNamedQuery("getUserCount").getSingleResult()).intValue();
    }

    public boolean userExistByAuth(String auth) {
        try {
            getEm().createNamedQuery("getIdByAuth").setParameter(1, auth).getSingleResult();
            return true;
        } catch (NoResultException ex) {
            return false;
        }
    }

    public boolean userExistByName(String name) {
        try {
            getEm().createNamedQuery("getIdByName").setParameter(1, name).getSingleResult();
            return true;
        } catch (NoResultException ex) {
            return false;
        }
    }

    public List<User> getVouchees(String auth) {
        return getEm().createNamedQuery("getVouchees").setParameter(1, auth).getResultList();
    }

    public List<User> getPromotedUsers(String auth) {
        return getEm().createNamedQuery("getPromotedUsers").setParameter(1, auth).getResultList();
    }

    public List<User> getFriendList(String auth) {
        return getEm().createNamedQuery("getFriendList").setParameter(1, auth).getResultList();
    }

    public List<User> getFriendOf(String auth) {
        return getEm().createNamedQuery("getFriendOf").setParameter(1, auth).getResultList();
    }

    public Long getId(String auth) throws IllegalArgumentException, UserManagerException {
        if (auth == null) {
            throw new IllegalArgumentException("Auth not specified!");
        }
        Long id = null;
        try {
            id = (Long) getEm().createNamedQuery("getIdByAuth").setParameter(1, auth).getSingleResult();
        } catch (NoResultException ex) {
            throw new UserManagerException("User not found!", ex);
        }
        return id;
    }

    public void persistUser(User u) {
        try {
            getEm().getTransaction().begin();
            getEm().persist(u);
            getEm().getTransaction().commit();
        } catch (Exception ex) {
            try {
                getEm().getTransaction().rollback();
            } catch (Exception exx) {
                log.info("Rollback failed: " + exx.getMessage());
            }
            log.info("Transaction failed: " + ex.getMessage());
        }
    }

    public void persistUsers(Collection<User> users) {
        try {
            getEm().getTransaction().begin();
            for (User u : users) {
                getEm().persist(u);
            }
            getEm().getTransaction().commit();
        } catch (Exception ex) {
            try {
                getEm().getTransaction().rollback();
            } catch (Exception exx) {
                log.info("Rollback failed: " + exx.getMessage());
            }
            log.info("Transaction failed: " + ex.getMessage());
        }
    }

    public String getRecentGames(User u) {
        StringBuilder sb = new StringBuilder();
        List<Game> games = getGm().getRecentGames(u);
        sb.append("Recent games: ");
        if (games.size() > 0) {
            for (Game g : games) {
                sb.append(getGm().getGameName(g.getMode(), g.getNum()) + ", ");
            }
            sb.delete(sb.length() - 2, sb.length());
        }
        else {
            sb.append("no game played yet");
        }
        return new String(sb);
    }

    public String getTopPlayersList() {
        return getTopPlayersList(10);
    }

    /* starting at 1 */
    public String getTopPlayersList(int end) {
        int playerNum = getUserCount();
        if (end < 9) {
            end = 10;
        }
        if (end > playerNum) {
            end = playerNum;
        }
        int start = end - 10;

        List<User> result = getEm().createNamedQuery("getTopUsers").setFirstResult(start).setMaxResults(10).getResultList();

        return new String(league.getMh().getUserListWithExp(result));
    }

    public List<User> getGroup(String groupName) {
        return getGroup(getConfig().getRankAccessByString(groupName));
    }

    public List<User> getGroup(int accessLevel) {
        return getEm().createNamedQuery("getUsersByAccess").setParameter(1, accessLevel).getResultList();
    }

    public String whois(String auth) throws UserManagerException {
        return whois(getUser(auth));
    }

    public String whois(Long id) throws UserManagerException {
        return whois(getUser(id));
    }

    public String whois(User u) {
        /*
        Fuco[Root]: Vouched by Fuco, Promoted by Fuco; Inactive;
        Managers: Pimpo, Pepso[2/2]. Vouchers: Da_SwOOp[1/3]. Censors: OFFak, idefix[2/3].
        Leaders: Iacek, Fatallik, Sacco, nEkro, Warlog, ervss, MoQwai, Frido[8/10].
        33/75 vouchees. Confidence factor: Average
         * */
        if (u.getAccessLevel() == 0) {
            return u.getName() + "[" + getConfig().getProperty("league.rank.zeroAccess") + "]";
        }

        Format dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yy");
        StringBuilder sb = new StringBuilder();

        int myRankNum = getConfig().getRankNumByAccess(u.getAccessLevel());

        sb.append(u.getName()).append("[").append(getConfig().getRankStringByAccess(u.getAccessLevel())).append("]: ");
        switch (getConfig().getRankPromoteMode(myRankNum)) {
            case INVITE:
                sb.append("Invited by ");
                break;
            case DEFAULT:
            case PROMOTE:
                sb.append("Vouched by ");
                break;
        }

        if (getConfig().getProperty("league.flags.groupVouching") == null) {
            sb.append(u.getVouchedBy().getName());
        }
        else {
            sb.append(getConfig().getRankStringByAccess(u.getVouchedBy().getAccessLevel()));
        }

        sb.append("(").append(dateFormat.format(u.getVouchDate())).append("); ");
        if (u.getPromotedBy() != null) {
            sb.append("Promoted by ").append(u.getPromotedBy().getName()).append("; ");
        }

        sb.append("Rated " + getConfig().getExpRank(u.getExp()) + "(" + u.getRank() + "/" + getUserCount() + "); ");

        int num = 0;
        int promoted = 0;
        int invited = 0;
        int def = 0;

        StringBuilder sbp = new StringBuilder(); //builder for promote
        StringBuilder sbd = new StringBuilder(); //builder for default
        StringBuilder sbi = new StringBuilder(); //builder for invite
        StringBuilder sbtemp = null; //temp builder
        List<User> userlist = new ArrayList<User>();

        sbp.append("[Promoted users] ");
        sbi.append("[Invited users] ");

        for (int i = myRankNum; i >= 0; i--) {
            if (getConfig().getRankPromoteLimit(myRankNum, i) > 0) {
                switch (getConfig().getRankShowMode(i)) {
                    case YES:
                        num = 0;
                        sbtemp = new StringBuilder();
                        sbtemp.append(getConfig().getRankStringByNum(i)).append("s: ");
                        if (getConfig().getRankAccessByNum(i) >
                          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
                            userlist = u.getPromotedUsers();
                        }
                        else {
                            userlist = u.getVouchedUsers();
                        }
                        if (getConfig().getRankAccessByNum(i) !=
                          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
                            for (User p : userlist) {
                                if ((p.getAccessLevel() >= getConfig().getRankAccessByNum(i)) &&
                                  (p.getAccessLevel() < getConfig().getRankAccessByNum(i + 1))) {
                                    sbtemp.append(p.getName()).append(", ");
                                    num++;
                                }
                            }
                        }
                        else {
                            num = userlist.size();
                        }
                        if (num > 0) {
                            sbtemp.delete(sbtemp.length() - 2, sbtemp.length());
                        }
                        sbtemp.append("[").append(num).append("/").append(getConfig().getRankPromoteLimit(myRankNum, i)).append("]. ");

                        switch (getConfig().getRankPromoteMode(i)) {
                            case INVITE:
                                sbi.append(sbtemp);
                                invited++;
                                break;
                            case DEFAULT:
                                sbd.append(sbtemp);
                                def++;
                                break;
                            case PROMOTE:
                                sbp.append(sbtemp);
                                promoted++;
                                break;
                        }
                        break;
                    case NUMBER:
                        num = 0;
                        sbtemp = new StringBuilder();
                        sbtemp.append(getConfig().getRankStringByNum(i)).append("s");
                        if (getConfig().getRankAccessByNum(i) >
                          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
                            userlist = u.getPromotedUsers();
                        }
                        else {
                            userlist = u.getVouchedUsers();
                        }
                        if (getConfig().getRankAccessByNum(i) !=
                          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
                            for (User p : userlist) {
                                if ((p.getAccessLevel() >= getConfig().getRankAccessByNum(i)) &&
                                  (p.getAccessLevel() < getConfig().getRankAccessByNum(i + 1))) {
                                    num++;
                                }
                            }
                        }
                        else {
                            num = userlist.size();
                        }

                        sbtemp.append("[").append(num).append("/").append(getConfig().getRankPromoteLimit(myRankNum, i)).append("]. ");

                        switch (getConfig().getRankPromoteMode(i)) {
                            case INVITE:
                                sbi.append(sbtemp);
                                invited++;
                                break;
                            case DEFAULT:
                                sbd.append(sbtemp);
                                def++;
                                break;
                            case PROMOTE:
                                sbp.append(sbtemp);
                                promoted++;
                                break;
                        }
                        break;
                    case NO:
                        break;
                }
            }
        }

        if (promoted > 0) {
            sb.append(sbp);
        }

        if (invited > 0) {
            sb.append(sbi);
        }

        if (def > 0) {
            sb.append(sbd);
        }

        /*if (getConfig().getRankPromoteLimit(myRankNum, getConfig().getRankNumByAccess(
        Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel")))) > 0) {
        sb.append(getConfig().getRankStringByAccess(
        Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel")))).append("s");
        sb.append("[").append(u.getVouchedUsers().size()).append("/").append(getConfig().getRankPromoteLimit(myRankNum,
        getConfig().getRankNumByAccess(Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))))).append("]. ");
        }*/

        /*sb.append("Confidence factor: ").append(u.getConfidence()).append("; ");*/
        sb.append(getRecentGames(u)).append("; ");
        return new String(sb);
    }

    public String friendlist(User u) {
        StringBuilder sb = new StringBuilder();


        sb.append(u.getName() + "'s friend list: ");
        for (User f : u.getFriendList()) {
            sb.append(f.getName()).append(", ");
        }

        if (u.getFriendList().size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        else {
            sb.append("Nobody!");
        }

        return new String(sb);
    }

    public String friendof(User u) {
        StringBuilder sb = new StringBuilder();


        sb.append(u.getName() + " is friend of: ");
        for (User f : u.getFriendOf()) {
            sb.append(f.getName()).append(", ");
        }

        if (u.getFriendOf().size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        else {
            sb.append("Nobody!");
        }

        return new String(sb);
    }

    public int getPromotedNum(User u, int rankNum) {
        int num = 0;
        List<User> userlist = new ArrayList<User>();

        if (getConfig().getRankAccessByNum(rankNum) >
          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
            userlist = u.getPromotedUsers();
        }
        else {
            userlist = u.getVouchedUsers();
        }
        if (getConfig().getRankAccessByNum(rankNum) !=
          Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
            for (User p : userlist) {
                if ((p.getAccessLevel() >= getConfig().getRankAccessByNum(rankNum)) &&
                  (p.getAccessLevel() < getConfig().getRankAccessByNum(rankNum + 1))) {
                    num++;
                }
            }
        }
        else {
            num = userlist.size();
        }
        return num;
    }

    public boolean canAddUser(String auth, String name) throws UserManagerException {
        try {
            getEm().createNamedQuery("getIdByAuth").setParameter(1, auth).getSingleResult();
            throw new UserManagerException("User with auth " + auth + " already exists!");
        } catch (NoResultException ex) {
        }

        try {
            getEm().createNamedQuery("getIdByAuth").setParameter(1, name).getSingleResult();
            throw new UserManagerException("User with auth " + name + " already exists!");
        } catch (NoResultException ex) {
        }

        try {
            getEm().createNamedQuery("getIdByName").setParameter(1, auth).getSingleResult();
            throw new UserManagerException("User with name " + auth + " already exists!");
        } catch (NoResultException ex) {
        }

        try {
            getEm().createNamedQuery("getIdByName").setParameter(1, name).getSingleResult();
            throw new UserManagerException("User with name " + name + " already exists!");
        } catch (NoResultException ex) {
        }

        return true;
    }

    public String getNameWithRank(User u) {
        //return u.getName() + "[" + getConfig().getExpRank(u.getExp() - (u.getExp() % getConfig().getExpRankSpread())) + "]";
        return u.getName() + "[" + getConfig().getExpRank(u.getExp()) + "]";
    }

    public String getNameWithGameExp(User u, Game g) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        return u.getName() + "[" +
          ((g.getMatchResult().equals(resultSet[0])) ? g.getUserExp(u).getExpStr() : g.getUserExp(u).getAltexpStr()) + "]";
    }

    private Config getConfig() {
        return league.getConfig();
    }

    private EntityManager getEm() {
        return league.getEm();
    }

    private GameManager getGm() {
        return league.getGm();
    }

    public void sortUsers() {
        List<User> users = getUsers();
        Collections.sort(users, new UserReversedExpComparator());
        int i = 1;
        for (User u : users) {
            if (u.getGames() > 0) {
                u.setRank(i++);
            } else {
                u.setRank(getUserCount());
            }
        }
        persistUsers(users);
    }

    public void resetStats() {
        List<User> users = getUsers();
        for (User u : users) {
            u.setWin(0);
            u.setLoss(0);
            u.setDraw(0);
            u.setExp(Integer.parseInt(getConfig().getProperty("league.exp.startingExp.invite")));
            if (u.getAccessLevel() >= Integer.parseInt(getConfig().getProperty("league.defaultVouchAccessLevel"))) {
                u.setExp(Integer.parseInt(getConfig().getProperty("league.exp.startingExp.vouch")));
            }
            u.setStreak(0);
            u.setBestStreak(0);
            u.setWorstStreak(0);
            u.setConfidence(500);
            u.setRank(0);
        }
        persistUsers(users);
    }
}
