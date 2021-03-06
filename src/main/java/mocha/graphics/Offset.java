package mocha.graphics;

public final class Offset implements mocha.foundation.Copying<Offset> {
	// specify amount to offset a position, positive for right or down, negative for left or up
	public float horizontal;
	public float vertical;

	public static Offset zero() {
		return new Offset();
	}

	public Offset() {
		this(0.0f, 0.0f);
	}

	public Offset(float horizontal, float vertical) {
		this.horizontal = horizontal;
		this.vertical = vertical;
	}

	public Offset(Offset offset) {
		this(offset.horizontal, offset.vertical);
	}


	public void set(Offset offset) {
		if (this != offset) {
			if (offset == null) {
				this.horizontal = 0.0f;
				this.vertical = 0.0f;
			} else {
				this.horizontal = offset.horizontal;
				this.vertical = offset.vertical;
			}
		}
	}

	public boolean equals(Offset offset) {
		return (this == offset) || (this.horizontal == offset.horizontal && this.vertical == offset.vertical);
	}

	public String toString() {
		return String.format("[%s,%s]", ((Float) this.horizontal), ((Float) this.vertical));
	}


	public Size toSize() {
		return new Size(this.horizontal, this.vertical);
	}

	public Offset copy() {
		return new Offset(this.horizontal, this.vertical);
	}

}
