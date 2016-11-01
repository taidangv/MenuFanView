package com.dvtai.fanview;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
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
import android.widget.Scroller;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taidangvan on 10/31/16.
 */

public class FanView extends FrameLayout {

	private static final String TAG = "FanView";

	private static final int MAX_ITEM_COUNT = 17;
	private static final float ANGLE_ITEM_MAX = 15;
	private static final float ANGLE_MENU_MAX = 90F;
	private static final float ANGLE_MENU_MIN = 0F;

	enum Direction {
		OPEN,
		CLOSE,
		UNSPECIFIED
	}

	private List<View> mListItem = new ArrayList<>();
	private List<Angle> mListAngle;
	private int mItemWidth = getResources().getDimensionPixelSize(R.dimen.fan_menu_size);
	private int mItemHeight = getResources().getDimensionPixelSize(R.dimen.fan_menu_item_height);

	private LayoutInflater layoutInflater;
	private GestureDetectorCompat gestureDetector;
	private Scroller flingScroller;
	private ValueAnimator flingAnimator;
	private Direction currentDirection = Direction.UNSPECIFIED;

	private float lastX;
	private float lastY;

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
		flingScroller = new Scroller(getContext());
		flingAnimator = new ValueAnimator();
		flingAnimator.addUpdateListener(flingAnimatorUpdateListener);
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
		for (int i = 0; i < MAX_ITEM_COUNT; i++) {
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
		for (int i = 0; i < MAX_ITEM_COUNT; i++) {
			Angle angle = new Angle();
			angle.value = 0;
			angle.maxSelf = Math.min(ANGLE_MENU_MAX, i * ANGLE_ITEM_MAX);
			mListAngle.add(angle);
		}
	}

	private void render() {
		for (int i = 0; i < mListItem.size(); i++) {
			mListItem.get(i).setRotation(mListAngle.get(i).value);
		}
	}

	private void resolveAngleForItems(float deltaAngle, Direction direction) {
		int size = mListAngle.size();
		if (direction == Direction.OPEN) {
			for (int i = size - 1; i >= 1; i--) {
				Angle a = mListAngle.get(i);
				if (a.isMax()) continue; // no need to resolve angle for this item

				a.value = Math.min(a.maxSelf, a.value + deltaAngle);
				if (!a.isMax() && i + 1 < size) {
					if (mListAngle.get(i + 1).isMax()) {
						a.value = Math.min(a.maxSelf, a.value + deltaAngle);
					} else {
						a.value = Math.max(ANGLE_MENU_MIN, mListAngle.get(i + 1).value - ANGLE_ITEM_MAX);
					}
				}
			}
			//Log.w(TAG, "Direction.OPEN deltaAngle=" + deltaAngle + getLog());

		} else {
			for (int i = 1; i < size; i++) {
				Angle a = mListAngle.get(i);
				Angle prev = mListAngle.get(i - 1);
				if (a.isMin()) continue;

				if (a.value < ANGLE_MENU_MAX) {
					a.value = Math.max(ANGLE_MENU_MIN, a.value + deltaAngle);
				} else {
					if (prev.value + ANGLE_ITEM_MAX < ANGLE_MENU_MAX) {
						a.value = prev.value + ANGLE_ITEM_MAX;
					} else {
						break;
					}
				}
			}
			//Log.w(TAG, "Direction.CLOSE deltaAngle=" + deltaAngle + getLog());
		}

		render();
	}

	private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			if (flingAnimator.isRunning()) {
				flingAnimator.cancel();
			}
			lastX = e.getX();
			lastY = e.getY();
			Log.e(TAG, "onDown: x:" + lastX + " y:" + lastY);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			Log.d(TAG, String.format("onScroll: (%d/%d) - (%d/%d) - distanceX:%d - distanceY:%d", (int) e1.getX(), (int) e1.getY(), (int) e2.getX(), (int) e2.getY(), (int) distanceX, (int) distanceY));
			float deltaAngle = calculateAngle(lastX, lastY) - calculateAngle(e2.getX(), e2.getY());

			currentDirection = distanceY > 0 ? Direction.OPEN : Direction.CLOSE;
			resolveAngleForItems(deltaAngle * -1, currentDirection);

			lastX = e2.getX();
			lastY = e2.getY();
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			flingScroller.fling((int) e1.getX(), (int) e1.getY(), (int) velocityX, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
			Log.e(TAG, String.format("X:%d, Y:%d, finalX:%d, finalY:%d, velocity:%f, duration:%d", flingScroller.getCurrX(), flingScroller.getCurrY(), flingScroller.getFinalX(), flingScroller.getFinalY(), flingScroller.getCurrVelocity(), flingScroller.getDuration()));
			flingAnimator.setIntValues(flingScroller.getCurrY(), flingScroller.getFinalY());
			lastX = flingScroller.getStartX();
			lastY = flingScroller.getStartY();
			flingAnimator.setDuration(flingScroller.getDuration());
			flingAnimator.setInterpolator(new TimeInterpolator() {
						@Override
						public float getInterpolation(float input) {
							return (flingScroller.timePassed() * 1.0F) / flingScroller.getDuration();
						}
					});
			flingAnimator.start();
			return true;
		}


	};

	private ValueAnimator.AnimatorUpdateListener flingAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			//Log.e(TAG, "value: " + animation.getAnimatedValue());
			if (flingScroller.computeScrollOffset()) {
				Log.e(TAG, String.format("X:%d, Y:%d", flingScroller.getCurrX(), flingScroller.getCurrY()));
				float deltaAngle = calculateAngle(lastX, lastY) - calculateAngle(flingScroller.getCurrX(), flingScroller.getCurrY());
				resolveAngleForItems(deltaAngle * -2, currentDirection);
				lastX = flingScroller.getCurrX();
				lastY = flingScroller.getCurrY();
			}
		}
	};

	private float calculateAngle(double x, double y) {
		double revertX = getWidth() - x;
		double revertY = getHeight() - y;
		if (revertX == 0) {
			return 90;
		}
		double rad = Math.atan(revertY / revertX);
		float angle = (float) (rad * (180 / Math.PI));
		//Log.d(TAG, "calculateAngle: Rx=" + revertX + " Ry=" + revertY + " angle=" + angle);
		return angle;
	}

	private static class Angle {
		float value;
		float maxSelf;

		boolean isMax() {
			return value >= maxSelf;
		}

		boolean isMin() {
			return value == ANGLE_MENU_MIN;
		}
	}

//	private String getLog() {
//		String str = "";
//		for (int i = 0; i < mListAngle.size(); i++) {
//			str += String.format(" %d-%d", i, (int) mListAngle.get(i).value);
//		}
//		return str;
//	}
}
