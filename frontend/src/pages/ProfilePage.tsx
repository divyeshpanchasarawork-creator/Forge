import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { SectionHeader } from '@/components/ui/SectionHeader';
import { StatTile } from '@/components/ui/StatTile';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Callout } from '@/components/ui/Callout';
import { Badge } from '@/components/ui/Badge';
import { User, Code2, Save, RefreshCw, Target, Sparkles, Activity, Gauge, TrendingDown } from 'lucide-react';
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
      <h1 className="text-xl font-semibold tracking-tight">Profile</h1>

      {/* Target Level */}
      <Card className="border-primary/20">
        <CardHeader>
          <SectionHeader title="Target Level" icon={<Target className="h-4 w-4" />} />
        </CardHeader>
        <CardContent className="space-y-6">
          <p className="text-sm text-muted-foreground">
            Set your career target. Everything — recommendations, difficulty mix, problem targets — adapts to this level.
          </p>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">Your Target: <span className="text-primary">Level {targetLevel}</span></span>
              <span className="text-sm font-semibold text-foreground">{currentConfig.label}</span>
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
          <div className="space-y-4 rounded-xl bg-secondary/50 p-5">
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <StatTile label="Target Problems" value={currentConfig.targetTotal} tone="primary" />
              <StatTile label="Easy" value={`${currentConfig.easyPct}%`} tone="success" />
              <StatTile label="Medium" value={`${currentConfig.mediumPct}%`} tone="warning" />
              <StatTile label="Hard" value={`${currentConfig.hardPct}%`} tone="danger" />
            </div>

            <div className="flex h-3 overflow-hidden rounded-full bg-secondary" role="img" aria-label="Difficulty split preview">
              <div className="bg-success transition-all" style={{ width: `${currentConfig.easyPct}%` }} />
              <div className="bg-warning transition-all" style={{ width: `${currentConfig.mediumPct}%` }} />
              <div className="bg-destructive transition-all" style={{ width: `${currentConfig.hardPct}%` }} />
            </div>

            <p className="text-center text-caption text-muted-foreground">
              {currentConfig.companies}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Analysis Schedule */}
      <Card className="border-primary/20">
        <CardHeader>
          <SectionHeader title="Analysis Schedule" icon={<RefreshCw className="h-4 w-4" />} />
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
              className="rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-sm text-foreground focus:border-primary focus:outline-none"
            >
              <option value="">No scheduled analysis</option>
              {Array.from({ length: 48 }, (_, i) => {
                const h = String(Math.floor(i / 2)).padStart(2, '0');
                const m = i % 2 === 0 ? '00' : '30';
                return <option key={i} value={`${h}:${m}`}>{`${h}:${m}`}</option>;
              })}
            </select>
          </div>
          <div className="text-caption text-muted-foreground">
            Generations used today: {user?.dailyGenerationsUsed ?? 0} / 4
          </div>
          <div className="text-caption text-muted-foreground">
            Timezone: <span className="font-medium text-foreground">{browserTimezone}</span> — scheduled analysis runs in your local time.
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <SectionHeader title="Account Information" icon={<User className="h-4 w-4" />} />
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Username</label>
            <Input
              type="text"
              value={user?.username || ''}
              disabled
              className="opacity-60"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Display Name</label>
            <Input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Email</label>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <SectionHeader title="LeetCode Integration" icon={<Code2 className="h-4 w-4" />} />
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">LeetCode Username</label>
            <Input
              type="text"
              value={leetcodeUsername}
              onChange={(e) => setLeetcodeUsername(e.target.value)}
              placeholder="your_leetcode_username"
            />
          </div>

          {syncVisible && (
            <Button
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
              loading={syncMutation.isPending}
              variant="secondary"
            >
              <RefreshCw className="h-4 w-4" />
              {syncMutation.isPending ? 'Syncing…' : 'Sync LeetCode Data'}
            </Button>
          )}

          {syncMutation.isError && (
            <Callout tone="danger">Sync failed. Check your LeetCode username and try again.</Callout>
          )}

          {lcStats && (
            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <StatTile label="Total Solved" value={lcStats.totalSolved} tone="primary" />
                <StatTile label="Day Streak" value={lcStats.streak} tone="warning" />
                <StatTile label="Ranking" value={lcStats.ranking ? `#${lcStats.ranking.toLocaleString()}` : '-'} />
                <StatTile label="Active Days" value={lcStats.totalActiveDays} tone="success" />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <StatTile label="Easy" value={lcStats.easySolved} tone="success" />
                <StatTile label="Medium" value={lcStats.mediumSolved} tone="warning" />
                <StatTile label="Hard" value={lcStats.hardSolved} tone="danger" />
              </div>

              {lcStats.lastSyncedAt && (
                <p className="text-caption text-muted-foreground">
                  Last synced: {new Date(lcStats.lastSyncedAt).toLocaleString()}
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <SectionHeader title="Recommendation Engine" icon={<Sparkles className="h-4 w-4" />} />
        </CardHeader>
        <CardContent className="space-y-4">
          {reportLoading ? (
            <p className="text-sm text-muted-foreground">Loading engine health…</p>
          ) : reportError ? (
            <p className="text-sm text-destructive">Couldn't load engine health. Try again later.</p>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <KpiCard
                  icon={<Activity className="h-5 w-5 text-muted-foreground" />}
                  value={`${engineReport?.sampleCount ?? 0} / ${engineReport?.minSamples ?? 10}`}
                  label="Scored Samples"
                  tooltip={`Attempts with a stored signal snapshot used to evaluate the scorer. Calibration needs at least ${engineReport?.minSamples ?? 10} scored samples.`}
                />
                <KpiCard
                  icon={<Target className="h-5 w-5 text-muted-foreground" />}
                  value={fmt(engineReport?.liveAuc)}
                  label="Live Rank-AUC"
                  tooltip="Rank correlation between the active scorer and actual outcomes. 1.0 is a perfect ranking, 0.5 is random. Shows n/a until there are both success and failure samples."
                />
                <KpiCard
                  icon={<Gauge className="h-5 w-5 text-muted-foreground" />}
                  value={fmt(engineReport?.liveMse)}
                  label="Live MSE"
                  tooltip="Mean squared error between the predicted score and reward (quality/5, scaled to 0-100)."
                />
                <KpiCard
                  icon={<TrendingDown className="h-5 w-5 text-muted-foreground" />}
                  value={fmt(engineReport?.liveLogLoss)}
                  label="Live Log-Loss"
                  tooltip="Binary log-loss of the active scorer treating reward >= 0.6 as success."
                />
              </div>

              <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-secondary/50 px-4 py-3">
                <div className="text-caption text-muted-foreground">
                  {engineReport?.lastCalibratedAt
                    ? `Weights v${engineReport.version} · calibrated ${new Date(engineReport.lastCalibratedAt).toLocaleString()}`
                    : engineReport?.version != null
                      ? `Weights v${engineReport.version} — recorded metrics, not yet recalibrated.`
                      : 'No calibration applied yet — the scorer is using initial default weights.'}
                </div>
                <Button
                  onClick={() => calibrateMutation.mutate()}
                  disabled={calibrateMutation.isPending}
                  loading={calibrateMutation.isPending}
                  variant="secondary"
                >
                  <Sparkles className="h-4 w-4" />
                  {calibrateMutation.isPending ? 'Calibrating…' : 'Run Calibration'}
                </Button>
              </div>
              {calibrationMessage && (
                <Callout tone={calibrationMessage.applied ? 'success' : 'warning'}>
                  <p className="text-caption">{calibrationMessage.text}</p>
                </Callout>
              )}
              {calibrateMutation.isError && (
                <p className="text-sm text-destructive">Calibration failed. Try again later.</p>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center gap-4">
        <Button onClick={() => profileMutation.mutate()} disabled={profileMutation.isPending} loading={profileMutation.isPending}>
          <Save className="h-4 w-4" />
          {profileMutation.isPending ? 'Saving…' : 'Save Changes'}
        </Button>
        {saved && <Badge variant="success">Profile updated!</Badge>}
      </div>
    </div>
  );
}
