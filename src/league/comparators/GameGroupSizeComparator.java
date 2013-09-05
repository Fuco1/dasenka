/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package league.comparators;

import java.util.Comparator;
import league.entities.GameGroup;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class GameGroupSizeComparator implements Comparator<GameGroup> {

    public int compare(GameGroup o1, GameGroup o2) {
        return o1.getGroupMembers().size() - o2.getGroupMembers().size();
    }
}
