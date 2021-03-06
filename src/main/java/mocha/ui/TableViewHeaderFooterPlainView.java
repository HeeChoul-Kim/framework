package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Size;

public class TableViewHeaderFooterPlainView extends TableViewHeaderFooterView {
	public static final String REUSE_IDENTIFIER = "TableViewHeaderFooterPlainViewIdentifier";

	public TableViewHeaderFooterPlainView(String reuseIdentifier) {
		super(reuseIdentifier);

		Label textLabel = this.getTextLabel();

		textLabel.setFont(Font.getBoldSystemFontWithSize(14.0f));
		textLabel.setTextColor(Color.BLACK);
		textLabel.setBackgroundColor(Color.TRANSPARENT);
		textLabel.setShadowColor(Color.white(0.0f, 0.5f));
		textLabel.setShadowOffset(new Size(0.0f, 1.0f));

		this.setBackgroundColor(Color.GRAY);
	}

}
