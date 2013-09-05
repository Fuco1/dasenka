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
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
@Entity
public class GameUserOrder implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private int listOrder;
    @OneToOne
    private User userOrderRef;
    @ManyToOne
    private Game gameOrderRef;

    public GameUserOrder() {
    }

    public GameUserOrder(User userOrderRef, int listOrder, Game gameOrderRef) {
        setUserOrderRef(userOrderRef);
        setListOrder(listOrder);
        setGameOrderRef(gameOrderRef);
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public Game getGameOrderRef() {
        return gameOrderRef;
    }

    public void setGameOrderRef(Game gameOrderRef) {
        this.gameOrderRef = gameOrderRef;
    }

    public int getListOrder() {
        return listOrder;
    }

    public void setListOrder(int listOrder) {
        this.listOrder = listOrder;
    }

    public User getUserOrderRef() {
        return userOrderRef;
    }

    public void setUserOrderRef(User userOrderRef) {
        this.userOrderRef = userOrderRef;
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
        if (!(object instanceof GameUserOrder)) {
            return false;
        }
        GameUserOrder other = (GameUserOrder) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        //return "league.entities.GameUserOrder[id=" + id + "]";
        return gameOrderRef.getNum() + " " + userOrderRef.getName() + " " + listOrder;
    }
}
