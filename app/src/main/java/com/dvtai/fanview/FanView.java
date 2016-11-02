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

	private enum Direction {
		OPEN,
		CLOSE,
		UNSPECIFIED
	}

	private static class Angle {
		float degree;
		float maxSelf;

		boolean isMax() {
			return degree >= maxSelf;
		}

		boolean isMin() {
			return degree == MASTER_ANGLE_MIN;
		}
	}

	private static final String TAG = "FanView";
	private static final String TAG_ANGLE = "TDAngle";

	private static final int ITEM_COUNT = 20;
	private static final float ITEM_ANGLE = 12;
	private static final float MASTER_ANGLE_MAX = 90F;
	private static final float MASTER_ANGLE_MIN = 0F;

	private List<View> mListItem = new ArrayList<>();
	private List<Angle> mListAngle;
	private int mItemWidth = getResources().getDimensionPixelSize(R.dimen.fan_menu_size);
	private int mItemHeight = getResources().getDimensionPixelSize(R.dimen.fan_menu_item_height);

	private LayoutInflater mLayoutInflater;
	private GestureDetectorCompat mGestureDetector;
	private Scroller mFlingScroller;
	private ValueAnimator mFlingAnimator;

	private Direction mCurrentDirection = Direction.UNSPECIFIED;
	private float mLastX;
	private float mLastY;

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
		mLayoutInflater = LayoutInflater.from(getContext());
		mGestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
		mFlingScroller = new Scroller(getContext());
		mFlingAnimator = new ValueAnimator();
		mFlingAnimator.addUpdateListener(flingAnimatorUpdateListener);
		addItems();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}

	private void addItems() {
		removeAllViews();
		prepareItems();
		prepareAngles();
		renderItems();
	}

	private void prepareItems() {
		mListItem = new ArrayList<>();
		for (int i = 0; i < ITEM_COUNT; i++) {
			// create item view, layout param
			ViewGroup itemView = (ViewGroup) mLayoutInflater.inflate(R.layout.item_fan, null);
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
		for (int i = 0; i < ITEM_COUNT; i++) {
			Angle angle = new Angle();
			angle.degree = 0;
			angle.maxSelf = Math.min(MASTER_ANGLE_MAX, i * ITEM_ANGLE);
			mListAngle.add(angle);
		}
	}

	private void renderItems() {
		for (int i = 0; i < mListItem.size(); i++) {
			float degree = mListAngle.get(i).degree;
			mListItem.get(i).setRotation(degree);
		}
	}

	private void resolveAngleForItems(float deltaAngle, Direction direction) {
		int size = mListAngle.size();
		if (direction == Direction.OPEN) {
			for (int i = size - 1; i >= 1; i--) {
				Angle me = mListAngle.get(i);
				Angle prev = (i <= size - 2) ? mListAngle.get(i + 1) : null;

				if (me.isMax()) continue;
				if (prev == null || prev.isMax()) {
					me.degree = Math.min(me.maxSelf, me.degree + deltaAngle);
				} else if (!prev.isMax() && prev.degree > ITEM_ANGLE) {
					me.degree = prev.degree - ITEM_ANGLE;
				} else {
					break;
				}
			}
		} else {
			for (int i = 1; i < size; i++) {
				Angle me = mListAngle.get(i);
				Angle prev = mListAngle.get(i - 1);

				if (me.isMin()) continue;
				if (me.degree < MASTER_ANGLE_MAX) {
					me.degree = Math.max(MASTER_ANGLE_MIN, me.degree + deltaAngle);
				} else if (prev.degree + ITEM_ANGLE < MASTER_ANGLE_MAX) {
					me.degree = prev.degree + ITEM_ANGLE;
				} else {
					break;
				}
			}
		}
	}

	private float calculateAngleDegree(double x, double y) {
		double normalizedX = -(getWidth() - x);
		double normalizedY = getHeight() - y;
		if (normalizedX == 0) {
			return 90F;
		}
		double rad = Math.atan(normalizedY / normalizedX);
		return (float) (rad * (180 / Math.PI));
	}

	private float calculateDeltaRotationArc(float startDegree, float endDegree) {
		float normalizedStartArc = (startDegree < 0) ? (-1 * startDegree) : (180 - startDegree);
		float normalizedEndArc = (endDegree < 0) ? (-1 * endDegree) : (180 - endDegree);
		return normalizedEndArc - normalizedStartArc;
	}


	private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			Log.w(TAG, String.format("GestureDetector.onDown: X:%d Y:%d", (int) e.getX(), (int) e.getY()));
			if (mFlingAnimator.isRunning()) mFlingAnimator.cancel();
			mLastX = e.getX();
			mLastY = e.getY();
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			Log.w(TAG, String.format("GestureDetector.onScroll: (%d/%d) - (%d/%d) - distanceX:%d - distanceY:%d",
					(int) e1.getX(), (int) e1.getY(), (int) e2.getX(), (int) e2.getY(), (int) distanceX, (int) distanceY));
			// calculate angle degree
			float lastDegree = calculateAngleDegree(mLastX, mLastY);
			float nowDegree = calculateAngleDegree(e2.getX(), e2.getY());
			float deltaArc = calculateDeltaRotationArc(lastDegree, nowDegree);
			mCurrentDirection = distanceY > 0 ? Direction.OPEN : Direction.CLOSE;
			mLastX = e2.getX();
			mLastY = e2.getY();
			// resolve items
			resolveAngleForItems(deltaArc, mCurrentDirection);
			renderItems();
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// scroller
			mFlingScroller.fling((int) e1.getX(), (int) e1.getY(), (int) velocityX, (int) velocityY,
					Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
			mLastX = mFlingScroller.getStartX();
			mLastY = mFlingScroller.getStartY();
			// start value animation
			mFlingAnimator.setIntValues(mFlingScroller.getCurrY(), mFlingScroller.getFinalY());
			mFlingAnimator.setDuration(mFlingScroller.getDuration());
			mFlingAnimator.setInterpolator(new TimeInterpolator() {
				@Override
				public float getInterpolation(float input) {
					return (mFlingScroller.timePassed() * 1.0F) / mFlingScroller.getDuration();
				}
			});
			mFlingAnimator.start();
			return true;
		}
	};


	private ValueAnimator.AnimatorUpdateListener flingAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			if (mFlingScroller.computeScrollOffset()) {
				// calculate angle degree
				float lastDegree = calculateAngleDegree(mLastX, mLastY);
				float nowDegree = calculateAngleDegree(mFlingScroller.getCurrX(), mFlingScroller.getCurrY());
				float deltaArc = calculateDeltaRotationArc(lastDegree, nowDegree);
				mLastX = mFlingScroller.getCurrX();
				mLastY = mFlingScroller.getCurrY();
				// resolve items
				//Log.w(TAG, String.format("ValueAnimator.fling: X:%d, Y:%d - Angle:%d", mFlingScroller.getCurrX(), mFlingScroller.getCurrY(), (int) deltaDegree));
				//Log.w(TAG_ANGLE, String.format("scroll: last:%d now:%d delta:%d", (int) last, (int) now, (int) deltaDegree));
				resolveAngleForItems(deltaArc, mCurrentDirection);
				renderItems();
			}
		}
	};
}
