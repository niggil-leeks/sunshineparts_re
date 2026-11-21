package nihil.sunshine.parts;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import nihil.sunshine.parts.launcher.LauncherHook;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class ControlBus {
    public static final String ACTION = "nihil.sunshine.parts.FLIGHT_CONTROL";
    private static final Map<String, String> features = Collections.synchronizedMap(new LinkedHashMap());
    private static final AtomicBoolean registered = new AtomicBoolean();
    private static String scope = "";
    public static volatile boolean systemReady;

    public static void feature(String str, String str2) {
        features.put(str, str2);
    }

    public static void prepare(String str, ClassLoader classLoader) {
        scope = str;
        if ("android".equals(str)) {
            try {
                XposedBridge.hookAllMethods(Class.forName("com.android.server.SystemServer", false, classLoader), "startOtherServices", new XC_MethodHook() { // from class: nihil.sunshine.parts.ControlBus.1
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        try {
                            Object objCallStaticMethod = XposedHelpers.callStaticMethod(Class.forName("android.app.ActivityThread"), "currentActivityThread", new Object[0]);
                            Method declaredMethod = objCallStaticMethod.getClass().getDeclaredMethod("getSystemContext", new Class[0]);
                            declaredMethod.setAccessible(true);
                            ControlBus.attach((Context) declaredMethod.invoke(objCallStaticMethod, new Object[0]));
                        } catch (Throwable th) {
                            ControlBus.feature("control", "unavailable");
                        }
                    }
                });
            } catch (Throwable th) {
                feature("control", "unavailable");
            }
        } else {
            try {
                XposedHelpers.findAndHookMethod(Application.class, "attach", new Object[]{Context.class, new XC_MethodHook() { // from class: nihil.sunshine.parts.ControlBus.2
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (!methodHookParam.hasThrowable()) {
                            ControlBus.attach((Context) methodHookParam.args[0]);
                        }
                    }
                }});
            } catch (Throwable th2) {
                feature("control", "unavailable");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void attach(final Context context) {
        boolean z = true;
        if (registered.compareAndSet(false, true)) {
            LicenseRuntime.attach(context, scope);
            if ("android".equals(scope)) {
                try {
                    RefreshBoundary.attach(context);
                } catch (Throwable th) {
                }
            }
            try {
                if ("com.android.systemui".equals(scope) || "com.sec.android.app.launcher".equals(scope)) {
                    if (!"com.android.systemui".equals(scope) || RuntimeProfile.recovery()) {
                        z = false;
                    }
                    AdaptiveWorkload.install(context, z);
                }
            } catch (Throwable th2) {
                feature("adaptive_workload", "Unavailable; control connection retained");
            }
            if ("com.android.systemui".equals(scope) && !RuntimeProfile.recovery()) {
                feature("smart_ram", "initializing");
                new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: nihil.sunshine.parts.ControlBus$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ControlBus.lambda$attach$0(context);
                    }
                });
            }
            try {
                context.registerReceiver(new BroadcastReceiver() { // from class: nihil.sunshine.parts.ControlBus.3
                    @Override // android.content.BroadcastReceiver
                    public void onReceive(Context context2, Intent intent) {
                        String stringExtra;
                        if (!ControlBus.ACTION.equals(intent.getAction()) || (stringExtra = intent.getStringExtra("nonce")) == null || !stringExtra.matches("[a-f0-9]{24,64}")) {
                            return;
                        }
                        if ("access_reload".equals(intent.getStringExtra("op"))) {
                            LicenseRuntime.reload();
                        }
                        if (!LicenseRuntime.blocked() && !RuntimeProfile.recovery() && "com.android.systemui".equals(ControlBus.scope)) {
                            if ("profile_start".equals(intent.getStringExtra("op"))) {
                                PanelProfiler.start(context2);
                            } else if ("profile_stop".equals(intent.getStringExtra("op"))) {
                                PanelProfiler.stop();
                            }
                        }
                        if ("reload".equals(intent.getStringExtra("op"))) {
                            float fMotionScale = VisualPolicy.motionScale();
                            boolean zNativeBlur = VisualPolicy.nativeBlur();
                            boolean zPreparedBlur = VisualPolicy.preparedBlur();
                            boolean z2 = Config.lightTone;
                            boolean z3 = Config.gpuBlur;
                            boolean z4 = Config.lightUi;
                            boolean z5 = Config.extraLightUi;
                            boolean z6 = Config.tileLayout;
                            boolean z7 = Config.tileWork;
                            boolean z8 = Config.composeLayout;
                            boolean z9 = Config.composeHost;
                            int iMediaStep = AdaptiveWorkload.mediaStep();
                            int iLevel = AdaptiveWorkload.level();
                            int i = Config.transitionEffects;
                            Config.load();
                            if (i != Config.transitionEffects) {
                                TransitionEffects.reload();
                            }
                            if (!RuntimeProfile.recovery()) {
                                if ("android".equals(ControlBus.scope)) {
                                    RefreshHook.reload();
                                }
                                if ("com.android.systemui".equals(ControlBus.scope)) {
                                    SmartRam.configure();
                                    SoundAliveWork.configure();
                                }
                                if (iMediaStep != AdaptiveWorkload.mediaStep()) {
                                    SystemUiLite.applyMediaStep();
                                }
                                if (iLevel != AdaptiveWorkload.level()) {
                                    MediaWork.configure();
                                    PanelMaterialWork.configure();
                                }
                                if (fMotionScale != VisualPolicy.motionScale()) {
                                    SystemUiLite.configureMotion();
                                }
                                if (zNativeBlur != VisualPolicy.nativeBlur() || z2 != Config.lightTone) {
                                    SystemUiLite.refreshBlur();
                                }
                                if (zNativeBlur != VisualPolicy.nativeBlur() || zPreparedBlur != VisualPolicy.preparedBlur() || z2 != Config.lightTone || z3 != Config.gpuBlur || iLevel != AdaptiveWorkload.level()) {
                                    PreparedBlur.reload();
                                }
                                if (z6 != Config.tileLayout || z7 != Config.tileWork || z8 != Config.composeLayout || z9 != Config.composeHost || iLevel != AdaptiveWorkload.level()) {
                                    ControlBus.syncLegacy();
                                }
                            }
                        }
                        try {
                            JSONObject jSONObjectStatus = ControlBus.status();
                            setResultCode(250);
                            setResultData("SP250:" + stringExtra + ":" + Base64.encodeToString(jSONObjectStatus.toString().getBytes("UTF-8"), 2));
                        } catch (Throwable th3) {
                            setResultCode(-1);
                        }
                    }
                }, new IntentFilter(ACTION), "android.permission.DUMP", new Handler(Looper.getMainLooper()), 2);
                feature("control", "ready");
            } catch (Throwable th3) {
                registered.set(false);
                feature("control", "unavailable");
            }
        }
    }

    static /* synthetic */ void lambda$attach$0(Context context) {
        try {
            SmartRam.install(context);
        } catch (Throwable th) {
            SmartRam.error = th.getClass().getSimpleName();
            feature("smart_ram", "unavailable");
            feature("smart_ram_error", SmartRam.error);
            Log.w("SunshinePartsUI", "Memory initialization failed", th);
        }
    }

    static void syncLegacy() {
        try {
            Field declaredField = Class.forName("nihil.lain.sunshine.RuntimeOptions").getDeclaredField("values");
            declaredField.setAccessible(true);
            boolean[] zArr = (boolean[]) ((boolean[]) declaredField.get(null)).clone();
            zArr[0] = false;
            zArr[1] = VisualPolicy.tileLayout();
            zArr[2] = VisualPolicy.tileWork();
            zArr[3] = Config.composeLayout;
            zArr[4] = Config.composeHost;
            zArr[5] = false;
            declaredField.set(null, zArr);
        } catch (Throwable th) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static JSONObject status() throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("version", Config.VERSION);
        boolean z = true;
        jSONObject.put("authorized_runtime", !LicenseRuntime.blocked());
        jSONObject.put("launcher_preset_reads", LauncherHook.presetReads);
        jSONObject.put("launcher_diagnostics_skipped", LauncherHook.diagnosticsSkipped);
        if ("com.android.systemui".equals(scope)) {
            jSONObject.put("soundalive_read", SoundAliveWork.status());
            jSONObject.put("edge_extension", EdgeExtensionWork.status());
        }
        jSONObject.put("runtime_profile", RuntimeProfile.recovery() ? "recovery" : "visual-policy-v2");
        jSONObject.put("launcher_preset_effective", Config.launcherProfile());
        jSONObject.put("smart_ram_checks", SmartRam.checks);
        jSONObject.put("smart_ram_trims", SmartRam.trims);
        jSONObject.put("smart_ram_ready", SmartRam.ready);
        jSONObject.put("smart_ram_error", SmartRam.error);
        jSONObject.put("recovery_mode", RuntimeProfile.recovery());
        jSONObject.put("runtime_restart_required", Config.recoveryMode != RuntimeProfile.recovery());
        jSONObject.put("visual_profile_applied", !RuntimeProfile.recovery());
        jSONObject.put("surface_diagnostics_skipped", SurfaceDiagnosticWork.skipped);
        jSONObject.put("surface_diagnostics_error", SurfaceDiagnosticWork.error);
        jSONObject.put("scope", scope);
        if (!registered.get() || ("com.android.systemui".equals(scope) && !systemReady)) {
            z = false;
        }
        jSONObject.put("active", z);
        jSONObject.put("display", Config.property("ro.build.display.id"));
        jSONObject.put("oneui", Config.property("ro.build.version.oneui"));
        synchronized (features) {
            jSONObject.put("features", new JSONObject(new LinkedHashMap(features)));
        }
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("recovery_mode", Config.recoveryMode);
        jSONObject2.put("smart_ram", Config.smartRam);
        jSONObject2.put("soundalive_async", Config.soundAliveAsync);
        jSONObject2.put("reduce_edge_extension", Config.reduceEdgeExtension);
        jSONObject2.put("launcher_preset", Config.launcherPreset);
        jSONObject2.put("transition_effects", Config.transitionProfile());
        jSONObject2.put("native_blur", VisualPolicy.nativeBlur());
        jSONObject2.put("native_animations", VisualPolicy.nativeAnimations());
        jSONObject2.put("panel_motion_scale", VisualPolicy.motionScale());
        jSONObject2.put("blur_worker", VisualPolicy.workerBlur());
        jSONObject2.put("refresh", Config.refresh);
        jSONObject2.put("launcher_motion", Config.launcherMotion);
        jSONObject2.put("lean_qs", VisualPolicy.leanQs());
        jSONObject2.put("prepared_blur", VisualPolicy.preparedBlur());
        jSONObject2.put("gpu_blur", Config.gpuBlur);
        jSONObject2.put("tile_layers", false);
        jSONObject2.put("tile_layout", VisualPolicy.tileLayout());
        jSONObject2.put("tile_work", VisualPolicy.tileWork());
        jSONObject2.put("compose_layout", Config.composeLayout);
        jSONObject2.put("compose_host", Config.composeHost);
        jSONObject2.put("light_tone", Config.lightTone);
        jSONObject2.put("media_step", AdaptiveWorkload.mediaStep());
        jSONObject2.put("adaptive_workload", Config.adaptiveWorkload);
        jSONObject2.put("light_ui", Config.lightUi);
        jSONObject2.put("extra_light_ui", Config.extraLightUi);
        jSONObject2.put("workload_mode", Config.workloadMode);
        jSONObject.put("options", jSONObject2);
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("native_blur", Config.nativeBlur);
        jSONObject3.put("native_animations", Config.nativeAnimations);
        jSONObject3.put("prepared_blur", Config.preparedBlur);
        jSONObject3.put("lean_qs", Config.leanQs);
        jSONObject3.put("tile_layout", Config.tileLayout);
        jSONObject3.put("tile_work", Config.tileWork);
        jSONObject3.put("media_step", Config.mediaStep);
        jSONObject.put("saved_options", jSONObject3);
        jSONObject.put("compose_motion_hosts", QsComposeMotion.created);
        jSONObject.put("compose_motion_changes", QsComposeMotion.changed);
        jSONObject.put("compose_fast_path", QsComposeMotion.fastPaths);
        jSONObject.put("blur_requested", PreparedBlur.requested);
        jSONObject.put("blur_source_captures_reduced", PreparedBlur.sourceCapturesReduced);
        jSONObject.put("blur_fraction_updates", BlurFractionWork.updates);
        jSONObject.put("blur_applied", PreparedBlur.applied);
        jSONObject.put("blur_coalesced", PreparedBlur.replaced);
        jSONObject.put("blur_discarded", PreparedBlur.discarded);
        jSONObject.put("blur_errors", PreparedBlur.errors);
        jSONObject.put("gpu_blur_created", GpuPanelBlur.created);
        jSONObject.put("gpu_blur_draws", GpuPanelBlur.hardwareDraws);
        jSONObject.put("gpu_blur_fallbacks", GpuPanelBlur.fallbacks);
        jSONObject.put("gpu_blur_error", GpuPanelBlur.error);
        jSONObject.put("tile_layer_placements", TileLayers.placements);
        jSONObject.put("tile_layer_owners", TileLayers.firstOwners);
        jSONObject.put("tile_layer_error", TileLayers.error);
        jSONObject.put("notification_translation_stacks_avoided", StackTranslationWork.avoided);
        jSONObject.put("notification_translation_error", StackTranslationWork.error);
        jSONObject.put("blur_native_captures_reduced", PreparedBlur.nativeCapturesReduced);
        jSONObject.put("qs_gesture_updates", QsGestureWork.updates);
        jSONObject.put("qs_gesture_error", QsGestureWork.error);
        jSONObject.put("blur_deliveries_avoided", BlurDelivery.avoided);
        jSONObject.put("blur_delivery_error", BlurDelivery.error);
        jSONObject.put("blur_warmups", PreparedBlur.warmups);
        jSONObject.put("blur_worker_max_ms", PreparedBlur.workerMaxMs);
        jSONObject.put("blur_opening_requests", PreparedBlur.openingRequests);
        jSONObject.put("blur_native_main_calls", PreparedBlur.nativeMainCalls);
        jSONObject.put("blur_native_unscoped", PreparedBlur.nativeUnscoped);
        jSONObject.put("blur_software_final", PreparedBlur.softwareCaptures);
        jSONObject.put("blur_fraction_fast_paths", PreparedBlur.fractionFastPaths);
        jSONObject.put("blur_immediate_backgrounds", PanelBackdrop.immediate);
        jSONObject.put("blur_wallpaper_previews", PanelBackdrop.wallpaperReady);
        jSONObject.put("blur_current_previews", PanelBackdrop.screenshotReady);
        jSONObject.put("blur_preview_error", PanelBackdrop.error);
        jSONObject.put("blur_preview_cache_hits", PanelBackdrop.cacheHits);
        jSONObject.put("blur_preview_cached", PanelBackdrop.isReady());
        jSONObject.put("framework_pin", FrameVoteWork.pinStatus);
        jSONObject.put("gesture_exclusion_policy_reads", GestureExclusionWork.reads);
        jSONObject.put("blur_preview_errors", PanelBackdrop.failures);
        jSONObject.put("shortened", NoShadeMotion.shortened);
        jSONObject.put("active_motion_hooks", NoShadeMotion.hooks);
        jSONObject.put("frame_vote_active", FrameVoteWork.active);
        jSONObject.put("frame_vote_calls", FrameVoteWork.calls);
        jSONObject.put("frame_vote_writes", FrameVoteWork.writes);
        jSONObject.put("frame_vote_forced", FrameVoteWork.forced);
        jSONObject.put("frame_vote_error", FrameVoteWork.error);
        jSONObject.put("media_preserved", NoShadeMotion.preserved);
        jSONObject.put("qs_alpha_direct", QsHotPaths.alphaWrites);
        jSONObject.put("frame_rate_stacks_skipped", QsHotPaths.frameStacksSkipped);
        jSONObject.put("media_curve_hits", QsHotPaths.curveHits);
        jSONObject.put("media_curve_probe_verified", QsHotPaths.curveProbeVerified);
        jSONObject.put("media_curve_error", QsHotPaths.curveError);
        jSONObject.put("qs_duplicate_targets", QsHotPaths.duplicateTargets);
        jSONObject.put("qs_direct_snapshot_reads", QsHotPaths.directTargetReads);
        jSONObject.put("qs_state_bindings_reused", QsHotPaths.stateBindingsReused);
        jSONObject.put("qs_state_bindings_built", QsHotPaths.stateBindingsBuilt);
        jSONObject.put("qs_state_bindings_evicted", QsHotPaths.stateBindingEvictions);
        jSONObject.put("qs_target_mode", QsHotPaths.targetMode);
        jSONObject.put("qs_target_error", QsHotPaths.targetError);
        jSONObject.put("qs_owned_target_instances", QsHotPaths.ownedTargetInstances);
        jSONObject.put("qs_target_creation_scopes", QsHotPaths.targetCreationScopes);
        jSONObject.put("notification_palette_passes", NotificationPalette.passes);
        jSONObject.put("notification_palette_hits", NotificationPalette.hits);
        jSONObject.put("notification_palette_misses", NotificationPalette.misses);
        jSONObject.put("icon_dimension_passes", IconDimensions.passes);
        jSONObject.put("icon_dimension_hits", IconDimensions.hits);
        jSONObject.put("icon_dimension_native_loads", IconDimensions.nativeLoads);
        jSONObject.put("icon_dimension_unchanged_hits", IconDimensions.unchangedHits);
        jSONObject.put("icon_dimension_snapshots_created", IconDimensions.snapshotsCreated);
        jSONObject.put("icon_dimension_error", IconDimensions.error);
        jSONObject.put("density_query_frames", DensityQueries.frames);
        jSONObject.put("density_query_hits", DensityQueries.hits);
        jSONObject.put("density_query_native_loads", DensityQueries.nativeLoads);
        jSONObject.put("density_query_error", DensityQueries.error);
        jSONObject.put("opening_window_batches", OpeningBatch.batches);
        jSONObject.put("opening_window_deferred_calls", OpeningBatch.deferredCalls);
        jSONObject.put("opening_window_flush_calls", OpeningBatch.flushCalls);
        jSONObject.put("opening_window_multi_request_batches", OpeningBatch.multiRequestBatches);
        jSONObject.put("opening_window_error", OpeningBatch.error);
        jSONObject.put("thermal_status", AdaptiveWorkload.thermalStatus);
        jSONObject.put("reduced_ui_workload", AdaptiveWorkload.reduced());
        jSONObject.put("effective_media_step", AdaptiveWorkload.mediaStep());
        jSONObject.put("workload_mode", Config.workloadMode);
        jSONObject.put("workload_level", AdaptiveWorkload.level());
        jSONObject.put("battery_temperature", AdaptiveWorkload.batteryTemperature);
        jSONObject.put("split_expansion_events", SplitPanelWork.expansionEvents);
        jSONObject.put("split_native_updates", SplitPanelWork.updates);
        jSONObject.put("split_work_error", SplitPanelWork.error);
        jSONObject.put("blur_workload_edge", AdaptiveWorkload.reduced() ? AdaptiveWorkload.blurEdge() : 0);
        jSONObject.put("reduced_blur_captures", AdaptiveWorkload.reducedCaptures);
        if ("android".equals(scope)) {
            jSONObject.put("display_policy_state", RefreshHook.state());
        }
        jSONObject.put("qs_panel_stacks_skipped", PanelDiagnostics.stateStacksSkipped);
        jSONObject.put("qs_touch_logs_skipped", PanelDiagnostics.touchLogsSkipped);
        if ("com.android.systemui".equals(scope)) {
            jSONObject.put("profile_state", PanelProfiler.state());
            jSONObject.put("profile_file", PanelProfiler.path());
            jSONObject.put("profile_error", PanelProfiler.error());
        }
        return jSONObject;
    }

    private ControlBus() {
    }
}
