package league.main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class Config {

    private class RankPromoteLimits {

        private List<Integer> limits = new ArrayList<Integer>();

        public int get(int n) {
            return limits.get(n);
        }

        public void add(int lim) {
            this.limits.add(lim);
        }
    }

    private class CommandData {

        private String name;
        private int access;
        private int accessLimit;

        public CommandData() {
        }

        public CommandData(String name, int access, int accessLimit) {
            setName(name);
            setAccess(access);
            setAccessLimit(accessLimit);
        }

        public int getAccess() {
            return access;
        }

        public void setAccess(int access) {
            this.access = access;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAccessLimit() {
            return accessLimit;
        }

        public void setAccessLimit(int accessLimit) {
            this.accessLimit = accessLimit;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CommandData other = (CommandData) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }
    }
    private Properties config;
    private List<String> accessRanks = new ArrayList<String>();
    private List<Integer> accessLevels = new ArrayList<Integer>();
    private List<RankPromoteLimits> promoteLimits = new ArrayList<RankPromoteLimits>();
    private List<WhoisRankShowMode> rankShowMode = new ArrayList<WhoisRankShowMode>();
    private List<RankPromoteMode> rankPromoteMode = new ArrayList<RankPromoteMode>();
    private int rankNum;
    private Map<String, CommandData> commands = new HashMap<String, CommandData>();
    private Map<Integer, String> exprank = new HashMap<Integer, String>();
    private int expRankSpread;
    private int expRankBottom;
    private int expRankTop;
    private int expRankNum;
    private int playerNum;
    private int[] baseKFactorGameNum;
    private int[] baseKFactor;
    private int[] streakModCount;
    private double[] streakModifier;
    private int[] streakRankNum;
    private String[] streakRankString;
    private Map<String, String> commandGameTypeList = new HashMap<String, String>();
    private Map<String, String> switchToGameType = new HashMap<String, String>();
    private Map<String, String> commandMapping = new HashMap<String, String>();
    private Set<String> fullCommandList = new HashSet<String>(); //also with mappings

    Config(Properties config) {
        this.config = config;
        init();
    }

    public boolean reload() {
        config.clear();
        accessRanks.clear();
        accessLevels.clear();
        promoteLimits.clear();
        rankShowMode.clear();
        rankPromoteMode.clear();
        commands.clear();
        exprank.clear();
        commandGameTypeList.clear();
        switchToGameType.clear();
        commandMapping.clear();
        fullCommandList.clear();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream("clientConfig.properties");
            config.load(fis);
            init();
            return true;
        } catch (IOException ex) {
            System.out.println("Can't reload the file!");
            return false;
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                System.out.println("Can't close the file!");
            }
        }
    }

    private void init() {
        rankNum = Integer.parseInt(config.getProperty("league.rank.number"));
        for (int i = 0; i < rankNum; i++) {
            accessRanks.add(config.getProperty("league.rank.string." + i));
            accessLevels.add(Integer.parseInt(config.getProperty("league.rank.access." + i)));
        }

        for (int i = 0; i < rankNum; i++) {
            RankPromoteLimits l = new RankPromoteLimits();
            for (int j = 0; j < rankNum; j++) {
                l.add(Integer.parseInt(config.getProperty("league.rank.limit." + i + "." + j)));
            }
            promoteLimits.add(l);
        }

        String currentMode = null;
        for (int i = 0; i < rankNum; i++) {
            currentMode = config.getProperty("league.rank.showInWhois." + i);
            if (currentMode.equalsIgnoreCase("yes")) {
                rankShowMode.add(WhoisRankShowMode.YES);
            }
            else if (currentMode.equalsIgnoreCase("no")) {
                rankShowMode.add(WhoisRankShowMode.NO);
            }
            else if (currentMode.equalsIgnoreCase("number")) {
                rankShowMode.add(WhoisRankShowMode.NUMBER);
            }
        }

        for (int i = 0; i < rankNum; i++) {
            currentMode = config.getProperty("league.rank.promoteMode." + i);
            if (currentMode.equalsIgnoreCase("invite")) {
                rankPromoteMode.add(RankPromoteMode.INVITE);
            }
            else if (currentMode.equalsIgnoreCase("default")) {
                rankPromoteMode.add(RankPromoteMode.DEFAULT);
            }
            else if (currentMode.equalsIgnoreCase("promote")) {
                rankPromoteMode.add(RankPromoteMode.PROMOTE);
            }
        }

        String defaultAccessLimit = config.getProperty("league.command.defaultAccessLimit");
        String[] commandList = config.getProperty("league.command.list").split("\\s");
        for (String c : commandList) {
            commands.put(c, new CommandData(c,
              Integer.parseInt(config.getProperty("league.command.access." + c)),
              Integer.parseInt(config.getProperty("league.command.access." + c + ".top", defaultAccessLimit))));
        }

        expRankSpread = Integer.parseInt(config.getProperty("league.exprank.spread"));
        expRankBottom = Integer.parseInt(config.getProperty("league.exprank.bottom"));
        expRankNum = Integer.parseInt(config.getProperty("league.exprank.num"));
        expRankTop = expRankBottom + expRankNum * expRankSpread;

        for (int i = 0; i < expRankNum; i++) {
            exprank.put(expRankBottom + i * expRankSpread, config.getProperty("league.exprank.string." + i));
        }

        playerNum = Integer.parseInt(config.getProperty("league.numberOfPlayers"));

        int baseKFactorNum = Integer.parseInt(config.getProperty("league.baseKFactorNum"));
        baseKFactorGameNum = new int[baseKFactorNum];
        baseKFactor = new int[baseKFactorNum + 1];

        for (int i = 0; i < baseKFactorNum; i++) {
            baseKFactorGameNum[i] = Integer.parseInt(config.getProperty("league.baseKFactorGameNum." + i));
            baseKFactor[i] = Integer.parseInt(config.getProperty("league.baseKFactor." + i));
        }
        baseKFactor[baseKFactorNum] = Integer.parseInt(config.getProperty("league.baseKFactor." + baseKFactorNum));

        int streakModifiers = Integer.parseInt(config.getProperty("league.streakModifier"));
        streakModCount = new int[streakModifiers];
        streakModifier = new double[streakModifiers];
        for (int i = 0; i < streakModifiers; i++) {
            String tmp[] = config.getProperty("league.streakModifier." + i).split("\\s");
            streakModCount[i] = Integer.parseInt(tmp[0]);
            streakModifier[i] = Double.parseDouble(tmp[1]);
        }

        int totalStreakRankNum = Integer.parseInt(config.getProperty("league.streakrank.num"));
        streakRankNum = new int[totalStreakRankNum];
        streakRankString = new String[totalStreakRankNum];
        for (int i = 0; i < totalStreakRankNum; i++) {
            String tmp[] = config.getProperty("league.streakrank.string." + i).split("\\s", 2);
            streakRankNum[i] = Integer.parseInt(tmp[0]);
            streakRankString[i] = tmp[1];
        }

        String[] gameCommandList = config.getProperty("league.gameTypes.commands").split("\\s");
        for (String c : gameCommandList) {
            commandGameTypeList.put(c, config.getProperty("league.gameTypes." + c));
            for (String s : config.getProperty("league.gameTypes." + c).split("\\s")) {
                switchToGameType.put(config.getProperty("league.gameTypes." + c + "." + s), s);
            }
        }

        String[] mapping;
        for (String c : commandList) {
            if (config.getProperty("league.command.mapping." + c) != null) {
                mapping = config.getProperty("league.command.mapping." + c).split("\\s");
                for (String m : mapping) {
                    commandMapping.put(m, c);
                }
            }
        }

        for (String c : commandList) {
            fullCommandList.add(c);
        }

        for (String m : commandMapping.keySet()) {
            fullCommandList.add(m);
        }
    }

    public Properties getConfig() {
        return config;
    }

    public String getProperty(String prop) {
        return config.getProperty(prop);
    }

    public String getProperty(String prop, String def) {
        return config.getProperty(prop, def);
    }

    public int getRankNum() {
        return rankNum;
    }

    public List<String> getAccessRanks() {
        return accessRanks;
    }

    public String getRankStringByAccess(int access) {
        return accessRanks.get(getRankNumByAccess(access));
    }

    public String getRankStringByNum(int rank) {
        return accessRanks.get(rank);
    }

    public int getRankNumByAccess(int access) {
        int i;
        for (i = 0; i < rankNum; ++i) {
            if (accessLevels.get(i) >= access) {
                break;
            }
        }
        return i;
    }

    public int getRankAccessByString(String rank) {
        int i;
        for (i = 0; i < rankNum; ++i) {
            if (accessRanks.get(i).equals(rank)) {
                break;
            }
        }
        return accessLevels.get(i);
    }

    public int getRankAccessByNum(int rank) {
        if (rank >= rankNum) {
            return 10000;
        }
        return accessLevels.get(rank);
    }

    public WhoisRankShowMode getRankShowMode(int rankNum) {
        return rankShowMode.get(rankNum);
    }

    public int getRankPromoteLimit(int myRank, int rank) {
        return promoteLimits.get(myRank).get(rank);
    }

    public int getRankPromoteLimitByAccess(int myAccess, int access) {
        return promoteLimits.get(getRankNumByAccess(myAccess)).get(getRankNumByAccess(access));
    }

    public RankPromoteMode getRankPromoteMode(int rankNum) {
        return rankPromoteMode.get(rankNum);
    }

    public int getCommandAccess(String command) {
        return commands.get(command).getAccess();
    }

    public int getCommandAccessLimit(String command) {
        return commands.get(command).getAccessLimit();
    }

    public Set<String> getCommands() {
        return commands.keySet();
    }

    public Set<String> getFullCommandList() {
        return fullCommandList;
    }

    public String getExpRank(int exp) {
        exp -= (exp % getExpRankSpread());

        if (exp < expRankBottom) {
            return exprank.get(expRankBottom);
        }
        if (exp > expRankTop) {
            return exprank.get(expRankTop);
        }
        return exprank.get(exp);
    }

    public int getExpRankSpread() {
        return expRankSpread;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public int getPlayerNumTeam() {
        return playerNum / 2;
    }

    public String getMapVersion() {
        return config.getProperty("league.mapVersion");
    }

    public int getEloGapModifier() {
        return Integer.parseInt(config.getProperty("league.eloGapModifier"));
    }

    public int getCommonGamesGapModifier() {
        return Integer.parseInt(config.getProperty("league.commonGamesGapModifier"));
    }

    public int getTeamCountModifier() {
        return Integer.parseInt(config.getProperty("league.teamCountModifier"));
    }

    public int getDefaultTeamFactor() {
        return Integer.parseInt(config.getProperty("league.defaultTeamFactor"));
    }

    public double getStreakCasebonusModifier() {
        return Double.parseDouble(config.getProperty("league.streakCasebonusModifier"));
    }

    public int getBaseKFactor(int games) {
        int i;
        for (i = baseKFactorGameNum.length - 1; i >= 0; i--) {
            if (baseKFactorGameNum[i] < games) {
                break;
            }
        }
        return baseKFactor[++i];
    }

    public double getStreakModifier(int streak) {
        for (int i = 0; i < streakModCount.length; i++) {
            if (Math.abs(streak) >= streakModCount[i]) {
                return streakModifier[i];
            }
        }
        return 1;
    }

    public String getStreakString(int streak) {
        for (int i = 0; i < streakRankNum.length; i++) {
            if (streak >= streakRankNum[i]) {
                return (!streakRankString[i].equals("$null")) ? streakRankString[i] : null;
            }
        }
        return null;
    }

    public int getRecentGamesLimit() {
        return Integer.parseInt(config.getProperty("league.recentGamesLimit"));
    }

    public String getGameTypeFromSwitch(String sw) {
        return switchToGameType.get(sw);
    }

    public boolean isValidMapping(String mapping) {
        return commandMapping.keySet().contains(mapping);
    }

    public boolean hasMapping(String command) {
        return (config.getProperty("league.command.mapping." + command) != null);
    }

    public String getCommandFromMapping(String mapping) {
        return commandMapping.get(mapping);
    }
}
