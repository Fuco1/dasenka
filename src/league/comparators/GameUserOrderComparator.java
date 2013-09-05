/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package league.comparators;

import java.util.Comparator;
import league.entities.GameUserOrder;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class GameUserOrderComparator implements Comparator<GameUserOrder> {
    public int compare(GameUserOrder o1, GameUserOrder o2) {
        return o1.getListOrder() - o2.getListOrder();
    }
}
