import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { User, Code2, Save, RefreshCw, Trophy, Flame, TrendingUp, Target, Sparkles, Activity, Gauge, TrendingDown } from 'lucide-react';
import { authApi, leetcodeApi, calibrationApi } from '@/api';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getTargetLevel } from '@/lib/targetLevels';
import KpiCard from '@/components/ui/KpiCard';

const fmt = (v?: number | null) =>
  v != null && Number.isFinite(v) ? v.toFixed(2) : '—';

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const queryClient = useQueryClient();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [email, setEmail] = useState(user?.email || '');
  const [leetcodeUsername, setLeetcodeUsername] = useState(user?.leetcodeUsername || '');
  const [targetLevel, setTargetLevel] = useState(user?.targetLevel ?? 5);
  const [preferredAnalysisTime, setPreferredAnalysisTime] = useState(user?.preferredAnalysisTime || '');
  const [saved, setSaved] = useState(false);
  const [syncVisible, setSyncVisible] = useState(!!user?.leetcodeUsername);

  const currentConfig = getTargetLevel(targetLevel);
  const browserTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  useEffect(() => {
    setSyncVisible(!!leetcodeUsername);
  }, [leetcodeUsername]);

  const profileMutation = useMutation({
    mutationFn: () => authApi.updateProfile({ displayName, email, leetcodeUsername, targetLevel, preferredAnalysisTime: preferredAnalysisTime || undefined, timezone: browserTimezone }),
    onSuccess: (res) => {
      setUser?.(res.data.data);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    },
  });

  const syncMutation = useMutation({
    mutationFn: () => leetcodeApi.sync(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leetcode-stats'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      queryClient.invalidateQueries({ queryKey: ['practice', 'queue'] });
      queryClient.invalidateQueries({ queryKey: ['memory'] });
      queryClient.invalidateQueries({ queryKey: ['roadmap-analysis'] });
    },
  });

  const { data: lcData } = useQuery({
    queryKey: ['leetcode-stats'],
    queryFn: () => leetcodeApi.getStats().then((res) => res.data.data),
    enabled: !!user?.leetcodeUsername,
  });

  const lcStats = lcData;

  const { data: engineReport, isLoading: reportLoading, isError: reportError } = useQuery({
    queryKey: ['engine-report'],
    queryFn: () => calibrationApi.getReport().then((res) => res.data.data),
    retry: 1,
  });

  const [calibrationMessage, setCalibrationMessage] = useState<{ text: string; applied: boolean } | null>(null);

  const calibrateMutation = useMutation({
    mutationFn: () => calibrationApi.runCalibration(),
    onSuccess: (res) => {
      const result = res.data.data;
      setCalibrationMessage({ text: result.message, applied: result.applied });
      queryClient.invalidateQueries({ queryKey: ['engine-report'] });
    },
    onError: () => {
      setCalibrationMessage(null);
    },
  });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Profile</h1>

      {/* Target Level */}
      <Card className="border-primary/20">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="h-4 w-4 text-primary" />
            Target Level
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <p className="text-sm text-muted-foreground">
            Set your career target. Everything — recommendations, difficulty mix, problem targets — adapts to this level.
          </p>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">Your Target: <span className="text-primary">Level {targetLevel}</span></span>
              <span className={`text-sm font-semibold ${currentConfig.color}`}>{currentConfig.label}</span>
            </div>

            <input
              type="range"
              min={1}
              max={10}
              value={targetLevel}
              onChange={(e) => setTargetLevel(Number(e.target.value))}
              className="w-full accent-primary"
            />

            <div className="flex justify-between text-xs text-muted-foreground">
              <span>1 — Service</span>
              <span>5 — Product</span>
              <span>10 — Elite</span>
            </div>
          </div>

          {/* Live preview */}
          <div className="rounded-xl bg-secondary/50 p-5 space-y-4">
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              <div className="text-center">
                <p className="text-lg font-bold text-primary">{currentConfig.targetTotal}</p>
                <p className="text-xs text-muted-foreground">Target Problems</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-green-400">{currentConfig.easyPct}%</p>
                <p className="text-xs text-muted-foreground">Easy</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-yellow-400">{currentConfig.mediumPct}%</p>
                <p className="text-xs text-muted-foreground">Medium</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-red-400">{currentConfig.hardPct}%</p>
                <p className="text-xs text-muted-foreground">Hard</p>
              </div>
            </div>

            <div className="flex h-3 overflow-hidden rounded-full bg-secondary">
              <div className="bg-green-400 transition-all" style={{ width: `${currentConfig.easyPct}%` }} />
              <div className="bg-yellow-400 transition-all" style={{ width: `${currentConfig.mediumPct}%` }} />
              <div className="bg-red-400 transition-all" style={{ width: `${currentConfig.hardPct}%` }} />
            </div>

            <p className="text-xs text-muted-foreground text-center">
              {currentConfig.companies}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Analysis Schedule */}
      <Card className="border-primary/20">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <RefreshCw className="h-4 w-4 text-primary" />
            Analysis Schedule
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Set a preferred time for daily analysis. When enabled, Forge will auto-generate your roadmap
            once daily within 15 minutes of your chosen time (max 4/day).
          </p>
          <div className="flex items-center gap-3">
            <select
              value={preferredAnalysisTime}
              onChange={(e) => setPreferredAnalysisTime(e.target.value)}
              className="rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground focus:border-primary focus:outline-none"
            >
              <option value="">No scheduled analysis</option>
              {Array.from({ length: 48 }, (_, i) => {
                const h = String(Math.floor(i / 2)).padStart(2, '0');
                const m = i % 2 === 0 ? '00' : '30';
                return <option key={i} value={`${h}:${m}`}>{`${h}:${m}`}</option>;
              })}
            </select>
          </div>
          <div className="text-xs text-muted-foreground">
            Generations used today: {user?.dailyGenerationsUsed ?? 0} / 4
          </div>
          <div className="text-xs text-muted-foreground">
            Timezone: <span className="font-medium text-foreground">{browserTimezone}</span> — scheduled analysis runs in your local time.
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-4 w-4" />
            Account Information
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Username</label>
            <input
              type="text"
              value={user?.username || ''}
              disabled
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground opacity-60"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Display Name</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Code2 className="h-4 w-4" />
            LeetCode Integration
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">LeetCode Username</label>
            <input
              type="text"
              value={leetcodeUsername}
              onChange={(e) => setLeetcodeUsername(e.target.value)}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="your_leetcode_username"
            />
          </div>

          {syncVisible && (
            <button
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-sm font-medium text-primary hover:bg-primary/20 disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
              {syncMutation.isPending ? 'Syncing...' : 'Sync LeetCode Data'}
            </button>
          )}

          {syncMutation.isError && (
            <p className="text-sm text-red-400">Sync failed. Check your LeetCode username and try again.</p>
          )}

          {lcStats && (
            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <p className="text-2xl font-bold text-primary">{lcStats.totalSolved}</p>
                  <p className="text-xs text-muted-foreground">Total Solved</p>
                </div>
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <Flame className="h-4 w-4 text-orange-400" />
                    <p className="text-2xl font-bold text-orange-400">{lcStats.streak}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">Day Streak</p>
                </div>
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <Trophy className="h-4 w-4 text-yellow-400" />
                    <p className="text-2xl font-bold text-yellow-400">{lcStats.ranking ? `#${lcStats.ranking.toLocaleString()}` : '-'}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">Ranking</p>
                </div>
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <TrendingUp className="h-4 w-4 text-green-400" />
                    <p className="text-2xl font-bold text-green-400">{lcStats.totalActiveDays}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">Active Days</p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="flex-1 rounded-lg bg-green-500/10 px-3 py-2 text-center">
                  <p className="text-lg font-bold text-green-400">{lcStats.easySolved}</p>
                  <p className="text-xs text-green-400/70">Easy</p>
                </div>
                <div className="flex-1 rounded-lg bg-yellow-500/10 px-3 py-2 text-center">
                  <p className="text-lg font-bold text-yellow-400">{lcStats.mediumSolved}</p>
                  <p className="text-xs text-yellow-400/70">Medium</p>
                </div>
                <div className="flex-1 rounded-lg bg-red-500/10 px-3 py-2 text-center">
                  <p className="text-lg font-bold text-red-400">{lcStats.hardSolved}</p>
                  <p className="text-xs text-red-400/70">Hard</p>
                </div>
              </div>

              {lcStats.lastSyncedAt && (
                <p className="text-xs text-muted-foreground">
                  Last synced: {new Date(lcStats.lastSyncedAt).toLocaleString()}
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-primary" />
            Recommendation Engine
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {reportLoading ? (
            <p className="text-sm text-muted-foreground">Loading engine health…</p>
          ) : reportError ? (
            <p className="text-sm text-red-400">Couldn't load engine health. Try again later.</p>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <KpiCard
                  icon={<Activity className="h-5 w-5 text-primary" />}
                  value={`${engineReport?.sampleCount ?? 0} / ${engineReport?.minSamples ?? 10}`}
                  label="Scored Samples"
                  tooltip={`Attempts with a stored signal snapshot used to evaluate the scorer. Calibration needs at least ${engineReport?.minSamples ?? 10} scored samples.`}
                />
                <KpiCard
                  icon={<Target className="h-5 w-5 text-primary" />}
                  value={fmt(engineReport?.liveAuc)}
                  label="Live Rank-AUC"
                  tooltip="Rank correlation between the active scorer and actual outcomes. 1.0 is a perfect ranking, 0.5 is random. Shows n/a until there are both success and failure samples."
                />
                <KpiCard
                  icon={<Gauge className="h-5 w-5 text-primary" />}
                  value={fmt(engineReport?.liveMse)}
                  label="Live MSE"
                  tooltip="Mean squared error between the predicted score and reward (quality/5, scaled to 0-100)."
                />
                <KpiCard
                  icon={<TrendingDown className="h-5 w-5 text-primary" />}
                  value={fmt(engineReport?.liveLogLoss)}
                  label="Live Log-Loss"
                  tooltip="Binary log-loss of the active scorer treating reward >= 0.6 as success."
                />
              </div>

              <div className="flex items-center justify-between gap-3 rounded-lg bg-secondary/50 px-4 py-3">
                <div className="text-xs text-muted-foreground">
                  {engineReport?.lastCalibratedAt
                    ? `Weights v${engineReport.version} · calibrated ${new Date(engineReport.lastCalibratedAt).toLocaleString()}`
                    : engineReport?.version != null
                      ? `Weights v${engineReport.version} — recorded metrics, not yet recalibrated.`
                      : 'No calibration applied yet — the scorer is using initial default weights.'}
                </div>
                <button
                  onClick={() => calibrateMutation.mutate()}
                  disabled={calibrateMutation.isPending}
                  className="flex shrink-0 items-center gap-2 rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-sm font-medium text-primary hover:bg-primary/20 disabled:opacity-50"
                >
                  <Sparkles className={`h-4 w-4 ${calibrateMutation.isPending ? 'animate-pulse' : ''}`} />
                  {calibrateMutation.isPending ? 'Calibrating...' : 'Run Calibration'}
                </button>
              </div>
              {calibrationMessage && (
                <p className={`text-sm ${calibrationMessage.applied ? 'text-green-400' : 'text-amber-400'}`}>
                  {calibrationMessage.text}
                </p>
              )}
              {calibrateMutation.isError && (
                <p className="text-sm text-red-400">Calibration failed. Try again later.</p>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center gap-4">
        <button
          onClick={() => profileMutation.mutate()}
          disabled={profileMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          <Save className="h-4 w-4" />
          {profileMutation.isPending ? 'Saving...' : 'Save Changes'}
        </button>
        {saved && <span className="text-sm text-green-400">Profile updated!</span>}
      </div>
    </div>
  );
}
