package mocha.ui;

import mocha.graphics.Rect;

import java.lang.reflect.Method;
import java.util.*;

public class Control extends View {

	public enum ControlEvent {
		TOUCH_DOWN,
		TOUCH_DOWN_REPEAT,
		TOUCH_DRAG_INSIDE,
		TOUCH_DRAG_OUTSIDE,
		TOUCH_DRAG_ENTER,
		TOUCH_DRAG_EXIT,
		TOUCH_UP_INSIDE,
		TOUCH_UP_OUTSIDE,
		TOUCH_CANCEL,

		VALUE_CHANGED,

		EDITING_DID_BEGIN,
		EDITING_CHANGED,
		EDITING_DID_END,
		EDITING_DID_END_ON_EXIT,

		ALL_TOUCH_EVENTS,
		ALL_EDITING_EVENTS,
		APPLICATION_RESERVED,
		SYSTEM_RESERVED,
		ALL_EVENTS
	}

	public enum VerticalAlignment {
		CENTER,
		TOP,
		BOTTOM,
		FILL
	}

	public enum HorizontalAlignment {
		CENTER,
		LEFT,
		RIGHT,
		FILL
	}

	public enum State {
		NORMAL,
		HIGHLIGHTED,
		DISABLED,
		SELECTED,
		APPLICATION,
		RESERVED;

		public static EnumSet<State> toSet(State[] states) {
			if (states != null && states.length == 1) {
				return EnumSet.of(states[0]);
			} else if (states != null && states.length > 0) {
				return EnumSet.of(states[0], states);
			} else {
				return EnumSet.noneOf(State.class);
			}
		}
	}

	public interface ActionTarget {
		public void onControlEvent(Control control, ControlEvent controlEvent, Event event);
	}


	public @interface TargetAction {

	}

	private boolean enabled;
	private boolean selected;
	private boolean highlighted;
	private boolean tracking;
	private boolean touchInside;
	private HorizontalAlignment contentHorizontalAlignment;
	private VerticalAlignment contentVerticalAlignment;
	private Map<ActionTarget, EnumSet<ControlEvent>> registeredActions;
	private EnumSet<State> cachedState;
	private State[] cachedStates;
	private boolean shouldPerformHapticFeedbackOnTouchUpInside;
	private boolean shouldPlayClickSoundOnTouchUpInside;

	public Control() {
		super();
	}

	public Control(Rect frame) {
		super(frame);
	}

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.registeredActions = new HashMap<>();
		this.enabled = true;
		this.contentHorizontalAlignment = HorizontalAlignment.CENTER;
		this.contentVerticalAlignment = VerticalAlignment.CENTER;

