package com.aragaer.jtt;
// vim: et ts=4 sts=4 sw=4

import java.util.List;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.*;
import org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm;
import org.robolectric.util.ServiceController;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.net.Uri;

import com.aragaer.jtt.core.TransitionProvider;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class JttServiceTest {

    private TransitionProviderProbe transitionProvider;

    @Before
    public void setup() {
        transitionProvider = new TransitionProviderProbe();
        transitionProvider.onCreate();
        ShadowContentResolver.registerProvider(TransitionProvider.AUTHORITY, transitionProvider);
    }

    @Test
    public void shouldStartTicking() {
        ServiceController<JttService> controller = Robolectric.buildService(JttService.class);
        controller.attach().create().withIntent(new Intent(Robolectric.application, JttService.class)).startCommand(0, 0);

        List<ScheduledAlarm> alarms = getScheduledAlarms();
        assertThat(alarms.size(), equalTo(1));
    }

    @Test
    public void shouldNotUpdateLocationTwice() {
        ServiceController<JttService> controller = Robolectric.buildService(JttService.class);
        controller
            .attach()
            .create()
            .withIntent(new Intent(Robolectric.application, JttService.class))
            .startCommand(0, 0)
            .startCommand(0, 0);
        assertThat(transitionProvider.locationUpdateCount, equalTo(1));
    }

    private List<ScheduledAlarm> getScheduledAlarms() {
        AlarmManager am = (AlarmManager) Robolectric.application
            .getSystemService(Context.ALARM_SERVICE);
        return Robolectric.shadowOf(am).getScheduledAlarms();
    }

    static class TransitionProviderProbe extends TransitionProvider {
        public int locationUpdateCount;

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            locationUpdateCount++;
            return super.update(uri, values, selection, selectionArgs);
        }
    }
}