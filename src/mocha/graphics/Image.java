/*
 *  @author Shaun
 *	@date 11/20/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.content.res.Resources;
import android.graphics.*;
import android.util.DisplayMetrics;
import mocha.ui.EdgeInsets;
import mocha.ui.Screen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Image extends mocha.foundation.Object {
	private float scale;
	private Size size;
	private Bitmap bitmap;
	private EdgeInsets capInsets;
	private NinePatch ninePatch;

	public static Image imageNamed(int resourceId) {
		android.content.Context context = Screen.mainScreen().getContext();
		Resources resources = context.getResources();
		return new Image(BitmapFactory.decodeResource(resources, resourceId));
	}

	public Image(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.size = new Size(bitmap.getScaledWidth(DisplayMetrics.DENSITY_MEDIUM), bitmap.getScaledHeight(DisplayMetrics.DENSITY_MEDIUM));
		this.scale = (float)bitmap.getDensity() / (float)DisplayMetrics.DENSITY_MEDIUM;
	}

	/**
	 * Creates and returns a new image object with the specified cap insets.
	 *
	 * You use this method to add cap insets to an image or to change the existing cap
	 * insets of an image. In both cases, you get back a new image and the original
	 * image remains untouched.
	 *
	 * During scaling or resizing of the image, areas covered by a cap are not scaled
	 * or resized. Instead, the pixel area not covered by the cap in each direction is
	 * tiled, left-to-right and top-to-bottom, to resize the image. This technique is
	 * often used to create variable-width buttons, which retain the same rounded
	 * corners but whose center region grows or shrinks as needed. For best
	 * performance, use a tiled area that is a 1x1 pixel area in size.
	 *
	 * @param capInsets The values to use for the cap insets.
	 * @return A new image object with the specified cap insets.
	 */
	public Image resizableImageWithCapInsets(EdgeInsets capInsets) {
		if(capInsets == null) {
			return new Image(this.bitmap);
		} else {
			Image image = new Image(this.bitmap);
			image.capInsets = capInsets.copy();

			int top = (int)(capInsets.top * image.scale);
			int left = (int)(capInsets.left * image.scale);
			int bottom = (int)((image.size.height - capInsets.bottom) * image.scale);
			int right = (int)((image.size.width - capInsets.right) * image.scale);

			if(right - left <= 0) {
				MWarn("Left/Right cap insets are bigger than the source image width, results undefined: %f + %f >= %f (scaled:  %d - %d <= 0)", capInsets.left, capInsets.right, image.size.width, right, left, image.bitmap.getWidth());
			}

			if(bottom - top <= 0) {
				MWarn("Top/Bottom cap insets are bigger than the source image height, results undefined: %f + %f >= %f (scaled:  %d - %d <= 0)", capInsets.top, capInsets.bottom, image.size.height, bottom, top, image.bitmap.getHeight());
			}

			ByteBuffer chunk = getNinePatchChunk(bitmap.getWidth(), bitmap.getHeight(), top, left, bottom, right);
			image.ninePatch = new NinePatch(bitmap, chunk.array(), null);

			return image;
		}
	}

	/**
	 * Creates and returns a new image object with the specified cap values.
	 *
	 * During scaling or resizing of the image, areas covered by a cap are not scaled or
	 * resized. Instead, the 1-pixel wide area not covered by the cap in each direction
	 * is what is scaled or resized. This technique is often used to create variable-width
	 * buttons, which retain the same rounded corners but whose center region grows or
	 * shrinks as needed.
	 *
	 * You use this method to add cap values to an image or to change the existing cap
	 * values of an image. In both cases, you get back a new image and the original
	 * image remains untouched.
	 *
	 * NOTE:
	 *  * right cap is calculated as width - leftCapWidth - 1
	 *  * bottom cap is calculated as height - topCapHeight - 1
	 *
	 * @param leftCapWidth The value to use for the left cap width. Specify 0 if you
	 *                        want the entire image to be horizontally stretchable.
	 * @param topCapHeight The value to use for the top cap width. Specify 0 if you
	 *                        want the entire image to be vertically stretchable.
	 * @return A new image object with the specified cap values.
	 */
	public Image stretchableImage(int leftCapWidth, int topCapHeight) {
		EdgeInsets insets = new EdgeInsets();
		insets.left = leftCapWidth;
		insets.right = this.size.width - leftCapWidth - 1.0f;
		insets.top = topCapHeight;
		insets.bottom = this.size.height - topCapHeight - 1.0f;

		return this.resizableImageWithCapInsets(insets);
	}


	public void recycle() {
		this.bitmap.recycle();
	}

	public Size getSize() {
		return size.copy();
	}

	public float getScale() {
		return scale;
	}

	public EdgeInsets getCapInsets() {
		return this.capInsets != null ? this.capInsets.copy() : null;
	}

	public float getLeftCapWidth() {
		return this.capInsets != null ? this.capInsets.left : 0.0f;
	}

	public float getTopCapHeight() {
		return this.capInsets != null ? this.capInsets.top : 0.0f;
	}

	public void draw(Context context, Rect rect) {
		Canvas canvas = context.getCanvas();

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
		paint.setColor(0xff000000);

		if(this.ninePatch != null) {
			this.ninePatch.draw(canvas, rect.toSystemRect(context.getScale()), paint);
		} else {
			canvas.drawBitmap(bitmap, new android.graphics.Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), rect.toSystemRectF(context.getScale()), paint);
		}
	}

	public void draw(Context context, Point point) {
		this.draw(context, new Rect(point, this.size));
	}

	/**
	 * Create a nine patch chunk for resizable images
	 *
	 * @param width bitmap width
	 * @param height bitmap height
	 * @param startY y point to start stretching from
	 * @param startX x point to start stretching from
	 * @param endY y point to stop stretching at
	 * @param endX x point to stop stretching at
	 * @return byte buffer for nine patch images
	 */
	private static ByteBuffer getNinePatchChunk(int width, int height, int startY, int startX, int endY, int endX) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(84).order(ByteOrder.nativeOrder());

		byteBuffer.put((byte)0x01); // ??
		byteBuffer.put((byte)0x02); // horizontal count
		byteBuffer.put((byte)0x02); // vertical count
		byteBuffer.put((byte)0x09); // color count

		byteBuffer.put(new byte[8]); // skip 8 bytes

		byteBuffer.putInt(startX); // padding left
		byteBuffer.putInt(width - endX); // padding right
		byteBuffer.putInt(startY); // padding top
		byteBuffer.putInt(height - endY); // padding bottom

		byteBuffer.put(new byte[4]); // skip 4 bytes

		byteBuffer.putInt(startX).putInt(endX); // horizontal line
		byteBuffer.putInt(startY).putInt(endY); // vertical line

		// Colors
		for(int i = 0; i < 9; i++) {
			byteBuffer.putInt(1);
		}

		return byteBuffer;
	}

}
