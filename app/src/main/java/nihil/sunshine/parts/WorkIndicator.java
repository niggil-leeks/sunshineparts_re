package nihil.sunshine.parts;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

/* JADX INFO: loaded from: classes.dex */
final class WorkIndicator {
    private Dialog dialog;
    private int generation;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Activity owner;
    private Runnable pending;

    WorkIndicator(Activity activity) {
        this.owner = activity;
    }

    static long completionDelay(boolean z, long j) {
        if (z) {
            return Math.max(0L, 800 - Math.max(0L, j));
        }
        return 0L;
    }

    int begin() {
        return begin(220L);
    }

    int begin(long j) {
        end();
        final int i = this.generation;
        this.pending = new Runnable() { // from class: nihil.sunshine.parts.WorkIndicator$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m112lambda$begin$0$nihilsunshinepartsWorkIndicator(i);
            }
        };
        this.main.postDelayed(this.pending, j);
        return i;
    }

    /* JADX INFO: renamed from: lambda$begin$0$nihil-sunshine-parts-WorkIndicator, reason: not valid java name */
    /* synthetic */ void m112lambda$begin$0$nihilsunshinepartsWorkIndicator(int i) {
        if (i != this.generation || this.owner.isFinishing() || this.owner.isDestroyed() || !this.owner.hasWindowFocus()) {
            return;
        }
        Dialog dialog = new Dialog(this.owner);
        dialog.requestWindowFeature(1);
        dialog.setCancelable(false);
        FrameLayout frameLayout = new FrameLayout(this.owner);
        int iRound = Math.round(this.owner.getResources().getDisplayMetrics().density * 48.0f);
        ProgressBar progressBar = new ProgressBar(this.owner, null, android.R.attr.progressBarStyle);
        progressBar.setIndeterminate(true);
        progressBar.setContentDescription(I18n.text(this.owner, "Loading"));
        frameLayout.addView(progressBar, new FrameLayout.LayoutParams(iRound, iRound, 17));
        dialog.setContentView(frameLayout);
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.clearFlags(2);
        this.dialog = dialog;
        dialog.show();
        int i2 = iRound * 2;
        window.setLayout(i2, i2);
        window.setGravity(17);
    }

    void end(int i) {
        if (i == this.generation) {
            end();
        }
    }

    void end() {
        this.generation++;
        if (this.pending != null) {
            this.main.removeCallbacks(this.pending);
            this.pending = null;
        }
        if (this.dialog != null) {
            this.dialog.dismiss();
            this.dialog = null;
        }
    }
}
