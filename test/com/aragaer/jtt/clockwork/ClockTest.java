package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import org.junit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.aragaer.jtt.astronomy.DayInterval;
import static com.aragaer.jtt.core.JttTime.TICKS_PER_DAY;
import static com.aragaer.jtt.core.JttTime.TICKS_PER_INTERVAL;


public class ClockTest {

    private Clock clock;
    private ComponentFactory components;
    private TestMetronome metronome;
    private TestAstrolabe astrolabe;
    private TestChime chime;

    @Before
    public void setUp() {
        components = new TestClockFactory();
        metronome = (TestMetronome) components.getMetronome();
        astrolabe = (TestAstrolabe) components.getAstrolabe();
        chime = (TestChime) components.getChime();
        clock = new Clock(components);
    }

    @Test
    public void shouldTriggerEvent() {
        metronome.tick(42);
        assertThat("chime ding number", chime.getLastTick(), equalTo(42));
    }

    @Test
    public void shouldUpdateLocationWhenAdjusted() {
        astrolabe.setNextResult(DayInterval.Day(0, 1));
        assertThat(astrolabe.updateLocationCalls, equalTo(0));
        clock.adjust();
        assertThat(astrolabe.updateLocationCalls, equalTo(1));
    }

    @Test
    public void shouldStartMetronomeBasedOnAstrolabeResult() {
        astrolabe.setNextResult(DayInterval.Day(10, 10 + TICKS_PER_INTERVAL * 5));

        clock.adjust();

        assertThat(metronome.start, equalTo(10L));
        assertThat(metronome.tickLength, equalTo(5L));
    }

    @Test
    public void tick() {
        clock.tick(42);
        assertThat("chime ding number", chime.getLastTick(), equalTo(42));
    }

    @Test
    public void shouldReAdjustWhenIntervalEnds() {
        long dayStart = 10;
        long dayTickLength = 5;
        long dayEnd = dayStart + dayTickLength * TICKS_PER_INTERVAL;
        astrolabe.setNextResult(DayInterval.Day(dayStart, dayEnd));
        clock.adjust();

        metronome.tick(5);

        assertThat("metronome start at sunrise", metronome.start, equalTo(dayStart));
        assertThat("metronome tick length", metronome.tickLength, equalTo(dayTickLength));

        long nightStart = dayEnd;
        long nightTickLength = 2;
        long nightEnd = nightStart + nightTickLength * TICKS_PER_INTERVAL;

        astrolabe.setNextResult(DayInterval.Night(nightStart, nightEnd));

        metronome.tick(TICKS_PER_INTERVAL);

        assertThat(metronome.start, equalTo(nightStart));
        assertThat(metronome.tickLength, equalTo(nightTickLength));
    }

    @Test
    public void shouldDingChimesWhenMetronomeTicks() {
        int tickNumber = 42;
        metronome.tick(tickNumber);

        assertThat("chime number", chime.getLastTick(), equalTo(tickNumber));
    }

    @Test
    public void shouldUseDayTime() {
        int tickNumber = 42;

        astrolabe.setNextResult(DayInterval.Day(0, 1));
        clock.adjust();
        metronome.tick(tickNumber);

        assertThat("chime number", chime.getLastTick(), equalTo(tickNumber + TICKS_PER_INTERVAL));
    }

    @Test
    public void shouldSwitchIntervals() {
        long night1TickLength = 2;
        long day1TickLength = 5;
        long night2TickLength = 3;
        long day2TickLength = 6;
        long sunset1 = 10;
        long sunrise1 = sunset1 + night1TickLength * TICKS_PER_INTERVAL;
        long sunset2 = sunrise1 + day1TickLength * TICKS_PER_INTERVAL;
        long sunrise2 = sunset2 + night2TickLength * TICKS_PER_INTERVAL;
        int lastTick = 0;
        int tickCount;

        astrolabe.setNextResult(DayInterval.Night(sunset1, sunrise1));
        clock.adjust();
        astrolabe.setNextResult(DayInterval.Day(sunrise1, sunset2));

        tickCount = 2;
        lastTick += tickCount;
        metronome.tick(tickCount);
        assertThat("chime number", chime.getLastTick(), equalTo(lastTick));
        assertThat(metronome.tickLength, equalTo(night1TickLength));


        tickCount = 50;
        lastTick += tickCount;
        metronome.tick(tickCount);
        assertThat("chime number", chime.getLastTick(), equalTo(lastTick));

        tickCount = TICKS_PER_INTERVAL-53;
        lastTick += tickCount;
        metronome.tick(tickCount);
        assertThat("chime number", chime.getLastTick(), equalTo(lastTick));
        assertThat(metronome.tickLength, equalTo(night1TickLength));

        tickCount = 1;
        lastTick += tickCount;
        metronome.tick(tickCount + 2);
        assertThat("chime number ignores overrun", chime.getLastTick(), equalTo(lastTick));
        assertThat(metronome.tickLength, equalTo(day1TickLength));

        astrolabe.setNextResult(DayInterval.Night(sunset2, sunrise2));

        tickCount = 20;
        lastTick += tickCount;
        metronome.tick(tickCount);
        assertThat("chime number", chime.getLastTick(), equalTo(lastTick));
        assertThat(metronome.tickLength, equalTo(day1TickLength));

        tickCount = TICKS_PER_INTERVAL-20;
        lastTick += tickCount;
        metronome.tick(tickCount + 10);
        assertThat("chime number ignores overrun", chime.getLastTick(), equalTo(0));
        assertThat(metronome.tickLength, equalTo(night2TickLength));
    }
}
