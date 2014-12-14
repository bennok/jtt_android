package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import javax.inject.Inject;

import static com.aragaer.jtt.core.JttTime.TICKS_PER_INTERVAL;


public class Cogs {

    private Astrolabe astrolabe;
    @Inject Chime chime;
    private int teethPassed;

    public void attachChime(Chime newChime) {
        chime = newChime;
    }

    public void rotate(int teeth) {
        if (TICKS_PER_INTERVAL - teeth < teethPassed % TICKS_PER_INTERVAL)
            astrolabe.onIntervalEnded();
        else
            teethPassed += teeth;
        chime.ding(teethPassed);
    }

    public void setLastTick(int tick) {
        teethPassed = tick;
    }

    public void bindToAstrolabe(Astrolabe astrolabe) {
        this.astrolabe = astrolabe;
    }

    public void switchToNightGear() {
        teethPassed = 0;
    }

    public void switchToDayGear() {
        teethPassed = TICKS_PER_INTERVAL;
    }

}
