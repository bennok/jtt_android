package com.aragaer.jtt;

import static com.aragaer.jtt.TickBroadcast.ACTION_JTT_TICK;
import com.aragaer.jtt.core.*;
import com.aragaer.jtt.location.Location;
import com.aragaer.jtt.resources.RuntimeResources;
import com.aragaer.jtt.resources.StringResources;
import com.aragaer.jtt.resources.StringResources.StringResourceChangeListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.widget.RemoteViews;

public class JttStatus extends BroadcastReceiver implements StringResourceChangeListener {
	private static final int APP_ID = 0;

	private final Context context;
	private final StringResources sr;
	private final Hour h = new Hour(0);
	private long start, end;
	private final NotificationManager nm;

	public JttStatus(final Context ctx) {
		context = ctx;
		nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		sr = RuntimeResources.get(context).getInstance(StringResources.class);
		sr.registerStringResourceChangeListener(this,
			StringResources.TYPE_HOUR_NAME | StringResources.TYPE_TIME_FORMAT);

		context.registerReceiver(this, new IntentFilter(ACTION_JTT_TICK));
	}

	public void release() {
		nm.cancel(APP_ID);
		sr.unregisterStringResourceChangeListener(this);
		context.unregisterReceiver(this);
	}

	private void updateLocation(Context context) {
		Location location = Settings.getLocation(context);
		ContentValues locationCV = new ContentValues(2);
		locationCV.put("lat", location.getLatitude());
		locationCV.put("lon", location.getLongitude());
		context.getContentResolver().update(TransitionProvider.LOCATION, locationCV, null, null);
	}

	@Override
	public void onReceive(Context ctx, Intent intent) {
		final String action = intent.getAction();
		if (!action.equals(ACTION_JTT_TICK))
			return;

		updateLocation(ctx);

		final int wrapped = intent.getIntExtra("jtt", 0);
		Hour.fromWrapped(wrapped, h);

		ThreeIntervals transitions = TransitionProvider.getSurroundingTransitions(ctx, System.currentTimeMillis());
		final int lower = Hour.lowerBoundary(h.num),
			upper = Hour.upperBoundary(h.num);
		start = Hour.getHourBoundary(transitions.getCurrentStart(), transitions.getCurrentEnd(), lower);
		end = Hour.getHourBoundary(transitions.getCurrentStart(), transitions.getCurrentEnd(), upper);
		if (end < start) {// Cock or Hare
			if (h.quarter >= 2) // we've passed the transition
				start = Hour.getHourBoundary(transitions.getPreviousStart(), transitions.getCurrentStart(), lower);
			else
				end = Hour.getHourBoundary(transitions.getCurrentEnd(), transitions.getNextEnd(), upper);
		}

		show();
	}

	private void show() {
		final int hf = h.quarter * JttTime.TICKS_PER_QUARTER + h.quarter_parts;
		final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.notification);

		rv.setTextViewText(R.id.image, Hour.Glyphs[h.num]);
		rv.setTextViewText(R.id.title, sr.getHrOf(h.num));
		rv.setTextViewText(R.id.quarter, sr.getQuarter(h.quarter));
		rv.setProgressBar(R.id.fraction, JttTime.TICKS_PER_HOUR, hf, false);
		rv.setTextViewText(R.id.start, sr.format_time(start));
		rv.setTextViewText(R.id.end, sr.format_time(end));

		final Notification n = new Notification.Builder(context)
			.setContent(rv)
			.setOngoing(true)
			.setSmallIcon(R.drawable.notification_icon, h.num)
			.setContentIntent(PendingIntent.getActivity(
					context, 0, new Intent(context, MainActivity.class), 0))
			.getNotification();

		nm.notify(APP_ID, n);
	}

	public void onStringResourcesChanged(final int changes) {
		show();
	}
}
