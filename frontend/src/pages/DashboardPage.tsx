import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { dashboardApi, recommendationsApi, practiceApi, analyticsApi } from '@/api';
import { parseApiError } from '@/lib/error';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import KpiCard from '@/components/ui/KpiCard';
import { DashboardSkeleton } from '@/components/ui/LoadingSkeleton';
import { Badge } from '@/components/ui/Badge';
import { useAuth } from '@/contexts/AuthContext';
import {
  Flame, Target, RefreshCw, BookOpen, Zap,
  Code2, AlertTriangle, Brain, Sparkles, ArrowRight,
  Clock, CalendarCheck2, NotebookPen, ListChecks,
} from 'lucide-react';
import { useState } from 'react';
import type { PracticeQueueResponse } from '@/types';

const REVISION_MINS = 5;
const DIFFICULTY_MINS: Record<string, number> = { Easy: 20, Medium: 35, Hard: 50 };

function formatTime(minutes: number) {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
}

function ReadinessRing({ score }: { score: number }) {
  const r = 34;
  const c = 2 * Math.PI * r;
  const pct = Math.min(100, Math.max(0, score));
  const color = score >= 80 ? '#22c55e' : score >= 50 ? '#eab308' : score >= 30 ? '#f97316' : '#ef4444';
  return (
    <div className="relative h-24 w-24 shrink-0">
      <svg className="h-24 w-24 -rotate-90" viewBox="0 0 80 80" aria-hidden="true">
        <circle cx="40" cy="40" r={r} fill="none" stroke="var(--color-secondary)" strokeWidth="7" />
        <circle
          cx="40" cy="40" r={r} fill="none" stroke={color} strokeWidth="7"
          strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - pct / 100)}
          className="transition-all duration-700"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-xl font-bold leading-none">{score}</span>
        <span className="mt-1 text-[9px] uppercase tracking-wider text-muted-foreground">Ready</span>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [remaining, setRemaining] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get().then((res) => res.data.data),
  });

  const { data: practiceQueue } = useQuery<PracticeQueueResponse>({
    queryKey: ['practice', 'queue'],
    queryFn: () => practiceApi.getQueue().then((res) => res.data.data),
    staleTime: 20_000,
  });

  const { data: weekly } = useQuery({
    queryKey: ['analytics', 'weekly'],
    queryFn: () => analyticsApi.getWeekly().then((res) => res.data.data),
  });

  const generateMutation = useMutation({
    mutationFn: () => recommendationsApi.generate().then((res) => res.data.data),
    onSuccess: (res) => {
      setRemaining(res.remainingGenerations);
      setGenerateError(null);
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
    onError: (err) => setGenerateError(parseApiError(err)),
  });

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  if (!data) return null;

  const kh = data.knowledgeHealth || {};
  const lc = data.leetcodeStats;
  const tp = data.targetProgress;
  const hasLeetcode = !!user?.leetcodeUsername;
  const dueRevs = data.revisionsDue || [];
  const queue = practiceQueue?.queue ?? [];

  const queueMins =
    dueRevs.length * REVISION_MINS +
    queue.reduce((sum, p) => sum + (DIFFICULTY_MINS[p.difficulty] ?? 35), 0);
  const hasWork = dueRevs.length > 0 || queue.length > 0;

  const mission = hasWork
    ? dueRevs.length > 0 && queue.length > 0
      ? `You have ${dueRevs.length} revision${dueRevs.length !== 1 ? 's' : ''} and ${queue.length} problem${queue.length !== 1 ? 's' : ''} ready. Knock out the queue in about ${formatTime(queueMins)}.`
      : dueRevs.length > 0
        ? `You have ${dueRevs.length} revision${dueRevs.length !== 1 ? 's' : ''} due today. A quick review protects your long-term retention.`
        : `Solve ${queue.length} curated problem${queue.length !== 1 ? 's' : ''} to build breadth in your weak areas.`
    : data.todayMission;

  const ctaLabel = dueRevs.length > 0 ? `Continue — Review ${dueRevs.length} topic${dueRevs.length !== 1 ? 's' : ''}` : queue.length > 0 ? `Start — Solve ${queue.length} problem${queue.length !== 1 ? 's' : ''}` : 'Explore Practice';
  const ctaTo = dueRevs.length > 0 ? '/app/revision' : '/app/problems';

  const streak = lc?.streak || 0;
  const readiness = tp?.readinessScore ?? 0;

  return (
    <div className="space-y-6">
      {/* Today's Mission — Hero */}
      <section
        className="fade-in-up relative overflow-hidden rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/10 via-card to-card p-6 md:p-8"
      >
        <div className="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-primary/10 blur-3xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold uppercase tracking-widest text-primary">{data.greeting}</p>
            <h1 className="mt-1 text-2xl font-bold tracking-tight md:text-3xl">Today's Mission</h1>
            <p className="mt-2 max-w-xl text-sm leading-relaxed text-muted-foreground md:text-base">
              {mission}
            </p>
            <div className="mt-5 flex flex-wrap items-center gap-3">
              <button
                onClick={() => navigate(ctaTo)}
                className="group inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-all hover:brightness-110 active:scale-[0.97]"
              >
                {ctaLabel}
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </button>
              {streak > 0 && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-orange-500/10 px-3 py-1.5 text-xs font-medium text-orange-400">
                  <Flame className="h-3.5 w-3.5" />
                  {streak}-day streak
                </span>
              )}
              {tp && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary">
                  <Target className="h-3.5 w-3.5" />
                  Level {tp.targetLevel} target
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-5">
            {tp && <ReadinessRing score={readiness} />}
            <div className="space-y-2 text-sm">
              {hasWork && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Clock className="h-4 w-4 text-primary" />
                  <span>Est. <span className="font-semibold text-foreground">{formatTime(queueMins)}</span></span>
                </div>
              )}
              {dueRevs.length > 0 && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <RefreshCw className="h-4 w-4 text-orange-400" />
                  <span><span className="font-semibold text-foreground">{dueRevs.length}</span> revisions due</span>
                </div>
              )}
              {queue.length > 0 && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Code2 className="h-4 w-4 text-green-400" />
                  <span><span className="font-semibold text-foreground">{queue.length}</span> problems queued</span>
                </div>
              )}
              {!hasWork && (
                <p className="max-w-[180px] text-xs text-muted-foreground">All caught up. Your queue is clear for today.</p>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Today's Queue */}
      <section className="fade-in-up" style={{ animationDelay: '60ms' }}>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between gap-2">
              <span className="flex items-center gap-2">
                <ListChecks className="h-4 w-4 text-primary" />
                Today's Queue
              </span>
              {hasWork && (
                <span className="rounded-full bg-secondary px-3 py-1 text-xs font-medium text-muted-foreground">
                  ~{formatTime(queueMins)} total
                </span>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!hasWork ? (
              <div className="rounded-xl border border-dashed border-border py-10 text-center">
                <p className="text-sm font-medium">Queue is clear</p>
                <p className="mt-1 text-xs text-muted-foreground">No revisions or problems waiting. Enjoy the moment — or practice something new.</p>
                <Link to="/app/problems" className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-4 py-2 text-xs font-medium text-primary transition-colors hover:bg-primary/20">
                  Browse Practice <ArrowRight className="h-3 w-3" />
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {dueRevs.slice(0, 4).map((rev: any) => (
                  <Link
                    key={rev.id}
                    to="/app/revision"
                    className="group flex items-center gap-3 rounded-xl bg-secondary/40 px-4 py-3 transition-colors hover:bg-secondary"
                  >
                    <RefreshCw className="h-4 w-4 shrink-0 text-orange-400" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{rev.topicTitle}</p>
                      <p className="truncate text-xs text-muted-foreground">{rev.topicCategory}</p>
                    </div>
                    <Badge variant={rev.priority <= 1 ? 'destructive' : 'warning'}>P{rev.priority}</Badge>
                    <ArrowRight className="h-4 w-4 shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                  </Link>
                ))}
                {queue.slice(0, 4).map((problem) => (
                  <a
                    key={problem.titleSlug}
                    href={`https://leetcode.com/problems/${problem.titleSlug}/`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group flex items-center gap-3 rounded-xl bg-secondary/40 px-4 py-3 transition-colors hover:bg-secondary"
                  >
                    <Code2 className="h-4 w-4 shrink-0 text-green-400" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{problem.title}</p>
                      <p className="truncate text-xs text-muted-foreground">{problem.topicTag || 'Practice'}</p>
                    </div>
                    <Badge variant="outline">{problem.difficulty}</Badge>
                    <ArrowRight className="h-4 w-4 shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                  </a>
                ))}
                {(dueRevs.length > 4 || queue.length > 4) && (
                  <p className="pt-1 text-center text-xs text-muted-foreground">
                    {dueRevs.length + queue.length - 8} more in your full queue
                  </p>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      {/* This Week + KPIs */}
      <section className="fade-in-up" style={{ animationDelay: '120ms' }}>
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CalendarCheck2 className="h-4 w-4 text-primary" />
                This Week
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-2 text-center">
                <div className="rounded-xl bg-secondary/50 p-3">
                  <p className="text-lg font-bold">{weekly?.hoursStudied ? Number(weekly.hoursStudied).toFixed(1) : '0'}h</p>
                  <p className="text-[11px] text-muted-foreground">Studied</p>
                </div>
                <div className="rounded-xl bg-secondary/50 p-3">
                  <p className="text-lg font-bold">{weekly?.revisionsCompleted ?? 0}</p>
                  <p className="text-[11px] text-muted-foreground">Reviewed</p>
                </div>
                <div className="rounded-xl bg-secondary/50 p-3">
                  <p className="text-lg font-bold">{weekly?.journalEntries ?? 0}</p>
                  <p className="text-[11px] text-muted-foreground">Logged</p>
                </div>
              </div>
              <Link to="/app/journal" className="mt-3 flex items-center gap-1.5 text-xs text-muted-foreground transition-colors hover:text-primary">
                <NotebookPen className="h-3.5 w-3.5" />
                Log today in your journal
              </Link>
            </CardContent>
          </Card>

          <div className="grid grid-cols-2 gap-4 lg:col-span-2 md:grid-cols-4">
            <KpiCard
              icon={<Target className="h-5 w-5 text-primary" />}
              value={data.currentFocus}
              label="Current Focus"
              tooltip="Your weakest topic. Focus your study sessions here to close the gap."
            />
            <KpiCard
              icon={<RefreshCw className="h-5 w-5 text-orange-400" />}
              value={dueRevs.length}
              label="Revisions Due"
              tooltip="Number of topics scheduled for review today via SM-2 spaced repetition."
            />
            <KpiCard
              icon={<Brain className="h-5 w-5 text-purple-400" />}
              value={kh.averageRetention != null ? `${Math.round(kh.averageRetention)}%` : '-'}
              label="Avg Retention"
              tooltip="Estimated knowledge retention across all topics based on Ebbinghaus curve."
            />
            <KpiCard
              icon={<Clock className={`h-5 w-5 ${(kh.overdueRevisions || 0) > 0 ? 'text-red-400' : 'text-green-400'}`} />}
              value={kh.overdueRevisions || 0}
              label="Overdue"
              tooltip="Topics past their scheduled revision date. Review these ASAP."
            />
          </div>
        </div>
      </section>

      {/* Knowledge Health */}
      <section className="fade-in-up" style={{ animationDelay: '180ms' }}>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Brain className="h-5 w-5 text-primary" />
              Knowledge Health
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <div className="rounded-xl bg-secondary/50 p-3 text-center">
                <p className="text-xl font-bold text-primary">{kh.totalTopics || 0}</p>
                <p className="text-xs text-muted-foreground">Total Topics</p>
              </div>
              <div className="rounded-xl bg-secondary/50 p-3 text-center">
                <p className="text-xl font-bold">{kh.averageMastery || 0}%</p>
                <p className="text-xs text-muted-foreground">Avg Mastery</p>
              </div>
              <div className="rounded-xl bg-secondary/50 p-3 text-center">
                <p className="text-xl font-bold text-yellow-400">{kh.averageConfidence || 0}/10</p>
                <p className="text-xs text-muted-foreground">Avg Confidence</p>
              </div>
              <div className="rounded-xl bg-secondary/50 p-3 text-center">
                <p className="text-xl font-bold text-purple-400">
                  {kh.averageRetention != null ? `${Math.round(kh.averageRetention)}%` : '-'}
                </p>
                <p className="text-xs text-muted-foreground">Avg Retention</p>
              </div>
            </div>

            <div className="mt-4">
              <div className="mb-2 flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Topic Status</span>
                <span className="font-medium">{kh.totalTopics || 0} total</span>
              </div>
              <div className="flex h-4 overflow-hidden rounded-full bg-secondary">
                <div
                  className="bg-red-400 transition-all"
                  style={{ width: `${kh.totalTopics ? ((kh.notStartedTopics || 0) / kh.totalTopics) * 100 : 0}%` }}
                />
                <div
                  className="bg-yellow-400 transition-all"
                  style={{ width: `${kh.totalTopics ? ((kh.inProgressTopics || 0) / kh.totalTopics) * 100 : 0}%` }}
                />
                <div
                  className="bg-green-400 transition-all"
                  style={{ width: `${kh.totalTopics ? ((kh.masteredTopics || 0) / kh.totalTopics) * 100 : 0}%` }}
                />
              </div>
              <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs">
                <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-red-400" /> Not Started ({kh.notStartedTopics || 0})</span>
                <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-yellow-400" /> In Progress ({kh.inProgressTopics || 0})</span>
                <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-green-400" /> Mastered ({kh.masteredTopics || 0})</span>
                {(kh.overdueRevisions || 0) > 0 && (
                  <span className="flex items-center gap-1 text-red-400">
                    <AlertTriangle className="h-3 w-3" /> {kh.overdueRevisions} overdue
                  </span>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </section>

      {/* Target Progress */}
      {tp && tp.targetTotal > 0 && (
        <section className="fade-in-up" style={{ animationDelay: '240ms' }}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Target className="h-4 w-4 text-primary" />
                Target Progress — Level {tp.targetLevel}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <div className="mb-1 flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Problems Solved</span>
                  <span className="font-medium">{tp.totalSolved} / {tp.targetTotal}</span>
                </div>
                <div className="h-3 overflow-hidden rounded-full bg-secondary">
                  <div
                    className="h-3 rounded-full bg-primary transition-all"
                    style={{ width: `${Math.min(100, (tp.totalSolved / tp.targetTotal) * 100)}%` }}
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="rounded-lg bg-green-500/10 p-3 text-center">
                  <p className="text-lg font-bold text-green-400">{tp.difficultyGap.currentEasy}</p>
                  <p className="text-xs text-muted-foreground">Easy / {tp.difficultyGap.targetEasy}</p>
                </div>
                <div className="rounded-lg bg-yellow-500/10 p-3 text-center">
                  <p className="text-lg font-bold text-yellow-400">{tp.difficultyGap.currentMedium}</p>
                  <p className="text-xs text-muted-foreground">Medium / {tp.difficultyGap.targetMedium}</p>
                </div>
                <div className="rounded-lg bg-red-500/10 p-3 text-center">
                  <p className="text-lg font-bold text-red-400">{tp.difficultyGap.currentHard}</p>
                  <p className="text-xs text-muted-foreground">Hard / {tp.difficultyGap.targetHard}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>
      )}

      {/* LeetCode Stats */}
      {lc ? (
        <section className="fade-in-up rounded-2xl border border-primary/20 bg-primary/5 p-5" style={{ animationDelay: '300ms' }}>
          <div className="mb-3 flex items-center gap-2">
            <Code2 className="h-4 w-4 text-primary" />
            <h2 className="text-sm font-semibold text-primary">LeetCode Progress</h2>
            {lc.streak > 0 && (
              <span className="ml-auto flex items-center gap-1 rounded-full bg-orange-500/10 px-2.5 py-0.5 text-xs font-medium text-orange-400">
                <Flame className="h-3 w-3" />
                {lc.streak}-day streak
              </span>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
            <div className="text-center">
              <p className="text-2xl font-bold">{lc.totalSolved}</p>
              <p className="text-xs text-muted-foreground">Total</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-green-400">{lc.easySolved}</p>
              <p className="text-xs text-muted-foreground">Easy</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-yellow-400">{lc.mediumSolved}</p>
              <p className="text-xs text-muted-foreground">Medium</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-red-400">{lc.hardSolved}</p>
              <p className="text-xs text-muted-foreground">Hard</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold">{lc.ranking ? `#${lc.ranking.toLocaleString()}` : '-'}</p>
              <p className="text-xs text-muted-foreground">Ranking</p>
            </div>
          </div>
        </section>
      ) : hasLeetcode ? (
        <div className="rounded-2xl border border-dashed border-border p-5 text-center">
          <Code2 className="mx-auto mb-2 h-8 w-8 text-muted-foreground/50" />
          <p className="text-sm text-muted-foreground">Sync LeetCode in Profile to see stats here</p>
        </div>
      ) : null}

      {/* Recommendations & Journal */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between gap-2">
              <CardTitle className="flex items-center gap-2">
                <Zap className="h-4 w-4" />
                Recommendations
              </CardTitle>
              <div className="flex items-center gap-2">
                {remaining != null && remaining >= 0 && (
                  <span className="hidden text-xs text-muted-foreground sm:inline">
                    {remaining} left today
                  </span>
                )}
                <button
                  onClick={() => generateMutation.mutate()}
                  disabled={generateMutation.isPending}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Sparkles className="h-3.5 w-3.5" />
                  {generateMutation.isPending ? 'Generating...' : 'Generate'}
                </button>
              </div>
            </div>
            {generateError && (
              <p className="text-xs text-red-400">{generateError}</p>
            )}
          </CardHeader>
          <CardContent className="space-y-3">
            {data.recommendations?.length === 0 && (
              <p className="text-sm text-muted-foreground">All caught up. No recommendations right now.</p>
            )}
            {(data.recommendations ?? []).slice(0, 5).map((rec: any) => (
              <div key={rec.id} className="rounded-lg bg-secondary/50 px-4 py-3">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium">{rec.title}</p>
                  {rec.priority <= 2 && (
                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      rec.priority === 1
                        ? 'bg-red-500/10 text-red-400'
                        : 'bg-amber-500/10 text-amber-400'
                    }`}>
                      {rec.priority === 1 ? 'High' : 'Medium'}
                    </span>
                  )}
                </div>
                <p className="mt-1 text-xs text-muted-foreground">{rec.reason}</p>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BookOpen className="h-4 w-4" />
              Today's Journal
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{data.recentJournal || 'No entry today. Reflect in the Journal tab.'}</p>
            <p className="mt-3 text-xs text-muted-foreground/60">
              Log your daily energy, mood, and wins to track your learning journey.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
