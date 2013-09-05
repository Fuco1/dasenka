package league.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static league.main.League.*;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
@Entity
@Table(name = "USER_TABLE")
@NamedQueries({@NamedQuery(name = "getVouchees",
                           query = "SELECT u.vouchedUsers FROM User u WHERE u.auth LIKE ?1"),
               @NamedQuery(name = "getPromotedUsers",
                           query = "SELECT u.promotedUsers FROM User u WHERE u.auth LIKE ?1"),
               @NamedQuery(name = "getFriendList",
                           query = "SELECT u.friendList FROM User u WHERE u.auth LIKE ?1"),
               @NamedQuery(name = "getFriendOf",
                           query = "SELECT u.friendOf FROM User u WHERE u.auth LIKE ?1"),
               @NamedQuery(name = "getIdByAuth",
                           query = "SELECT u.id FROM User u WHERE u.auth LIKE ?1"),
               @NamedQuery(name = "getIdByName",
                           query = "SELECT u.id FROM User u WHERE u.name LIKE ?1"),
               @NamedQuery(name = "getAccessByAuth",
                           query = "SELECT u.accessLevel FROM User u WHERE u.auth LIKE ?1"),
               @NamedQuery(name = "getAccessById",
                           query = "SELECT u.accessLevel FROM User u WHERE u.id = ?1"),
               @NamedQuery(name = "getUsers",
                           query = "SELECT u FROM User u"),
               @NamedQuery(name = "getUserCount",
                           query = "SELECT COUNT(u) FROM User u"),
               @NamedQuery(name = "getTopUsers",
                           query = "SELECT u FROM User u ORDER BY u.rank ASC"),
               @NamedQuery(name = "getUsersByAccess",
                           query = "SELECT u FROM User u WHERE u.accessLevel = ?1")
})
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String auth;
    private int accessLevel;
    private int win;
    private int loss;
    private int draw;
    private int exp;
    private int streak;
    private int bestStreak;
    private int worstStreak;
    @OneToOne
    private Game lastGame;
    @OneToOne
    private Game currentGame;
    private int confidence;
    private String name;
    private int rank;
    @ManyToOne
    private User vouchedBy;
    @ManyToOne
    private User promotedBy;

    //@OneToMany(mappedBy = "vouchedBy")
    @OneToMany(mappedBy = "vouchedBy")
    private List<User> vouchedUsers = new ArrayList<User>();
    //@OneToMany(mappedBy = "promotedBy")
    @OneToMany(mappedBy = "promotedBy")
    private List<User> promotedUsers = new ArrayList<User>();
    //@ManyToMany
    @ManyToMany
    private List<User> friendList = new ArrayList<User>();
    //@ManyToMany(mappedBy = "friendList")
    @ManyToMany(mappedBy = "friendList")
    private List<User> friendOf = new ArrayList<User>();
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date vouchDate;

    public User() {
    }

    public User(String auth, String name, User vouchedBy) {
        setAuth(auth);
        setName(name);
        setVouchedBy(vouchedBy);
        setPromotedBy(null);

        setAccessLevel(10);
        setWin(0);
        setLoss(0);
        setDraw(0);
        setExp(1000);
        setStreak(0);
        setBestStreak(0);
        setWorstStreak(0);
        setConfidence(500);
        setRank(0);

        setLastGame(null);
        setCurrentGame(null);

        setVouchDate(new Date());
    }

    public List<User> getVouchedUsers() {
        return vouchedUsers;
    }

    public void setVouchedUsers(List<User> vouchedUsers) {
        this.vouchedUsers = vouchedUsers;
    }

    public List<User> getPromotedUsers() {
        return promotedUsers;
    }

    public void setPromotedUsers(List<User> promotedUsers) {
        this.promotedUsers = promotedUsers;
    }

    public List<User> getFriendList() {
        return friendList;
    }

    public void setFriendList(List<User> friendList) {
        this.friendList = friendList;
    }

    public List<User> getFriendOf() {
        return friendOf;
    }

    public void setFriendOf(List<User> friendOf) {
        this.friendOf = friendOf;
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public int getWorstStreak() {
        return worstStreak;
    }

    public void setWorstStreak(int worstStreak) {
        this.worstStreak = worstStreak;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getLoss() {
        return loss;
    }

    public void setLoss(int loss) {
        this.loss = loss;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getPromotedBy() {
        return promotedBy;
    }

    public void setPromotedBy(User promotedBy) {
        this.promotedBy = promotedBy;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public Date getVouchDate() {
        return vouchDate;
    }

    public void setVouchDate(Date vouchDate) {
        this.vouchDate = vouchDate;
    }

    public User getVouchedBy() {
        return vouchedBy;
    }

    public void setVouchedBy(User vouchedBy) {
        this.vouchedBy = vouchedBy;
    }

    public int getWin() {
        return win;
    }

    public void setWin(int win) {
        this.win = win;
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(Game currentGame) {
        this.currentGame = currentGame;
    }

    public Game getLastGame() {
        return lastGame;
    }

    public void setLastGame(Game lastGame) {
        this.lastGame = lastGame;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        if ((this.auth == null) ? (other.auth != null) : !this.auth.equals(other.auth)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 17 * hash + (this.auth != null ? this.auth.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "league.entities.User[id=" + id + "]";
    }

    public int getGames() {
        return getWin() + getLoss() + getDraw();
    }

    /*******************************************************/
    public void incConfidence(int confidence) {
        this.confidence += confidence;
    }

    public void incWin(int win) {
        this.win += win;
    }

    public void incLoss(int loss) {
        this.loss += loss;
    }

    public void incDraw(int draw) {
        this.draw += draw;
    }

    public void incExp(int exp) {
        this.exp += exp;
    }

    public void incStreak(int streak) {
        this.streak += streak;
    }
    /*******************************************************/
}
