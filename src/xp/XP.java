package xp;

import java.util.ArrayList;
import java.util.List;

public class XP {
    private int currNum;
    private final int maxNum;
    List<Integer> xpList;

    public XP(int currNum) {
        this.currNum = 0;
        maxNum = Integer.MAX_VALUE;
        xpList = new ArrayList<>();
    }

    public int getCurrXP(int xp) { return xpList.get(xp); }
    public void addXP(int xp) {
        if (xp > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        xpList.add(xp);
    }
}
