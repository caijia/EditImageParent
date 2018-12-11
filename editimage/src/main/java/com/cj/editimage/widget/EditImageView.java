package com.cj.editimage.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

import com.cj.editimage.helper.MoveGestureDetector;
import com.cj.editimage.helper.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditImageView extends AppCompatImageView implements MoveGestureDetector.OnMoveGestureListener {

    private static final int OVAL = 1;
    private static final int LINE = 2;
    private static final int RECT = 3;
    private Matrix imageMatrix = new Matrix();
    private boolean isHandle;
    private int shapeType = LINE;
    private List<Shape> shapeList;
    private Paint paint;
    private Shape shape;
    private MoveGestureDetector gestureDetector;
    private Rect drawableBoundsRect;
    private Rect textBounds = new Rect();
    private Paint textPaint = new Paint();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

    public EditImageView(Context context) {
        this(context, null);
    }

    public EditImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        shapeList = new ArrayList<>();
        setScaleType(ScaleType.MATRIX);
        gestureDetector = new MoveGestureDetector(context, this);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(Util.dpToPx(context, 1));
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * 图片适应裁剪框的宽高比
     */
    private void adjustClipBorderAspect() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        if (isHandle) {
            return;
        }

        isHandle = true;

        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();

        int vWidth = getWidth();
        int vHeight = getHeight();

        float drawableAspect = (float) dWidth / dHeight;
        float scaleWidth, scaleHeight;
        float scale;
        if (vWidth < vHeight * drawableAspect) {
            scaleWidth = vWidth;
            scale = scaleWidth / dWidth;
            scaleHeight = dHeight * scale;

        } else {
            scaleHeight = vHeight;
            scale = scaleHeight / dHeight;
            scaleWidth = dWidth * scale;
        }

        float xOffset = (vWidth - scaleWidth) / 2;
        float yOffset = (vHeight - scaleHeight) / 2;

        imageMatrix.reset();
        imageMatrix.postScale(scale, scale);
        imageMatrix.postTranslate(xOffset, yOffset);
        setImageMatrix(imageMatrix);

        RectF rectF = getDrawableBounds(imageMatrix);
        drawableBoundsRect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }

    @Override
    public void onMoveGestureScroll(MotionEvent downEvent, MotionEvent currentEvent,
                                    int pointerIndex, float dx, float dy, float distanceX,
                                    float distanceY) {
        addShapePoint(currentEvent);
    }

    private void addShapePoint(MotionEvent e) {
        boolean contains = contains(e.getX(), e.getY());
        if (contains) {
            if (shape == null) {
                shape = new Shape();
                shape.shapeType = shapeType;
                shapeList.add(shape);
            }

            if (shape.hasPoint()) {
                shape.addPoint(e.getX(), e.getY());
                if (shapeType == LINE) {
                    shape.path.lineTo(e.getX(), e.getY());
                }

            } else {
                shape.addPoint(e.getX(), e.getY());
                if (shapeType == LINE) {
                    shape.path.moveTo(e.getX(), e.getY());
                }
            }
            invalidate(drawableBoundsRect);
        }
    }

    @Override
    public void onMoveGestureUpOrCancel(MotionEvent e) {
        shape = null;
    }

    @Override
    public void onMoveGestureDoubleTap(MotionEvent event) {
    }

    @Override
    public boolean onMoveGestureBeginTap(MotionEvent e) {
        addShapePoint(e);
        return false;
    }

    private boolean contains(float x, float y) {
        RectF drawableBounds = getDrawableBounds(imageMatrix);
        return drawableBounds.contains(x, y);
    }

    private RectF getDrawableBounds(Matrix matrix) {
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            Log.d("editImageView", "drawable is null");
            return;
        }

        final Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();
        if (originalBitmap == null || originalBitmap.isRecycled()) {
            Log.d("editImageView", "bitmap recycled");
            return;
        }
        super.onDraw(canvas);
        adjustClipBorderAspect();

        if (shapeList == null) {
            return;
        }

        for (Shape s : shapeList) {
            drawShape(canvas, s);
        }
    }

    private void drawShape(Canvas canvas, Shape s) {
        int shapeType = s.shapeType;
        switch (shapeType) {
            case LINE:
                canvas.drawPath(s.path, paint);
                break;

            case OVAL:
                if (s.hasTwoPoint()) {
                    PointF firstPoint = s.getFirstPoint();
                    PointF lastPoint = s.getLastPoint();
                    canvas.drawOval(new RectF(firstPoint.x, firstPoint.y, lastPoint.x, lastPoint.y), paint);
                }
                break;

            case RECT:
                if (s.hasTwoPoint()) {
                    PointF firstPoint = s.getFirstPoint();
                    PointF lastPoint = s.getLastPoint();
                    canvas.drawRect(new RectF(firstPoint.x, firstPoint.y, lastPoint.x, lastPoint.y), paint);
                }
                break;
        }
    }

    public void drawOval() {
        this.shapeType = OVAL;
    }

    public void drawLine() {
        this.shapeType = LINE;
    }

    public void drawRect() {
        this.shapeType = RECT;
    }

    public void cancelPreviousDraw() {
        if (shapeList == null || shapeList.isEmpty()) {
            return;
        }

        shapeList.remove(shapeList.size() - 1);
        invalidate(drawableBoundsRect);
    }

    /**
     * 获取涂鸦后的合成图片
     *
     * @return
     */
    public Bitmap getCustomBitmap() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }

        final Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
        Canvas bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawBitmap(originalBitmap, 0, 0, null);
        if (!originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }

        final float[] matrixValues = new float[9];
        imageMatrix.getValues(matrixValues);
        float tranX = matrixValues[Matrix.MTRANS_X];
        float tranY = matrixValues[Matrix.MTRANS_Y];
        float scale = matrixValues[Matrix.MSCALE_X];

        for (Shape s : shapeList) {
            List<PointF> points = s.points;
            int index = 0;
            Path linePath = null;

            if (s.shapeType == LINE) {
                linePath = new Path();
            }

            for (PointF point : points) {
                point.x = (point.x - tranX) / scale;
                point.y = (point.y - tranY) / scale;

                if (linePath != null) {
                    if (index == 0) {
                        linePath.moveTo(point.x, point.y);
                    } else {
                        linePath.lineTo(point.x, point.y);
                    }
                }
                index++;
            }

            if (s.shapeType == LINE) {
                bitmapCanvas.drawPath(linePath, paint);
            } else {
                drawShape(bitmapCanvas, s);
            }
        }

        String text = getCurrentDate();
        textPaint.setColor(Color.RED);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(spToPx(10));
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        int margin = (int) dpToPx(8);
        bitmapCanvas.drawText(text, drawable.getIntrinsicWidth() - textBounds.width() - margin,
                drawable.getIntrinsicHeight() - margin, textPaint);
        bitmapCanvas.save();
        return bitmap;
    }

    private String getCurrentDate() {
        return dateFormat.format(new Date());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private static class Shape {

        int shapeType;

        List<PointF> points = new ArrayList<>();

        Path path = new Path();

        void addPoint(float x, float y) {
            points.add(new PointF(x, y));
        }

        boolean hasPoint() {
            return !points.isEmpty();
        }

        boolean hasTwoPoint() {
            return points.size() > 1;
        }

        PointF getFirstPoint() {
            return points.get(0);
        }

        PointF getLastPoint() {
            return points.get(points.size() - 1);
        }

    }
}
