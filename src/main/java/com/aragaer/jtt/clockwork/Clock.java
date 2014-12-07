package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import com.aragaer.jtt.astronomy.DayInterval;

import static com.aragaer.jtt.core.JttTime.TICKS_PER_INTERVAL;


public class Clock {
    private final Astrolabe astrolabe;
    private final Chime chime;
    private final Metronome metronome;

    public Clock(ComponentFactory components) {
        this(components.getAstrolabe(), components.getChime(), components.getMetronome());
    }

    public Clock(Astrolabe astrolabe, Chime chime, Metronome metronome) {
        this.astrolabe = astrolabe;
        this.chime = chime;
        this.metronome = metronome;
        this.metronome.attachTo(this);
    }

    public void adjust() {
        astrolabe.updateLocation();
        rewind();
    }

    private int lastTick;

    private void rewind() {
        DayInterval interval = astrolabe.getCurrentInterval();
		long tickLength = interval.getLength() / TICKS_PER_INTERVAL;
        if (interval.isDay())
            lastTick = TICKS_PER_INTERVAL;
        else
            lastTick = 0;
        metronome.start(interval.getStart(), tickLength);
    }

    public void tick(int ticks) {
        if (TICKS_PER_INTERVAL - ticks < lastTick % TICKS_PER_INTERVAL)
            rewind();
        else
            lastTick += ticks;
        chime.ding(lastTick);
    }
}