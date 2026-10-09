package com.cornspies.fusionfactions.missile;

// A block entity that an EMP missile shuts down (DESIGN.md §6.6): it loses its FE and stops working until the
// given game time.
public interface EmpTarget {
    void empHit(long disabledUntil);
}
