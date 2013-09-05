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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
@Entity
public class Results implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne
    private Game resultGameRef;
    @OneToMany
    private List<User> voted = new ArrayList<User>();
    @OneToMany(cascade = CascadeType.ALL)
    private List<StringWrapper> votes = new ArrayList<StringWrapper>();
    private int sent;
    private int scrg;

    public Results() {
    }

    public Results(Game resultGameRef) {
        setResultGameRef(resultGameRef);
        setSent(0);
        setScrg(0);
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public Game getResultGameRef() {
        return resultGameRef;
    }

    public void setResultGameRef(Game resultGameRef) {
        this.resultGameRef = resultGameRef;
    }

    public int getScrg() {
        return scrg;
    }

    public void setScrg(int scrg) {
        this.scrg = scrg;
    }

    public void incScrg(int scrg) {
        this.scrg += scrg;
    }

    public int getSent() {
        return sent;
    }

    public void setSent(int sent) {
        this.sent = sent;
    }

    public void incSent(int sent) {
        this.sent += sent;
    }

    public List<User> getVoted() {
        return voted;
    }

    public void setVoted(List<User> voted) {
        this.voted = voted;
    }

    public List<StringWrapper> getVotes() {
        return votes;
    }

    public void setVotes(List<StringWrapper> votes) {
        this.votes = votes;
    }

    public List<String> getVotesStr() {
        List<String> re = new ArrayList<String>();
        for (StringWrapper sw: getVotes()) {
            re.add(sw.getText());
        }
        return re;
    }

    public void add(String text) {
        StringWrapper sw = new StringWrapper();
        sw.setText(text);
        getVotes().add(sw);
    }

    /*public List<String> getVotes() {
    return votes;
    }

    public void setVotes(List<String> votes) {
    this.votes = votes;
    }*/
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Results)) {
            return false;
        }
        Results other = (Results) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "league.entities.Results[id=" + id + "]";
    }
}
