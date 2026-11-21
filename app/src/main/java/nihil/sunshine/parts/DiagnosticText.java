package nihil.sunshine.parts;

import java.util.Locale;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class DiagnosticText {
    private static final String[][] LABELS = {new String[]{"surface_diagnostic_work", "Surface transaction diagnostics"}, new String[]{"gesture_exclusion_work", "System gesture exclusion areas"}, new String[]{"blur_alpha_scope", "Blur alpha scope"}, new String[]{"static_glass", "Static panel background"}, new String[]{"panel_motion", "Panel motion"}, new String[]{"view_panel_motion", "Panel view motion"}, new String[]{"tile_height", "Tile height"}, new String[]{"qs_compose_motion", "QS Compose motion"}, new String[]{"blur_delivery", "Blur delivery"}, new String[]{"qs_gesture_work", "QS gesture handling"}, new String[]{"tile_graphics_layers", "Tile graphics layers"}, new String[]{"gpu_panel_blur", "GPU panel blur"}, new String[]{"blur_fraction_work", "Blur opacity updates"}, new String[]{"blur_source_size", "Blur capture size"}, new String[]{"current_blur_preview", "Blur preview"}, new String[]{"prepared_blur", "Prepared blur"}, new String[]{"qs_alpha_path", "QS opacity updates"}, new String[]{"compose_frame_diagnostics", "Frame rate diagnostics"}, new String[]{"media_curve_cache", "Media interpolation cache"}, new String[]{"qs_direct_snapshot", "Direct QS state reads"}, new String[]{"qs_target_coalescing", "Repeated QS targets"}, new String[]{"qs_target_ownership", "QS animation ownership"}, new String[]{"notification_palette", "Notification colors"}, new String[]{"icon_dimensions", "Icon dimensions"}, new String[]{"density_queries", "Display density queries"}, new String[]{"opening_window_batch", "Window layout batching"}, new String[]{"panel_state_diagnostics", "Panel state diagnostics"}, new String[]{"panel_touch_diagnostics", "Panel touch diagnostics"}, new String[]{"media_wave", "Media waves"}, new String[]{"split_panel_work", "Split panel motion"}, new String[]{"scene-warning", "Scene warnings"}, new String[]{"blur-frame-log", "Blur frame logging"}, new String[]{"flex-guard", "Flex layout guard"}, new String[]{"qs-job", "QS update jobs"}, new String[]{"qs-capture-timing", "Capture timing"}, new String[]{"tile-work", "Tile updates"}, new String[]{"legacy_layout_engine", "Panel layout engine"}, new String[]{"adaptive_workload", "Visual workload selection"}, new String[]{"smart_ram", "Smart RAM management"}, new String[]{"smart_ram_error", "Memory initialization error"}, new String[]{"control", "Module connection"}, new String[]{"notification_translation_work", "Notification movement"}, new String[]{"launcher_motion", "Home screen animations"}, new String[]{"launcher_diagnostic_work", "Home screen diagnostics"}, new String[]{"frame_vote_work", "Frame rate votes"}};
    private static final String[][] VALUES = {new String[]{"Native frame votes and flags without diagnostic formatting", "Native frame rate votes with less logging"}, new String[]{"View vote diagnostic path removed; native ViewGroup forwarding retained", "Native frame rate votes with less logging"}, new String[]{"Native apply preserved; named-transaction diagnostic work removed", "Native surface commits with less logging"}, new String[]{"Native unchanged-rectangle guard enabled", "Unchanged gesture areas reused by Android"}, new String[]{"initializing", "Waiting for application initialization"}, new String[]{"ready", "Installed"}, new String[]{"attached", "Integrated"}, new String[]{"disabled", "Off"}, new String[]{"native path", "Native path"}, new String[]{"native fallback", "Native fallback"}, new String[]{"unavailable", "Unavailable"}, new String[]{"creation", "Owned by each animation"}, new String[]{"per-pass cache", "Reused within each update"}, new String[]{"short native history", "Short native history"}, new String[]{"wallpaper fallback", "Wallpaper preview"}, new String[]{"Unchanged background deliveries bypassed", "Repeated backgrounds avoided"}, new String[]{"Native hit areas without diagnostic formatting", "Native touch areas with less logging"}, new String[]{"Native placement; inactive experimental hooks removed", "Native graphics layers"}, new String[]{"Native ROM blur; experimental GPU drawable disabled", "Experimental renderer disabled"}, new String[]{"Native alpha and mirror state without diagnostic strings", "Native opacity with less logging"}, new String[]{"Worker QUICK_PANEL pixels reduced before screenshot IPC", "Smaller captures before processing"}, new String[]{"Native capture size; reduced filter retained", "Native capture dimensions"}, new String[]{"Native frame votes; unverified framework replacements retired", "Native frame rate votes"}, new String[]{"Native Snapshot getters; no reflective state bindings", "Direct native state access"}, new String[]{"Native resource path; speculative cache retired", "Native icon dimensions"}, new String[]{"Native query path; per-frame wrapper retired", "Native density queries"}, new String[]{"Native batching; extra input wrapper retired", "Native window layout batching"}, new String[]{"Latest diagnostic state retained without stack/string/buffer work", "Latest state retained with less logging"}, new String[]{"formatting bypass", "Touch logging reduced"}, new String[]{"Native motion with lighter bookkeeping", "Native motion with fewer repeated updates"}, new String[]{"Event-driven visual workload", "Updated when the battery temperature changes"}, new String[]{"Pressure-aware module buffer maintenance; no forced GC or app kills", "Module buffers released under memory pressure"}, new String[]{"Unavailable; control connection retained", "Unavailable"}, new String[]{"Native translation without diagnostic stacks", "Native movement with fewer diagnostic allocations"}};

    static String label(String str) {
        for (String[] strArr : LABELS) {
            if (strArr[0].equals(str)) {
                return strArr[1];
            }
        }
        return str;
    }

    static String value(String str) {
        if (str != null && str.startsWith("Native fallback: framework fingerprint")) {
            return "Framework version not recognized";
        }
        if (str == null || str.isEmpty()) {
            return "Not reported";
        }
        for (String[] strArr : VALUES) {
            if (strArr[0].equals(str)) {
                return strArr[1];
            }
        }
        return str.replace("SystemUI", "System UI");
    }

    static String status(String str, String str2, JSONObject jSONObject, int i) {
        if ("initializing".equals(str2)) {
            return value(str2);
        }
        if (str2 == null || str2.toLowerCase(Locale.ROOT).contains("unavailable") || str2.toLowerCase(Locale.ROOT).contains("fallback")) {
            return value(str2);
        }
        if (jSONObject != null) {
            if ("prepared_blur".equals(str)) {
                return jSONObject.optBoolean("prepared_blur") ? "Installed for this profile" : "Disabled by this profile";
            }
            if ("current_blur_preview".equals(str)) {
                return "Single native blur pass";
            }
            if ("static_glass".equals(str) && jSONObject.optBoolean("native_blur")) {
                return "Native blur selected";
            }
            if (("panel_motion".equals(str) || "view_panel_motion".equals(str) || "qs_compose_motion".equals(str) || "tile_height".equals(str)) && jSONObject.optBoolean("native_animations")) {
                if (i == 1 || i == 3) {
                    return "Panel animations at 0.8×";
                }
                return i == 2 ? "Panel animations at 0.5×" : "Native animations selected";
            }
            if ("smart_ram".equals(str)) {
                return jSONObject.optBoolean("smart_ram") ? "Monitoring memory pressure" : "Off";
            }
            if ("adaptive_workload".equals(str)) {
                return jSONObject.optInt("workload_mode") == 1 ? "Temperature-based selection" : "Manual selection";
            }
        }
        return value(str2);
    }

    private DiagnosticText() {
    }
}
