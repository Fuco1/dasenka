/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

/**
 *
 * @author Matus
 */
@Entity
@NamedQueries({@NamedQuery(name = "getSelfBetsByGame",
                           query = "SELECT b FROM Bet b WHERE b.gameBetRef = ?1 and b.betType = 'selfbet'"),
               @NamedQuery(name = "getSelfBetByGameAndUserID",
                           query = "SELECT b FROM Bet b WHERE b.gameBetRef = ?1 and b.userBetRef = ?2" +
    " and b.betType = 'selfbet'")
})
public class Bet implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne
    private User userBetRef;
    @OneToOne
    private Game gameBetRef;
    private int betValue;
    private double betWinRatio;
    private String betType;

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public int getBetValue() {
        return betValue;
    }

    public void setBetValue(int betValue) {
        this.betValue = betValue;
    }

    public double getBetWinRatio() {
        return betWinRatio;
    }

    public void setBetWinRatio(double betWinRatio) {
        this.betWinRatio = betWinRatio;
    }

    public Game getGameBetRef() {
        return gameBetRef;
    }

    public void setGameBetRef(Game gameBetRef) {
        this.gameBetRef = gameBetRef;
    }

    public User getUserBetRef() {
        return userBetRef;
    }

    public void setUserBetRef(User userBetRef) {
        this.userBetRef = userBetRef;
    }

    public String getBetType() {
        return betType;
    }

    public void setBetType(String betType) {
        this.betType = betType;
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
        if (!(object instanceof Bet)) {
            return false;
        }
        Bet other = (Bet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "league.entities.Bet[id=" + id + "]";
    }
}
