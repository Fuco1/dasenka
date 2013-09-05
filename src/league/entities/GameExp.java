package league.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
@Entity
public class GameExp implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private int exp;
    private int altexp;
    @OneToOne
    private User userRef;
    @ManyToOne
    private Game game;

    public GameExp() {
    }

    public GameExp(User userRef, Game game) {
        this.userRef = userRef;
        this.game = game;
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getAltexp() {
        return altexp;
    }

    public void setAltexp(int altexp) {
        this.altexp = altexp;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public User getUserRef() {
        return userRef;
    }

    public void setUserRef(User userRef) {
        this.userRef = userRef;
    }

    public String getExpStr() {
        return new String((exp > 0) ? ("+" + Integer.toString(exp)) : Integer.toString(exp));
    }

    public String getAltexpStr() {
        return new String((altexp > 0) ? ("+" + Integer.toString(altexp)) : Integer.toString(altexp));
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
        if (!(object instanceof GameExp)) {
            return false;
        }
        GameExp other = (GameExp) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "league.entities.GameExp[id=" + id + "]";
    }
}
