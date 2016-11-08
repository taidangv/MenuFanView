package com.dvtai.fanview;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taidangvan on 10/31/16.
 */

public class FanView extends FrameLayout {

	private float mLastXIntercept;
	private float mLastYIntercept;

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
	private static final String TAG_DEV = "Dev";

	private static final float ITEM_ANGLE = 12;

	// currently, support 0 <= angle <= 180
	private static final float MASTER_ANGLE_MAX = 180F;
	private static final float MASTER_ANGLE_MIN = 0F;

	private List<View> mListItem = new ArrayList<>();
	private List<Angle> mListAngle;
	private int mItemWidth = getResources().getDimensionPixelSize(R.dimen.fan_menu_size);
	private int mItemHeight = getResources().getDimensionPixelSize(R.dimen.fan_menu_item_height);

	private GestureDetectorCompat mGestureDetector;
	private Scroller mFlingScroller;
	private ValueAnimator mFlingAnimator;
	private Adapter mAdapter;

	private float mLastX;
	private float mLastY;

	private boolean mIsScrolling;

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
		mGestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
		mFlingScroller = new Scroller(getContext());
		mFlingAnimator = new ValueAnimator();
		mFlingAnimator.addUpdateListener(flingAnimatorUpdateListener);
		addItems();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsScrolling = false;
				break;
			case MotionEvent.ACTION_DOWN:
				if (mFlingAnimator.isRunning()) mFlingAnimator.cancel();
				mLastXIntercept = ev.getX();
				mLastYIntercept = ev.getY();
				mLastX = ev.getX();
				mLastY = ev.getY();
				mIsScrolling = false;
				break;
			case MotionEvent.ACTION_MOVE:
				if (mIsScrolling) {
					return true;
				}

				float xDiff = (ev.getX() - mLastXIntercept);
				float yDiff = (ev.getY() - mLastYIntercept);

				int diff = (int) Math.sqrt(xDiff * xDiff + yDiff * yDiff);

				if (diff > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
					mIsScrolling = true;
					return true;
				}
				break;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
	}

	private void addItems() {
		removeAllViews();
		setAdapter(null);
	}

	public void setAdapter(Adapter adapter) {
		mAdapter = adapter;
		populateItems();
		prepareAngles();
		renderItems();
	}

	public void populateItems() {
		removeAllViews();
		if (mAdapter == null) return;

		int count = mAdapter.getCount();
		for (int i = 0; i < count; i++) {
			View itemView = mAdapter.getView(i, null, this);
			LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
			lp.width = mItemWidth;
			lp.height = mItemHeight;
			itemView.setLayoutParams(lp);

			// set pivot
			itemView.setPivotX(mItemWidth - (float) mItemHeight / 2);
			itemView.setPivotY((float) mItemHeight / 2);

			mListItem.add(itemView);
			addView(itemView, 0);
		}
	}

	private void prepareAngles() {
		mListAngle = new ArrayList<>();
		if (mAdapter == null) return;
		for (int i = 0; i < mAdapter.getCount(); i++) {
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

	private void resolveAngleForItems(float deltaDegree) {
		int size = mListAngle.size();
		boolean isOpening = deltaDegree > 0;
		if (isOpening) {
			for (int i = size - 1; i >= 1; i--) {
				Angle me = mListAngle.get(i);
				Angle prev = (i <= size - 2) ? mListAngle.get(i + 1) : null;

				if (me.isMax()) continue;
				if (prev == null || prev.isMax()) {
					me.degree = Math.min(me.maxSelf, me.degree + deltaDegree);
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
					me.degree = Math.max(MASTER_ANGLE_MIN, me.degree + deltaDegree);
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
		float degree = (float) (rad * (180 / Math.PI));

		float degreeIn360;
		if ((normalizedX < 0f && normalizedY < 0f)
				|| (normalizedX < 0f && normalizedY > 0f)) {
			degreeIn360 = 180 + degree;
		} else {
			degreeIn360 = degree;
		}
		return degreeIn360;
	}

	private float calculateDeltaRotation(float startDegree, float endDegree) {
		float normalizedStartArc = 180 - startDegree;
		float normalizedEndArc = 180 - endDegree;
		return normalizedEndArc - normalizedStartArc;
	}


	private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			performClick();
			return true;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			if (mFlingAnimator.isRunning()) mFlingAnimator.cancel();
			mLastX = e.getX();
			mLastY = e.getY();
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			// calculate angle degree
			float lastDegree = calculateAngleDegree(mLastX, mLastY);
			float nowDegree = calculateAngleDegree(e2.getX(), e2.getY());
			float deltaArc = calculateDeltaRotation(lastDegree, nowDegree);
			mLastX = e2.getX();
			mLastY = e2.getY();
			// resolve items
			resolveAngleForItems(deltaArc);
			renderItems();
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// scroller
			mFlingScroller.fling((int) mLastXIntercept, (int) mLastYIntercept, (int) velocityX, (int) velocityY,
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
				if (mFlingScroller.getCurrX() > getWidth() && mFlingScroller.getCurrY() > getHeight()) {
					mFlingAnimator.cancel();
					return;
				}
				// calculate angle degree
				float lastDegree = calculateAngleDegree(mLastX, mLastY);
				float nowDegree = calculateAngleDegree(mFlingScroller.getCurrX(), mFlingScroller.getCurrY());
				float deltaArc = calculateDeltaRotation(lastDegree, nowDegree);
				mLastX = mFlingScroller.getCurrX();
				mLastY = mFlingScroller.getCurrY();
				// resolve items
				resolveAngleForItems(deltaArc);
				renderItems();
			}
		}
	};

	private static final int TOGGLE_MENU_ANIMATION_DURATION = 150;

	public Animator createOpenMenuAnimator() {
		ValueAnimator anim = ValueAnimator.ofFloat(MASTER_ANGLE_MIN, MASTER_ANGLE_MAX);
		anim.setDuration(TOGGLE_MENU_ANIMATION_DURATION);
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			private float last = -1;

			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float now = (float) valueAnimator.getAnimatedValue();
				if (last > -1) {
					resolveAngleForItems(now - last);
					renderItems();
				}
				last = now;
			}
		});
		return anim;
	}

	public Animator createCloseMenuAnimator() {
		float maxCurrentRotation = MASTER_ANGLE_MAX;
		for (Angle angle : mListAngle) {
			if (angle.degree >= MASTER_ANGLE_MAX) maxCurrentRotation += ITEM_ANGLE;
		}

		ValueAnimator anim = ValueAnimator.ofFloat(maxCurrentRotation, MASTER_ANGLE_MIN);
		anim.setDuration(TOGGLE_MENU_ANIMATION_DURATION);
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			private float last = -1;

			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				float now = (float) valueAnimator.getAnimatedValue();
				if (last > -1) {
					resolveAngleForItems(now - last);
					renderItems();
				}
				last = now;
			}
		});
		return anim;
	}
}