		this.shouldPerformHapticFeedbackOnTouchUpInside = false;
		this.shouldPlayClickSoundOnTouchUpInside = true;
	}

	public boolean getShouldPerformHapticFeedbackOnTouchUpInside() {
		return shouldPerformHapticFeedbackOnTouchUpInside;
	}

	public void setShouldPerformHapticFeedbackOnTouchUpInside(boolean shouldPerformHapticFeedbackOnTouchUpInside) {
		this.shouldPerformHapticFeedbackOnTouchUpInside = shouldPerformHapticFeedbackOnTouchUpInside;
	}

	public boolean getShouldPlayClickSoundOnTouchUpInside() {
		return shouldPlayClickSoundOnTouchUpInside;
	}

	public void setShouldPlayClickSoundOnTouchUpInside(boolean shouldPlayClickSoundOnTouchUpInside) {
		this.shouldPlayClickSoundOnTouchUpInside = shouldPlayClickSoundOnTouchUpInside;
	}

	public void addTargetAction(Object target, String actionMethodName, ControlEvent... controlEvents) {
		this.addActionTarget(new RuntimeTargetAction(target, actionMethodName, this.getClass()), controlEvents);
	}

	public void addTargetAction(Object target, Method action, ControlEvent... controlEvents) {
		this.addActionTarget(new RuntimeTargetAction(target, action, this.getClass()), controlEvents);
	}

	public void addActionTarget(ActionTarget actionTarget, ControlEvent... controlEvents) {
		EnumSet<ControlEvent> registeredEvents;

		if ((registeredEvents = this.registeredActions.get(actionTarget)) == null) {
			registeredEvents = EnumSet.noneOf(ControlEvent.class);
		}

		Collections.addAll(registeredEvents, controlEvents);

		this.registeredActions.put(actionTarget, registeredEvents);
	}

	public void removeActionTarget(ActionTarget actionTarget, ControlEvent... controlEvents) {
		EnumSet<ControlEvent> registeredEvents = this.registeredActions.get(actionTarget);
		if (registeredEvents == null) return;

		for (ControlEvent controlEvent : controlEvents) {
			registeredEvents.remove(controlEvent);
		}

		if (registeredEvents.size() == 0) {
			this.registeredActions.remove(actionTarget);
		}
	}

	void removeAllActionTargets() {
		this.registeredActions.clear();
	}

	public ControlEvent[] allControlEvents() {
		EnumSet<ControlEvent> registeredEvents = EnumSet.noneOf(ControlEvent.class);

		for (EnumSet<ControlEvent> controlEvents : this.registeredActions.values()) {
			registeredEvents.addAll(controlEvents);
		}

		return registeredEvents.toArray(new ControlEvent[registeredEvents.size()]);
	}

	public void sendActionsForControlEvents(ControlEvent... controlEvents) {
		this.sendActionsForControlEvents(null, controlEvents);
	}

	void sendActionsForControlEvents(Event event, ControlEvent... controlEvents) {
		if (this.registeredActions.size() == 0) return;

		boolean hasSentTouchUpInsideAction = false;

		for (ActionTarget actionTarget : this.registeredActions.keySet()) {
			for (ControlEvent controlEvent : controlEvents) {
				if (this.registeredActions.get(actionTarget).contains(controlEvent)) {

					if (controlEvent == ControlEvent.TOUCH_UP_INSIDE && !hasSentTouchUpInsideAction) {
						// We have to fire this stuff before the action, because if the action
						// removes us from the window, they won't fire.

						if (this.shouldPerformHapticFeedbackOnTouchUpInside) {
							this.performHapticFeedback();
						}

						if (this.shouldPlayClickSoundOnTouchUpInside) {
							this.playClickSound();
						}

						hasSentTouchUpInsideAction = true;
					}

					actionTarget.onControlEvent(this, controlEvent, event);
				}
			}
		}
	}

	protected boolean beginTracking(Touch touch, Event event) {
		return true;
	}

	protected boolean continueTracking(Touch touch, Event event) {
		return true;
	}

	protected void endTracking(Touch touch, Event event) {

	}

	protected void cancelTracking(Event event) {

	}

	public void touchesBegan(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);
		this.touchInside = tracking;
		this.tracking = this.beginTracking(touch, event);
		this.setHighlighted(true);

		if (this.tracking) {

			ControlEvent[] controlEvents;

			if (touch.getTapCount() > 1) {
				controlEvents = new ControlEvent[]{ControlEvent.TOUCH_DOWN, ControlEvent.TOUCH_DOWN_REPEAT};
			} else {
				controlEvents = new ControlEvent[]{ControlEvent.TOUCH_DOWN};
			}

			this.sendActionsForControlEvents(event, controlEvents);
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);
		boolean wasTouchInside = this.touchInside;
		this.touchInside = this.pointInside(touch.locationInView(this), event);

		if (this.highlighted != this.touchInside) {
			this.setHighlighted(this.touchInside);
		}

		if (this.tracking) {
			this.tracking = this.continueTracking(touch, event);

			if (this.tracking) {
				ControlEvent dragEvent = this.touchInside ? ControlEvent.TOUCH_DRAG_INSIDE : ControlEvent.TOUCH_DRAG_OUTSIDE;
				ControlEvent[] controlEvents;

				if (!wasTouchInside && this.touchInside) {
					controlEvents = new ControlEvent[]{dragEvent, ControlEvent.TOUCH_DRAG_ENTER};
				} else if (wasTouchInside && !this.touchInside) {
					controlEvents = new ControlEvent[]{dragEvent, ControlEvent.TOUCH_DRAG_EXIT};
				} else {
					controlEvents = new ControlEvent[]{dragEvent};
				}

				this.sendActionsForControlEvents(event, controlEvents);
			}
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);
		this.touchInside = this.pointInside(touch.locationInView(this), event);

		if (this.highlighted) {
			this.setHighlighted(false);
		}

		if (this.tracking) {
			this.endTracking(touch, event);
			this.sendActionsForControlEvents(event, this.touchInside ? ControlEvent.TOUCH_UP_INSIDE : ControlEvent.TOUCH_UP_OUTSIDE);
		}

		this.tracking = false;
		this.touchInside = false;
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		if (this.highlighted) {
			this.setHighlighted(false);
		}

		if (this.tracking) {
			this.cancelTracking(event);
			this.sendActionsForControlEvents(event, ControlEvent.TOUCH_CANCEL);
		}

		this.tracking = false;
		this.touchInside = false;
	}


	public EnumSet<State> getState() {
		if (this.cachedState == null) {
			this.cachedState = EnumSet.of(State.NORMAL);

			if (this.highlighted) {
				this.cachedState.add(State.HIGHLIGHTED);
			}

			if (this.selected) {
				this.cachedState.add(State.SELECTED);
			}

			if (!this.enabled) {
				this.cachedState.add(State.DISABLED);
			}
		}

		return this.cachedState;
	}

	State[] getStates() {
		if (this.cachedStates == null) {
			EnumSet<State> state = this.getState();
			this.cachedStates = state.toArray(new State[state.size()]);
		}

		return this.cachedStates;
	}

	protected void stateWillChange() {

	}

	protected void stateDidChange() {
		this.cachedState = null;
		this.cachedStates = null;

		this.setNeedsDisplay();
		this.setNeedsLayout();
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.stateWillChange();
			this.enabled = enabled;
			this.setUserInteractionEnabled(this.enabled);
			this.stateDidChange();
		}
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.stateWillChange();
			this.selected = selected;
			this.stateDidChange();
		}
	}

	public boolean isHighlighted() {
		return this.highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		if (this.highlighted != highlighted) {
			this.stateWillChange();
			this.highlighted = highlighted;
			this.stateDidChange();
		}
	}

	public boolean isTracking() {
		return tracking;
	}

	public boolean isTouchInside() {
		return touchInside;
	}

	public HorizontalAlignment getContentHorizontalAlignment() {
		return contentHorizontalAlignment;
	}

	public void setContentHorizontalAlignment(HorizontalAlignment contentHorizontalAlignment) {
		this.contentHorizontalAlignment = contentHorizontalAlignment;
	}

	public VerticalAlignment getContentVerticalAlignment() {
		return contentVerticalAlignment;
	}

	public void setContentVerticalAlignment(VerticalAlignment contentVerticalAlignment) {
		this.contentVerticalAlignment = contentVerticalAlignment;
	}

	/**
	 * @param states States to convert to a set
	 *
	 * @return State set
	 *
	 * @deprecated {@link State#toSet(mocha.ui.Control.State[])}
	 */
	static EnumSet<State> getStateSet(State... states) {
		return State.toSet(states);
	}


	static class RuntimeTargetAction extends mocha.ui.RuntimeTargetAction implements ActionTarget {

		RuntimeTargetAction(Object target, String actionMethodName, Class controlClass) {
			super(target, actionMethodName, Event.class, controlClass);
		}

		RuntimeTargetAction(Object target, Method action, Class controlClass) {
			super(target, action, Event.class, controlClass);
		}

		public void onControlEvent(Control control, ControlEvent controlEvent, Event event) {
			this.invoke(event, control);
		}
	}

}
