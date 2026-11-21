package nihil.sunshine.parts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class CpuMonitor {
    static final String READ = "for p in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do [ -d \"$p\" ] || continue; read -r lo < \"$p/scaling_min_freq\" || lo=0; read -r hi < \"$p/scaling_max_freq\" || hi=0; read -r cur < \"$p/scaling_cur_freq\" || cur=0; read -r hw < \"$p/cpuinfo_max_freq\" || hw=0; read -r cores < \"$p/related_cpus\" || cores=''; read -r floor < \"$p/cpuinfo_min_freq\" || floor=0; printf '%s|%s|%s|%s|%s|%s|%s\\n' \"${p##*/}\" \"$lo\" \"$hi\" \"$cur\" \"$hw\" \"$cores\" \"$floor\"; done\nwhile read -r cpu user nice system idle iowait irq softirq steal rest; do case \"$cpu\" in cpu[0-9]*) printf 'stat|%s|%s|%s|%s|%s|%s|%s|%s|%s\\n' \"$cpu\" \"$user\" \"$nice\" \"$system\" \"$idle\" \"$iowait\" \"$irq\" \"$softirq\" \"$steal\" ;; cpu) ;; *) break ;; esac; done < /proc/stat\n";
    static final int SAMPLE_MS = 200;
    static final String SCRIPT = "[ \"$(id -u)\" = 0 ] || exit 13\nfor p in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do [ -d \"$p\" ] || continue; read -r available < \"$p/scaling_available_frequencies\" || available=''; printf 'opps|%s|%s\\n' \"${p##*/}\" \"$available\"; done\nwhile :; do\nfor p in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do [ -d \"$p\" ] || continue; read -r lo < \"$p/scaling_min_freq\" || lo=0; read -r hi < \"$p/scaling_max_freq\" || hi=0; read -r cur < \"$p/scaling_cur_freq\" || cur=0; read -r hw < \"$p/cpuinfo_max_freq\" || hw=0; read -r cores < \"$p/related_cpus\" || cores=''; read -r floor < \"$p/cpuinfo_min_freq\" || floor=0; printf '%s|%s|%s|%s|%s|%s|%s\\n' \"${p##*/}\" \"$lo\" \"$hi\" \"$cur\" \"$hw\" \"$cores\" \"$floor\"; done\nwhile read -r cpu user nice system idle iowait irq softirq steal rest; do case \"$cpu\" in cpu[0-9]*) printf 'stat|%s|%s|%s|%s|%s|%s|%s|%s|%s\\n' \"$cpu\" \"$user\" \"$nice\" \"$system\" \"$idle\" \"$iowait\" \"$irq\" \"$softirq\" \"$steal\" ;; cpu) ;; *) break ;; esac; done < /proc/stat\nprintf '@\\n'\nIFS= read -r sunshine_next || exit 0\ndone\n";
    private volatile Process process;
    private volatile boolean stopped;
    private final Thread thread;

    interface Launcher {
        Process start(String str) throws IOException;
    }

    interface Listener {
        void error(String str);

        void sample(long j, List<CpuPolicy> list);
    }

    CpuMonitor(Listener listener) {
        this(listener, new Launcher() { // from class: nihil.sunshine.parts.CpuMonitor$$ExternalSyntheticLambda0
            @Override // nihil.sunshine.parts.CpuMonitor.Launcher
            public final Process start(String str) {
                return new ProcessBuilder("su", "-c", str).redirectErrorStream(true).start();
            }
        });
    }

    CpuMonitor(final Listener listener, final Launcher launcher) {
        this.thread = new Thread(new Runnable() { // from class: nihil.sunshine.parts.CpuMonitor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m4lambda$new$1$nihilsunshinepartsCpuMonitor(launcher, listener);
            }
        }, "SunshineCpuReadings");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    /* JADX INFO: renamed from: lambda$new$1$nihil-sunshine-parts-CpuMonitor, reason: not valid java name */
    /* synthetic */ void m4lambda$new$1$nihilsunshinepartsCpuMonitor(Launcher launcher, Listener listener) {
        Process process;
        String line;
        CpuFrequencies cpuFrequencies;
        try {
            try {
                Process processStart = launcher.start(SCRIPT);
                this.process = processStart;
                if (this.stopped) {
                    closeLease(processStart);
                    processStart.destroy();
                    Process process2 = this.process;
                    if (process2 != null) {
                        closeLease(process2);
                        process2.destroy();
                        return;
                    }
                    return;
                }
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processStart.getInputStream(), "UTF-8"));
                try {
                    ArrayList arrayList = new ArrayList();
                    CpuLoad cpuLoad = new CpuLoad();
                    HashMap map = new HashMap();
                    while (!this.stopped && (line = bufferedReader.readLine()) != null) {
                        if (line.equals("@")) {
                            long jNanoTime = System.nanoTime();
                            List<CpuPolicy> listFinish = cpuLoad.finish(arrayList);
                            if (arrayList.isEmpty()) {
                                listener.error("CPU readings are not available.");
                            } else {
                                listener.sample(System.nanoTime() / 1000000, listFinish);
                            }
                            arrayList.clear();
                            if (this.stopped) {
                                break;
                            }
                            long jNanoTime2 = 200 - ((System.nanoTime() - jNanoTime) / 1000000);
                            if (jNanoTime2 > 0) {
                                Thread.sleep(jNanoTime2);
                            }
                            if (this.stopped) {
                                break;
                            }
                            processStart.getOutputStream().write(10);
                            processStart.getOutputStream().flush();
                        } else if (line.startsWith("opps|")) {
                            String[] strArrSplit = line.split("\\|", 3);
                            if (strArrSplit.length == 3 && strArrSplit[1].matches("policy[0-9]{1,3}") && map.size() < 16 && (cpuFrequencies = CpuFrequencies.parse(strArrSplit[2])) != null) {
                                map.put(strArrSplit[1], cpuFrequencies);
                            }
                        } else if (line.startsWith("stat|")) {
                            cpuLoad.accept(line);
                        } else {
                            CpuPolicy cpuPolicy = CpuHistory.parse(line);
                            if (cpuPolicy != null && arrayList.size() < 16) {
                                arrayList.add(cpuPolicy.withFrequencies((CpuFrequencies) map.get(cpuPolicy.name)));
                            }
                        }
                    }
                    bufferedReader.close();
                    if (!this.stopped) {
                        listener.error("CPU monitor stopped. Check root access.");
                    }
                    process = this.process;
                    if (process == null) {
                        return;
                    }
                    closeLease(process);
                    process.destroy();
                } catch (Throwable th) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (Exception e) {
                if (!this.stopped) {
                    listener.error("Couldn't read CPU clocks. Check root access.");
                }
                process = this.process;
                if (process == null) {
                }
            }
        } catch (Throwable th3) {
            Process process3 = this.process;
            if (process3 != null) {
                closeLease(process3);
                process3.destroy();
            }
            throw th3;
        }
    }

    private static void closeLease(Process process) {
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
        }
    }

    void stop() {
        this.stopped = true;
        Process process = this.process;
        if (process != null) {
            closeLease(process);
            this.thread.interrupt();
            process.destroy();
        }
    }
}
