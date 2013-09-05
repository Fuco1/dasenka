/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package league.managers;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import league.entities.Bet;
import league.entities.Game;
import league.entities.User;
import league.exceptions.BetManagerException;
import league.main.Config;
import league.main.League;
import org.apache.log4j.Logger;

/**
 *
 * @author Matus
 */
public class BetManager {

    private League league;
    private static Logger log = Logger.getLogger(BetManager.class);

    public BetManager(League league) {
        this.league = league;
    }

    public List<Bet> getSelfBetsFromGame(Game g) {
        return validateBets(getEm().createNamedQuery("getSelfBetsByGame").setParameter(1, g).getResultList(), g);
    }

    public Bet getSelfBet(Game g, User u) {
        Bet re;
        try {
            re = (Bet) getEm().createNamedQuery("getSelfBetByGameAndUserID").setParameter(1, g).setParameter(2, u).getSingleResult();
        } catch (Exception ex) {
            return null;
        }
        return re;
    }

    private List<Bet> validateBets(List<Bet> bets, Game g) {
        List<Bet> re = new ArrayList<Bet>();
        for (Bet b : bets) {
            if (g.getPlayers().contains(b.getUserBetRef())) {
                re.add(b);
            }
            else {
                try {
                    removeBet(b.getId());
                } catch (BetManagerException ex) {
                    log.error(ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        return re;
    }

    public Bet removeBet(Long id) throws BetManagerException, IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("Id not specified!");
        }
        Bet remove = null;
        try {
            remove = getEm().find(Bet.class, id);
            getEm().getTransaction().begin();
            getEm().remove(remove);
            getEm().getTransaction().commit();
        } catch (Exception ex) {
            throw new BetManagerException("Unable to remove bet with id: " + id, ex);
        }
        return remove;
    }

    public void persistBet(Bet b) {
        getEm().getTransaction().begin();
        getEm().persist(b);
        getEm().getTransaction().commit();
    }

    private Config getConfig() {
        return league.getConfig();
    }

    private EntityManager getEm() {
        return league.getEm();
    }

    private GameManager getGm() {
        return league.getGm();
    }

    private UserManager getUm() {
        return league.getUm();
    }
}
