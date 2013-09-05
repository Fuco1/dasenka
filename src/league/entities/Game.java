package league.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import league.comparators.GameUserOrderComparator;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
@NamedQueries({@NamedQuery(name = "getNumberOfGames",
                           query = "SELECT COUNT(g) FROM Game g"),
               @NamedQuery(name = "getGameIdByNum",
                           query = "SELECT g.id FROM Game g WHERE g.num = ?1"),
               @NamedQuery(name = "getPlayersByNum",
                           query = "SELECT g.players FROM Game g WHERE g.num = ?1"),
               @NamedQuery(name = "getExpDataByNum",
                           query = "SELECT g.exp FROM Game g WHERE g.num = ?1"),
               @NamedQuery(name = "getGamesInProgress",
                           query = "SELECT g FROM Game g WHERE g.matchResult = 'progress'"),
               //FROM Player p, IN (p.teams) AS t
               @NamedQuery(name = "getCommonGamesCount",
                           query = "SELECT COUNT(g) FROM Game g, IN (g.userOrder) u1, IN (g.userOrder) u2 WHERE " +
    "((((u1.userOrderRef = ?1) AND (u1.listOrder IN (0, 1, 2, 3, 4))) AND" +
    "((u2.userOrderRef = ?2) AND (u2.listOrder IN (0, 1, 2, 3, 4)))) AND" +
    "(u1.gameOrderRef = u2.gameOrderRef)) OR" +
    "((((u1.userOrderRef = ?1) AND (u1.listOrder IN (5, 6, 7, 8, 9))) AND" +
    "((u2.userOrderRef = ?2) AND (u2.listOrder IN (5, 6, 7, 8, 9)))) AND" +
    "(u1.gameOrderRef = u2.gameOrderRef))"),
               @NamedQuery(name = "getCommonGames",
                           query = "SELECT g FROM Game g, IN (g.userOrder) u1, IN (g.userOrder) u2 WHERE " +
    "((((u1.userOrderRef = ?1) AND (u1.listOrder IN (0, 1, 2, 3, 4))) AND" +
    "((u2.userOrderRef = ?2) AND (u2.listOrder IN (0, 1, 2, 3, 4)))) AND" +
    "(u1.gameOrderRef = u2.gameOrderRef)) OR" +
    "((((u1.userOrderRef = ?1) AND (u1.listOrder IN (5, 6, 7, 8, 9))) AND" +
    "((u2.userOrderRef = ?2) AND (u2.listOrder IN (5, 6, 7, 8, 9)))) AND" +
    "(u1.gameOrderRef = u2.gameOrderRef))"),
               @NamedQuery(name = "getCommonGamesAgainst",
                           query = "SELECT g FROM Game g, IN (g.userOrder) u1, IN (g.userOrder) u2 WHERE " +
    "((((u1.userOrderRef = ?1) AND (u1.listOrder IN (0, 1, 2, 3, 4))) AND" +
    "((u2.userOrderRef = ?2) AND (u2.listOrder IN (5, 6, 7, 8, 9)))) AND" +
    "(u1.gameOrderRef = u2.gameOrderRef)) OR" +
    "((((u1.userOrderRef = ?1) AND (u1.listOrder IN (5, 6, 7, 8, 9))) AND" +
    "((u2.userOrderRef = ?2) AND (u2.listOrder IN (0, 1, 2, 3, 4)))) AND" +
    "(u1.gameOrderRef = u2.gameOrderRef))"),
               @NamedQuery(name = "getRecentGames",
                           query = "SELECT g FROM Game g, IN (g.players) AS p WHERE p = ?1 ORDER BY g.matchDate DESC")
})
@Entity
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private int num;
    private String type;
    private String mode;
    private String matchResult;
    @OneToOne
    private User owner;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date matchDate;
    @OneToMany
    private List<User> players = new ArrayList<User>();
    @OneToMany(mappedBy = "gameOrderRef", cascade = CascadeType.ALL)
    private List<GameUserOrder> userOrder = new ArrayList<GameUserOrder>();
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<GameExp> exp = new ArrayList<GameExp>();
    @OneToMany(mappedBy = "gameRef", cascade = CascadeType.ALL)
    private List<GameGroup> groups = new ArrayList<GameGroup>();
    @OneToOne(mappedBy = "resultGameRef", cascade = CascadeType.ALL)
    private Results results;

    public Game() {
    }

    public Game(String type, String mode) {
        setNum(0);
        setType(type);
        setMode(mode);
        setResults(new Results(this));

        setMatchResult("not started");
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public List<GameExp> getExp() {
        return exp;
    }

    public void setExp(List<GameExp> exp) {
        this.exp = exp;
    }

    public Date getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(Date matchDate) {
        this.matchDate = matchDate;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public List<User> getPlayers() {
        return players;
    }

    public List<User> getOrderedPlayers() {
        List<User> re = new ArrayList();
        Collections.sort(userOrder, new GameUserOrderComparator());
        for (GameUserOrder g : userOrder) {
            re.add(g.getUserOrderRef());
        }
        return re;
    }

    public void setPlayers(List<User> players) {
        this.players = players;
    }

    public List<GameUserOrder> getUserOrder() {
        return userOrder;
    }

    public void setUserOrder(List<GameUserOrder> userOrder) {
        this.userOrder = userOrder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<GameGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<GameGroup> groups) {
        this.groups = groups;
    }

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public List<User> getSent() {
        return getOrderedPlayers().subList(0, 5);
    }

    public List<User> getScrg() {
        return getOrderedPlayers().subList(5, 10);
    }

    public List<User> getWinner(String[] resultSet) {
        if (getMatchResult().equals(resultSet[0])) {
            return getSent();
        }
        else if (getMatchResult().equals(resultSet[1])) {
            return getScrg();
        }
        else return null;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Game)) {
            return false;
        }
        Game other = (Game) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "league.entities.Game[id=" + id + "]";
    }

    public GameGroup getUserGroup(User u) {
        for (GameGroup g : groups) {
            if (g.isMember(u)) {
                return g;
            }
        }
        return null;
    }

    public int getUserOrder(User u) {
        int i = 0;
        for (GameUserOrder gu : userOrder) {
            if (gu.getUserOrderRef().equals(u)) {
                break;
            }
            i++;
        }
        return i;
    }

    public GameExp getUserExp(User u) {
        for (GameExp ge : getExp()) {
            if (ge.getUserRef().equals(u)) {
                return ge;
            }
        }
        return null;
    }

    public void clearGameGroups() {
        for (GameGroup gg : getGroups()) {
            gg.getGroupMembers().clear();
        }
        getGroups().clear();
    }
}
