package com.way.selectcontact.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.QuickContactBadge;

public class CheckableQuickContactBadge extends ImageView implements
		Checkable {
	private boolean mChecked = false;
	private int mCheckMarkBackgroundColor;
	private CheckableFlipDrawable mDrawable;

	public CheckableQuickContactBadge(Context context) {
		super(context);
		init(context);
	}

	public CheckableQuickContactBadge(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CheckableQuickContactBadge(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public CheckableQuickContactBadge(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}

	private void init(Context context) {
		setCheckMarkBackgroundColor(getResources().getColor(
				getResIdFromAttribute(context,
						android.support.v7.appcompat.R.attr.colorPrimary)));
	}

	public static int getResIdFromAttribute(final Context context,
			final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.resourceId;
	}

	public void setCheckMarkBackgroundColor(int color) {
		mCheckMarkBackgroundColor = color;
		if (mDrawable != null) {
			mDrawable.setCheckMarkBackgroundColor(color);
		}
	}

	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		setChecked(checked, true);
	}

	public void setChecked(boolean checked, boolean animate) {
		if (mChecked == checked) {
			return;
		}

		mChecked = checked;
		if (mDrawable != null) {
			applyCheckState(animate);
		}
	}

	@Override
	public void setImageDrawable(Drawable d) {
		if (d != null) {
			if (mDrawable == null) {
				mDrawable = new CheckableFlipDrawable(d, getResources(),
						mCheckMarkBackgroundColor, 150);
				applyCheckState(false);
			} else {
				mDrawable.setFront(d);
			}
			d = mDrawable;
		}
		super.setImageDrawable(d);
	}

	private void applyCheckState(boolean animate) {
		mDrawable.flipTo(!mChecked);
		if (!animate) {
			mDrawable.reset();
		}
	}
}
