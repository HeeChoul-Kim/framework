/**
 *  @author Shaun
 *  @date 3/1/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.List;

public class NativeView <V extends android.view.View> extends View {

	private V nativeView;
	boolean trackingTouches;

	// Used internally if we're going to add the native view
	// to the hierarchy somewhere else and NativeView shouldn't
	// touch it's layout
	private boolean unmanagedNativeView;

	public NativeView(V nativeView) {
		if(this.getLayer().getViewGroup() == null) {
			throw new RuntimeException("NativeView currently only works when using ViewLayerNative.");
		}

		this.setNativeView(nativeView);
		this.setUserInteractionEnabled(true);
	}

	public void setUserInteractionEnabled(boolean userInteractionEnabled) {
		super.setUserInteractionEnabled(userInteractionEnabled);

		if(this.nativeView != null) {
			this.nativeView.setEnabled(userInteractionEnabled);
			this.nativeView.setClickable(userInteractionEnabled);
			this.nativeView.setLongClickable(userInteractionEnabled);
		}
	}

	public void setNativeView(V nativeView) {
		if(this.nativeView != null) {
			this.getLayer().getViewGroup().removeView(this.nativeView);
		}

		this.nativeView = nativeView;

		if(this.nativeView != null && !this.unmanagedNativeView) {
			this.nativeView.setOnTouchListener(new android.view.View.OnTouchListener() {
				public boolean onTouch(android.view.View view, MotionEvent motionEvent) {
					if(getWindow().canDeliverToNativeView(NativeView.this, motionEvent, view)) {
						view.onTouchEvent(motionEvent);
					}

					return true;
				}
			});

			this.getLayer().getViewGroup().addView(this.nativeView);
		}
	}

	boolean isUnmanagedNativeView() {
		return unmanagedNativeView;
	}

	void setUnmanagedNativeView(boolean unmanagedNativeView) {
		this.unmanagedNativeView = unmanagedNativeView;

		if(this.unmanagedNativeView) {
			if(this.nativeView != null) {
				this.nativeView.setOnClickListener(null);
				((ViewGroup)this.nativeView.getParent()).removeView(this.nativeView);
			}
		} else {
			// TODO
		}
	}

	public V getNativeView() {
		return nativeView;
	}

	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);

		if(this.nativeView != null) {
			this.nativeView.setBackgroundColor(backgroundColor);
		}
	}

	public void layoutSubviews() {
		super.layoutSubviews();
		this.updateNativeViewFrame();
	}

	public void setHidden(boolean hidden) {
		super.setHidden(hidden);
	}

	private void updateNativeViewFrame() {
		if(this.nativeView == null || this.unmanagedNativeView) return;

		android.graphics.Rect frame = this.getFrame().toSystemRect(this.scale);
		int width = frame.width();
		int height = frame.height();

		if(this.getLayer().getViewGroup() instanceof ViewLayerNative) {
			this.nativeView.setMinimumWidth(width);
			this.nativeView.setMinimumHeight(height);
			this.nativeView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
			this.nativeView.measure(
					android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
					android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
			);
			this.nativeView.forceLayout();
			this.nativeView.layout(0, 0, width, height);
		} else {
			this.nativeView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
		}
	}

	public void touchesBegan(List<Touch> touches, Event event) {
		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent())) {
			super.touchesBegan(touches, event);
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent()))  {
			super.touchesMoved(touches, event);
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {

		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent()))  {
			super.touchesEnded(touches, event);
		}
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent()))  {
			super.touchesEnded(touches, event);
		}
	}

}