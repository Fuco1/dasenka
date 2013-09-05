/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.entities.nodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import league.entities.Game;
import league.entities.User;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class Vote {

    private Game g;
    private List<User> voted = new ArrayList<User>();
    private Map<String, Integer> votes = new HashMap<String, Integer>();
    private String id;

    public Vote(Game g, String id) {
        this.g = g;
        this.id = id;
    }

    public Game getG() {
        return g;
    }

    public void setG(Game g) {
        this.g = g;
    }

    public List<User> getVoted() {
        return voted;
    }

    public void setVoted(List<User> voted) {
        this.voted = voted;
    }

    public Map<String, Integer> getVotes() {
        return votes;
    }

    public void setVotes(Map<String, Integer> votes) {
        this.votes = votes;
    }

    public void addVoter(User u) {
        this.voted.add(u);
    }

    public void addVote(String vote) {
        if (this.votes.get(vote) == null) {
            this.votes.put(vote, 1);
        }
        else {
            Integer i = this.votes.get(vote);
            i++;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Vote other = (Vote) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
}
