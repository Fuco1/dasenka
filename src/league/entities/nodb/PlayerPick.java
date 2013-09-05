/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.entities.nodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import league.entities.Game;
import league.entities.User;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class PlayerPick {

    private Game g;
    private User picker1;
    private User picker2;
    private int round;
    private List<User> sent = new ArrayList<User>();
    private List<User> scrg = new ArrayList<User>();
    private boolean picker1ready;
    private boolean picker2ready;

    public PlayerPick(User picker1, User picker2, Game game) {
        Random r = new Random();
        if (r.nextInt(100) > 50) {
            setPicker1(picker1);
            setPicker2(picker2);
            sent.add(picker1);
            scrg.add(picker2);
        }
        else {
            setPicker1(picker2);
            setPicker2(picker1);
            sent.add(picker2);
            scrg.add(picker1);
        }
        
        this.g = game;
        
        setPicker1ready(false);
        setPicker2ready(false);

        setRound(0);
    }

    public User getPicker1() {
        return picker1;
    }

    public void setPicker1(User picker1) {
        this.picker1 = picker1;
    }

    public User getPicker2() {
        return picker2;
    }

    public void setPicker2(User picker2) {
        this.picker2 = picker2;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public void incRound(int round) {
        this.round += round;
    }

    public List<User> getScrg() {
        return scrg;
    }

    public void setScrg(List<User> scrg) {
        this.scrg = scrg;
    }

    public List<User> getSent() {
        return sent;
    }

    public void setSent(List<User> sent) {
        this.sent = sent;
    }

    public boolean isPicker1ready() {
        return picker1ready;
    }

    public void setPicker1ready(boolean picker1ready) {
        this.picker1ready = picker1ready;
    }

    public boolean isPicker2ready() {
        return picker2ready;
    }

    public void setPicker2ready(boolean picker2ready) {
        this.picker2ready = picker2ready;
    }

    public User whopick() {
        return whopick(getRound());
    }
    
    public User whopick(int round) {
        switch (round) {
            case 0:
                return picker1;
            case 1:
                return picker2;
            case 2:
                return picker2;
            case 3:
                return picker1;
            case 4:
                return picker1;
            case 5:
                return picker2;
            case 6:
                return picker2;
            case 7:
                return picker1;
            case 8:
                return picker1;
            case 9:
                return picker2;
        }
        return null;
    }

    public boolean canPick() {
        return isPicker1ready() && isPicker2ready();
    }

    public List<User> getPool() {
        List<User> pool = g.getPlayers();
        pool.removeAll(sent);
        pool.removeAll(scrg);
        return pool;
    }
}
