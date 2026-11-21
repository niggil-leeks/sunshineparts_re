package nihil.sunshine.parts;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LocaleManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;
import android.window.OnBackInvokedCallback;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class MainActivity extends Activity {
    private int accent;
    private boolean accessInitialized;
    private BroadcastReceiver accessUpdates;
    private ScrollView activeScroll;
    private CareArt.Ambient ambient;
    private int background;
    private BroadcastReceiver batteryEvents;
    private LinearLayout body;
    private LinearLayout bottomActions;
    private boolean busy;
    private LinearLayout card;
    private boolean checkAfterStop;
    private boolean cpuBound;
    private CpuHistoryView cpuChart;
    private TextView cpuCluster;
    private TextView cpuLimit;
    private CpuMonitorService cpuService;
    private TextView cpuStatus;
    private long detailsAt;
    private boolean detailsLoading;
    private int divider;
    private boolean entryCheck;
    private int floatingSurface;
    private boolean foreground;
    private Button healthAction;
    private CareArt.HealthMaterial healthMaterial;
    private TextView healthSummary;
    private TextView healthTitle;
    private View healthView;
    private int ink;
    private LinearLayout lastRow;
    private boolean leaving;
    private RootOps ops;
    private int pageGeneration;
    private LinearLayout pageRoot;
    private long policyAt;
    private BroadcastReceiver policyObserver;
    private boolean reportLoading;
    private int screen;
    private int secondary;
    private int selectedAccent;
    private int surface;
    private Toolbar toolbar;
    private WorkIndicator workIndicator;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private JSONObject state = new JSONObject();
    private JSONObject info = new JSONObject();
    private String connectionIssue = "";
    private final int[] scrollOffsets = new int[14];
    private final CpuMonitorService.Observer cpuObserver = new CpuMonitorService.Observer() { // from class: nihil.sunshine.parts.MainActivity.1
        @Override // nihil.sunshine.parts.CpuMonitorService.Observer
        public void sample(CpuHistory cpuHistory, List<CpuPolicy> list) {
            if (MainActivity.this.screen == 10 && !MainActivity.this.isFinishing() && !MainActivity.this.isDestroyed()) {
                MainActivity.this.cpuPolicies = list;
                MainActivity.this.cpuHistory = cpuHistory;
                if (MainActivity.this.foreground) {
                    MainActivity.this.updateCpuChart();
                    if (MainActivity.this.cpuStatus != null) {
                        MainActivity.this.cpuStatus.setVisibility(8);
                    }
                }
            }
        }

        @Override // nihil.sunshine.parts.CpuMonitorService.Observer
        public void error(String str) {
            if (MainActivity.this.screen != 10 || MainActivity.this.cpuStatus == null) {
                return;
            }
            MainActivity.this.cpuStatus.setText(str);
            MainActivity.this.cpuStatus.setVisibility(0);
        }
    };
    private final ServiceConnection cpuConnection = new ServiceConnection() { // from class: nihil.sunshine.parts.MainActivity.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (MainActivity.this.cpuBound) {
                MainActivity.this.cpuService = ((CpuMonitorService.LocalBinder) iBinder).service();
                MainActivity.this.cpuService.observe(MainActivity.this.cpuObserver);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            MainActivity.this.cpuService = null;
        }
    };
    private CpuHistory cpuHistory = new CpuHistory();
    private List<CpuPolicy> cpuPolicies = Collections.emptyList();
    private String selectedCpu = "";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable refreshHealth = new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda36
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.m54lambda$new$0$nihilsunshinepartsMainActivity();
        }
    };
    private int accessPendingPage = -1;
    private final Runnable accessDeadline = new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda37
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.m55lambda$new$7$nihilsunshinepartsMainActivity();
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    interface Job {
        String run() throws Exception;
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface Pick {
        void run(String str);
    }

    static /* synthetic */ int access$1608(MainActivity mainActivity) {
        int i = mainActivity.pageGeneration;
        mainActivity.pageGeneration = i + 1;
        return i;
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        if (this.accessInitialized && this.checkAfterStop) {
            this.checkAfterStop = false;
            checkEntryAccess();
        }
        if (this.ops != null && this.batteryEvents == null) {
            this.batteryEvents = new BroadcastReceiver() { // from class: nihil.sunshine.parts.MainActivity.3
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    try {
                        MainActivity.this.info.put("battery_temperature", intent.getIntExtra("temperature", -1));
                    } catch (JSONException e) {
                    }
                    if (MainActivity.this.screen == 0) {
                        MainActivity.this.m54lambda$new$0$nihilsunshinepartsMainActivity();
                    }
                }
            };
            registerReceiver(this.batteryEvents, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        }
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.checkAfterStop = true;
        this.foreground = false;
        endLoading();
        detachCpuMonitor();
        this.main.removeCallbacks(this.refreshHealth);
        if (this.batteryEvents != null) {
            unregisterReceiver(this.batteryEvents);
            this.batteryEvents = null;
        }
        super.onStop();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    private int color(String str) {
        return getColor(getResources().getIdentifier(str, "color", getPackageName()));
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        int i;
        int[] intArray;
        super.onCreate(bundle);
        final long jUptimeMillis = SystemClock.uptimeMillis();
        final int i2 = 0;
        getWindow().setDecorFitsSystemWindows(false);
        getWindow().setNavigationBarColor(0);
        getWindow().setNavigationBarContrastEnforced(false);
        this.background = color("parts_background");
        this.surface = color("parts_surface");
        this.ink = color("parts_text");
        this.secondary = color("parts_secondary");
        this.accent = color("parts_accent");
        this.divider = color("parts_divider");
        boolean z = (getResources().getConfiguration().uiMode & 48) == 32;
        int i3 = android.R.color.system_accent1_600;
        try {
            this.accent = getColor(z ? android.R.color.system_accent1_300 : 17170495);
        } catch (Exception e) {
        }
        this.selectedAccent = this.accent;
        if (z) {
            i3 = android.R.color.system_accent1_200;
        }
        try {
            this.selectedAccent = getColor(i3);
        } catch (Exception e2) {
        }
        try {
            this.surface = getColor(z ? android.R.color.system_neutral2_900 : android.R.color.system_neutral1_10);
        } catch (Exception e3) {
        }
        this.floatingSurface = z ? -12961219 : -197377;
        this.divider = CareArt.blend(this.surface, this.secondary, 0.22f);
        if (Build.VERSION.SDK_INT != 36 || !Config.supported(Build.DISPLAY)) {
            startupFailure("Unsupported system", "This build requires Retriever, One UI 8.0 and Android 16.");
            return;
        }
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, new OnBackInvokedCallback() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda9
            @Override // android.window.OnBackInvokedCallback
            public final void onBackInvoked() {
                this.f$0.m56lambda$onCreate$1$nihilsunshinepartsMainActivity();
            }
        });
        this.ops = new RootOps(this);
        this.accessUpdates = new BroadcastReceiver() { // from class: nihil.sunshine.parts.MainActivity.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (MainActivity.this.accessInitialized) {
                    MainActivity.this.armAccessDeadline();
                }
            }
        };
        registerReceiver(this.accessUpdates, new IntentFilter("nihil.sunshine.parts.ACCESS_CHANGED"), "android.permission.DUMP", this.main, 2);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackgroundColor(this.background);
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        progressBar.setIndeterminate(true);
        progressBar.setContentDescription(I18n.text(this, "Loading"));
        linearLayout.addView(progressBar, new LinearLayout.LayoutParams(dp(48.0f), dp(48.0f)));
        setContentView(linearLayout);
        if (bundle != null) {
            this.selectedCpu = bundle.getString("cpu_cluster", "");
        }
        if (bundle != null && (intArray = bundle.getIntArray("scrollOffsets")) != null) {
            System.arraycopy(intArray, 0, this.scrollOffsets, 0, Math.min(intArray.length, this.scrollOffsets.length));
        }
        if (getIntent().getBooleanExtra("cpu_monitor", false)) {
            i = 10;
        } else {
            i = bundle == null ? 0 : bundle.getInt("screen", 0);
        }
        getIntent().removeExtra("cpu_monitor");
        if (i >= 0 && i < this.scrollOffsets.length) {
            i2 = i;
        }
        this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m61lambda$onCreate$6$nihilsunshinepartsMainActivity(i2, jUptimeMillis);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m56lambda$onCreate$1$nihilsunshinepartsMainActivity() {
        if (this.busy) {
            return;
        }
        if (this.screen != 0) {
            show(PartsNavigation.parent(this.screen));
        } else {
            finish();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$6$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m61lambda$onCreate$6$nihilsunshinepartsMainActivity(final int i, long j) {
        final JSONObject[] jSONObjectArr = {null};
        StartupGate.Check check = new StartupGate.Check() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda11
            @Override // nihil.sunshine.parts.StartupGate.Check
            public final void run() throws Exception {
                this.f$0.m57lambda$onCreate$2$nihilsunshinepartsMainActivity();
            }
        };
        final RootOps rootOps = this.ops;
        Objects.requireNonNull(rootOps);
        final StartupGate.Result resultCheck = StartupGate.check(check, new StartupGate.Check() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda12
            @Override // nihil.sunshine.parts.StartupGate.Check
            public final void run() throws Exception {
                rootOps.verifyPlatform();
            }
        }, new StartupGate.Check() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda13
            @Override // nihil.sunshine.parts.StartupGate.Check
            public final void run() throws Exception {
                this.f$0.m58lambda$onCreate$3$nihilsunshinepartsMainActivity(jSONObjectArr);
            }
        });
        if (resultCheck.error instanceof SerialAccess.Denied) {
            this.main.post(new MainActivity$$ExternalSyntheticLambda14(this));
            return;
        }
        if (!resultCheck.canEnter()) {
            this.main.post(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m59lambda$onCreate$4$nihilsunshinepartsMainActivity(resultCheck);
                }
            });
            return;
        }
        final JSONObject jSONObjectQuickInfo = this.ops.quickInfo();
        final JSONObject jSONObject = jSONObjectArr[0] == null ? new JSONObject() : jSONObjectArr[0];
        final String message = resultCheck.error == null ? "" : resultCheck.error.getMessage();
        this.main.postDelayed(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m60lambda$onCreate$5$nihilsunshinepartsMainActivity(jSONObject, jSONObjectQuickInfo, message, resultCheck, i);
            }
        }, Math.max(0L, 500 - (SystemClock.uptimeMillis() - j)));
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m57lambda$onCreate$2$nihilsunshinepartsMainActivity() throws Exception {
        this.ops.verifyRoot();
        try {
            if (!PassageGate.hasValidPassage(this, this.ops) && !SerialAccess.verify(this.ops)) {
                throw new SerialAccess.Denied();
            }
        } catch (Exception e) {
            throw new SerialAccess.Denied();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$3$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m58lambda$onCreate$3$nihilsunshinepartsMainActivity(JSONObject[] jSONObjectArr) throws Exception {
        jSONObjectArr[0] = this.ops.control("com.android.systemui", "status");
    }

    /* JADX INFO: renamed from: lambda$onCreate$4$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m59lambda$onCreate$4$nihilsunshinepartsMainActivity(StartupGate.Result result) {
        if (!isFinishing() && !isDestroyed()) {
            startupFailure(result.state == 0 ? "Root access unavailable" : "Unsupported system", result.error == null ? "Action failed" : result.error.getMessage());
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$5$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m60lambda$onCreate$5$nihilsunshinepartsMainActivity(JSONObject jSONObject, JSONObject jSONObject2, String str, StartupGate.Result result, int i) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        this.accessInitialized = true;
        armAccessDeadline();
        this.state = jSONObject;
        this.info = jSONObject2;
        if (str == null) {
            str = "";
        }
        this.connectionIssue = str;
        if (result.state != 2) {
            i = 5;
        }
        show(i);
        if (result.state != 2) {
            new AlertDialog.Builder(this).setTitle(I18n.text(this, "Restart required")).setMessage(I18n.text(this, "Access verified. Restart the device to load the module.")).setPositiveButton(I18n.text(this, "OK"), (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: renamed from: lambda$new$7$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m55lambda$new$7$nihilsunshinepartsMainActivity() {
        if (this.accessInitialized) {
            checkEntryAccess();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void armAccessDeadline() {
        AccessLease accessLease;
        this.main.removeCallbacks(this.accessDeadline);
        if (PassageGate.hasValidPassage(this, this.ops) || (accessLease = SerialAccess.lastLease) == null) {
            return;
        }
        this.main.postDelayed(this.accessDeadline, Math.max(0L, accessLease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(this))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeDeniedAccess() {
        finishAndRemoveTask();
        Process.killProcess(Process.myPid());
    }

    private void checkEntryAccess() {
        if (PassageGate.hasValidPassage(this, this.ops) || this.entryCheck || this.ops == null) {
            return;
        }
        this.entryCheck = true;
        this.busy = true;
        final int iBeginLoading = beginLoading();
        this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda34
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m28lambda$checkEntryAccess$9$nihilsunshinepartsMainActivity(iBeginLoading);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$checkEntryAccess$9$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m28lambda$checkEntryAccess$9$nihilsunshinepartsMainActivity(final int i) {
        final boolean zVerify;
        try {
            zVerify = SerialAccess.verify(this.ops);
        } catch (Exception e) {
            zVerify = false;
        }
        this.main.post(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda33
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m27lambda$checkEntryAccess$8$nihilsunshinepartsMainActivity(i, zVerify);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$checkEntryAccess$8$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m27lambda$checkEntryAccess$8$nihilsunshinepartsMainActivity(int i, boolean z) {
        this.entryCheck = false;
        this.busy = false;
        endLoading(i);
        if (!z) {
            closeDeniedAccess();
            return;
        }
        armAccessDeadline();
        if (this.accessPendingPage >= 0) {
            int i2 = this.accessPendingPage;
            this.accessPendingPage = -1;
            show(i2);
        }
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        if (this.activeScroll != null) {
            this.scrollOffsets[this.screen] = this.activeScroll.getScrollY();
        }
        bundle.putIntArray("scrollOffsets", this.scrollOffsets);
        bundle.putInt("screen", this.screen);
        bundle.putString("cpu_cluster", this.selectedCpu);
        super.onSaveInstanceState(bundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ProgressBar spinner() {
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        progressBar.setIndeterminate(true);
        progressBar.setContentDescription(I18n.text(this, "Loading"));
        return progressBar;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView text(String str, int i, int i2) {
        LocalizedText localizedText = new LocalizedText(this);
        localizedText.setText(str);
        localizedText.setTextSize(i);
        localizedText.setTextColor(i2);
        localizedText.setTypeface(Typeface.create(Typeface.create("sec", 0), 400, false));
        localizedText.setLineSpacing(dp(2.0f), 1.0f);
        return localizedText;
    }

    private int beginLoading() {
        if (this.workIndicator == null) {
            this.workIndicator = new WorkIndicator(this);
        }
        return this.workIndicator.begin();
    }

    private int beginConnectionLoading() {
        if (this.workIndicator == null) {
            this.workIndicator = new WorkIndicator(this);
        }
        return this.workIndicator.begin(0L);
    }

    private void endLoading() {
        if (this.workIndicator != null) {
            this.workIndicator.end();
        }
    }

    private void endLoading(int i) {
        if (this.workIndicator != null) {
            this.workIndicator.end(i);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showError(String str) {
        if (str == null || str.isEmpty() || isFinishing() || isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(this).setTitle(I18n.text(this, "Couldn't complete action")).setMessage(I18n.text(this, str)).setPositiveButton(I18n.text(this, "OK"), (DialogInterface.OnClickListener) null).show();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (this.accessInitialized) {
            if (intent.getBooleanExtra("cpu_monitor", false)) {
                this.accessPendingPage = 10;
            }
            checkEntryAccess();
        } else if (intent.getBooleanExtra("cpu_monitor", false) && this.ops != null && !this.busy) {
            show(10);
        }
        intent.removeExtra("cpu_monitor");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public GradientDrawable shape(int i, int i2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(i2));
        return gradientDrawable;
    }

    private void startupFailure(String str, String str2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(28.0f), dp(56.0f), dp(28.0f), dp(32.0f));
        linearLayout.setBackgroundColor(this.background);
        linearLayout.addView(text(str, 25, this.ink));
        if (str2 == null) {
            str2 = "Action failed";
        }
        TextView textViewText = text(str2, 15, this.secondary);
        textViewText.setTextIsSelectable(true);
        textViewText.setPadding(0, dp(20.0f), 0, dp(24.0f));
        linearLayout.addView(textViewText);
        LocalizedButton localizedButton = new LocalizedButton(this);
        localizedButton.setText("Try again");
        localizedButton.setOnClickListener(new View.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda95
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m88lambda$startupFailure$10$nihilsunshinepartsMainActivity(view);
            }
        });
        linearLayout.addView(localizedButton);
        LocalizedButton localizedButton2 = new LocalizedButton(this);
        localizedButton2.setText("Support");
        localizedButton2.setOnClickListener(new View.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda96
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m89lambda$startupFailure$11$nihilsunshinepartsMainActivity(view);
            }
        });
        linearLayout.addView(localizedButton2);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(linearLayout);
        setContentView(scrollView);
    }

    /* JADX INFO: renamed from: lambda$startupFailure$10$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m88lambda$startupFailure$10$nihilsunshinepartsMainActivity(View view) {
        recreate();
    }

    /* JADX INFO: renamed from: lambda$startupFailure$11$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m89lambda$startupFailure$11$nihilsunshinepartsMainActivity(View view) {
        entrance(false);
    }

    private void entrance(boolean z) {
        if (z) {
            startupFailure("Check module status", "Check the SystemUI connection.");
            return;
        }
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://t.me/lainsproject")));
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this).setTitle(I18n.text(this, "Support")).setMessage("https://t.me/lainsproject").setPositiveButton(I18n.text(this, "Close"), (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void show(int i) {
        final int i2;
        CareArt.Ambient ambient;
        if (i == 13) {
            i2 = 4;
        } else if (i == 6) {
            i2 = 0;
        } else {
            i2 = i == 1 ? 9 : i;
        }
        boolean z = this.pageRoot == null;
        if (i2 != 10) {
            stopCpuMonitor();
        }
        this.bottomActions = null;
        final int i3 = this.screen;
        if (this.activeScroll != null) {
            this.scrollOffsets[i3] = this.activeScroll.getScrollY();
        }
        if (this.pageRoot != null) {
            this.pageRoot.animate().cancel();
        }
        this.screen = i2;
        this.pageGeneration++;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        if (i2 == 0) {
            ambient = new CareArt.Ambient(this.background, this.accent);
        } else {
            ambient = null;
        }
        this.ambient = ambient;
        linearLayout.setBackground(this.ambient != null ? this.ambient : new ColorDrawable(this.background));
        this.pageRoot = linearLayout;
        this.healthView = null;
        this.healthMaterial = null;
        linearLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda53
            @Override // android.view.View.OnApplyWindowInsetsListener
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                return this.f$0.m85lambda$show$12$nihilsunshinepartsMainActivity(view, windowInsets);
            }
        });
        this.toolbar = new Toolbar(this);
        this.toolbar.setBackgroundColor(0);
        this.toolbar.setTitleTextColor(this.accent);
        this.toolbar.setTitleTextAppearance(this, android.R.style.TextAppearance.DeviceDefault.Widget.ActionBar.Title);
        String str = new String[]{"Sunshine Parts", "Visual effects", "Performance", "Memory", "Advanced settings", "Module status", "", "Processing speed", "UI workload", "Customization", "Policy limits", "Diagnostics", "Module report", "Home screen animations"}[i2];
        this.toolbar.setTitle(i2 == 0 ? "" : I18n.text(this, str));
        if (i2 != 0) {
            this.toolbar.setContentInsetStartWithNavigation(dp(64.0f));
            this.toolbar.setTitleMarginStart(dp(8.0f));
            this.toolbar.setNavigationIcon(getDrawable(getResources().getIdentifier("ic_arrow_back", "drawable", getPackageName())));
            if (this.toolbar.getNavigationIcon() != null) {
                this.toolbar.getNavigationIcon().setTint(this.accent);
            }
            this.toolbar.setNavigationContentDescription(I18n.text(this, "Back"));
            this.toolbar.setNavigationOnClickListener(new View.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda54
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m86lambda$show$13$nihilsunshinepartsMainActivity(i2, view);
                }
            });
        }
        if (i2 == 0) {
            this.toolbar.setContentInsetsAbsolute(dp(24.0f), dp(12.0f));
        }
        final FrameLayout frameLayout = new FrameLayout(this);
        final CareHeader careHeader = new CareHeader(this, this.toolbar, str, this.background, this.accent, this.floatingSurface);
        final CareScrollView careScrollView = new CareScrollView(this, careHeader.expandedHeight - careHeader.collapsedHeight);
        this.activeScroll = careScrollView;
        careScrollView.setFillViewport(true);
        careScrollView.setClipToPadding(false);
        this.body = new LinearLayout(this);
        this.body.setOrientation(1);
        this.body.setPadding(dp(12.0f), 0, dp(12.0f), dp(28.0f));
        this.body.addView(new View(this), new LinearLayout.LayoutParams(-1, careHeader.expandedHeight + dp(8.0f)));
        careScrollView.addView(this.body);
        frameLayout.addView(careScrollView, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.addView(careHeader, new FrameLayout.LayoutParams(-1, careHeader.expandedHeight, 48));
        linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        careScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda55
            @Override // android.view.View.OnScrollChangeListener
            public final void onScrollChange(View view, int i4, int i5, int i6, int i7) {
                this.f$0.m87lambda$show$14$nihilsunshinepartsMainActivity(careHeader, view, i4, i5, i6, i7);
            }
        });
        setContentView(linearLayout);
        linearLayout.requestApplyInsets();
        this.card = null;
        if (i2 == 0) {
            home();
        } else if (i2 == 1) {
            visual();
        } else if (i2 == 2) {
            performance();
        } else if (i2 == 3) {
            memory();
        } else if (i2 == 4) {
            advanced();
        } else if (i2 == 5) {
            status();
        } else if (i2 == 7) {
            processing();
        } else if (i2 == 8) {
            workload();
        } else if (i2 == 9) {
            personalize();
        } else if (i2 == 10) {
            policyLimits();
        } else if (i2 == 11) {
            diagnostics();
        } else if (i2 == 13) {
            launcherAnimations();
        } else {
            report();
        }
        I18n.apply(this, linearLayout);
        final int i4 = this.scrollOffsets[i2];
        final boolean z2 = i2 != i3 || z;
        final int i5 = i2;
        careScrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // from class: nihil.sunshine.parts.MainActivity.5
            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public boolean onPreDraw() {
                careScrollView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (MainActivity.this.activeScroll != careScrollView) {
                    return true;
                }
                careScrollView.scrollTo(0, i4);
                careHeader.scroll(careScrollView.getScrollY());
                MainActivity.this.updateAura();
                if (z2) {
                    CareArt.enter(frameLayout, i5 < i3);
                }
                return true;
            }
        });
        ensureDetails(i5);
        if (i5 == 10) {
            startCpuMonitor();
        }
        if (i5 == 12) {
            refreshReport();
        }
    }

    /* JADX INFO: renamed from: lambda$show$12$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ WindowInsets m85lambda$show$12$nihilsunshinepartsMainActivity(View view, WindowInsets windowInsets) {
        Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        view.setPadding(insets.left, insets.top, insets.right, 0);
        if (this.body != null) {
            this.body.setPadding(dp(12.0f), 0, dp(12.0f), Math.max(dp(16.0f), insets.bottom + dp(8.0f)));
        }
        if (this.bottomActions != null) {
            this.bottomActions.setPadding(dp(24.0f), dp(8.0f), dp(24.0f), Math.max(dp(16.0f), insets.bottom + dp(8.0f)));
        }
        return windowInsets;
    }

    /* JADX INFO: renamed from: lambda$show$13$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m86lambda$show$13$nihilsunshinepartsMainActivity(int i, View view) {
        if (this.busy) {
            return;
        }
        if (i == 0) {
            finish();
        } else {
            show(PartsNavigation.parent(i));
        }
    }

    /* JADX INFO: renamed from: lambda$show$14$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m87lambda$show$14$nihilsunshinepartsMainActivity(CareHeader careHeader, View view, int i, int i2, int i3, int i4) {
        careHeader.scroll(i2);
        updateAura();
    }

    private void section(String str) {
        this.card = null;
        TextView textViewText = text(str, 13, this.secondary);
        textViewText.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
        textViewText.setPadding(dp(20.0f), dp(10.0f), dp(20.0f), dp(8.0f));
        this.body.addView(textViewText);
    }

    private LinearLayout group() {
        if (this.card == null) {
            this.card = new LinearLayout(this);
            this.card.setOrientation(1);
            this.card.setBackground(shape(this.surface, 28));
            this.card.setClipToOutline(true);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.bottomMargin = dp(14.0f);
            this.body.addView(this.card, layoutParams);
        }
        return this.card;
    }

    private LinearLayout row(String str, String str2) {
        LinearLayout linearLayoutGroup = group();
        if (linearLayoutGroup.getChildCount() > 0) {
            View view = new View(this);
            view.setBackgroundColor(this.divider);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(0.5f));
            layoutParams.setMargins(dp(24.0f), 0, dp(24.0f), 0);
            linearLayoutGroup.addView(view, layoutParams);
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(16);
        linearLayout.setMinimumHeight(dp(60.0f));
        linearLayout.setPadding(dp(20.0f), dp(14.0f), dp(20.0f), dp(14.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        TextView textViewText = text(str, 17, this.ink);
        textViewText.setLineSpacing(0.0f, 1.0f);
        textViewText.setBreakStrategy(2);
        textViewText.setHyphenationFrequency(0);
        linearLayout2.addView(textViewText);
        if (str2 != null && !str2.isEmpty()) {
            TextView textViewText2 = text(DescriptionCopy.summary(I18n.text(this, str2), str2), 13, this.secondary);
            textViewText2.setPadding(0, dp(2.0f), 0, 0);
            textViewText2.setLineSpacing(0.0f, 1.0f);
            linearLayout2.addView(textViewText2);
        }
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayoutGroup.addView(linearLayout, new LinearLayout.LayoutParams(-1, -2));
        this.lastRow = linearLayout;
        return linearLayout;
    }

    private void selectedRow(String str, String str2, boolean z) {
        LinearLayout linearLayout = (LinearLayout) row(str, str2).getChildAt(0);
        if (!z || linearLayout.getChildCount() <= 1) {
            return;
        }
        ((TextView) linearLayout.getChildAt(1)).setTextColor(this.selectedAccent);
    }

    private void clickable(LinearLayout linearLayout, final Runnable runnable) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        linearLayout.setForeground(getDrawable(typedValue.resourceId));
        linearLayout.setClickable(true);
        linearLayout.setFocusable(true);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m30lambda$clickable$15$nihilsunshinepartsMainActivity(runnable, view);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$clickable$15$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m30lambda$clickable$15$nihilsunshinepartsMainActivity(Runnable runnable, View view) {
        if (!SerialAccess.usable(this)) {
            closeDeniedAccess();
        } else if (!this.busy) {
            runnable.run();
        }
    }

    private void link(String str, String str2, Runnable runnable) {
        clickable(row(str, str2), runnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int visualLevel() {
        return AdaptiveWorkload.select(selectedMode(), this.info.optInt("battery_temperature", -1));
    }

    private boolean option(String str, boolean z) {
        int iVisualLevel = visualLevel();
        if (VisualPolicy.managed(str, iVisualLevel)) {
            if ("native_blur".equals(str) || "lean_qs".equals(str) || "tile_layout".equals(str) || "tile_work".equals(str) || "native_animations".equals(str)) {
                return true;
            }
            if ("prepared_blur".equals(str)) {
                return iVisualLevel == 1 || iVisualLevel == 3;
            }
            return false;
        }
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("options");
        JSONObject jSONObjectOptJSONObject2 = this.state.optJSONObject("saved_options");
        if (jSONObjectOptJSONObject2 == null || !jSONObjectOptJSONObject2.has(str)) {
            return jSONObjectOptJSONObject == null ? z : jSONObjectOptJSONObject.optBoolean(str, z);
        }
        return jSONObjectOptJSONObject2.optBoolean(str, z);
    }

    private void toggle(String str, String str2, boolean z, final Pick pick) {
        LinearLayout linearLayoutRow = row(str, str2);
        final CareSwitch careSwitch = new CareSwitch(this, this.accent, this.surface, this.secondary);
        careSwitch.setContentDescription(str);
        careSwitch.setShowText(false);
        careSwitch.setChecked(z);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = dp(18.0f);
        linearLayoutRow.addView(careSwitch, layoutParams);
        final boolean[] zArr = {false};
        careSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda88
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                this.f$0.m93lambda$toggle$16$nihilsunshinepartsMainActivity(zArr, careSwitch, pick, compoundButton, z2);
            }
        });
        clickable(linearLayoutRow, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda89
            @Override // java.lang.Runnable
            public final void run() {
                Switch r0 = careSwitch;
                r0.setChecked(!r0.isChecked());
            }
        });
    }

    /* JADX INFO: renamed from: lambda$toggle$16$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m93lambda$toggle$16$nihilsunshinepartsMainActivity(boolean[] zArr, Switch r3, Pick pick, CompoundButton compoundButton, boolean z) {
        if (zArr[0]) {
            return;
        }
        if (this.busy || this.detailsLoading) {
            zArr[0] = true;
            r3.setChecked(true ^ z);
            zArr[0] = false;
            return;
        }
        pick.run(z ? "1" : "0");
    }

    private void moduleSwitch(final String str, String str2, final String str3, boolean z) {
        if ("gpu_blur".equals(str3) || "compose_layout".equals(str3) || "compose_host".equals(str3)) {
            str2 = "Experimental integration unavailable in this build.";
        }
        toggle(str, str2, option(str3, z), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda90
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str4) {
                this.f$0.m53lambda$moduleSwitch$19$nihilsunshinepartsMainActivity(str, str3, str4);
            }
        });
        if (VisualPolicy.managed(str3, visualLevel())) {
            lockRow(this.lastRow);
        }
    }

    /* JADX INFO: renamed from: lambda$moduleSwitch$19$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m53lambda$moduleSwitch$19$nihilsunshinepartsMainActivity(final String str, final String str2, final String str3) {
        runToggle(str, "1".equals(str3), new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda62
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m52lambda$moduleSwitch$18$nihilsunshinepartsMainActivity(str3, str, str2);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$moduleSwitch$18$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m52lambda$moduleSwitch$18$nihilsunshinepartsMainActivity(String str, String str2, String str3) throws Exception {
        boolean zEquals = str.equals("1");
        String strDisplayPolicy = I18n.format(this, zEquals ? "%1$s enabled." : "%1$s disabled.", I18n.text(this, str2));
        if ("refresh".equals(str3)) {
            strDisplayPolicy = this.ops.displayPolicy(zEquals);
        } else {
            this.ops.flag(str3, zEquals);
        }
        JSONObject jSONObjectReload = this.ops.reload();
        if (jSONObjectReload.getJSONObject("options").getBoolean(str3) != zEquals) {
            throw new Exception("SystemUI did not confirm this setting.");
        }
        this.state = jSONObjectReload;
        if ("refresh".equals(str3)) {
            this.info = this.ops.info();
            JSONObject jSONObjectOptJSONObject = jSONObjectReload.optJSONObject("framework_status");
            strDisplayPolicy = strDisplayPolicy + "\nFramework: " + (jSONObjectOptJSONObject == null ? "Scope not connected. Check LSPosed and restart." : jSONObjectOptJSONObject.optString("display_policy_state", "Awaiting display update"));
            this.info.put("display_policy_status", strDisplayPolicy);
        }
        return "recovery_mode".equals(str3) ? I18n.text(this, "Restart to apply the runtime mode.") : strDisplayPolicy;
    }

    private void lockRow(View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(false);
        view.setClickable(false);
        view.setFocusable(false);
        view.setAlpha(0.45f);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableChild(viewGroup.getChildAt(i));
            }
        }
    }

    private void disableChild(View view) {
        view.setEnabled(false);
        view.setClickable(false);
        view.setFocusable(false);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableChild(viewGroup.getChildAt(i));
            }
        }
    }

    private void note(String str) {
        TextView textViewText = text(str, 13, this.secondary);
        textViewText.setPadding(dp(24.0f), dp(4.0f), dp(24.0f), dp(14.0f));
        this.body.addView(textViewText);
        this.card = null;
    }

    private String gib(long j) {
        return String.format(Locale.US, "%.1f", Float.valueOf(j / 1048576.0f));
    }

    private CareState care(JSONObject jSONObject) {
        return this.state.optBoolean("recovery_mode") ? CareState.recovery() : CareState.temperature(this.state.optBoolean("active"), jSONObject.optInt("battery_temperature", -1), SystemClock.elapsedRealtime(), this.ops.backup.getLong("last_optimize_elapsed", 0L));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: updateHealth, reason: merged with bridge method [inline-methods] */
    public void m54lambda$new$0$nihilsunshinepartsMainActivity() {
        if (this.screen != 0 || this.healthTitle == null || this.healthSummary == null || this.healthAction == null) {
            return;
        }
        CareState careStateCare = care(this.info);
        boolean z = (getResources().getConfiguration().uiMode & 48) == 32;
        this.healthTitle.setText(careStateCare.title);
        int iStatusColor = statusColor(careStateCare, z);
        this.healthTitle.setTextColor(iStatusColor);
        if (this.ambient != null) {
            this.ambient.status(iStatusColor);
        }
        if (this.healthMaterial != null) {
            this.healthMaterial.status(iStatusColor);
        }
        this.healthSummary.setText(careStateCare.summary);
        this.healthAction.setVisibility(careStateCare.optimize ? 0 : 8);
        this.main.removeCallbacks(this.refreshHealth);
        long j = this.ops.backup.getLong("last_optimize_elapsed", 0L);
        long jElapsedRealtime = 45000 - (SystemClock.elapsedRealtime() - j);
        if (j <= 0 || jElapsedRealtime <= 0 || jElapsedRealtime > 45000) {
            return;
        }
        this.main.postDelayed(this.refreshHealth, jElapsedRealtime + 50);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAura() {
        if (this.ambient == null || this.healthView == null || this.pageRoot == null || this.activeScroll == null) {
            return;
        }
        this.ambient.focus(this.pageRoot.getWidth() * 0.5f, ((this.pageRoot.getPaddingTop() + this.healthView.getTop()) - this.activeScroll.getScrollY()) + (this.healthView.getHeight() * 0.87f), Math.max(1.0f, this.healthView.getWidth() * 0.57f));
    }

    private void home() {
        CareState careStateCare = care(this.info);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setPadding(dp(24.0f), dp(26.0f), dp(24.0f), dp(26.0f));
        this.healthMaterial = new CareArt.HealthMaterial(this.surface, dp(28.0f));
        linearLayout.setBackground(this.healthMaterial);
        this.healthView = linearLayout;
        linearLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda78
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                this.f$0.m33lambda$home$20$nihilsunshinepartsMainActivity(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        TextView textViewText = text(careStateCare.title, 32, statusColor(careStateCare, (getResources().getConfiguration().uiMode & 48) == 32));
        textViewText.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
        textViewText.setGravity(17);
        linearLayout.addView(textViewText);
        TextView textViewText2 = text(careStateCare.summary, 15, this.secondary);
        textViewText2.setGravity(17);
        textViewText2.setPadding(0, dp(12.0f), 0, 0);
        linearLayout.addView(textViewText2);
        this.healthTitle = textViewText;
        this.healthSummary = textViewText2;
        LocalizedButton localizedButton = new LocalizedButton(this);
        localizedButton.setText("Optimize now");
        localizedButton.setAllCaps(false);
        localizedButton.setTextSize(18.0f);
        localizedButton.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
        localizedButton.setTextColor(CareArt.blend(this.background, this.ink, 0.04f));
        localizedButton.setBackground(new RippleDrawable(ColorStateList.valueOf(855638016), shape(this.accent, 100), null));
        localizedButton.setMinWidth(dp(190.0f));
        localizedButton.setOnClickListener(new View.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda79
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m34lambda$home$21$nihilsunshinepartsMainActivity(view);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, dp(56.0f));
        layoutParams.topMargin = dp(18.0f);
        linearLayout.addView(localizedButton, layoutParams);
        this.healthAction = localizedButton;
        m54lambda$new$0$nihilsunshinepartsMainActivity();
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.bottomMargin = dp(20.0f);
        this.body.addView(linearLayout, layoutParams2);
        this.card = null;
        long jOptLong = this.info.optLong("total_kb");
        long jOptLong2 = this.info.optLong("available_kb");
        float f = jOptLong > 0 ? 1.0f - (jOptLong2 / jOptLong) : 0.0f;
        metric("Memory", jOptLong > 0 ? Math.round(100.0f * f) + "% used" : "Not reported", gib(jOptLong - jOptLong2) + " GB / " + gib(jOptLong) + " GB", f, 2, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda80
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m35lambda$home$22$nihilsunshinepartsMainActivity();
            }
        });
        destination("Performance", modeName() + " visual workload", 0, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda81
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m36lambda$home$23$nihilsunshinepartsMainActivity();
            }
        });
        destination("Customization", "Visual effects and advanced settings", 1, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda82
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m37lambda$home$24$nihilsunshinepartsMainActivity();
            }
        });
        section("Additional care");
        link("Module status", "Panel connection", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda83
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m38lambda$home$25$nihilsunshinepartsMainActivity();
            }
        });
        link("Module report", "Panel activity and display settings", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda84
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m39lambda$home$26$nihilsunshinepartsMainActivity();
            }
        });
        link("Support", "Get help and project updates", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda85
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m40lambda$home$27$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$home$20$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m33lambda$home$20$nihilsunshinepartsMainActivity(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updateAura();
    }

    /* JADX INFO: renamed from: lambda$home$21$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m34lambda$home$21$nihilsunshinepartsMainActivity(View view) {
        if (this.busy) {
            return;
        }
        optimizeNow();
    }

    /* JADX INFO: renamed from: lambda$home$22$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m35lambda$home$22$nihilsunshinepartsMainActivity() {
        show(3);
    }

    /* JADX INFO: renamed from: lambda$home$23$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m36lambda$home$23$nihilsunshinepartsMainActivity() {
        show(2);
    }

    /* JADX INFO: renamed from: lambda$home$24$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m37lambda$home$24$nihilsunshinepartsMainActivity() {
        show(9);
    }

    /* JADX INFO: renamed from: lambda$home$25$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m38lambda$home$25$nihilsunshinepartsMainActivity() {
        show(5);
    }

    /* JADX INFO: renamed from: lambda$home$26$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m39lambda$home$26$nihilsunshinepartsMainActivity() {
        show(12);
    }

    /* JADX INFO: renamed from: lambda$home$27$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m40lambda$home$27$nihilsunshinepartsMainActivity() {
        entrance(false);
    }

    private void destination(String str, String str2, int i, Runnable runnable) {
        LinearLayout linearLayoutRow = row(str, str2);
        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(new CareArt.Glyph(i, this.secondary, dp(20.0f)));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(20.0f), dp(20.0f));
        layoutParams.rightMargin = dp(12.0f);
        linearLayoutRow.addView(imageView, 0, layoutParams);
        clickable(linearLayoutRow, runnable);
    }

    private void personalize() {
        section("Language");
        final LocaleManager localeManager = (LocaleManager) getSystemService(LocaleManager.class);
        String languageTags = localeManager.getApplicationLocales().toLanguageTags();
        final String str = "es";
        if (languageTags.startsWith("pt")) {
            str = "pt";
        } else if (languageTags.startsWith("tr")) {
            str = "tr";
        } else if (!languageTags.startsWith("es")) {
            str = languageTags.startsWith("en") ? "en" : "";
        }
        final String[] strArr = {"System default", "English", "Português", "Türkçe", "Español"};
        final String[] strArr2 = {"", "en", "pt", "tr", "es"};
        link("App language", strArr[Arrays.asList(strArr2).indexOf(str)], new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m74lambda$personalize$29$nihilsunshinepartsMainActivity(strArr, strArr2, str, localeManager);
            }
        });
        visual();
    }

    /* JADX INFO: renamed from: lambda$personalize$29$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m74lambda$personalize$29$nihilsunshinepartsMainActivity(String[] strArr, String[] strArr2, String str, final LocaleManager localeManager) {
        choose("App language", strArr, strArr2, str, new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda32
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str2) {
                localeManager.setApplicationLocales(LocaleList.forLanguageTags(str2));
            }
        });
    }

    private int statusColor(CareState careState, boolean z) {
        if (careState.kind == 0) {
            return -12019201;
        }
        if (careState.kind == 1) {
            return z ? -11480196 : -15301304;
        }
        if ("Hot".equals(careState.title)) {
            return z ? -39579 : -3790808;
        }
        if (careState.kind == 2) {
            return z ? -17304 : -6202624;
        }
        return this.secondary;
    }

    private void policyLimits() {
        section("CPU activity");
        this.cpuChart = new CpuHistoryView(this, this.ink, this.secondary, this.accent, this.surface);
        group().addView(this.cpuChart, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout linearLayoutRow = row("CPU cluster", "");
        this.cpuCluster = text("", 14, this.accent);
        linearLayoutRow.addView(this.cpuCluster);
        clickable(linearLayoutRow, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda60
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m76lambda$policyLimits$31$nihilsunshinepartsMainActivity();
            }
        });
        LinearLayout linearLayoutRow2 = row("Current limit", "");
        TextView textView = (TextView) ((LinearLayout) linearLayoutRow2.getChildAt(0)).getChildAt(0);
        textView.setTextColor(this.secondary);
        textView.setTextSize(15.0f);
        this.cpuLimit = text("—", 16, this.ink);
        linearLayoutRow2.addView(this.cpuLimit);
        this.cpuStatus = text("", 13, this.secondary);
        this.cpuStatus.setPadding(dp(24.0f), dp(8.0f), dp(24.0f), dp(10.0f));
        this.cpuStatus.setVisibility(8);
        this.body.addView(this.cpuStatus);
        this.card = null;
        note("Each point pairs CPU load with the reported frequency. The highlighted point is the latest reading.");
        note("Updates every 200 ms while this page remains open, including in the background. Leave this page to stop monitoring.");
        updateCpuChart();
    }

    /* JADX INFO: renamed from: lambda$policyLimits$31$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m76lambda$policyLimits$31$nihilsunshinepartsMainActivity() {
        if (this.cpuPolicies.isEmpty()) {
            return;
        }
        int size = this.cpuPolicies.size();
        String[] strArr = new String[size];
        String[] strArr2 = new String[size];
        for (int i = 0; i < size; i++) {
            strArr[i] = CpuClusters.name(this.cpuPolicies, this.cpuPolicies.get(i));
            strArr2[i] = this.cpuPolicies.get(i).name;
        }
        choose("CPU cluster", strArr, strArr2, this.selectedCpu, new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda92
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m75lambda$policyLimits$30$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$policyLimits$30$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m75lambda$policyLimits$30$nihilsunshinepartsMainActivity(String str) {
        this.selectedCpu = str;
        updateCpuChart();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCpuChart() {
        if (this.screen != 10 || this.cpuChart == null) {
            return;
        }
        CpuPolicy cpuPolicy = null;
        for (CpuPolicy cpuPolicy2 : this.cpuPolicies) {
            if (cpuPolicy2.name.equals(this.selectedCpu)) {
                cpuPolicy = cpuPolicy2;
            }
        }
        if (cpuPolicy == null && !this.cpuPolicies.isEmpty()) {
            cpuPolicy = this.cpuPolicies.get(0);
            this.selectedCpu = cpuPolicy.name;
        }
        if (cpuPolicy != null) {
            if (this.cpuCluster != null) {
                this.cpuCluster.setText(CpuClusters.name(this.cpuPolicies, cpuPolicy));
            }
            if (this.cpuLimit != null) {
                this.cpuLimit.setText(CpuPolicy.ghz(cpuPolicy.maximum));
            }
        }
        this.cpuChart.readings(this.cpuHistory, this.selectedCpu);
    }

    private void startCpuMonitor() {
        if (!this.foreground || this.screen != 10 || this.ops == null || this.cpuChart == null || isFinishing() || this.cpuBound) {
            return;
        }
        try {
            Intent intent = new Intent(this, (Class<?>) CpuMonitorService.class);
            startForegroundService(intent);
            this.cpuBound = bindService(intent, this.cpuConnection, 1);
            if (!this.cpuBound) {
                stopService(intent);
            }
        } catch (RuntimeException e) {
            this.cpuBound = false;
            if (this.cpuStatus != null) {
                this.cpuStatus.setText("Couldn't start CPU monitoring.");
                this.cpuStatus.setVisibility(0);
            }
        }
    }

    private void detachCpuMonitor() {
        if (this.cpuService != null) {
            this.cpuService.remove(this.cpuObserver);
            this.cpuService = null;
        }
        if (this.cpuBound) {
            this.cpuBound = false;
            try {
                unbindService(this.cpuConnection);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void stopCpuMonitor() {
        detachCpuMonitor();
        stopService(new Intent(this, (Class<?>) CpuMonitorService.class));
    }

    private void ensureDetails(final int i) {
        if (this.ops == null || this.busy || this.detailsLoading) {
            return;
        }
        if (i != 2 && i != 3 && i != 4 && i != 7) {
            return;
        }
        long j = i == 10 ? this.policyAt : this.detailsAt;
        if (j <= 0 || SystemClock.elapsedRealtime() - j >= 30000) {
            this.detailsLoading = true;
            final int iBeginLoading = beginLoading();
            this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda86
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m32lambda$ensureDetails$33$nihilsunshinepartsMainActivity(i, iBeginLoading);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$ensureDetails$33$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m32lambda$ensureDetails$33$nihilsunshinepartsMainActivity(final int i, final int i2) {
        String message;
        JSONObject jSONObjectInfo;
        try {
            jSONObjectInfo = this.ops.info(i == 10);
            message = "";
        } catch (Exception e) {
            message = e.getMessage() == null ? "Couldn't read settings" : e.getMessage();
            jSONObjectInfo = null;
        }
        final JSONObject jSONObject = jSONObjectInfo;
        final String str = message;
        runOnUiThread(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda42
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m31lambda$ensureDetails$32$nihilsunshinepartsMainActivity(i2, i, jSONObject, str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$ensureDetails$32$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m31lambda$ensureDetails$32$nihilsunshinepartsMainActivity(int i, int i2, JSONObject jSONObject, String str) {
        this.detailsLoading = false;
        endLoading(i);
        if (isFinishing() || isDestroyed()) {
            return;
        }
        this.detailsAt = SystemClock.elapsedRealtime();
        if (i2 == 10) {
            this.policyAt = this.detailsAt;
        }
        if (jSONObject != null) {
            this.info = jSONObject;
        }
        if (this.screen == i2) {
            show(i2);
            if (!str.isEmpty()) {
                showError(str);
            }
        }
    }

    private void metric(String str, String str2, String str3, float f, int i, Runnable runnable) {
        LinearLayout linearLayoutGroup = group();
        if (linearLayoutGroup.getChildCount() > 0) {
            View view = new View(this);
            view.setBackgroundColor(this.divider);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(0.5f));
            layoutParams.setMargins(dp(20.0f), 0, dp(20.0f), 0);
            linearLayoutGroup.addView(view, layoutParams);
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(20.0f), dp(18.0f), dp(20.0f), dp(18.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(16);
        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(new CareArt.Glyph(i, this.secondary, dp(20.0f)));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(20.0f), dp(20.0f));
        layoutParams2.rightMargin = dp(12.0f);
        linearLayout2.addView(imageView, layoutParams2);
        linearLayout2.addView(text(str, 17, this.ink));
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setGravity(16);
        linearLayout3.setPadding(0, dp(15.0f), 0, dp(9.0f));
        TextView textViewText = text(str2, 16, i == 0 ? this.accent : this.ink);
        textViewText.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
        linearLayout3.addView(textViewText, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textViewText2 = text(str3, 13, this.ink);
        textViewText2.setGravity(8388613);
        linearLayout3.addView(textViewText2, new LinearLayout.LayoutParams(-2, -2));
        linearLayout.addView(linearLayout3);
        linearLayout.addView(new CareArt.Meter(this, f, this.accent, this.surface), new LinearLayout.LayoutParams(-1, dp(10.0f)));
        clickable(linearLayout, runnable);
        linearLayoutGroup.addView(linearLayout, new LinearLayout.LayoutParams(-1, -2));
    }

    private void optimizeNow() {
        if (this.busy || this.detailsLoading) {
            return;
        }
        if (this.activeScroll != null) {
            this.scrollOffsets[this.screen] = this.activeScroll.getScrollY();
        }
        new OptimizationSession().start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class OptimizationSession {
        final CareOptimizeArt art;
        final String[] checking = {"Checking module connection…", "Checking visual profile…", "Checking memory…", "Checking diagnostic logging…"};
        final Button done;
        final int generation;
        final TextView heading;
        final LinearLayout hero;
        final LinearLayout list;
        final TextView summary;

        OptimizationSession() {
            int scrollY = MainActivity.this.activeScroll == null ? 0 : MainActivity.this.activeScroll.getScrollY();
            MainActivity.this.busy = true;
            MainActivity.access$1608(MainActivity.this);
            this.generation = MainActivity.this.pageGeneration;
            MainActivity.this.healthTitle = null;
            MainActivity.this.healthSummary = null;
            MainActivity.this.healthAction = null;
            MainActivity.this.main.removeCallbacks(MainActivity.this.refreshHealth);
            boolean z = (MainActivity.this.getResources().getConfiguration().uiMode & 48) == 32;
            final LinearLayout linearLayout = new LinearLayout(MainActivity.this);
            linearLayout.setOrientation(1);
            MainActivity.this.pageRoot = linearLayout;
            MainActivity.this.ambient = new CareArt.Ambient(MainActivity.this.background, MainActivity.this.accent);
            MainActivity.this.ambient.status(MainActivity.this.selectedAccent);
            linearLayout.setBackground(MainActivity.this.ambient);
            final CareHeader careHeader = new CareHeader(MainActivity.this, new Toolbar(MainActivity.this), "Sunshine Parts", MainActivity.this.background, MainActivity.this.accent, MainActivity.this.floatingSurface);
            FrameLayout frameLayout = new FrameLayout(MainActivity.this);
            final CareScrollView careScrollView = new CareScrollView(MainActivity.this, careHeader.expandedHeight - careHeader.collapsedHeight);
            MainActivity.this.activeScroll = careScrollView;
            careScrollView.setFillViewport(true);
            careScrollView.setClipToPadding(false);
            careScrollView.setVerticalScrollBarEnabled(false);
            careScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda4
                @Override // android.view.View.OnScrollChangeListener
                public final void onScrollChange(View view, int i, int i2, int i3, int i4) {
                    this.f$0.m103x2d488e36(careHeader, view, i, i2, i3, i4);
                }
            });
            MainActivity.this.body = new LinearLayout(MainActivity.this);
            MainActivity.this.body.setOrientation(1);
            MainActivity.this.body.setPadding(MainActivity.this.dp(12.0f), 0, MainActivity.this.dp(12.0f), MainActivity.this.dp(12.0f));
            MainActivity.this.body.addView(new View(MainActivity.this), new LinearLayout.LayoutParams(-1, careHeader.expandedHeight + MainActivity.this.dp(8.0f)));
            careScrollView.addView(MainActivity.this.body);
            frameLayout.addView(careScrollView, new FrameLayout.LayoutParams(-1, -1));
            frameLayout.addView(careHeader, new FrameLayout.LayoutParams(-1, careHeader.expandedHeight, 48));
            linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            this.hero = new LinearLayout(MainActivity.this);
            this.hero.setOrientation(1);
            this.hero.setGravity(17);
            this.hero.setPadding(MainActivity.this.dp(20.0f), MainActivity.this.dp(12.0f), MainActivity.this.dp(20.0f), MainActivity.this.dp(20.0f));
            MainActivity.this.healthMaterial = new CareArt.HealthMaterial(MainActivity.this.surface, MainActivity.this.dp(28.0f));
            MainActivity.this.healthMaterial.status(MainActivity.this.selectedAccent);
            this.hero.setBackground(MainActivity.this.healthMaterial);
            MainActivity.this.healthView = this.hero;
            FrameLayout frameLayout2 = new FrameLayout(MainActivity.this);
            this.art = new CareOptimizeArt(MainActivity.this, MainActivity.this.selectedAccent, MainActivity.this.surface, z);
            frameLayout2.addView(this.art, new FrameLayout.LayoutParams(-1, -1));
            this.hero.addView(frameLayout2, new LinearLayout.LayoutParams(MainActivity.this.dp(132.0f), MainActivity.this.dp(132.0f)));
            this.heading = MainActivity.this.text("Optimizing", 27, MainActivity.this.selectedAccent);
            this.heading.setGravity(17);
            this.heading.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
            this.heading.setVisibility(0);
            this.hero.addView(this.heading, new LinearLayout.LayoutParams(-1, -2));
            this.summary = MainActivity.this.text("The module is ready to use.", 13, MainActivity.this.secondary);
            this.summary.setGravity(17);
            this.summary.setPadding(MainActivity.this.dp(6.0f), MainActivity.this.dp(7.0f), MainActivity.this.dp(6.0f), 0);
            this.summary.setVisibility(4);
            this.hero.addView(this.summary, new LinearLayout.LayoutParams(-1, -2));
            this.hero.setContentDescription(I18n.text(MainActivity.this, "Optimizing"));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.bottomMargin = MainActivity.this.dp(16.0f);
            MainActivity.this.body.addView(this.hero, layoutParams);
            this.list = new LinearLayout(MainActivity.this);
            this.list.setOrientation(1);
            this.list.setBackground(MainActivity.this.shape(MainActivity.this.surface, 28));
            this.list.setClipToOutline(true);
            MainActivity.this.body.addView(this.list, new LinearLayout.LayoutParams(-1, -2));
            MainActivity.this.bottomActions = new LinearLayout(MainActivity.this);
            MainActivity.this.bottomActions.setOrientation(1);
            MainActivity.this.bottomActions.setGravity(1);
            MainActivity.this.bottomActions.setVisibility(8);
            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{MainActivity.this.background & 16777215, (MainActivity.this.background & 16777215) | 1879048192, (MainActivity.this.background & 16777215) | (-402653184), MainActivity.this.background});
            gradientDrawable.setColors(new int[]{MainActivity.this.background & 16777215, (MainActivity.this.background & 16777215) | 1879048192, (MainActivity.this.background & 16777215) | (-402653184), MainActivity.this.background}, new float[]{0.0f, 0.38f, 0.72f, 1.0f});
            MainActivity.this.bottomActions.setBackground(gradientDrawable);
            this.done = new LocalizedButton(MainActivity.this);
            this.done.setText("Finish");
            this.done.setAllCaps(false);
            this.done.setTextSize(18.0f);
            this.done.setTextColor(-1);
            this.done.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
            this.done.setBackground(new RippleDrawable(ColorStateList.valueOf(855638016), MainActivity.this.shape(MainActivity.this.accent, 100), null));
            this.done.setMinWidth(MainActivity.this.dp(216.0f));
            this.done.setOnClickListener(new View.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda5
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m104x569ce377(view);
                }
            });
            MainActivity.this.bottomActions.addView(this.done, new LinearLayout.LayoutParams(-2, MainActivity.this.dp(52.0f)));
            frameLayout.addView(MainActivity.this.bottomActions, new FrameLayout.LayoutParams(-1, -2, 80));
            linearLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda6
                @Override // android.view.View.OnApplyWindowInsetsListener
                public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    return this.f$0.m105x7ff138b8(linearLayout, view, windowInsets);
                }
            });
            MainActivity.this.setContentView(linearLayout);
            I18n.apply(MainActivity.this, linearLayout);
            linearLayout.requestApplyInsets();
            final int iMin = Math.min(scrollY, careHeader.expandedHeight - careHeader.collapsedHeight);
            careScrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // from class: nihil.sunshine.parts.MainActivity.OptimizationSession.1
                @Override // android.view.ViewTreeObserver.OnPreDrawListener
                public boolean onPreDraw() {
                    int height = (careScrollView.getHeight() + careHeader.expandedHeight) - careHeader.collapsedHeight;
                    if (MainActivity.this.body.getMinimumHeight() != height) {
                        MainActivity.this.body.setMinimumHeight(height);
                        return false;
                    }
                    careScrollView.getViewTreeObserver().removeOnPreDrawListener(this);
                    careScrollView.scrollTo(0, iMin);
                    careHeader.scroll(careScrollView.getScrollY());
                    OptimizationSession.this.aura();
                    return true;
                }
            });
            this.hero.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda7
                @Override // android.view.View.OnLayoutChangeListener
                public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    this.f$0.m106xa9458df9(view, i, i2, i3, i4, i5, i6, i7, i8);
                }
            });
            this.hero.post(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.aura();
                }
            });
        }

        /* JADX INFO: renamed from: lambda$new$0$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m103x2d488e36(CareHeader careHeader, View view, int i, int i2, int i3, int i4) {
            careHeader.scroll(i2);
            aura();
        }

        /* JADX INFO: renamed from: lambda$new$1$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m104x569ce377(View view) {
            MainActivity.this.activeScroll = null;
            MainActivity.this.show(0);
        }

        /* JADX INFO: renamed from: lambda$new$2$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ WindowInsets m105x7ff138b8(LinearLayout linearLayout, View view, WindowInsets windowInsets) {
            Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            linearLayout.setPadding(insets.left, insets.top, insets.right, 0);
            MainActivity.this.body.setPadding(MainActivity.this.dp(12.0f), 0, MainActivity.this.dp(12.0f), MainActivity.this.bottomActions.getVisibility() == 0 ? insets.bottom + MainActivity.this.dp(132.0f) : Math.max(MainActivity.this.dp(16.0f), insets.bottom + MainActivity.this.dp(8.0f)));
            MainActivity.this.bottomActions.setPadding(MainActivity.this.dp(24.0f), MainActivity.this.dp(36.0f), MainActivity.this.dp(24.0f), Math.max(MainActivity.this.dp(16.0f), insets.bottom + MainActivity.this.dp(8.0f)));
            return windowInsets;
        }

        /* JADX INFO: renamed from: lambda$new$3$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m106xa9458df9(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            aura();
        }

        void aura() {
            if (!alive() || MainActivity.this.ambient == null) {
                return;
            }
            MainActivity.this.ambient.focus(MainActivity.this.pageRoot.getWidth() * 0.5f, (((MainActivity.this.pageRoot.getPaddingTop() + MainActivity.this.activeScroll.getTop()) + MainActivity.this.healthView.getTop()) - MainActivity.this.activeScroll.getScrollY()) + (MainActivity.this.healthView.getHeight() * 0.87f), Math.max(1.0f, MainActivity.this.healthView.getWidth() * 0.57f));
        }

        boolean alive() {
            return (MainActivity.this.isFinishing() || MainActivity.this.isDestroyed() || MainActivity.this.pageGeneration != this.generation) ? false : true;
        }

        void start() {
            step(0);
        }

        void step(final int i) {
            int i2;
            if (alive()) {
                LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                linearLayout.setGravity(16);
                linearLayout.setMinimumHeight(MainActivity.this.dp(52.0f));
                linearLayout.setPadding(MainActivity.this.dp(20.0f), MainActivity.this.dp(8.0f), MainActivity.this.dp(20.0f), MainActivity.this.dp(8.0f));
                ImageView imageView = new ImageView(MainActivity.this);
                if (i == 1) {
                    i2 = 0;
                } else {
                    i2 = 3;
                    if (i != 3) {
                        i2 = 2;
                    }
                }
                imageView.setImageDrawable(new CareArt.Glyph(i2, MainActivity.this.secondary, MainActivity.this.dp(18.0f)));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MainActivity.this.dp(18.0f), MainActivity.this.dp(18.0f));
                layoutParams.rightMargin = MainActivity.this.dp(8.0f);
                linearLayout.addView(imageView, layoutParams);
                final TextView textViewText = MainActivity.this.text(this.checking[i], 16, MainActivity.this.selectedAccent);
                textViewText.setMaxLines(Integer.MAX_VALUE);
                textViewText.setBreakStrategy(0);
                textViewText.setHyphenationFrequency(0);
                textViewText.setLineSpacing(0.0f, 1.0f);
                textViewText.setGravity(16);
                linearLayout.addView(textViewText, new LinearLayout.LayoutParams(0, -2, 1.0f));
                FrameLayout frameLayout = new FrameLayout(MainActivity.this);
                final ProgressBar progressBarSpinner = MainActivity.this.spinner();
                frameLayout.addView(progressBarSpinner, new FrameLayout.LayoutParams(MainActivity.this.dp(22.0f), MainActivity.this.dp(22.0f), 17));
                final CareProgress careProgress = new CareProgress(MainActivity.this, 2, MainActivity.this.selectedAccent);
                careProgress.setVisibility(8);
                frameLayout.addView(careProgress, new FrameLayout.LayoutParams(-1, -1));
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(MainActivity.this.dp(24.0f), MainActivity.this.dp(24.0f));
                layoutParams2.leftMargin = MainActivity.this.dp(8.0f);
                linearLayout.addView(frameLayout, layoutParams2);
                this.list.addView(linearLayout, new LinearLayout.LayoutParams(-1, -2));
                appear(linearLayout, 0L);
                this.art.progress((i + 0.15f) / 4.0f);
                final long jUptimeMillis = SystemClock.uptimeMillis();
                MainActivity.this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m109x9fb97f43(i, textViewText, progressBarSpinner, careProgress, jUptimeMillis);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$step$7$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m109x9fb97f43(final int i, final TextView textView, final ProgressBar progressBar, final CareProgress careProgress, long j) {
            String message;
            boolean z;
            try {
                synchronized (SerialAccess.mutationLock()) {
                    if (!SerialAccess.usable(MainActivity.this)) {
                        throw new SerialAccess.Denied();
                    }
                    message = check(i);
                    z = true;
                    MainActivity.this.ops.snapshotAuthorizationTargets(true);
                    MainActivity.this.ops.checkpointAccessBaseline();
                }
            } catch (SerialAccess.Denied e) {
                Handler handler = MainActivity.this.main;
                final MainActivity mainActivity = MainActivity.this;
                handler.post(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        mainActivity.closeDeniedAccess();
                    }
                });
                return;
            } catch (Exception e2) {
                message = e2.getMessage() == null ? "Couldn't complete optimization" : e2.getMessage();
                z = false;
            }
            final boolean z2 = z;
            final String str = message;
            MainActivity.this.main.postDelayed(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m108x76652a02(z2, textView, str, progressBar, careProgress, i);
                }
            }, Math.max(0L, 620 - (SystemClock.uptimeMillis() - j)));
        }

        /* JADX INFO: renamed from: lambda$step$6$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m108x76652a02(boolean z, TextView textView, String str, ProgressBar progressBar, CareProgress careProgress, final int i) {
            if (alive()) {
                if (!z) {
                    textView.setMaxLines(Integer.MAX_VALUE);
                }
                textView.setText(str);
                progressBar.setVisibility(8);
                careProgress.setVisibility(0);
                careProgress.stage(z ? 2 : 3);
                this.art.progress((i + 1) / 4.0f);
                if (!z) {
                    finish(false, str);
                } else if (i < 3) {
                    MainActivity.this.main.postDelayed(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda9
                        @Override // java.lang.Runnable
                        public final void run() {
                            this.f$0.m107x4d10d4c1(i);
                        }
                    }, 150L);
                } else {
                    finish(true, "");
                }
            }
        }

        /* JADX INFO: renamed from: lambda$step$5$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m107x4d10d4c1(int i) {
            step(i + 1);
        }

        String check(int i) throws Exception {
            switch (i) {
                case 0:
                    MainActivity.this.state = MainActivity.this.ops.control("com.android.systemui", "status");
                    if (MainActivity.this.state.optBoolean("active")) {
                        return "Module connected and active in the system interface";
                    }
                    throw new Exception(I18n.text(MainActivity.this, "Check module status"));
                case 1:
                    if (MainActivity.this.state.optBoolean("recovery_mode")) {
                        return "Native interface preserved with module effects paused";
                    }
                    return MainActivity.this.visualLevel() == 0 ? "Visual settings checked for the selected workload" : I18n.format(MainActivity.this, "Profile %1$s applied with lighter visual detail", I18n.text(MainActivity.this, MainActivity.this.modeName()));
                case 2:
                    if (MainActivity.this.state.optBoolean("recovery_mode")) {
                        MainActivity.this.ops.quickInfo();
                        return "Memory checked with automatic cleanup paused";
                    }
                    JSONObject jSONObjectQuickInfo = MainActivity.this.ops.quickInfo();
                    if (CareState.memoryPressure(jSONObjectQuickInfo.optLong("available_kb", -1L), jSONObjectQuickInfo.optLong("total_kb"), jSONObjectQuickInfo.optLong("memory_threshold_kb"), jSONObjectQuickInfo.optBoolean("low_memory"))) {
                        MainActivity.this.ops.ram();
                        MainActivity.this.ops.backup.edit().putLong("last_clear_elapsed", SystemClock.elapsedRealtime()).apply();
                        return "Cached app cleanup requested";
                    }
                    return "Memory available without closing background apps";
                default:
                    if (MainActivity.this.state.optBoolean("recovery_mode")) {
                        return "Current logging settings kept";
                    }
                    if (MainActivity.this.ops.backup.getBoolean("reduced_logging", true)) {
                        MainActivity.this.ops.ensureReducedLogging();
                    }
                    MainActivity.this.info = MainActivity.this.ops.quickInfo();
                    MainActivity.this.ops.backup.edit().putLong("last_optimize_elapsed", SystemClock.elapsedRealtime()).apply();
                    return MainActivity.this.ops.backup.getBoolean("reduced_logging", true) ? "Repetitive diagnostic messages reduced for this session" : "Current logging settings kept";
            }
        }

        void finish(final boolean z, final String str) {
            this.art.complete(z);
            this.hero.setContentDescription(null);
            MainActivity.this.main.postDelayed(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$OptimizationSession$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m102xde775e0b(z, str);
                }
            }, 650L);
        }

        /* JADX INFO: renamed from: lambda$finish$8$nihil-sunshine-parts-MainActivity$OptimizationSession, reason: not valid java name */
        /* synthetic */ void m102xde775e0b(boolean z, String str) {
            String str2;
            String str3;
            int i;
            if (alive()) {
                TextView textView = this.heading;
                if (z) {
                    str2 = MainActivity.this.state.optBoolean("recovery_mode") ? "Recovery checks complete" : "Optimized";
                } else {
                    str2 = "Needs attention";
                }
                textView.setText(str2);
                TextView textView2 = this.summary;
                if (z) {
                    str3 = MainActivity.this.state.optBoolean("recovery_mode") ? "Native interface active. Module effects are paused." : "The module is ready to use.";
                } else {
                    str3 = "Some checks could not be completed.";
                }
                textView2.setText(str3);
                int i2 = -21166;
                if (!z) {
                    this.heading.setTextColor(-21166);
                }
                if (MainActivity.this.healthMaterial != null) {
                    CareArt.HealthMaterial healthMaterial = MainActivity.this.healthMaterial;
                    if (z) {
                        i = MainActivity.this.selectedAccent;
                    } else {
                        i = -21166;
                    }
                    healthMaterial.status(i);
                }
                if (MainActivity.this.ambient != null) {
                    CareArt.Ambient ambient = MainActivity.this.ambient;
                    if (z) {
                        i2 = MainActivity.this.selectedAccent;
                    }
                    ambient.status(i2);
                }
                appear(this.heading, 0L);
                appear(this.summary, 100L);
                MainActivity.this.bottomActions.setVisibility(0);
                appear(MainActivity.this.bottomActions, 200L);
                MainActivity.this.pageRoot.requestApplyInsets();
                MainActivity.this.busy = false;
                aura();
                if (!z) {
                    MainActivity.this.showError(str);
                }
            }
        }

        void appear(View view, long j) {
            view.setVisibility(0);
            view.animate().cancel();
            if (!ValueAnimator.areAnimatorsEnabled()) {
                view.setAlpha(1.0f);
                view.setTranslationY(0.0f);
            } else {
                view.setAlpha(0.0f);
                view.setTranslationY(MainActivity.this.dp(8.0f));
                view.animate().alpha(1.0f).translationY(0.0f).setStartDelay(j).setDuration(300L).setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)).start();
            }
        }
    }

    private int selectedMode() {
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("options");
        if (jSONObjectOptJSONObject == null) {
            return 1;
        }
        return jSONObjectOptJSONObject.optInt("workload_mode", 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String modeName() {
        int iSelectedMode = selectedMode();
        if (iSelectedMode == 0) {
            return "Off";
        }
        if (iSelectedMode == 2) {
            return "Light";
        }
        if (iSelectedMode == 3) {
            return "Extra light";
        }
        return iSelectedMode == 4 ? "Ultra light" : "Automatic";
    }

    private void workload() {
        section("Visual workload");
        String[] strArr = {"Off", "Automatic", "Light", "Extra light", "Ultra light"};
        String[] strArr2 = {"Use normal visual detail. Existing panel fixes stay on.", "Start with Light and reduce visual detail further as the battery warms up.", "Build each background once, with lighter blur and media-wave detail.", "Use the lightest blur path and fewer wave points. Background details are softer.", "Use a small prepared wallpaper blur and fewer media details. Keep animations at 0.8x."};
        String[] strArr3 = {"off", "auto", "light", "extra", "ultra"};
        int i = 0;
        while (i < 5) {
            final String str = strArr3[i];
            LinearLayout linearLayoutRow = row(strArr[i], strArr2[i]);
            RadioButton radioButton = new RadioButton(this);
            radioButton.setButtonTintList(choiceTint());
            radioButton.setChecked(selectedMode() == i);
            radioButton.setClickable(false);
            radioButton.setFocusable(false);
            linearLayoutRow.addView(radioButton);
            clickable(linearLayoutRow, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda97
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m101lambda$workload$35$nihilsunshinepartsMainActivity(str);
                }
            });
            i++;
        }
        note("These modes reduce visual work.");
    }

    /* JADX INFO: renamed from: lambda$workload$34$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m100lambda$workload$34$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.workload(str);
        this.state = this.ops.reload();
        JSONArray jSONArrayOptJSONArray = this.state.optJSONArray("pending_scopes");
        return (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() <= 0) ? "Applied" : "Saved. Restart to apply every integration.";
    }

    /* JADX INFO: renamed from: lambda$workload$35$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m101lambda$workload$35$nihilsunshinepartsMainActivity(final String str) {
        run("Visual workload", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda23
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m100lambda$workload$34$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    private String launcherName() {
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("options");
        String strOptString = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optString("launcher_preset", "low") : "low";
        if ("auto".equals(strOptString)) {
            return "Off";
        }
        if ("lowest".equals(strOptString)) {
            return "Simple";
        }
        if ("mass".equals(strOptString)) {
            return "Medium";
        }
        return "high".equals(strOptString) ? "High" : "Standard";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launcherAnimations() {
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("options");
        choose("Home screen animations", new String[]{"Off", "Simple", "Standard", "Medium", "High"}, new String[]{"auto", "lowest", "low", "mass", "high"}, jSONObjectOptJSONObject == null ? "low" : jSONObjectOptJSONObject.optString("launcher_preset", "low"), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda18
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m42lambda$launcherAnimations$37$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$launcherAnimations$36$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m41lambda$launcherAnimations$36$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.launcherPreset(str);
        this.state = this.ops.reload();
        return "Applied";
    }

    /* JADX INFO: renamed from: lambda$launcherAnimations$37$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m42lambda$launcherAnimations$37$nihilsunshinepartsMainActivity(final String str) {
        run("Home screen animations", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda61
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m41lambda$launcherAnimations$36$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void transitionEffects() {
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("options");
        choose("Transition effects", new String[]{"Standard", "No shadows", "No rounded corners"}, new String[]{"default", "shadow", "corners"}, jSONObjectOptJSONObject == null ? "shadow" : jSONObjectOptJSONObject.optString("transition_effects", "shadow"), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda67
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m95lambda$transitionEffects$39$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$transitionEffects$38$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m94lambda$transitionEffects$38$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.transitionEffects(str);
        this.state = this.ops.reload();
        return "Applied";
    }

    /* JADX INFO: renamed from: lambda$transitionEffects$39$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m95lambda$transitionEffects$39$nihilsunshinepartsMainActivity(final String str) {
        run("Transition effects", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda7
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m94lambda$transitionEffects$38$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    private void visual() {
        if (visualLevel() > 0) {
            note("Options controlled by the active visual profile are temporarily unavailable.");
        }
        section("Panel effects");
        moduleSwitch("Panel animations", "Panel transitions follow visual workload.", "native_animations", true);
        moduleSwitch("Panel blur", "Show background blur in Quick settings and notifications.", "native_blur", true);
        moduleSwitch("Hardware blur", "Use the GPU for prepared panel backgrounds in Light and Extra light.", "gpu_blur", true);
        note("Both effects are on by default. Layout and duplicate-work optimizations remain active in either mode.");
        section("Media");
        link("Media wave detail", new String[]{"Fine", "Balanced", "Light"}[optionStepIndex()], new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda28
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m98lambda$visual$42$nihilsunshinepartsMainActivity();
            }
        });
        if (visualLevel() > 0) {
            lockRow(this.lastRow);
        }
        note("Wave timing and playback speed remain native. This setting adjusts only the amount of geometry drawn.");
        section("More settings");
        link("Advanced settings", "Secondary optimizations and maintenance", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda29
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m99lambda$visual$43$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$visual$42$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m98lambda$visual$42$nihilsunshinepartsMainActivity() {
        choose("Media wave detail", new String[]{"Fine", "Balanced", "Light"}, new String[]{"2", "4", "8"}, Integer.toString(chosenMediaStep()), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda25
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m97lambda$visual$41$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$visual$41$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m97lambda$visual$41$nihilsunshinepartsMainActivity(final String str) {
        run("Media wave detail", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda19
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m96lambda$visual$40$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$visual$40$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m96lambda$visual$40$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.mediaStep(Integer.parseInt(str));
        this.state = this.ops.reload();
        return "Applied";
    }

    /* JADX INFO: renamed from: lambda$visual$43$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m99lambda$visual$43$nihilsunshinepartsMainActivity() {
        show(4);
    }

    private int chosenMediaStep() {
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("saved_options");
        JSONObject jSONObjectOptJSONObject2 = this.state.optJSONObject("options");
        if (jSONObjectOptJSONObject != null) {
            return jSONObjectOptJSONObject.optInt("media_step", 4);
        }
        if (jSONObjectOptJSONObject2 == null) {
            return 4;
        }
        return jSONObjectOptJSONObject2.optInt("media_step", 4);
    }

    private int optionStepIndex() {
        int iChosenMediaStep;
        if (visualLevel() <= 0) {
            iChosenMediaStep = chosenMediaStep();
        } else if (visualLevel() == 3) {
            iChosenMediaStep = 40;
        } else {
            iChosenMediaStep = visualLevel() == 2 ? 24 : 8;
        }
        if (iChosenMediaStep == 2) {
            return 0;
        }
        return iChosenMediaStep == 4 ? 1 : 2;
    }

    private String processingName() {
        String strOptString = this.info.optString("PROCESSING");
        if (strOptString.equals("1")) {
            return "High";
        }
        return strOptString.equals("2") ? "Maximum" : "Optimized";
    }

    private void performance() {
        section("UI workload");
        link("Visual workload", modeName(), new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m64lambda$performance$44$nihilsunshinepartsMainActivity();
            }
        });
        section("Processing");
        link("Processing speed", processingName(), new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m65lambda$performance$45$nihilsunshinepartsMainActivity();
            }
        });
        link("CPU power profile", this.info.optString("LIGHT").equals("1") ? "Low power" : "Standard", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m68lambda$performance$48$nihilsunshinepartsMainActivity();
            }
        });
        note("The compatibility profile applies CPU policy limits when Samsung's processing feature is absent. Low power limits CPU ceilings to 80% of the saved baseline. Thermal protections remain active.");
        section("Power management");
        toggle("Power saving", "Limit power use through Android's power service.", this.info.optString("POWER_SAVING").equals("1"), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda3
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m70lambda$performance$50$nihilsunshinepartsMainActivity(str);
            }
        });
        toggle("Adaptive battery", "Limit battery use for apps you use less often.", !this.info.optString("ADAPTIVE").equals("0"), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda4
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m72lambda$performance$52$nihilsunshinepartsMainActivity(str);
            }
        });
        section("Details");
        link("Policy limits", "View current CPU policy limits", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m73lambda$performance$53$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$performance$44$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m64lambda$performance$44$nihilsunshinepartsMainActivity() {
        show(8);
    }

    /* JADX INFO: renamed from: lambda$performance$45$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m65lambda$performance$45$nihilsunshinepartsMainActivity() {
        show(7);
    }

    /* JADX INFO: renamed from: lambda$performance$48$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m68lambda$performance$48$nihilsunshinepartsMainActivity() {
        choose("CPU power profile", new String[]{"Standard", "Low power"}, new String[]{"0", "1"}, this.info.optString("LIGHT"), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda98
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m67lambda$performance$47$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$performance$47$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m67lambda$performance$47$nihilsunshinepartsMainActivity(final String str) {
        run("CPU power profile", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda72
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m66lambda$performance$46$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$performance$46$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m66lambda$performance$46$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.light(str.equals("1"));
        this.info = this.ops.info();
        return "Applied";
    }

    /* JADX INFO: renamed from: lambda$performance$50$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m70lambda$performance$50$nihilsunshinepartsMainActivity(final String str) {
        runToggle("Power saving", "1".equals(str), new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda6
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m69lambda$performance$49$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$performance$49$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m69lambda$performance$49$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.powerSaving(str.equals("1"));
        this.info = this.ops.info();
        return "Applied";
    }

    /* JADX INFO: renamed from: lambda$performance$52$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m72lambda$performance$52$nihilsunshinepartsMainActivity(final String str) {
        runToggle("Adaptive battery", "1".equals(str), new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda71
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m71lambda$performance$51$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$performance$51$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m71lambda$performance$51$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.adaptive(str.equals("1"));
        this.info = this.ops.info();
        return "Applied";
    }

    /* JADX INFO: renamed from: lambda$performance$53$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m73lambda$performance$53$nihilsunshinepartsMainActivity() {
        show(10);
    }

    private void processing() {
        section("Choose a processing speed");
        String strOptString = this.info.optString("PROCESSING");
        String[] strArr = {"Optimized", "High", "Maximum"};
        String[] strArr2 = {"Restore saved CPU frequency floors for balanced everyday use.", "Raise CPU floors to at least 35% of the saved ceiling for quicker response.", "Raise CPU floors to at least 50% of the saved ceiling. Uses more power and can increase heat."};
        final int i = 0;
        while (i < 3) {
            LinearLayout linearLayoutRow = row(strArr[i], strArr2[i]);
            RadioButton radioButton = new RadioButton(this);
            radioButton.setButtonTintList(choiceTint());
            radioButton.setChecked(strOptString.equals(Integer.toString(i)) || (i == 0 && (strOptString.isEmpty() || strOptString.equals("null"))));
            radioButton.setClickable(false);
            radioButton.setFocusable(false);
            linearLayoutRow.addView(radioButton);
            clickable(linearLayoutRow, new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m78lambda$processing$55$nihilsunshinepartsMainActivity(i);
                }
            });
            i++;
        }
        note("Changes are checked against actual CPU policy files. This compatibility engine does not require the donor ROM's Samsung feature flag.");
    }

    /* JADX INFO: renamed from: lambda$processing$55$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m78lambda$processing$55$nihilsunshinepartsMainActivity(final int i) {
        run("Processing speed", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda58
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m77lambda$processing$54$nihilsunshinepartsMainActivity(i);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$processing$54$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m77lambda$processing$54$nihilsunshinepartsMainActivity(int i) throws Exception {
        this.ops.processing(i);
        this.info = this.ops.info();
        return "Applied";
    }

    private String settingName(String str) {
        if ("enabled".equals(str)) {
            return "Enabled";
        }
        if ("disabled".equals(str)) {
            return "Disabled";
        }
        if ("skiagl".equals(str)) {
            return "Skia GL";
        }
        if ("skiavk".equals(str)) {
            return "Skia Vulkan";
        }
        if (str.isEmpty() || "null".equals(str) || "device_default".equals(str)) {
            return "System default";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private void memory() {
        section("Memory");
        link("Release cached apps", "Active services are managed by Android", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda39
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m45lambda$memory$58$nihilsunshinepartsMainActivity();
            }
        });
        moduleSwitch("Smart RAM management", "Release unused module buffers under sustained memory pressure. Android manages app eviction.", "smart_ram", false);
        section("Background usage limits");
        final String strOptString = this.info.optString("CACHED");
        link("Maximum cached apps", (strOptString.equals("null") || strOptString.isEmpty()) ? "System default" : strOptString + " apps", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda40
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m48lambda$memory$61$nihilsunshinepartsMainActivity(strOptString);
            }
        });
        link("Cached app freezer", settingName(this.info.optString("FREEZER")), new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda41
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m51lambda$memory$64$nihilsunshinepartsMainActivity();
            }
        });
        note("Cached apps are not the same as running services. Lower limits can increase app reloads.");
    }

    /* JADX INFO: renamed from: lambda$memory$58$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m45lambda$memory$58$nihilsunshinepartsMainActivity() {
        confirm("Release cached apps?", "Recently used apps may need to reload. This does not force-stop every installed app.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda63
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m44lambda$memory$57$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$memory$57$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m44lambda$memory$57$nihilsunshinepartsMainActivity() {
        run("Memory", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda52
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m43lambda$memory$56$nihilsunshinepartsMainActivity();
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$memory$56$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m43lambda$memory$56$nihilsunshinepartsMainActivity() throws Exception {
        String strRam = this.ops.ram();
        this.info = this.ops.info();
        return strRam;
    }

    /* JADX INFO: renamed from: lambda$memory$61$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m48lambda$memory$61$nihilsunshinepartsMainActivity(String str) {
        choose("Maximum cached apps", new String[]{"System default", "4 apps", "8 apps", "12 apps", "16 apps", "24 apps", "32 apps", "48 apps", "64 apps"}, new String[]{"default", "4", "8", "12", "16", "24", "32", "48", "64"}, (str.equals("null") || str.isEmpty()) ? "default" : str, new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda24
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str2) {
                this.f$0.m47lambda$memory$60$nihilsunshinepartsMainActivity(str2);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$memory$60$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m47lambda$memory$60$nihilsunshinepartsMainActivity(final String str) {
        run("Cached app limit", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda57
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m46lambda$memory$59$nihilsunshinepartsMainActivity(str);
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$memory$59$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m46lambda$memory$59$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.cached(str);
        this.info = this.ops.info();
        return "Effective limit: " + this.info.optString("CUR_MAX_CACHED_PROCESSES", "not reported");
    }

    /* JADX INFO: renamed from: lambda$memory$64$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m51lambda$memory$64$nihilsunshinepartsMainActivity() {
        String[] strArr = {"Device default", "Enabled", "Disabled"};
        String strOptString = "device_default";
        String[] strArr2 = {"device_default", "enabled", "disabled"};
        if (!this.info.optString("FREEZER").equals("null") && !this.info.optString("FREEZER").isEmpty()) {
            strOptString = this.info.optString("FREEZER");
        }
        choose("Cached app freezer", strArr, strArr2, strOptString, new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda27
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m50lambda$memory$63$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$memory$63$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m50lambda$memory$63$nihilsunshinepartsMainActivity(final String str) {
        run("Cached app freezer", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda94
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m49lambda$memory$62$nihilsunshinepartsMainActivity(str);
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$memory$62$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m49lambda$memory$62$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.freezer(str);
        this.info = this.ops.info();
        return "Saved. Restart the device to apply the framework policy.";
    }

    private void advanced() {
        section("System recovery");
        moduleSwitch("Recovery mode", "Keep native system behavior. Restart the phone after changing this option.", "recovery_mode", false);
        if (this.state.optBoolean("runtime_restart_required")) {
            note("Restart to apply the runtime mode.");
        }
        if (this.state.optBoolean("recovery_mode")) {
            note("Native interface active. Module effects are paused.");
        }
        section("Secondary optimizations");
        moduleSwitch("Audio state updates", "Read the SoundAlive state in the background.", "soundalive_async", false);
        moduleSwitch("Prepared panel blur", "Prepare the panel background before it is needed.", "prepared_blur", true);
        moduleSwitch("Quick settings updates", "Combine repeated Quick settings updates.", "lean_qs", true);
        moduleSwitch("Tile layout", "Reduce repeated tile layout work.", "tile_layout", true);
        moduleSwitch("Tile updates", "Avoid repeating unchanged tile updates.", "tile_work", true);
        moduleSwitch("Panel layout reuse", "Reuse compatible Quick settings layout work.", "compose_layout", true);
        moduleSwitch("Keep panel content ready", "Keep compatible Quick settings content ready.", "compose_host", true);
        section("Home screen settings");
        moduleSwitch("Prefer 120 Hz", "Prefer 120 Hz when the device can use it. Power saving and thermal limits still apply.", "refresh", false);
        note("Native refresh settings are verified when this option is applied.");
        link("Home screen animations", "Off".equals(launcherName()) ? "Dynamic animation adjustment is on." : "Dynamic animation adjustment is off.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda44
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.launcherAnimations();
            }
        });
        link("Transition effects", "Adjust visual effects during app transitions.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda45
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.transitionEffects();
            }
        });
        moduleSwitch("Reduced edge extension", "Avoid edge extension during app transitions.", "reduce_edge_extension", true);
        moduleSwitch("Muted light surfaces", "Use quieter notification colors. Restart SystemUI after turning this off.", "light_tone", true);
        section("Maintenance");
        link("Restart SystemUI", "Restart only the verified SystemUI process", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda46
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m12lambda$advanced$67$nihilsunshinepartsMainActivity();
            }
        });
        link("Reset UI shader caches", "Move recognized shader files into a backup", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda47
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m15lambda$advanced$70$nihilsunshinepartsMainActivity();
            }
        });
        link("Restore original system settings", "Restore CPU limits and the first saved system settings", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda48
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m18lambda$advanced$73$nihilsunshinepartsMainActivity();
            }
        });
        section("Diagnostics");
        toggle("Reduce diagnostic logging", "Reduce repetitive messages. Warnings and errors stay available.", this.ops.reducedLoggingActive(), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda49
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m20lambda$advanced$75$nihilsunshinepartsMainActivity(str);
            }
        });
        link("Export diagnostic archive", "Record frames and logs for 15 seconds", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda50
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m22lambda$advanced$77$nihilsunshinepartsMainActivity();
            }
        });
        link("App renderer", settingName(this.info.optString("RENDERER")), new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda51
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m26lambda$advanced$81$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$advanced$67$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m12lambda$advanced$67$nihilsunshinepartsMainActivity() {
        confirm("Restart SystemUI?", "The status bar disappears briefly. The lock screen may return.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m11lambda$advanced$66$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$advanced$66$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m11lambda$advanced$66$nihilsunshinepartsMainActivity() {
        run("Restart SystemUI", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda75
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m10lambda$advanced$65$nihilsunshinepartsMainActivity();
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$advanced$65$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m10lambda$advanced$65$nihilsunshinepartsMainActivity() throws Exception {
        String strRestartSystemUi = this.ops.restartSystemUi();
        this.state = this.ops.control("com.android.systemui", "status");
        return strRestartSystemUi;
    }

    /* JADX INFO: renamed from: lambda$advanced$70$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m15lambda$advanced$70$nihilsunshinepartsMainActivity() {
        confirm("Reset shader caches?", "Use only for suspected corrupt caches. Shader recompilation can temporarily increase stutter.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda93
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m14lambda$advanced$69$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$advanced$68$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m13lambda$advanced$68$nihilsunshinepartsMainActivity() throws Exception {
        return this.ops.shaders();
    }

    /* JADX INFO: renamed from: lambda$advanced$69$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m14lambda$advanced$69$nihilsunshinepartsMainActivity() {
        run("UI shader caches", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda59
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m13lambda$advanced$68$nihilsunshinepartsMainActivity();
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$advanced$73$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m18lambda$advanced$73$nihilsunshinepartsMainActivity() {
        confirm("Restore original settings?", "Module switches are kept. A reboot may be needed for framework settings.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda87
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m17lambda$advanced$72$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$advanced$72$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$advanced$72$nihilsunshinepartsMainActivity() {
        run("Restore settings", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda74
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m16lambda$advanced$71$nihilsunshinepartsMainActivity();
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$advanced$71$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m16lambda$advanced$71$nihilsunshinepartsMainActivity() throws Exception {
        String strRestoreSettings = this.ops.restoreSettings();
        this.info = this.ops.info();
        return strRestoreSettings;
    }

    /* JADX INFO: renamed from: lambda$advanced$74$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m19lambda$advanced$74$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.setReducedLogging("1".equals(str));
        return "1".equals(str) ? "Reduced logging applied" : "Previous logging restored";
    }

    /* JADX INFO: renamed from: lambda$advanced$75$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$advanced$75$nihilsunshinepartsMainActivity(final String str) {
        runToggle("Reduce diagnostic logging", "1".equals(str), new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda56
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m19lambda$advanced$74$nihilsunshinepartsMainActivity(str);
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$advanced$77$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$advanced$77$nihilsunshinepartsMainActivity() {
        run("Diagnostic archive exported", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda68
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m21lambda$advanced$76$nihilsunshinepartsMainActivity();
            }
        }, true, false);
    }

    /* JADX INFO: renamed from: lambda$advanced$76$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m21lambda$advanced$76$nihilsunshinepartsMainActivity() throws Exception {
        return "Saved to " + this.ops.diagnostic().getAbsolutePath();
    }

    /* JADX INFO: renamed from: lambda$advanced$81$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m26lambda$advanced$81$nihilsunshinepartsMainActivity() {
        choose("App renderer", new String[]{"System default", "Skia Vulkan", "Skia OpenGL"}, new String[]{"default", "skiavk", "skiagl"}, this.info.optString("RENDERER"), new Pick() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda35
            @Override // nihil.sunshine.parts.MainActivity.Pick
            public final void run(String str) {
                this.f$0.m25lambda$advanced$80$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$advanced$80$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m25lambda$advanced$80$nihilsunshinepartsMainActivity(final String str) {
        confirm("Change app renderer?", "This lasts until reboot. Restart affected apps. The original value is saved.", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m24lambda$advanced$79$nihilsunshinepartsMainActivity(str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$advanced$79$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$advanced$79$nihilsunshinepartsMainActivity(final String str) {
        run("Renderer", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda43
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m23lambda$advanced$78$nihilsunshinepartsMainActivity(str);
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$advanced$78$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m23lambda$advanced$78$nihilsunshinepartsMainActivity(String str) throws Exception {
        this.ops.renderer(str);
        this.info = this.ops.info();
        return "Applied for the current boot.";
    }

    private void status() {
        section("Connection");
        if (!this.connectionIssue.isEmpty()) {
            note("Root access is available. The SystemUI connection needs attention.");
            note(this.connectionIssue);
        }
        selectedRow("Panel", this.state.optBoolean("active") ? "Connected" : "Restart needed", this.state.optBoolean("active"));
        link("Check connection", "Check that the panel module is responding", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m91lambda$status$83$nihilsunshinepartsMainActivity();
            }
        });
        section("Details");
        link("Diagnostics", "View integration details", new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m92lambda$status$84$nihilsunshinepartsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$status$82$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ String m90lambda$status$82$nihilsunshinepartsMainActivity() throws Exception {
        this.state = this.ops.control("com.android.systemui", "status");
        this.connectionIssue = "";
        this.info = this.ops.quickInfo();
        return "SystemUI connection verified.";
    }

    /* JADX INFO: renamed from: lambda$status$83$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m91lambda$status$83$nihilsunshinepartsMainActivity() {
        run("Connection", new Job() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda38
            @Override // nihil.sunshine.parts.MainActivity.Job
            public final String run() {
                return this.f$0.m90lambda$status$82$nihilsunshinepartsMainActivity();
            }
        }, false);
    }

    /* JADX INFO: renamed from: lambda$status$84$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m92lambda$status$84$nihilsunshinepartsMainActivity() {
        show(11);
    }

    private String oneUiVersion(String str) {
        try {
            int i = Integer.parseInt(str);
            return (i / 10000) + "." + ((i % 10000) / 100);
        } catch (Exception e) {
            return "8.0";
        }
    }

    private String friendly(String str) {
        return DiagnosticText.value(str);
    }

    private String activity(String str, boolean z) {
        return z ? Long.toString(this.state.optLong(str)) : "Disabled by this profile";
    }

    private void panelActivity() {
        String str;
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("options");
        boolean z = jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optBoolean("prepared_blur");
        boolean z2 = jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optBoolean("blur_worker");
        row("Blur results", activity("blur_applied", z2));
        row("Opening fast paths", activity("blur_fraction_fast_paths", z2));
        String str2 = "Disabled by this profile";
        row("Reduced native captures", visualLevel() > 0 ? Long.toString(this.state.optLong("blur_native_captures_reduced") + this.state.optLong("blur_source_captures_reduced")) : "Disabled by this profile");
        row("Direct QS state reads", Long.toString(this.state.optLong("qs_direct_snapshot_reads")));
        row("Repeated QS targets avoided", Long.toString(this.state.optLong("qs_duplicate_targets")));
        row("Lighter split-panel updates", Long.toString(this.state.optLong("split_native_updates")));
        row("Notification stacks avoided", Long.toString(this.state.optLong("notification_translation_stacks_avoided")));
        row("QS gesture updates", Long.toString(this.state.optLong("qs_gesture_updates")));
        row("Blur errors", Long.toString(this.state.optLong("blur_errors")));
        if (z) {
            str2 = this.state.optBoolean("blur_preview_cached") ? "Ready" : "Not prepared";
        }
        row("Prepared opening preview", str2);
        row("Preview uses", Long.toString(this.state.optLong("blur_preview_cache_hits")));
        row("Frame vote diagnostics avoided", this.state.optBoolean("frame_vote_active") ? Long.toString(this.state.optLong("frame_vote_writes")) : "Native path");
        if (this.state.optBoolean("smart_ram_ready")) {
            str = option("smart_ram", false) ? "Enabled" : "Off";
        } else {
            str = "Unavailable";
        }
        row("Memory monitoring", str);
        row("Memory checks", Long.toString(this.state.optLong("smart_ram_checks")));
        row("Buffer releases", Long.toString(this.state.optLong("smart_ram_trims")));
        String strOptString = this.state.optString("smart_ram_error");
        String strOptString2 = this.state.optString("blur_preview_error");
        if (!strOptString.isEmpty()) {
            row("Memory initialization error", strOptString);
        }
        if (!strOptString2.isEmpty()) {
            row("Preview error", strOptString2);
        }
    }

    private void diagnostics() {
        section("Integrations");
        JSONObject jSONObjectOptJSONObject = this.state.optJSONObject("features");
        if (jSONObjectOptJSONObject != null) {
            Iterator<String> itKeys = jSONObjectOptJSONObject.keys();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                row(DiagnosticText.label(next), DiagnosticText.status(next, jSONObjectOptJSONObject.optString(next), this.state.optJSONObject("options"), visualLevel()));
            }
        }
        section("Panel activity");
        panelActivity();
        note("Activity counters confirm that a path ran. They are not a measurement of frame rate or speed.");
    }

    private void report() {
        section("Current session");
        row("Visual profile", modeName());
        int iOptInt = this.info.optInt("battery_temperature", -1);
        row("Battery temperature", (iOptInt < 0 || iOptInt > 700) ? "Not available" : String.format(Locale.US, "%.1f °C", Float.valueOf(iOptInt / 10.0f)));
        section("Display policy");
        Display display = getDisplay();
        row("App display refresh", display == null ? "Not reported" : I18n.format(this, "%s Hz", UiNumbers.decimal(display.getRefreshRate(), getResources().getConfiguration().getLocales().get(0))));
        note("This is the app display rate, not a count of rendered frames. Native protection limits still apply.");
        section("Panel activity");
        panelActivity();
        note("This report covers the current SystemUI session. Counters reset when SystemUI restarts. They do not measure FPS or prove a performance gain.");
    }

    private void refreshReport() {
        if (this.ops == null || this.reportLoading || this.busy || this.screen != 12) {
            return;
        }
        this.reportLoading = true;
        final int i = this.pageGeneration;
        final int iBeginLoading = beginLoading();
        this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda69
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m81lambda$refreshReport$87$nihilsunshinepartsMainActivity(iBeginLoading, i);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refreshReport$87$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m81lambda$refreshReport$87$nihilsunshinepartsMainActivity(final int i, final int i2) {
        JSONObject jSONObjectControl;
        String message;
        JSONObject jSONObjectQuickInfo = null;
        try {
            jSONObjectControl = this.ops.control("com.android.systemui", "status");
            try {
                jSONObjectQuickInfo = this.ops.quickInfo();
                message = "";
            } catch (Exception e) {
                e = e;
                message = e.getMessage() == null ? "Action failed" : e.getMessage();
            }
        } catch (Exception e2) {
            e = e2;
            jSONObjectControl = null;
        }
        final String str = message;
        final JSONObject jSONObject = jSONObjectQuickInfo;
        final JSONObject jSONObject2 = jSONObjectControl;
        this.main.post(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda73
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m80lambda$refreshReport$86$nihilsunshinepartsMainActivity(i, i2, jSONObject2, jSONObject, str);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refreshReport$86$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m80lambda$refreshReport$86$nihilsunshinepartsMainActivity(int i, final int i2, JSONObject jSONObject, JSONObject jSONObject2, String str) {
        this.reportLoading = false;
        endLoading(i);
        if (isFinishing() || isDestroyed() || this.screen != 12) {
            return;
        }
        if (i2 != this.pageGeneration) {
            refreshReport();
            return;
        }
        if (jSONObject == null || jSONObject2 == null) {
            showError(str);
            return;
        }
        this.state = jSONObject;
        this.info = jSONObject2;
        final int scrollY = this.activeScroll.getScrollY();
        if (this.body.getChildCount() > 1) {
            this.body.removeViews(1, this.body.getChildCount() - 1);
        }
        this.card = null;
        report();
        I18n.apply(this, this.body);
        this.activeScroll.post(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda91
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m79lambda$refreshReport$85$nihilsunshinepartsMainActivity(i2, scrollY);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refreshReport$85$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m79lambda$refreshReport$85$nihilsunshinepartsMainActivity(int i, int i2) {
        if (this.screen == 12 && i == this.pageGeneration) {
            this.activeScroll.scrollTo(0, i2);
        }
    }

    private void choose(String str, String[] strArr, final String[] strArr2, String str2, final Pick pick) {
        int iIndexOf = Arrays.asList(strArr2).indexOf(str2);
        if (iIndexOf < 0) {
            iIndexOf = 0;
        }
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(I18n.text(this, str)).setSingleChoiceItems(I18n.labels(this, strArr), iIndexOf, new DialogInterface.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda64
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.lambda$choose$88(pick, strArr2, dialogInterface, i);
            }
        }).setNegativeButton(I18n.text(this, "Cancel"), (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda65
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                this.f$0.m29lambda$choose$89$nihilsunshinepartsMainActivity(alertDialogCreate, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    static /* synthetic */ void lambda$choose$88(Pick pick, String[] strArr, DialogInterface dialogInterface, int i) {
        dialogInterface.dismiss();
        pick.run(strArr[i]);
    }

    /* JADX INFO: renamed from: lambda$choose$89$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m29lambda$choose$89$nihilsunshinepartsMainActivity(AlertDialog alertDialog, DialogInterface dialogInterface) {
        ListView listView = alertDialog.getListView();
        tintChoices(listView);
        listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() { // from class: nihil.sunshine.parts.MainActivity.6
            @Override // android.view.ViewGroup.OnHierarchyChangeListener
            public void onChildViewAdded(View view, View view2) {
                MainActivity.this.tintChoices(view2);
            }

            @Override // android.view.ViewGroup.OnHierarchyChangeListener
            public void onChildViewRemoved(View view, View view2) {
            }
        });
    }

    private ColorStateList choiceTint() {
        return new ColorStateList(new int[][]{new int[]{-16842910}, new int[]{android.R.attr.state_checked}, new int[0]}, new int[]{(this.secondary & 16777215) | 1627389952, this.accent, this.secondary});
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tintChoices(View view) {
        if (view instanceof CompoundButton) {
            ((CompoundButton) view).setButtonTintList(choiceTint());
        }
        if (view instanceof CheckedTextView) {
            ((CheckedTextView) view).setCheckMarkTintList(choiceTint());
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                tintChoices(viewGroup.getChildAt(i));
            }
        }
    }

    private void confirm(String str, String str2, final Runnable runnable) {
        new AlertDialog.Builder(this).setTitle(I18n.text(this, str)).setMessage(I18n.text(this, str2)).setPositiveButton(I18n.text(this, "Continue"), new DialogInterface.OnClickListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda77
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                runnable.run();
            }
        }).setNegativeButton(I18n.text(this, "Cancel"), (DialogInterface.OnClickListener) null).show();
    }

    private void run(String str, Job job, boolean z) {
        run(str, job, z, true);
    }

    private void runToggle(String str, boolean z, Job job, boolean z2) {
        run(str, job, z2, true, Boolean.valueOf(z));
    }

    private void run(String str, Job job, boolean z, boolean z2) {
        run(str, job, z, z2, null);
    }

    private void run(final String str, final Job job, final boolean z, boolean z2, final Boolean bool) {
        if (this.busy || this.detailsLoading) {
            return;
        }
        if (!SerialAccess.usable(this)) {
            closeDeniedAccess();
            return;
        }
        this.busy = true;
        final boolean zEquals = "Connection".equals(str);
        final long jUptimeMillis = SystemClock.uptimeMillis();
        final int iBeginConnectionLoading = zEquals ? beginConnectionLoading() : beginLoading();
        this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda66
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m84lambda$run$93$nihilsunshinepartsMainActivity(job, str, z, bool, iBeginConnectionLoading, zEquals, jUptimeMillis);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$run$93$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m84lambda$run$93$nihilsunshinepartsMainActivity(Job job, final String str, final boolean z, Boolean bool, final int i, boolean z2, long j) {
        String message;
        final boolean z3;
        boolean z4 = false;
        try {
            synchronized (SerialAccess.mutationLock()) {
                if (!SerialAccess.usable(this)) {
                    throw new SerialAccess.Denied();
                }
                message = job.run();
                this.ops.snapshotAuthorizationTargets(true);
                this.ops.checkpointAccessBaseline();
            }
            z3 = true;
        } catch (SerialAccess.Denied e) {
            this.main.post(new MainActivity$$ExternalSyntheticLambda14(this));
            return;
        } catch (Exception e2) {
            message = e2.getMessage() == null ? "Action failed" : e2.getMessage();
            z3 = false;
        }
        final String strText = ActionFeedback.resultKey(str).isEmpty() ? I18n.format(this, "%1$s updated.", I18n.text(this, str)) : I18n.text(this, ActionFeedback.resultKey(str));
        if (z3 && !z && ActionFeedback.setting(str) && (message == null || (!message.contains("Restart") && !message.contains("restart")))) {
            z4 = true;
        }
        if (z3 && bool != null && !z && !"Recovery mode".equals(str)) {
            strText = I18n.text(this, ActionFeedback.toggleResultKey(str, bool.booleanValue()));
        } else if (!z4 && !ActionFeedback.generic(message)) {
            strText = ActionFeedback.punctuate(I18n.text(this, message));
        }
        this.main.postDelayed(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda70
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m83lambda$run$92$nihilsunshinepartsMainActivity(i, z, z3, str, strText);
            }
        }, WorkIndicator.completionDelay(z2, SystemClock.uptimeMillis() - j));
    }

    /* JADX INFO: renamed from: lambda$run$92$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m83lambda$run$92$nihilsunshinepartsMainActivity(int i, boolean z, boolean z2, String str, String str2) {
        this.busy = false;
        endLoading(i);
        if (isFinishing() || isDestroyed()) {
            return;
        }
        this.detailsAt = SystemClock.elapsedRealtime();
        this.policyAt = 0L;
        show(this.screen);
        if (z || !z2) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (!z2) {
                str = "Couldn't complete action";
            }
            final AlertDialog alertDialogCreate = builder.setTitle(I18n.text(this, str)).setMessage(I18n.text(this, str2)).setPositiveButton(I18n.text(this, "OK"), (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda26
                @Override // android.content.DialogInterface.OnShowListener
                public final void onShow(DialogInterface dialogInterface) {
                    this.f$0.m82lambda$run$91$nihilsunshinepartsMainActivity(alertDialogCreate, dialogInterface);
                }
            });
            alertDialogCreate.show();
        }
    }

    /* JADX INFO: renamed from: lambda$run$91$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m82lambda$run$91$nihilsunshinepartsMainActivity(AlertDialog alertDialog, DialogInterface dialogInterface) {
        I18n.apply(this, alertDialog.getWindow().getDecorView());
        TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextIsSelectable(true);
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.busy) {
            return;
        }
        if (this.screen != 0) {
            show(PartsNavigation.parent(this.screen));
        } else {
            super.onBackPressed();
        }
    }

    private void observePolicy() {
        if (this.policyObserver != null) {
            return;
        }
        this.policyObserver = new BroadcastReceiver() { // from class: nihil.sunshine.parts.MainActivity.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int iVisualLevel = MainActivity.this.visualLevel();
                try {
                    MainActivity.this.info.put("battery_temperature", intent.getIntExtra("temperature", -1));
                } catch (Exception e) {
                }
                if (iVisualLevel != MainActivity.this.visualLevel() && !MainActivity.this.busy && !MainActivity.this.detailsLoading && MainActivity.this.state.optBoolean("active")) {
                    if (MainActivity.this.screen == 1 || MainActivity.this.screen == 4 || MainActivity.this.screen == 8) {
                        MainActivity.this.show(MainActivity.this.screen);
                    }
                }
            }
        };
        registerReceiver(this.policyObserver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        observePolicy();
        this.foreground = true;
        if (this.screen == 10) {
            updateCpuChart();
        }
        startCpuMonitor();
        if (this.ops != null && this.screen == 12 && !this.busy) {
            refreshReport();
        } else {
            if (this.ops == null || !this.state.optBoolean("active") || this.busy) {
                return;
            }
            this.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda99
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m63lambda$onResume$95$nihilsunshinepartsMainActivity();
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$onResume$95$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m63lambda$onResume$95$nihilsunshinepartsMainActivity() {
        final JSONObject jSONObjectQuickInfo = this.ops.quickInfo();
        runOnUiThread(new Runnable() { // from class: nihil.sunshine.parts.MainActivity$$ExternalSyntheticLambda76
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m62lambda$onResume$94$nihilsunshinepartsMainActivity(jSONObjectQuickInfo);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onResume$94$nihil-sunshine-parts-MainActivity, reason: not valid java name */
    /* synthetic */ void m62lambda$onResume$94$nihilsunshinepartsMainActivity(JSONObject jSONObject) {
        if (isFinishing() || isDestroyed() || this.busy) {
            return;
        }
        this.info = jSONObject;
        if (this.screen == 0) {
            m54lambda$new$0$nihilsunshinepartsMainActivity();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.foreground = false;
        super.onPause();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            startCpuMonitor();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.main.removeCallbacks(this.accessDeadline);
        if (this.accessUpdates != null) {
            unregisterReceiver(this.accessUpdates);
            this.accessUpdates = null;
        }
        if (this.policyObserver != null) {
            try {
                unregisterReceiver(this.policyObserver);
            } catch (Exception e) {
            }
            this.policyObserver = null;
        }
        if (isFinishing()) {
            stopCpuMonitor();
        } else {
            detachCpuMonitor();
        }
        endLoading();
        this.main.removeCallbacksAndMessages(null);
        if (this.pageRoot != null) {
            this.pageRoot.animate().cancel();
        }
        super.onDestroy();
        this.worker.shutdown();
    }
}
