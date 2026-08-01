import { useMemo, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { analyticsApi, journalsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { ChartSkeleton, SkeletonCard } from '@/components/ui/LoadingSkeleton';
import { targetLevels, getTargetLevel } from '@/lib/targetLevels';
import {
  BarChart, Bar, LineChart, Line, ReferenceDot, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Cell,
} from 'recharts';
import {
  Target, Zap, BarChart3, Gauge, Flame, AlertTriangle,
  Code2, BookOpen, TrendingUp, Sparkles, ArrowRight, TrendingDown, Trophy, Lightbulb, Info,
} from 'lucide-react';
import type { AnalyticsResponse, Journal, WeeklyProgress, LearningCurveResponse, Insight } from '@/types';

const readinessColor = (score: number) => {
  if (score >= 80) return 'text-green-400';
  if (score >= 50) return 'text-yellow-400';
  if (score >= 30) return 'text-orange-400';
  return 'text-red-400';
};

const readinessBg = (score: number) => {
  if (score >= 80) return 'bg-green-500';
  if (score >= 50) return 'bg-yellow-500';
  if (score >= 30) return 'bg-orange-500';
  return 'bg-red-500';
};

const accentMap = {
  primary: 'bg-primary/10 text-primary',
  yellow: 'bg-yellow-500/10 text-yellow-400',
  orange: 'bg-orange-500/10 text-orange-400',
  red: 'bg-red-500/10 text-red-400',
  green: 'bg-green-500/10 text-green-400',
  purple: 'bg-purple-500/10 text-purple-400',
} as const;

type Accent = keyof typeof accentMap;

const insightMeta: Record<string, { icon: ReactNode; accent: Accent; value: (i: Insight) => string }> = {
  MASTERY: {
    icon: <TrendingUp className="h-4 w-4" />,
    accent: 'primary',
    value: (i) => `${Math.round(i.metric ?? 0)}% avg`,
  },
  SKILL: {
    icon: <Gauge className="h-4 w-4" />,
    accent: 'yellow',
    value: (i) => `${Math.round(i.metric ?? 0)} rating`,
  },
  CONSISTENCY: {
    icon: <Flame className="h-4 w-4" />,
    accent: 'orange',
    value: (i) => `${Math.round(i.metric ?? 0)}%`,
  },
  ACCURACY: {
    icon: <Target className="h-4 w-4" />,
    accent: 'green',
    value: (i) => `${Math.round(i.metric ?? 0)}%`,
  },
  PROGRESS: {
    icon: <BarChart3 className="h-4 w-4" />,
    accent: 'purple',
    value: (i) => `${Math.round(i.metric ?? 0)} solved`,
  },
  STREAK: {
    icon: <Flame className="h-4 w-4" />,
    accent: 'orange',
    value: (i) => `${Math.round(i.metric ?? 0)}-day streak`,
  },
};

const DIFF_MIX_EXPLAIN =
  'Your solved-problem split across Easy / Medium / Hard compared with the target mix for your level. Closing the Hard gap is the fastest way up.';

const insightExplain: Record<string, { what: string; improve: string }> = {
  MASTERY: {
    what: 'Average mastery (0–100) across the topics you are actively learning, built from your SM-2 easiness factor, retention decay, and recent attempt quality.',
    improve: 'Review your weakest categories and solve problems in the lowest-mastery topics to pull this up.',
  },
  SKILL: {
    what: 'Your Elo-style rating (starts near 1000; 0 means it has not been computed yet). It rises when you solve problems at or above your current rating.',
    improve: 'Tackle problems slightly above your level and keep up with revision to climb.',
  },
  CONSISTENCY: {
    what: 'Share of the last 14 days where you practiced or revised. Consistency is the strongest predictor of interview performance.',
    improve: 'Even a few focused minutes daily beats a big weekend session — build the habit.',
  },
  ACCURACY: {
    what: 'Share of tracked attempts resolved — SOLVED counts fully, PARTIAL counts half. Until you log attempts, there is nothing to measure.',
    improve: 'Log your result from the Practice page right after each problem to unlock accuracy tracking.',
  },
  PROGRESS: {
    what: 'Total problems marked solved on LeetCode, synced from your profile.',
    improve: 'Sync LeetCode after each session to keep this number fresh.',
  },
  STREAK: {
    what: 'Consecutive days with a journal entry. Daily reps compound into mastery.',
    improve: 'Log a journal entry every day, even a one-liner, to protect the streak.',
  },
};

function explainFor(insight: Insight): { what: string; improve: string } {
  if (insight.title === 'Difficulty Mix') return { what: DIFF_MIX_EXPLAIN, improve: 'Prioritize Hard problems at your level to close the gap.' };
  return insightExplain[insight.type] ?? {
    what: insight.message,
    improve: 'Keep practicing — this metric updates as you log more activity.',
  };
}

function InsightCard({ insight }: { insight: Insight }) {
  const meta = insightMeta[insight.type] || {
    icon: <Lightbulb className="h-4 w-4" />,
    accent: 'primary' as Accent,
    value: (i: Insight) => (i.metric != null ? `${i.metric}` : ''),
  };
  const explain = explainFor(insight);
  const isUnlock = insight.metric == null && !insight.display;
  const delta = insight.delta;
  const deltaBadge =
    typeof delta === 'number' && delta !== 0 ? (
      <span
        className={`inline-flex items-center gap-1 text-[11px] font-semibold ${delta > 0 ? 'text-green-400' : 'text-red-400'}`}
      >
        {delta > 0 ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
        {delta > 0 ? '+' : ''}{delta}
      </span>
    ) : null;

  return (
    <div
      className={`rounded-xl border p-5 shadow-soft transition-all ${
        isUnlock ? 'border-dashed border-border bg-card/40' : 'border-border bg-card hover:border-primary/20'
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className={`inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${accentMap[meta.accent]}`}>
          {meta.icon}
        </span>
        <span className="flex min-w-0 items-center gap-1">
          <span className="truncate text-right text-[11px] uppercase tracking-wider text-muted-foreground">{insight.title}</span>
          <button
            type="button"
            aria-label={`What is ${insight.title}?`}
            className="group/btn relative shrink-0 rounded-md p-0.5 text-muted-foreground/60 transition-colors hover:text-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
          >
            <Info className="h-3.5 w-3.5" />
            <span className="pointer-events-none absolute bottom-full right-0 z-20 mb-2 hidden w-64 rounded-lg border border-border bg-card p-3 text-left shadow-2xl group-hover/btn:block group-focus-within/btn:block">
              <p className="text-[11px] font-semibold text-foreground">What is this?</p>
              <p className="mt-1 text-[11px] leading-relaxed text-muted-foreground">{explain.what}</p>
              <p className="mt-1.5 text-[11px] leading-relaxed text-muted-foreground">
                <span className="font-medium text-foreground">How to improve:</span> {explain.improve}
              </p>
            </span>
          </button>
        </span>
      </div>
      {!isUnlock && (
        <div className="mt-3 flex items-center justify-between">
          <p className="text-xl font-bold leading-tight tracking-tight">{insight.display ?? meta.value(insight)}</p>
          {deltaBadge}
        </div>
      )}
      <p className={`text-xs leading-relaxed text-muted-foreground ${isUnlock ? 'mt-3' : 'mt-1.5'}`}>{insight.message}</p>
    </div>
  );
}

const curveLines = [
  { key: 'mastery', label: 'Mastery', color: '#6d5dfc' },
  { key: 'confidence', label: 'Confidence', color: '#22c55e' },
  { key: 'retention', label: 'Retention', color: '#f59e0b' },
  { key: 'skillRating', label: 'Skill', color: '#38bdf8' },
  { key: 'consistency', label: 'Consistency', color: '#c084fc' },
] as const;

function LearningCurveChart({ data }: { data: LearningCurveResponse }) {
  const [active, setActive] = useState<Set<string>>(new Set(['mastery', 'retention', 'skillRating']));

  const toggle = (key: string) => {
    setActive((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const points = data.points
    .map((p) => ({
      ...p,
      date: p.date.slice(5),
    }))
    .filter(
      (p) =>
        Number.isFinite(p.mastery) &&
        Number.isFinite(p.confidence) &&
        Number.isFinite(p.retention) &&
        Number.isFinite(p.skillRating) &&
        Number.isFinite(p.consistency),
    );

  const masteryMilestones = data.milestones
    .filter((m) => (m.type === 'MASTERY' || m.type === 'SKILL') && !!m.date && m.date.length >= 10)
    .map((m) => {
      const match = m.label.match(/(\d{3,4})/);
      return {
        ...m,
        x: m.date.slice(5),
        y: m.type === 'SKILL' ? (match ? Number(match[1]) : 1100) : match ? Number(match[1]) : 50,
        axis: m.type === 'SKILL' ? 'right' : 'left',
      };
    });

  const leftLines = curveLines.filter((l) => l.key !== 'skillRating');
  const skillLine = curveLines.find((l) => l.key === 'skillRating');

  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-2">
        {curveLines.map((l) => (
          <button
            key={l.key}
            onClick={() => toggle(l.key)}
            className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium transition-all ${
              active.has(l.key) ? 'bg-primary/10 text-primary' : 'bg-secondary text-muted-foreground'
            }`}
          >
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: l.color }} />
            {l.label}
          </button>
        ))}
      </div>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={points} margin={{ top: 10, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid stroke="var(--color-border)" strokeDasharray="3 3" />
          <XAxis dataKey="date" stroke="var(--color-border)" tick={{ fill: 'var(--color-muted-foreground)' }} fontSize={11} tickMargin={6} />
          <YAxis yAxisId="left" domain={[0, 100]} stroke="var(--color-border)" tick={{ fill: 'var(--color-muted-foreground)' }} fontSize={11} width={34} />
          <YAxis yAxisId="right" orientation="right" domain={[0, 2800]} tickCount={5} stroke="#38bdf8" tick={{ fill: 'var(--color-muted-foreground)' }} fontSize={11} width={40} />
          <Tooltip
            contentStyle={{
              backgroundColor: 'var(--color-card)',
              border: '1px solid var(--color-border)',
              borderRadius: '8px',
              fontSize: '12px',
              color: 'var(--color-foreground)',
            }}
          />
          {leftLines.map((l) =>
            active.has(l.key) ? (
              <Line
                key={l.key}
                yAxisId="left"
                type="monotone"
                dataKey={l.key}
                stroke={l.color}
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 3 }}
              />
            ) : null
          )}
          {skillLine && active.has(skillLine.key) ? (
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="skillRating"
              stroke={skillLine.color}
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 3 }}
            />
          ) : null}
          {masteryMilestones.map((m, i) => (
            <ReferenceDot
              key={i}
              x={m.x}
              y={m.y}
              yAxisId={m.axis as 'left' | 'right'}
              r={5}
              fill="#ef4444"
              stroke="#0a0a0b"
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
      {data.milestones.length > 0 && (
        <div className="mt-4 space-y-1.5">
          <p className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
            <Trophy className="h-3.5 w-3.5 text-yellow-400" /> Milestones
          </p>
          {data.milestones.map((m, i) => (
            <div key={i} className="flex items-center gap-2 text-xs">
              <span className="shrink-0 rounded bg-secondary px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">{m.date}</span>
              <span className="text-foreground">{m.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function SoWhat({ children }: { children: ReactNode }) {
  return (
    <p className="mt-4 flex items-start gap-2 rounded-lg bg-primary/5 px-3 py-2 text-xs leading-relaxed text-muted-foreground">
      <Sparkles className="mt-0.5 h-3.5 w-3.5 shrink-0 text-primary" />
      <span>
        <span className="font-medium text-foreground">So what?</span> {children}
      </span>
    </p>
  );
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function ConsistencyHeatmap({ journals }: { journals: Journal[] }) {
  const grid = useMemo(() => {
    const byDate = new Map<string, number>();
    for (const j of journals) {
      if (!j.entryDate) continue;
      const key = j.entryDate.slice(0, 10);
      byDate.set(key, (byDate.get(key) ?? 0) + (j.hoursStudied ?? 1));
    }

    const today = new Date();
    const start = new Date(today);
    start.setDate(today.getDate() - 27 * 7);
    start.setHours(0, 0, 0, 0);
    start.setDate(start.getDate() - start.getDay());

    const cells: { d: Date; v: number }[] = [];
    for (let i = 0; i < 28 * 7; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      cells.push({ d, v: byDate.get(key) ?? 0 });
    }

    const weeks: { d: Date; v: number }[][] = [];
    for (let i = 0; i < cells.length; i += 7) weeks.push(cells.slice(i, i + 7));

    const max = Math.max(1, ...cells.map((c) => c.v));

    const monthLabels: { index: number; label: string }[] = [];
    let prevMonth = -1;
    weeks.forEach((w, i) => {
      const m = w[0].d.getMonth();
      if (m !== prevMonth) {
        monthLabels.push({ index: i, label: MONTHS[m] });
        prevMonth = m;
      }
    });

    return { weeks, max, monthLabels };
  }, [journals]);

  const levels = ['bg-secondary', 'bg-primary/30', 'bg-primary/50', 'bg-primary/75', 'bg-primary'];

  const levelFor = (v: number) => {
    if (v <= 0) return 0;
    const ratio = v / grid.max;
    if (ratio <= 0.25) return 1;
    if (ratio <= 0.5) return 2;
    if (ratio <= 0.75) return 3;
    return 4;
  };

  const dayLabels = ['', 'Mon', '', 'Wed', '', 'Fri', ''];

  return (
    <div>
      <div className="ml-7 mb-1.5 flex gap-[3px]">
        {grid.weeks.map((_, i) => (
          <span key={i} className="w-3 text-[9px] font-medium text-muted-foreground">
            {grid.monthLabels.find((m) => m.index === i)?.label ?? ''}
          </span>
        ))}
      </div>
      <div className="flex gap-[3px]">
        <div className="mr-1.5 flex w-6 flex-col gap-[3px]">
          {dayLabels.map((dl, i) => (
            <span key={i} className="flex h-3 items-center text-[9px] leading-3 text-muted-foreground">
              {dl}
            </span>
          ))}
        </div>
        {grid.weeks.map((w, i) => (
          <div key={i} className="flex flex-col gap-[3px]">
            {w.map((c, j) => (
              <div
                key={j}
                title={`${c.d.toDateString()}: ${c.v > 0 ? `${c.v % 1 === 0 ? c.v : c.v.toFixed(1)}h logged` : 'no entry'}`}
                className={`h-3 w-3 rounded-[3px] ${levels[levelFor(c.v)]}`}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-end gap-1 text-[10px] text-muted-foreground">
        Less
        {levels.map((l, i) => (
          <span key={i} className={`h-3 w-3 rounded-[3px] ${l}`} />
        ))}
        More
      </div>
    </div>
  );
}

export default function AnalyticsPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery<AnalyticsResponse>({
    queryKey: ['analytics'],
    queryFn: () => analyticsApi.get().then((res) => res.data.data),
  });

  const { data: journals } = useQuery<Journal[]>({
    queryKey: ['journals', 'heatmap'],
    queryFn: () => journalsApi.getAll(0, 500).then((res) => res.data.data.content ?? []),
  });

  const { data: weekly } = useQuery<WeeklyProgress>({
    queryKey: ['analytics', 'weekly'],
    queryFn: () => analyticsApi.getWeekly().then((res) => res.data.data),
  });

  const { data: learningCurve } = useQuery<LearningCurveResponse>({
    queryKey: ['analytics', 'learning-curve'],
    queryFn: () => analyticsApi.getLearningCurve(30).then((res) => res.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <SkeletonCard className="h-56" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <SkeletonCard key={i} className="h-28" />
          ))}
        </div>
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <ChartSkeleton />
          <ChartSkeleton />
        </div>
      </div>
    );
  }

  if (!data) return null;

  const tl = data.targetLevel || 5;
  const target = getTargetLevel(tl);
  const diff = data.problemsByDifficulty || { easy: 0, medium: 0, hard: 0 };
  const total = data.totalProblems || 0;
  const rs = data.readinessScore || 0;
  const curEasyPct = total ? Math.round((diff.easy / total) * 100) : 0;
  const curMediumPct = total ? Math.round((diff.medium / total) * 100) : 0;
  const curHardPct = total ? Math.round((diff.hard / total) * 100) : 0;

  const targetPct: Record<string, number> = { Easy: target.easyPct, Medium: target.mediumPct, Hard: target.hardPct };
  const curPct: Record<string, number> = { Easy: curEasyPct, Medium: curMediumPct, Hard: curHardPct };
  const biggest = (['Hard', 'Medium', 'Easy'] as const)
    .map((k) => ({ k, gap: targetPct[k] - curPct[k] }))
    .sort((a, b) => b.gap - a.gap)[0];

  const toNext = Math.max(0, getTargetLevel(Math.min(10, tl + 1)).targetTotal - total);

  const insights: Insight[] = [
    ...(data.insights ?? []),
    {
      type: 'PROGRESS',
      title: 'Difficulty Mix',
      message: biggest.gap > 5
        ? `${biggest.k} lags your level-${tl} target by ${biggest.gap}pts — add more ${biggest.k.toLowerCase()}s.`
        : `Mix aligns with level ${tl}. Keep it balanced.`,
      display: `${curEasyPct}% E · ${curMediumPct}% M · ${curHardPct}% H`,
      metric: null,
      delta: null,
    },
    {
      type: 'STREAK',
      title: 'Journal Streak',
      message: `${weekly?.journalEntries ?? 0} of 7 days logged this week. Daily reps compound into mastery.`,
      metric: data.currentStreak,
      delta: null,
    },
  ];

  const difficultyData = [
    { name: 'Easy', count: diff.easy, fill: '#22c55e' },
    { name: 'Medium', count: diff.medium, fill: '#f59e0b' },
    { name: 'Hard', count: diff.hard, fill: '#ef4444' },
  ];

  const masteryData =
    data.masteryByCategory?.map((c) => ({
      category: c.category.substring(0, 10),
      mastery: c.averageMastery,
      fullMark: 100,
    })) || [];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Analytics</h1>

      {/* Company Readiness – Hero */}
      <section className="fade-in-up rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/10 via-card to-card p-6 shadow-soft">
        <CardTitle className="flex items-center gap-2">
          <Target className="h-4 w-4 text-primary" />
          Company Readiness
        </CardTitle>
        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="rounded-xl bg-secondary/50 p-6 text-center">
            <p className={`text-5xl font-bold ${readinessColor(rs)}`}>{rs}</p>
            <p className="mt-1 text-sm text-muted-foreground">Readiness Score</p>
            <p className="mt-2 text-xs text-muted-foreground">
              for <span className="font-medium text-primary">Level {tl} — {target.label}</span>
              <span className="block text-[10px]">{target.companies}</span>
            </p>
            <div className="mt-4 h-3 overflow-hidden rounded-full bg-secondary">
              <div
                className={`h-3 rounded-full transition-all ${readinessBg(rs)}`}
                style={{ width: `${rs}%` }}
              />
            </div>
          </div>

          <div className="space-y-1">
            {targetLevels.map((lv) => {
              const isCurrent = lv.level === tl;
              const isReached = lv.level < tl;
              return (
                <div
                  key={lv.level}
                  className={`flex items-center gap-3 rounded-lg px-3 py-1.5 transition-all ${
                    isCurrent ? 'border border-primary/20 bg-primary/10' : ''
                  } ${isReached || isCurrent ? '' : 'opacity-40'}`}
                >
                  <span className={`w-8 text-xs font-bold ${lv.color}`}>L{lv.level}</span>
                  <div className="min-w-0 flex-1">
                    <p className={`truncate text-sm leading-tight ${isCurrent ? 'font-medium text-primary' : ''}`}>
                      {lv.label}
                    </p>
                    {isCurrent && <p className="truncate text-[10px] text-muted-foreground">{lv.companies}</p>}
                  </div>
                  {isCurrent && <Zap className="h-3 w-3 shrink-0 text-primary" />}
                  {isReached && <span className="shrink-0 text-xs text-green-400">✓</span>}
                </div>
              );
            })}
          </div>
        </div>
        <SoWhat>
          At Level {tl} ({target.label}) you're {rs}/100 ready. The fastest path up is closing the {biggest.k}-problem
          gap, then clearing {toNext > 0 ? `${toNext} more` : 'the next target'} problems.
        </SoWhat>
      </section>

      {/* Learning Curve – Flagship */}
      <section className="fade-in-up" style={{ animationDelay: '40ms' }}>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-primary" />
              Learning Curve — Last 30 Days
            </CardTitle>
          </CardHeader>
          <CardContent>
            {learningCurve && learningCurve.points.length > 0 ? (
              <LearningCurveChart data={learningCurve} />
            ) : (
              <div className="flex h-[240px] flex-col items-center justify-center rounded-xl border border-dashed border-border text-center">
                <TrendingUp className="mb-2 h-8 w-8 text-muted-foreground/50" />
                <p className="text-sm font-medium">Your curve is empty</p>
                <p className="mx-auto mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">
                  Practice and revise — the engine snapshots your mastery, retention, and skill nightly to paint this curve.
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      {/* Insight Cards */}
      <section className="fade-in-up grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4" style={{ animationDelay: '60ms' }}>
        {insights.map((insight) => (
          <InsightCard key={insight.title} insight={insight} />
        ))}
      </section>

      {/* Charts */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <section className="fade-in-up" style={{ animationDelay: '120ms' }}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Code2 className="h-4 w-4 text-primary" />
                Problems by Difficulty
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={difficultyData}>
                  <XAxis dataKey="name" fontSize={12} stroke="var(--color-border)" tick={{ fill: 'var(--color-muted-foreground)' }} />
                  <YAxis fontSize={12} stroke="var(--color-border)" tick={{ fill: 'var(--color-muted-foreground)' }} />
                  <Tooltip
                    cursor={{ fill: 'var(--color-muted)', opacity: 0.5 }}
                    contentStyle={{
                      backgroundColor: 'var(--color-card)',
                      border: '1px solid var(--color-border)',
                      borderRadius: '8px',
                      color: 'var(--color-foreground)',
                    }}
                  />
                  <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                    {difficultyData.map((d) => (
                      <Cell key={d.name} fill={d.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
              <div className="mt-3 flex flex-wrap items-center justify-center gap-x-5 gap-y-1.5">
                {difficultyData.map((d) => (
                  <span key={d.name} className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                    <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: d.fill }} />
                    {d.name}
                    <span className="font-semibold text-foreground">{d.count}</span>
                  </span>
                ))}
              </div>
              <SoWhat>
                Hard problems matter most at level {tl} — {target.hardPct}% of your total should be Hard.
              </SoWhat>
            </CardContent>
          </Card>
        </section>

        <section className="fade-in-up" style={{ animationDelay: '180ms' }}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-green-400" />
                Mastery by Category
              </CardTitle>
            </CardHeader>
            <CardContent>
              {masteryData.length > 0 ? (
                <>
                  <ResponsiveContainer width="100%" height={240}>
                    <RadarChart data={masteryData}>
                      <PolarGrid stroke="var(--color-border)" />
                      <PolarAngleAxis dataKey="category" stroke="var(--color-border)" tick={{ fill: 'var(--color-muted-foreground)' }} fontSize={10} />
                      <PolarRadiusAxis angle={30} domain={[0, 100]} stroke="var(--color-border)" tick={{ fill: 'var(--color-muted-foreground)' }} fontSize={10} />
                      <Radar name="Mastery" dataKey="mastery" stroke="#6d5dfc" fill="#6d5dfc" fillOpacity={0.3} />
                    </RadarChart>
                  </ResponsiveContainer>
                  <SoWhat>
                    The lowest lobes are your highest-leverage review targets this week.
                  </SoWhat>
                </>
              ) : (
                <div className="flex h-[240px] items-center justify-center text-sm text-muted-foreground">
                  No topic data yet. Add topics or sync LeetCode.
                </div>
              )}
            </CardContent>
          </Card>
        </section>
      </div>

      {/* Consistency Heatmap */}
      <section className="fade-in-up" style={{ animationDelay: '240ms' }}>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Flame className="h-4 w-4 text-orange-400" />
              Consistency — Last 28 Weeks
            </CardTitle>
          </CardHeader>
          <CardContent>
            {(journals?.length ?? 0) > 0 ? (
              <ConsistencyHeatmap journals={journals ?? []} />
            ) : (
              <div className="rounded-xl border border-dashed border-border px-6 py-10 text-center">
                <Flame className="mx-auto mb-2 h-8 w-8 text-muted-foreground/50" />
                <p className="text-sm font-medium">Your grid is empty</p>
                <p className="mx-auto mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">
                  Every journal entry lights a cell — intensity follows hours studied. Consistency is the #1 predictor
                  of interview success, and this heatmap makes your momentum (or its absence) impossible to ignore.
                </p>
                <button
                  onClick={() => navigate('/app/journal')}
                  className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-4 py-2 text-xs font-medium text-primary transition-colors hover:bg-primary/20"
                >
                  Log today's entry
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      {/* Weakest & Strongest */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <section className="fade-in-up" style={{ animationDelay: '300ms' }}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-red-400">
                <AlertTriangle className="h-4 w-4" />
                Weakest Topics
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {data.weakestTopics?.length === 0 && (
                <p className="text-sm text-muted-foreground">No weak topics identified.</p>
              )}
              {data.weakestTopics?.map((t, i) => (
                <div key={i} className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{t.title}</p>
                    <p className="truncate text-xs text-muted-foreground">{t.category}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <div className="h-2 w-16 rounded-full bg-secondary">
                      <div className="h-2 rounded-full bg-red-400" style={{ width: `${t.confidence * 10}%` }} />
                    </div>
                    <span className="text-xs">{t.confidence}/10</span>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </section>

        <section className="fade-in-up" style={{ animationDelay: '360ms' }}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-green-400">
                <TrendingUp className="h-4 w-4" />
                Strongest Topics
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {data.strongestTopics?.length === 0 && (
                <p className="text-sm text-muted-foreground">No strong topics yet.</p>
              )}
              {data.strongestTopics?.map((t, i) => (
                <div key={i} className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{t.title}</p>
                    <p className="truncate text-xs text-muted-foreground">{t.category}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <div className="h-2 w-16 rounded-full bg-secondary">
                      <div className="h-2 rounded-full bg-green-400" style={{ width: `${t.confidence * 10}%` }} />
                    </div>
                    <span className="text-xs">{t.confidence}/10</span>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </section>
      </div>
    </div>
  );
}
