/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.managers;

import java.util.List;
import league.entities.Game;
import league.main.Config;
import league.main.League;
import league.wrappers.ClientWrapper;
import org.apache.log4j.Logger;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class TopicManager {

    private League league;
    private static Logger log = Logger.getLogger(TopicManager.class);

    public TopicManager(League league) {
        this.league = league;
    }

    public void setTopic(ClientWrapper app) {
        StringBuilder sb = new StringBuilder();
        sb.append(getConfig().getProperty("league.channelWelcome"));

        Game g = getGm().signupOpened();
        if (g != null) {
            sb.append(" || Sign-up is opened at the moment, ");
            sb.append("game type is \"" + g.getType() + "\", ");
            sb.append("mode is " + g.getMode() + ", ");
            if (g.getType().equals("challenge")) {
                sb.append(g.getPlayers().get(0).getName() + " and " + g.getPlayers().get(1).getName() +
                  " are the captains.");
            }
            else {
                //sb.append(g.getPlayers().get(0).getName() + " hosts the game.");
                sb.append(g.getOwner().getName() + " hosts the game.");
            }
        }

        List<Game> gamesInProgress = getGm().getGamesInProgress();
        if (gamesInProgress.size() > 0) {
            sb.append(" || Games currently in progress: ");
            for (Game gp : gamesInProgress) {
                sb.append(getGm().getGameName(gp.getMode(), gp.getNum())).append(" ");
            }
        }
        app.sendTopicMsg(getConfig().getProperty(app.getType() + ".mainChannelName"), new String(sb));
    }

    private Config getConfig() {
        return league.getConfig();
    }

    private GameManager getGm() {
        return league.getGm();
    }

    private UserManager getUm() {
        return league.getUm();
    }
}


