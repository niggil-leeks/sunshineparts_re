package nihil.sunshine.parts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class CpuHistoryView extends FrameLayout {
    private final int accent;
    private final DashPathEffect dashed;
    private final float fontScale;
    private String frequency;
    private final String frequencyTitle;
    private final int grid;
    private final int ink;
    private CpuFrequencies lastFrequencies;
    private String limit;
    private String load;
    private final CpuLoadBar loadBar;
    private final String loadTitle;
    private final ProgressBar loading;
    private final Paint.FontMetrics metrics;
    private CpuPolicy now;
    private final Paint paint;
    private final View plot;
    private CpuPlot range;
    private List<CpuHistory.Point> samples;
    private final int secondary;
    private String selected;
    private String[] tickLabels;
    private long[] ticks;
    private final int track;
    private final String waiting;

    private float dp(float f) {
        return f * getResources().getDisplayMetrics().density;
    }

    private float sp(float f) {
        return f * getResources().getDisplayMetrics().scaledDensity;
    }

    CpuHistoryView(Context context, int i, int i2, int i3, int i4) {
        super(context);
        this.paint = new Paint(1);
        this.metrics = new Paint.FontMetrics();
        this.samples = Collections.emptyList();
        this.selected = "";
        this.frequency = "—";
        this.load = "—";
        this.limit = "—";
        this.ticks = new long[0];
        this.tickLabels = new String[0];
        this.range = new CpuPlot(null);
        this.ink = i;
        this.secondary = i2;
        this.accent = i3;
        this.grid = CareArt.blend(i4, i2, 0.18f);
        this.track = CareArt.blend(i4, i2, 0.14f);
        this.fontScale = Math.max(1.0f, getResources().getConfiguration().fontScale);
        this.dashed = new DashPathEffect(new float[]{dp(3.0f), dp(3.0f)}, 0.0f);
        this.paint.setTypeface(Typeface.create("sec", 0));
        this.frequencyTitle = I18n.text(context, "Frequency");
        this.loadTitle = I18n.text(context, "CPU load");
        this.waiting = I18n.text(context, "Waiting for load readings…");
        setContentDescription(this.waiting);
        setFocusable(true);
        this.plot = new View(context) { // from class: nihil.sunshine.parts.CpuHistoryView.1
            @Override // android.view.View
            protected void onDraw(Canvas canvas) {
                CpuHistoryView.this.drawPlot(canvas);
            }
        };
        addView(this.plot);
        this.loadBar = new CpuLoadBar(context, i3, this.track);
        addView(this.loadBar);
        this.loading = new ProgressBar(context, null, android.R.attr.progressBarStyle);
        this.loading.setIndeterminate(true);
        this.loading.setContentDescription(I18n.text(context, "Loading"));
        addView(this.loading);
    }

    void readings(CpuHistory cpuHistory, String str) {
        this.samples = cpuHistory.points();
        this.selected = str;
        this.now = this.samples.isEmpty() ? null : CpuHistory.policy(this.samples.get(this.samples.size() - 1), this.selected);
        CpuPlot cpuPlot = new CpuPlot(this.now);
        CpuFrequencies cpuFrequencies = this.now != null ? this.now.frequencies : null;
        int i = 0;
        if (cpuPlot.minimum != this.range.minimum || cpuPlot.maximum != this.range.maximum || this.lastFrequencies != cpuFrequencies) {
            this.lastFrequencies = cpuFrequencies;
            this.ticks = cpuFrequencies != null ? cpuFrequencies.ticks() : cpuPlot.valid() ? new long[]{cpuPlot.minimum, cpuPlot.maximum} : new long[0];
            this.tickLabels = new String[this.ticks.length];
            for (int i2 = 0; i2 < this.ticks.length; i2++) {
                this.tickLabels[i2] = CpuPlot.number(this.ticks[i2]);
            }
        }
        this.range = cpuPlot;
        if (this.now != null) {
            this.frequency = CpuPolicy.ghz(this.now.current);
            this.limit = CpuPolicy.ghz(this.now.maximum);
            this.load = this.now.load >= 0.0d ? String.format(getResources().getConfiguration().getLocales().get(0), "%.0f%%", Double.valueOf(this.now.load)) : "—";
            setContentDescription(I18n.format(getContext(), "Frequency: %1$s. CPU load: %2$s. Current limit: %3$s.", this.frequency, this.load, this.limit));
        } else {
            this.frequency = "—";
            this.load = "—";
            this.limit = "—";
            setContentDescription(this.waiting);
        }
        this.loadBar.reading(this.now == null ? 0.0d : this.now.load, this.now != null && this.now.load >= 0.0d);
        ProgressBar progressBar = this.loading;
        if (this.now != null && this.now.load >= 0.0d) {
            i = 8;
        }
        progressBar.setVisibility(i);
        this.plot.invalidate();
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int iResolveSize = resolveSize(Math.round(dp(320.0f)), i);
        int iResolveSize2 = resolveSize(Math.round(dp(((this.fontScale - 1.0f) * 160.0f) + 310.0f)), i2);
        setMeasuredDimension(iResolveSize, iResolveSize2);
        this.plot.measure(View.MeasureSpec.makeMeasureSpec(iResolveSize, 1073741824), View.MeasureSpec.makeMeasureSpec(iResolveSize2, 1073741824));
        this.loadBar.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1, (iResolveSize - Math.round(iResolveSize * 0.56f)) - Math.round(dp(20.0f))), 1073741824), View.MeasureSpec.makeMeasureSpec(Math.round(dp(12.0f)), 1073741824));
        int iRound = Math.round(dp(40.0f));
        this.loading.measure(View.MeasureSpec.makeMeasureSpec(iRound, 1073741824), View.MeasureSpec.makeMeasureSpec(iRound, 1073741824));
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        this.plot.layout(0, 0, i5, i6);
        this.paint.setTextSize(sp(24.0f));
        this.paint.getFontMetrics(this.metrics);
        float fDp = dp((this.fontScale - 1.0f) * 36.0f);
        int iRound = Math.round(((dp(64.0f) + fDp) + ((this.metrics.ascent + this.metrics.descent) / 2.0f)) - dp(6.0f));
        int iRound2 = Math.round(i5 * 0.56f);
        this.loadBar.layout(iRound2, iRound, this.loadBar.getMeasuredWidth() + iRound2, this.loadBar.getMeasuredHeight() + iRound);
        int measuredWidth = this.loading.getMeasuredWidth();
        int i7 = i5 / 2;
        int iRound3 = Math.round(((((dp(120.0f) + fDp) + i6) - dp(48.0f)) - fDp) / 2.0f);
        int i8 = measuredWidth / 2;
        this.loading.layout(i7 - i8, iRound3 - i8, i7 + i8, iRound3 + i8);
    }

    private void text(Canvas canvas, String str, float f, float f2, int i, int i2, Paint.Align align, float f3) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(i2);
        this.paint.setTextAlign(align);
        this.paint.setTextSize(sp(i));
        float fMeasureText = this.paint.measureText(str);
        if (f3 > 0.0f && fMeasureText > f3) {
            this.paint.setTextSize((this.paint.getTextSize() * f3) / fMeasureText);
        }
        canvas.drawText(str, f, f2, this.paint);
    }

    private void line(Canvas canvas, float f, float f2, float f3, float f4, int i) {
        this.paint.setColor(i);
        this.paint.setStrokeWidth(dp(0.65f));
        canvas.drawLine(f, f2, f3, f4, this.paint);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void drawPlot(Canvas canvas) {
        char c;
        float f;
        Canvas canvas2;
        float f2;
        float f3;
        float f4;
        float f5;
        float f6 = 20.0f;
        float fDp = dp(20.0f);
        float width = getWidth();
        float height = getHeight();
        float f7 = width * 0.56f;
        float f8 = 1.0f;
        float fDp2 = dp((this.fontScale - 1.0f) * 36.0f);
        float fDp3 = dp(((this.fontScale - 1.0f) * 12.0f) + 35.0f);
        float f9 = f7 - fDp;
        text(canvas, this.frequencyTitle, fDp, fDp3, 12, this.secondary, Paint.Align.LEFT, f9 - dp(12.0f));
        text(canvas, this.frequency, fDp, dp(64.0f) + fDp2, 24, this.ink, Paint.Align.LEFT, f9 - dp(12.0f));
        text(canvas, this.loadTitle, f7, fDp3, 12, this.secondary, Paint.Align.LEFT, (width - f7) - fDp);
        this.paint.setTextSize(sp(11.0f));
        float fMax = 0.0f;
        for (String str : this.tickLabels) {
            fMax = Math.max(fMax, this.paint.measureText(str));
        }
        float fMax2 = fDp + Math.max(dp(32.0f), fMax) + dp(9.0f);
        float f10 = width - fDp;
        float fMeasureText = f10 - (this.paint.measureText("100") / 2.0f);
        float fDp4 = dp(120.0f) + fDp2;
        float fDp5 = (height - dp(48.0f)) - fDp2;
        text(canvas, "(GHz)", fDp, fDp4 - dp(17.0f), 11, this.secondary, Paint.Align.LEFT, dp(43.0f));
        this.paint.setPathEffect(this.dashed);
        int i = 0;
        while (true) {
            c = 0;
            f = 4.0f;
            if (i >= 5) {
                break;
            }
            float f11 = fMax2 + (((fMeasureText - fMax2) * i) / 4.0f);
            float f12 = fDp5;
            float f13 = fDp4;
            line(canvas, f11, f13, f11, f12, this.grid);
            text(canvas, Integer.toString(i * 25), f11, f12 + dp(((this.fontScale - 1.0f) * 10.0f) + 21.0f), 11, this.secondary, Paint.Align.CENTER, dp(40.0f));
            i++;
            fDp4 = f13;
            fDp5 = f12;
        }
        float f14 = fDp4;
        float f15 = fDp5;
        PathEffect pathEffect = null;
        if (!this.range.valid()) {
            canvas2 = canvas;
            f2 = 20.0f;
            f3 = 1.0f;
        } else {
            int i2 = 0;
            while (i2 < this.ticks.length) {
                float fFrequency = f15 - ((f15 - f14) * this.range.frequency(this.ticks[i2]));
                float f16 = fMeasureText;
                float f17 = f6;
                float f18 = f;
                float f19 = fMax2;
                int i3 = i2;
                line(canvas, f19, fFrequency, f16, fFrequency, this.grid);
                text(canvas, this.tickLabels[i3], f19 - dp(9.0f), dp(f18) + fFrequency, 11, this.secondary, Paint.Align.RIGHT, (f19 - fDp) - dp(9.0f));
                i2 = i3 + 1;
                f = f18;
                pathEffect = pathEffect;
                c = c;
                f6 = f17;
                fMax2 = f19;
                fMeasureText = f16;
                f8 = f8;
            }
            PathEffect pathEffect2 = pathEffect;
            f3 = f8;
            float f20 = fMax2;
            float f21 = fMeasureText;
            f2 = f6;
            float f22 = f;
            if (this.now.maximum > this.range.minimum && this.now.maximum < this.range.maximum) {
                float fFrequency2 = f15 - ((f15 - f14) * this.range.frequency(this.now.maximum));
                canvas2 = canvas;
                f4 = f20;
                f5 = f21;
                line(canvas2, f4, fFrequency2, f5, fFrequency2, CareArt.blend(this.grid, this.secondary, 0.55f));
            } else {
                canvas2 = canvas;
                f4 = f20;
                f5 = f21;
            }
            this.paint.setPathEffect(pathEffect2);
            int iSave = canvas2.save();
            canvas2.clipRect(f4 - dp(5.0f), f14 - dp(5.0f), dp(5.0f) + f5, f15 + dp(5.0f));
            for (int i4 = 0; i4 < this.samples.size(); i4++) {
                CpuPolicy cpuPolicyPolicy = CpuHistory.policy(this.samples.get(i4), this.selected);
                if (cpuPolicyPolicy != null && cpuPolicyPolicy.current > 0 && cpuPolicyPolicy.load >= 0.0d) {
                    float f23 = ((f5 - f4) * ((float) (cpuPolicyPolicy.load / 100.0d))) + f4;
                    float fFrequency3 = f15 - ((f15 - f14) * this.range.frequency(cpuPolicyPolicy.current));
                    this.paint.setColor((this.accent & 16777215) | (((((i4 + 1) * 100) / Math.max(1, this.samples.size())) + 25) << 24));
                    canvas2.drawCircle(f23, fFrequency3, dp(2.3f), this.paint);
                }
            }
            if (this.now.load >= 0.0d && this.now.current > 0) {
                float f24 = f4 + ((f5 - f4) * ((float) (this.now.load / 100.0d)));
                float fFrequency4 = f15 - ((f15 - f14) * this.range.frequency(this.now.current));
                this.paint.setColor((this.accent & 16777215) | 620756992);
                canvas2.drawCircle(f24, fFrequency4, dp(8.0f), this.paint);
                this.paint.setColor(this.accent);
                canvas2.drawCircle(f24, fFrequency4, dp(f22), this.paint);
            }
            canvas2.restoreToCount(iSave);
        }
        this.paint.setPathEffect(null);
        text(canvas2, "(%)", f10, f15 + dp(((this.fontScale - f3) * f2) + 37.0f), 11, this.secondary, Paint.Align.RIGHT, dp(40.0f));
    }
}
