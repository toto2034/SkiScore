package it.unisa.skiscore.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import it.unisa.skiscore.R;

/**
 * Custom circular gauge view that displays the Skiability Score as an animated arc.
 * Colors change based on score: red (<40), yellow (40-70), green/cyan (>70).
 */
public class SkiScoreView extends View {

    private Paint trackPaint;
    private Paint arcPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private RectF arcRect;

    private int score = 0;
    private float animatedSweep = 0f;
    private float strokeWidth = 14f;

    private static final float START_ANGLE = 135f;
    private static final float MAX_SWEEP = 270f;

    public SkiScoreView(Context context) {
        super(context);
        init();
    }

    public SkiScoreView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SkiScoreView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Track (background arc)
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.ring_track));

        // Foreground arc
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Score text
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setColor(Color.WHITE);

        // Label text
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.ice_blue));

        arcRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h);
        strokeWidth = size * 0.08f;
        float padding = strokeWidth / 2f + 4f;

        float left = (w - size) / 2f + padding;
        float top = (h - size) / 2f + padding;
        float right = (w + size) / 2f - padding;
        float bottom = (h + size) / 2f - padding;

        arcRect.set(left, top, right, bottom);

        trackPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeWidth(strokeWidth);

        textPaint.setTextSize(size * 0.28f);
        labelPaint.setTextSize(size * 0.10f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the track (background arc)
        canvas.drawArc(arcRect, START_ANGLE, MAX_SWEEP, false, trackPaint);

        // Draw the score arc
        if (animatedSweep > 0) {
            arcPaint.setColor(getScoreColor(score));
            canvas.drawArc(arcRect, START_ANGLE, animatedSweep, false, arcPaint);
        }

        // Draw score text
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();

        String scoreText = score + "%";
        float textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(scoreText, centerX, textY, textPaint);

        // Draw label below score
        String label = getScoreLabel(score);
        float labelY = textY + textPaint.getTextSize() * 0.6f;
        canvas.drawText(label, centerX, labelY, labelPaint);
    }

    /**
     * Set the score and animate the arc.
     */
    public void setScore(int newScore) {
        this.score = Math.max(0, Math.min(100, newScore));
        float targetSweep = (score / 100f) * MAX_SWEEP;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, targetSweep);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(animation -> {
            animatedSweep = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    /**
     * Set the score without animation (for RecyclerView reuse).
     */
    public void setScoreImmediate(int newScore) {
        this.score = Math.max(0, Math.min(100, newScore));
        this.animatedSweep = (score / 100f) * MAX_SWEEP;
        invalidate();
    }

    private int getScoreColor(int score) {
        if (score >= 70) {
            return ContextCompat.getColor(getContext(), R.color.neon_cyan);
        } else if (score >= 40) {
            return ContextCompat.getColor(getContext(), R.color.neon_yellow);
        } else {
            return ContextCompat.getColor(getContext(), R.color.neon_red);
        }
    }

    private String getScoreLabel(int score) {
        if (score >= 80) return "Eccellente";
        if (score >= 60) return "Buono";
        if (score >= 40) return "Discreto";
        return "Scarso";
    }
}
