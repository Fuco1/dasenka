/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */

/*
 * DELETE
FROM Player p
WHERE p.status = ’inactive’
AND p.teams IS EMPTY
 * */
@Entity
@NamedQueries({@NamedQuery(name = "removeGameGroups",
                           query = "DELETE FROM GameGroup g WHERE g.gameRef = ?1")
})
public class GameGroup implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne
    private User groupOwner;
    @OneToMany
    private List<User> groupMembers = new ArrayList<User>();
    @ManyToOne
    private Game gameRef;

    public GameGroup() {
    }

    public GameGroup(Game game, User owner) {
        setGameRef(game);
        setGroupOwner(owner);
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public Game getGameRef() {
        return gameRef;
    }

    public void setGameRef(Game gameRef) {
        this.gameRef = gameRef;
    }

    public List<User> getGroupMembers() {
        return groupMembers;
    }

    public void setGroupMembers(List<User> groupMembers) {
        this.groupMembers = groupMembers;
    }

    public User getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(User groupOwner) {
        this.groupOwner = groupOwner;
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
        if (!(object instanceof GameGroup)) {
            return false;
        }
        GameGroup other = (GameGroup) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "league.entities.GameGroups[id=" + id + "]";
    }

    public boolean isMember(User u) {
        return getGroupMembers().contains(u);
    }

    /*public int compareTo(GameGroup o) {
    return this.getGroupMembers().size() - o.getGroupMembers().size();
    }*/
}
