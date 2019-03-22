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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.cj.editimage.helper.MoveGestureDetector;
import com.cj.editimage.helper.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditImageView extends AppCompatImageView implements
        MoveGestureDetector.OnMoveGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private static final int OVAL = 1;
    private static final int PATH = 2;
    private static final int RECT = 3;
    private static final int LINE = 4;
    private Matrix imageMatrix = new Matrix();
    private boolean isHandle;
    private int shapeType = PATH;
    private List<Shape> shapeList;
    private Paint paint;
    private Shape shape;
    private MoveGestureDetector gestureDetector;
    private ScaleGestureDetector scaleGesture;
    private Rect textBounds = new Rect();
    private Paint textPaint = new Paint();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private int pointerCount;
    private boolean scaleAndTranslate = true;
    private Matrix invertImageMatrix = new Matrix();

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
        scaleGesture = new ScaleGestureDetector(context, this);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(Util.dpToPx(context, 1));
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        pointerCount = event.getPointerCount();
        scaleGesture.onTouchEvent(event);
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
    }

    private float[] getMapPoint(float x, float y) {
        invertImageMatrix.reset();
        imageMatrix.invert(invertImageMatrix);
        float[] src = {x, y};
        if (invertImageMatrix == null) {
            return src;
        }
        float[] dst = new float[2];
        invertImageMatrix.mapPoints(dst, src);
        return dst;
    }

    private void addShapePoint(MotionEvent e) {
        if (scaleAndTranslate) {
            return;
        }
        float[] invertPoint = getMapPoint(e.getX(), e.getY());
        float invertX = invertPoint[0];
        float invertY = invertPoint[1];
        boolean contains = contains(e.getX(), e.getY());
        if (contains) {
            if (shape == null) {
                shape = new Shape();
                shape.shapeType = shapeType;
                shapeList.add(shape);
            }

            if (shape.hasPoint()) {
                shape.addPoint(invertX, invertY);
                if (shapeType == PATH) {
                    shape.path.lineTo(invertX, invertY);
                }

            } else {
                shape.addPoint(invertX, invertY);
                if (shapeType == PATH) {
                    shape.path.moveTo(invertX, invertY);
                }
            }
            invalidate();
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
            return;
        }

        final Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();
        if (originalBitmap == null || originalBitmap.isRecycled()) {
            return;
        }
        super.onDraw(canvas);
        adjustClipBorderAspect();

        if (shapeList == null) {
            return;
        }

        int save = canvas.save();
        canvas.concat(imageMatrix);
        for (Shape s : shapeList) {
            drawShape(canvas, s);
        }
        canvas.restoreToCount(save);
    }

    /**
     * 是否编辑过
     *
     * @return
     */
    public boolean isEdited() {
        return !shapeList.isEmpty();
    }

    private void drawShape(Canvas canvas, Shape s) {
        int shapeType = s.shapeType;
        switch (shapeType) {
            case PATH:
                canvas.drawPath(s.path, paint);
                break;

            case LINE:
                if (s.hasTwoPoint()) {
                    PointF firstPoint = s.getFirstPoint();
                    PointF lastPoint = s.getLastPoint();
                    canvas.drawLine(firstPoint.x, firstPoint.y, lastPoint.x, lastPoint.y, paint);
                }
                break;

            case OVAL:
            case RECT:
                if (s.hasTwoPoint()) {
                    PointF firstPoint = s.getFirstPoint();
                    PointF lastPoint = s.getLastPoint();
                    float left = 0;
                    float top = 0;
                    float right = 0;
                    float bottom = 0;

                    boolean leftTopToRightBottom = firstPoint.x < lastPoint.x &&
                            firstPoint.y < lastPoint.y;
                    if (leftTopToRightBottom) {
                        left = firstPoint.x;
                        top = firstPoint.y;
                        right = lastPoint.x;
                        bottom = lastPoint.y;
                    }
                    boolean rightBottomToLeftTop = firstPoint.x > lastPoint.x &&
                            firstPoint.y > lastPoint.y;
                    if (rightBottomToLeftTop) {
                        left = lastPoint.x;
                        top = lastPoint.y;
                        right = firstPoint.x;
                        bottom = firstPoint.y;
                    }
                    boolean rightTopToLeftBottom = firstPoint.x > lastPoint.x &&
                            firstPoint.y < lastPoint.y;
                    if (rightTopToLeftBottom) {
                        left = lastPoint.x;
                        top = firstPoint.y;
                        right = firstPoint.x;
                        bottom = lastPoint.y;
                    }
                    boolean leftBottomToRightTop = firstPoint.x < lastPoint.x &&
                            firstPoint.y > lastPoint.y;
                    if (leftBottomToRightTop) {
                        left = firstPoint.x;
                        top = lastPoint.y;
                        right = lastPoint.x;
                        bottom = firstPoint.y;
                    }

                    boolean isRect = shapeType == RECT;
                    if (isRect) {
                        canvas.drawRect(new RectF(left, top, right, bottom), paint);
                    } else {
                        canvas.drawOval(new RectF(left, top, right, bottom), paint);
                    }
                }
                break;
        }
    }

    public void drawOval() {
        scaleAndTranslate = false;
        this.shapeType = OVAL;
    }

    public void drawLinePath() {
        scaleAndTranslate = false;
        this.shapeType = PATH;
    }

    public void drawLine() {
        scaleAndTranslate = false;
        this.shapeType = LINE;
    }

    public void drawRect() {
        scaleAndTranslate = false;
        this.shapeType = RECT;
    }

    public void cancelPreviousDraw() {
        if (shapeList == null || shapeList.isEmpty()) {
            return;
        }

        shapeList.remove(shapeList.size() - 1);
        invalidate();
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

        for (Shape s : shapeList) {
            List<PointF> points = s.points;
            int index = 0;
            Path linePath = null;

            if (s.shapeType == PATH) {
                linePath = new Path();
            }

            for (PointF point : points) {
                if (linePath != null) {
                    if (index == 0) {
                        linePath.moveTo(point.x, point.y);
                    } else {
                        linePath.lineTo(point.x, point.y);
                    }
                }
                index++;
            }

            if (s.shapeType == PATH) {
                bitmapCanvas.drawPath(linePath, paint);
            } else {
                drawShape(bitmapCanvas, s);
            }
        }

        //绘制右下角日期文字
        String text = getCurrentDate();
        textPaint.setColor(Color.RED);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(spToPx(10));
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        int margin = (int) dpToPx(8);
        bitmapCanvas.drawText(text, drawable.getIntrinsicWidth() - textBounds.width() - margin,
                drawable.getIntrinsicHeight() - margin, textPaint);
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

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float factor = detector.getScaleFactor();
        imageMatrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
        setImageMatrix(imageMatrix);
        return true;
    }

    @Override
    public void onMoveGestureScroll(MotionEvent downEvent, MotionEvent currentEvent,
                                    int pointerIndex, float dx, float dy, float distanceX,
                                    float distanceY) {
        if (scaleAndTranslate) {
            imageMatrix.postTranslate(dx, dy);
            setImageMatrix(imageMatrix);

        } else {
            addShapePoint(currentEvent);
        }
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return pointerCount > 1;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    public void scaleAndTranslate() {
        scaleAndTranslate = true;
    }

    /**
     * 获取手绘图片保存的数据
     *
     * @return
     */
    public String getShapeString() {
        if (shapeList == null || shapeList.isEmpty()) {
            return null;
        }

        try {
            JSONArray array = new JSONArray();
            for (Shape s : shapeList) {
                List<PointF> points = new ArrayList<>();
                switch (s.shapeType) {
                    case PATH:
                        points.addAll(s.getPoints());
                        break;

                    case LINE:
                    case OVAL:
                    case RECT:
                        if (s.hasTwoPoint()) {
                            PointF firstPoint = s.getFirstPoint();
                            PointF lastPoint = s.getLastPoint();
                            points.add(firstPoint);
                            points.add(lastPoint);
                        }
                        break;
                }

                if (points.isEmpty()) {
                    continue;
                }

                JSONObject shapeJo = new JSONObject();
                int shapeType = s.getShapeType();
                shapeJo.put("shapeType", shapeType);

                JSONArray pointArray = new JSONArray();
                for (PointF point : points) {
                    JSONObject pointJo = new JSONObject();
                    pointJo.put("x", point.x);
                    pointJo.put("y", point.y);
                    pointArray.put(pointJo);
                }
                shapeJo.put("points", pointArray);
                array.put(shapeJo);
            }

            if (array.length() > 0) {
                return array.toString();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setShapeData(String shapeData) {
        if (TextUtils.isEmpty(shapeData)) {
            return;
        }
        List<Shape> shapes = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(shapeData);
            int length = array.length();
            for (int i = 0; i < length; i++) {
                JSONObject shapeJo = array.getJSONObject(i);
                Shape shape = new Shape();
                shapes.add(shape);
                shape.setShapeType(shapeJo.getInt("shapeType"));

                JSONArray pointArray = shapeJo.getJSONArray("points");
                int pointLength = pointArray.length();
                for (int j = 0; j < pointLength; j++) {
                    JSONObject point = pointArray.getJSONObject(j);
                    float x = (float) point.getDouble("x");
                    float y = (float) point.getDouble("y");
                    if (shape.shapeType == PATH) {
                        if (j == 0) {
                            shape.path.moveTo(x, y);
                        } else {
                            shape.path.lineTo(x, y);
                        }
                    }
                    shape.addPoint(x, y);
                }
            }
            shapeList.addAll(shapes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

        public int getShapeType() {
            return shapeType;
        }

        public void setShapeType(int shapeType) {
            this.shapeType = shapeType;
        }

        public List<PointF> getPoints() {
            return points;
        }

        public void setPoints(List<PointF> points) {
            this.points = points;
        }
    }
}
