package com.aragaer.jtt.core;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Clockwork extends IntentService {
	private static final String TAG = "JTT CLOCKWORK";
	public static final String ACTION_JTT_TICK = "com.aragaer.jtt.action.TICK";
	private static final Intent TickAction = new Intent(ACTION_JTT_TICK);
	private static final int INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT;
	private final Hour hour = new Hour(0);

	public Clockwork() {
		super("CLOCKWORK");
	}

	public static class TimeChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(Intent.ACTION_TIME_CHANGED)
					|| action.equals(Intent.ACTION_DATE_CHANGED))
				try {
					schedule(context);
				} catch (IllegalStateException e) {
					Log.i(TAG, "Time change while service is not running, ignore");
				}
		}
	};

	public static void schedule(final Context context) {
		final AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		long now = System.currentTimeMillis();
		ThreeIntervals transitions = TransitionProvider.getSurroundingTransitions(
				context, now);

		final long tickFrequency = (transitions.getCurrentEnd() - transitions.getCurrentStart()) / (Hour.HOURS * Hour.HOUR_PARTS);

		final Intent TickActionInternal = new Intent(context, Clockwork.class);

		am.setRepeating(AlarmManager.RTC, transitions.getCurrentStart(),
				tickFrequency, PendingIntent.getService(context, 0,
						TickActionInternal, INTENT_FLAGS));
	}

	public static void unschedule(final Context context) {
		final AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(PendingIntent.getService(context, 0, new Intent(context,
				Clockwork.class), 0));
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final long now = System.currentTimeMillis();
		ThreeIntervals transitions = TransitionProvider.getSurroundingTransitions(this, now);
		Hour.fromTransitions(transitions, now, hour);

		TickAction.putExtra("hour", hour.num).putExtra("jtt", hour.wrapped);
		sendStickyBroadcast(TickAction);

		stopSelf();
	}
}
