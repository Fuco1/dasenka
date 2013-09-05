package league.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import league.comparators.GameGroupSizeComparator;
import league.comparators.UserAccessExpComparator;
import league.comparators.UserReversedExpComparator;
import league.entities.Bet;
import league.entities.Game;
import league.entities.GameExp;
import league.entities.GameGroup;
import league.entities.GameUserOrder;
import league.entities.User;
import league.entities.nodb.PlayerPick;
import league.entities.nodb.Vote;
import league.exceptions.GameManagerException;
import league.main.Config;
import league.main.League;
import league.main.Utility;
import org.apache.log4j.Logger;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class GameManager {

    private League league;
    private static Logger log = Logger.getLogger(GameManager.class);
    private PlayerPick pick;
    private Map<String, Vote> votes = new HashMap<String, Vote>();

    public GameManager(League league) {
        this.league = league;
    }

    public Map<String, Vote> getVotes() {
        return votes;
    }

    public Game startNewGame(String type, String mode) throws GameManagerException, IllegalArgumentException {
        if ((type == null) || (type.length() == 0)) {
            throw new IllegalArgumentException("Type is null");
        }
        if ((mode == null) || (mode.length() == 0)) {
            throw new IllegalArgumentException("Mode is null");
        }

        Game g = null;
        try {
            g = getGame(0);
        } catch (GameManagerException ex) {
            log.info(ex.getMessage() + " starting up new one!");
        }

        if (g != null) {
            throw new GameManagerException("Game already in progress!");
        }

        g = new Game(type, mode);

        try {
            getEm().getTransaction().begin();
            getEm().persist(g);
            getEm().getTransaction().commit();
        } finally {
            if (getEm().getTransaction().isActive()) {
                getEm().getTransaction().rollback();
            }
        }

        return g;
    }

    public Game getGame(Long id) throws GameManagerException {
        if (id == null) {
            throw new GameManagerException("Game not found!");
        }
        Game re = null;
        try {
            re = getEm().getReference(Game.class, id);
            re.setPlayers(getPlayers(re.getNum()));
            re.setExp(getExp(re.getNum()));

        } catch (EntityNotFoundException ex) {
            throw new GameManagerException("Game not found!");
        }

        if (re == null) {
            throw new GameManagerException("Game not found!");
        }
        return re;
    }

    public Game getGame(int num) throws GameManagerException {
        Long id = null;
        try {
            id = (Long) getEm().createNamedQuery("getGameIdByNum").setParameter(1, num).getSingleResult();
        } catch (NoResultException ex) {
            throw new GameManagerException("Game not found!", ex);
        }
        return getGame(id);
    }

    public Game removeGame(Long id) throws GameManagerException, IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("Id not specified!");
        }
        Game remove = null;
        try {
            remove = getEm().find(Game.class, id);
            getEm().getTransaction().begin();
            getEm().remove(remove);
            getEm().getTransaction().commit();
        } catch (Exception ex) {
            throw new GameManagerException("Unable to remove game with id: " + id, ex);
        }
        return remove;
    }

    public Game removeGame(int num) throws GameManagerException {
        Long remove = null;
        try {
            remove = (Long) getEm().createNamedQuery("getGameIdByNum").setParameter(1, num).getSingleResult();
        } catch (Exception ex) {
            throw new GameManagerException("Unable to remove game with num: " + num, ex);
        }
        return removeGame(remove);
    }

    public List<User> getPlayers(int num) {
        return getEm().createNamedQuery("getPlayersByNum").setParameter(1, num).getResultList();
    }

    public List<GameExp> getExp(int num) {
        return getEm().createNamedQuery("getExpDataByNum").setParameter(1, num).getResultList();
    }

    public void persistGame(Game g) {
        try {
            getEm().getTransaction().begin();
            getEm().persist(g);
            getEm().getTransaction().commit();
        } catch (Exception ex) {
            try {
                getEm().getTransaction().rollback();
            } catch (Exception exx) {
                log.info("Rollback failed: " + exx.getMessage());
            }
            log.info("Transaction failed: " + ex.getMessage());
        }
    }

    public void setUserListOrder(Game g) {
        int i = 0;
        g.getUserOrder().clear();
        for (User u : g.getPlayers()) {
            //System.out.println(u.getName());
            g.getUserOrder().add(new GameUserOrder(u, i, g));
            i++;
        }
    }

    public void clearGameGroups(Game g) {
        g.clearGameGroups();
        getEm().createNamedQuery("removeGameGroups").setParameter(1, g.getId()).executeUpdate();
    }

    public boolean gameIsOpened() {
        Game g = null;
        try {
            g = getGame(0);
        } catch (GameManagerException ex) {
            log.info("game not found!");
        }
        return (g != null);
    }

    private Config getConfig() {
        return league.getConfig();
    }

    private EntityManager getEm() {
        return league.getEm();
    }

    private UserManager getUm() {
        return league.getUm();
    }

    private BetManager getBm() {
        return league.getBm();
    }

    public PlayerPick getPick() {
        return pick;
    }

    public void initNewChall(User picker1, User picker2, Game game) {
        this.pick = new PlayerPick(picker1, picker2, game);
    }

    public int getCommonGamesCount(User u, User v) {
        return ((Long) getEm().createNamedQuery("getCommonGamesCount").setParameter(1, u).setParameter(2, v).getSingleResult()).intValue();
    }

    public List<Game> getCommonGames(User u, User v) {
        return getEm().createNamedQuery("getCommonGames").setParameter(1, u).setParameter(2, v).getResultList();
    }

    public List<Game> getCommonGamesAgainst(User u, User v) {
        return getEm().createNamedQuery("getCommonGamesAgainst").setParameter(1, u).setParameter(2, v).getResultList();
    }

    public List<Game> getRecentGames(User u) {
        return getEm().createNamedQuery("getRecentGames").setParameter(1, u).setMaxResults(getConfig().getRecentGamesLimit()).getResultList();
    }

    public String getInverseResult(String result) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        if (result.equals(resultSet[0])) {
            return resultSet[1];
        }
        else {
            return resultSet[0];
        }
    }

    public double e(double myExp, double enemyExp) {
        double re = 0;
        re = 1 / (1 + Math.pow(10, (enemyExp - myExp) / getConfig().getEloGapModifier()));
        return re;
    }

    private int ratingChange(double k, double score, double expected) {
        return (int) Math.round(k * (score - expected));
    }

    private int getKFactor(User u, Game g) {
        double kFactor = getConfig().getBaseKFactor(u.getGames());
        kFactor = kFactor * getConfig().getStreakModifier(u.getStreak());

        int common = 0;
        List<User> team = (g.getUserOrder(u) < getConfig().getPlayerNumTeam()) ? g.getSent() : g.getScrg();
        for (User tu : team) {
            if (!u.equals(tu)) {
                common += getCommonGamesCount(u, tu);
            }
        }
        kFactor *= Math.pow(0.5, common / getConfig().getCommonGamesGapModifier());
        return (int) kFactor;
    }

    public List<String> closeGame(Game g, String result) {
        List<String> re = new ArrayList<String>();
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        List<User> winnerTeam = null;
        List<User> loserTeam = null;
        if (result.equals(resultSet[0])) {
            winnerTeam = g.getSent();
            loserTeam = g.getScrg();
        }
        else if (result.equals(resultSet[1])) {
            winnerTeam = g.getScrg();
            loserTeam = g.getSent();
        }

        if (!result.equals(resultSet[2])) {
            for (User u : winnerTeam) {
                u.incWin(1);
                if (u.getStreak() > 0) {
                    u.incStreak(1);
                }
                else {
                    u.setStreak(1);
                }

                if (u.getStreak() > u.getBestStreak()) {
                    u.setBestStreak(u.getStreak());
                }
            }

            for (User u : loserTeam) {
                u.incLoss(1);
                if (u.getStreak() < 0) {
                    u.incStreak(-1);
                }
                else {
                    u.setStreak(-1);
                }

                if (u.getStreak() < u.getWorstStreak()) {
                    u.setWorstStreak(u.getStreak());
                }
            }

            // if we have selfbetting enabled, payout users
            if (getConfig().getProperty("league.selfbet.enable") != null) {
                StringBuilder sb = new StringBuilder();
                List<Bet> bets = getBm().getSelfBetsFromGame(g);
                for (Bet b : bets) {
                    if (winnerTeam.contains(b.getUserBetRef())) {
                        double win = b.getBetValue() * b.getBetWinRatio();
                        b.getUserBetRef().incExp((int) win);
                        sb.append(b.getUserBetRef().getName() + "[" + (int) win + "]; ");
                    }
                }
                if (sb.length() > 2) {
                    sb.delete(sb.length() - 2, sb.length());
                }
                re.add(new String(sb));
            }
            else {
                re.add(null);
            }
        }
        else {
            for (User u : g.getPlayers()) {
                u.incDraw(1);
            }
        }

        //set match result
        g.setMatchResult(result);

        List<GameExp> gameexp = new ArrayList<GameExp>();
        for (User u : g.getPlayers()) {
            gameexp.add(new GameExp(u, g));
        }
        g.setExp(gameexp);

        //exp is always as if sentinel won, altexp for scourge
        //sent exp
        calcExp(g, resultSet[0], true);
        //scrg exp
        calcExp(g, resultSet[1], false);

        if (result.equals(resultSet[0])) {
            for (User gu : g.getPlayers()) {
                gu.incExp(g.getUserExp(gu).getExp());
            }
        }
        else if (result.equals(resultSet[1])) {
            for (User gu : g.getPlayers()) {
                gu.incExp(g.getUserExp(gu).getAltexp());
            }
        }

        // remove players from current game
        for (User gu : g.getPlayers()) {
            gu.setCurrentGame(null);
            gu.setLastGame(g); //set lastgame to this game
        }

        //persist changes
        persistGame(g);
        getUm().sortUsers(); //this will also execute persist
        //getUm().persistUsers(g.getPlayers());

        return re;
    }

    public void calcExp(Game g, String result, boolean team1Result) {
        String resultSet[] = getConfig().getProperty("league.results").split("\\s");
        List<User> winnerTeam = null;
        List<User> loserTeam = null;
        if (result.equals(resultSet[0])) {
            winnerTeam = g.getSent();
            loserTeam = g.getScrg();
        }
        else if (result.equals(resultSet[1])) {
            winnerTeam = g.getScrg();
            loserTeam = g.getSent();
        }

        double winnerExp = 0;
        double loserExp = 0;
        int winnerCount = 0;
        int loserCount = 0;
        double winnerCasebonus = 0;
        double loserCasebonus = 0;
        double baseTeamFactor = 0;
        double teamGain = 0;
        double teamLoss = 0;

        //get winner exp and count
        double winnerExpTmp = 1;
        for (User u : winnerTeam) {
            winnerCount++;
            winnerExpTmp *= u.getExp();
        }
        winnerExp = Math.pow(10, Math.log10(winnerExpTmp) / winnerCount);

        //get loser exp and count
        double loserExpTmp = 1;
        for (User u : loserTeam) {
            loserCount++;
            loserExpTmp *= u.getExp();
        }
        loserExp = Math.pow(10, Math.log10(loserExpTmp) / loserCount);

        //System.out.println("Winner XP: " + winnerExp);
        //System.out.println("Loser XP: " + loserExp);

        //get streak casebonus
        for (User u : loserTeam) {
            if (u.getStreak() >= 4) {
                winnerCasebonus += u.getStreak() * getConfig().getStreakCasebonusModifier();
            }
        }

        //System.out.println("Winner casebonus: " + winnerCasebonus);
        //System.out.println("Loser casebonus: " + loserCasebonus);

        baseTeamFactor = getConfig().getDefaultTeamFactor();
        if (!g.getType().equals("challenge")) {
            baseTeamFactor *= 1.5;
        }

        if (Utility.getDateDiffInMinutes(new Date(), g.getMatchDate()) < 30) {
            baseTeamFactor /= 1.5;
        }
        else if (Utility.getDateDiffInMinutes(new Date(), g.getMatchDate()) < 45) {
            baseTeamFactor /= 1.3;
        }

        //System.out.println("BaseTeamFactor: " + baseTeamFactor);

        double teamFactor;
        teamFactor = baseTeamFactor - ((winnerCount - loserCount) * getConfig().getTeamCountModifier());
        teamGain = ratingChange(teamFactor, 1, e(winnerExp, loserExp));
        teamGain += winnerCasebonus;
        if (teamGain < 0) {
            teamGain = 0;
        }

        teamFactor = baseTeamFactor + ((loserCount - winnerCount) * getConfig().getTeamCountModifier());
        teamLoss = ratingChange(teamFactor, 0, e(loserExp, winnerExp));
        teamLoss += loserCasebonus;
        if (teamLoss > 0) {
            teamLoss = 0;
        }

        //System.out.println("Team gain: " + teamGain);
        //System.out.println("Team loss: " + teamLoss);

        double base;
        double xpChange;
        for (User u : winnerTeam) {
            base = getConfig().getBaseKFactor(u.getGames());
            //System.out.println("base: " + base);
            base *= getConfig().getStreakModifier(u.getStreak());
            //System.out.println("base after streak mod: " + base);
            xpChange = ratingChange(base, 1, e(u.getExp(), loserExp));
            //System.out.println("xpChange: " + xpChange);
            xpChange = xpChange + teamGain;
            //System.out.println("User " + u.getName() + " final xpChange: " + xpChange);

            xpChange = Math.round(xpChange);
            if (xpChange < 0) {
                xpChange = 0;
            }

            GameExp xp = g.getUserExp(u);
            if (team1Result) {
                xp.setExp((int) xpChange);
            }
            else {
                xp.setAltexp((int) xpChange);
            }
        }

        for (User u : loserTeam) {
            base = getConfig().getBaseKFactor(u.getGames());
            //System.out.println("base: " + base);
            base *= getConfig().getStreakModifier(u.getStreak());
            //System.out.println("base after streak mod: " + base);
            xpChange = ratingChange(base, 0, e(u.getExp(), winnerExp));
            //System.out.println("xpChange: " + xpChange);
            xpChange = xpChange + teamLoss;
            //System.out.println("User " + u.getName() + " final xpChange: " + xpChange);

            xpChange = Math.round(xpChange);
            if (xpChange > 0) {
                xpChange = 0;
            }

            GameExp xp = g.getUserExp(u);
            if (team1Result) {
                xp.setExp((int) xpChange);
            }
            else {
                xp.setAltexp((int) xpChange);
            }
        }
    }

    public boolean checkMode(String mode, String type) {
        String modeList = getConfig().getProperty("league.gameModes." + type);
        String[] modes = Utility.tokenize(modeList);
        for (String m : modes) {
            if (m.equals(mode)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkResult(String result) {
        String resultList = getConfig().getProperty("league.results");
        String[] results = Utility.tokenize(resultList);
        for (String m : results) {
            if (m.equals(result)) {
                return true;
            }
        }
        return false;
    }

    public Game signupOpened() {
        Game g = null;
        try {
            g = getGame(0);
        } catch (GameManagerException ex) {
        }
        return g;
    }

    public List<Game> getGamesInProgress() {
        return getEm().createNamedQuery("getGamesInProgress").getResultList();
    }

    public int getGameNum() {
        Long re = (Long) getEm().createNamedQuery("getNumberOfGames").getSingleResult();
        return re.intValue();
    }

    public String getGameName(String mode, int num) {
        return getConfig().getProperty("league.gamePrefix") + mode + num;
    }

    public String getGameName(Game g) {
        return getConfig().getProperty("league.gamePrefix") + g.getMode() + g.getNum();
    }

    public boolean verifyGroups(int[] groups) {
        Arrays.sort(groups);
        int sent = 0;
        int scrg = 0;

        boolean[] used = new boolean[groups.length];
        for (boolean b : used) {
            b = false;
        }

        for (int i = groups.length - 1; i >= 0; i--) {
            if (sent + groups[i] <= getConfig().getPlayerNumTeam()) {
                sent += groups[i];
                used[i] = true;
            }
        }

        for (int i = 0; i < groups.length; i++) {
            if (!used[i]) {
                scrg += groups[i];
            }
        }

        return ((sent <= getConfig().getPlayerNumTeam()) && (scrg <= getConfig().getPlayerNumTeam()));
    }

    public void createTeamsFromGroups(List<GameGroup> groups, List<User> sentTeam, List<User> scrgTeam) {
        Collections.sort(groups, new GameGroupSizeComparator());

        int sent = 0;
        int scrg = 0;

        sentTeam.clear();
        scrgTeam.clear();

        boolean[] used = new boolean[groups.size()];
        for (boolean b : used) {
            b = false;
        }

        for (int i = groups.size() - 1; i >= 0; i--) {
            if (sent + groups.get(i).getGroupMembers().size() <= getConfig().getPlayerNumTeam()) {
                sent += groups.get(i).getGroupMembers().size();
                used[i] = true;
            }
        }

        for (int i = 0; i < groups.size(); i++) {
            if (used[i]) {
                sentTeam.addAll(groups.get(i).getGroupMembers());
            }
            else {
                scrgTeam.addAll(groups.get(i).getGroupMembers());
            }
        }

        User sentCpt = Collections.max(sentTeam, new UserAccessExpComparator());
        User scrgCpt = Collections.max(scrgTeam, new UserAccessExpComparator());

        sentTeam.remove(sentCpt);
        sentTeam.add(0, sentCpt);

        scrgTeam.remove(scrgCpt);
        scrgTeam.add(0, scrgCpt);
    }

    public void createTeamsFromPlayers(List<User> players, List<User> sentTeam, List<User> scrgTeam) {
        Collections.sort(players, new UserReversedExpComparator());

        sentTeam.add(players.get(0));
        scrgTeam.add(players.get(1));
        scrgTeam.add(players.get(2));
        sentTeam.add(players.get(3));
        sentTeam.add(players.get(4));
        scrgTeam.add(players.get(5));
        scrgTeam.add(players.get(6));
        sentTeam.add(players.get(7));
        sentTeam.add(players.get(8));
        scrgTeam.add(players.get(9));

        Random r = new Random(new Date().getTime());
        List<User> tmp;
        if (r.nextInt(99) < 50) {
            tmp = sentTeam;
            sentTeam = scrgTeam;
            scrgTeam = tmp;
        }
    }
}
