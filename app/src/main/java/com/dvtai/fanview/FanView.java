package com.dvtai.fanview;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taidangvan on 10/31/16.
 */

public class FanView extends FrameLayout {

	private static final String TAG = "FanView";

	private static final int MAX_ITEM = 10;
	private static final float MAX_ANGLE_ITEM = 25;
	private static final float MAX_ANGLE = 90F;
	private static final float MIN_ANGLE = 0F;

	enum Direction {
		OPEN,
		CLOSE
	}

	private List<View> mListItem = new ArrayList<>();
	private List<Angle> mListAngle;
	private int mItemWidth = getResources().getDimensionPixelSize(R.dimen.fan_menu_size);
	private int mItemHeight = getResources().getDimensionPixelSize(R.dimen.fan_menu_item_height);

	private LayoutInflater layoutInflater;
	private GestureDetectorCompat gestureDetector;

	public FanView(Context context) {
		this(context, null);
	}

	public FanView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FanView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		layoutInflater = LayoutInflater.from(getContext());
		gestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
		addItems();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	private void addItems() {
		removeAllViews();
		prepareItems();
		prepareAngles();
		render();
	}

	private void prepareItems() {
		mListItem = new ArrayList<>();
		for (int i = 0; i < MAX_ITEM; i++) {
			// create item view, layout param
			ViewGroup itemView = (ViewGroup) layoutInflater.inflate(R.layout.item_fan, null);
			LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
			lp.width = mItemWidth;
			lp.height = mItemHeight;
			itemView.setLayoutParams(lp);
			// set pivot
			itemView.setPivotX(mItemWidth - (float) mItemHeight / 2);
			itemView.setPivotY((float) mItemHeight / 2);
			mListItem.add(itemView);
			// set some data
			TextView tv = (TextView) itemView.findViewById(R.id.tv_name);
			tv.setText((i + 1) + " - HOLLY FUCKING SHIT");

			addView(itemView, 0);
		}
	}

	private void prepareAngles() {
		mListAngle = new ArrayList<>();
		for (int i = 0; i < MAX_ITEM; i++) {
			Angle angle = new Angle();
			angle.value = 0;
			angle.maxSelf = Math.min(MAX_ANGLE, i * MAX_ANGLE_ITEM);
			mListAngle.add(angle);
		}
	}

	private void render() {
		for (int i = 0; i < mListItem.size(); i++) {
			mListItem.get(i).setRotation(mListAngle.get(i).value);
		}
	}

	private void resolveAngleForItems(float deltaAngle, Direction direction) {
		//Log.d(TAG, "Delta:" + deltaAngle + " Direction:" + ((direction == Direction.OPEN) ? "Open" : "Close"));

		int size = mListAngle.size();
		if (direction == Direction.OPEN) {
			Log.w(TAG, "Direction.OPEN-----------------------------");
			for (int i = size - 1; i >= 1; i--) {
				Angle a = mListAngle.get(i);
				if (a.isMax()) continue; // no need to resolve angle for this item

				a.value = Math.min(a.maxSelf, a.value + deltaAngle);
				if (!a.isMax() && i + 1 < size) {
					if (mListAngle.get(i + 1).isMax()) {
						a.value = Math.min(a.maxSelf, a.value + deltaAngle);
					} else {
						a.value = Math.max(MIN_ANGLE, mListAngle.get(i + 1).value - MAX_ANGLE_ITEM);
					}
				}
				Log.w(TAG, String.format("Direction.OPEN: i=%d value=%f", i, a.value));
			}
		} else {
			Log.w(TAG, "Direction.CLOSE-----------------------------");
			for (int i = 1; i < size; i++) {
				Angle a = mListAngle.get(i);
				if (a.isMin()) continue;

				a.value = Math.max(MIN_ANGLE, a.value + deltaAngle);
				if (MAX_ANGLE - a.value <= MAX_ANGLE_ITEM) {
					break;
				}

				Log.w(TAG, String.format("Direction.CLOSE: i=%d value=%f", i, a.value));
			}
		}

		render();
	}

	private GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {


		private float lastX;
		private float lastY;

		@Override
		public boolean onDown(MotionEvent e) {
			lastX = e.getX();
			lastY = e.getY();
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			Log.d(TAG, String.format("onScroll: (%f/%f) - (%f/%f) - distanceX:%f - distanceY:%f",
					e1.getX(), e1.getY(), e2.getX(), e2.getY(), distanceX, distanceY));
			float deltaAngle = calculateAngle(lastX, lastY) - calculateAngle(e2.getX(), e2.getY());

			Direction direction = distanceY > 0 ? Direction.OPEN : Direction.CLOSE;
			resolveAngleForItems(deltaAngle * -1, direction);

			lastX = e2.getX();
			lastY = e2.getY();
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {

		}

		private float calculateAngle(double x, double y) {
			double revertX = getWidth() - x;
			double revertY = getHeight() - y;
			double rad = Math.atan(revertY / revertX);
			float angle = (float) (rad * (180 / Math.PI));
			Log.d(TAG, "calculateAngle: Rx=" + revertX + " Ry=" + revertY + " angle=" + angle);
			return angle;
		}
	};

	private static class Angle {
		float value;
		float maxSelf;

		boolean isMax() {
			return value >= maxSelf;
		}

		boolean isMin() {
			return value == MIN_ANGLE;
		}
	}
}
