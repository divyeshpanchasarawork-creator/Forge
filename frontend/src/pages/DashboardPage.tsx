import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { dashboardApi, recommendationsApi, practiceApi, analyticsApi } from '@/api';
import { parseApiError } from '@/lib/error';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { SectionHeader } from '@/components/ui/SectionHeader';
import { StatTile } from '@/components/ui/StatTile';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { EmptyState } from '@/components/ui/EmptyState';
import { HeroCard } from '@/components/ui/HeroCard';
import { SignalChip } from '@/components/ui/SignalChip';
import { Button, buttonVariants } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import KpiCard from '@/components/ui/KpiCard';
import ReadinessRing from '@/components/ui/ReadinessRing';
import ApiErrorState from '@/components/ui/ApiErrorState';
import { DashboardSkeleton } from '@/components/ui/LoadingSkeleton';
import { useAuth } from '@/contexts/AuthContext';
import {
  Flame, Target, RefreshCw, BookOpen, Zap,
  Code2, Brain, Sparkles, ArrowRight,
  Clock, CalendarCheck2, NotebookPen, ListChecks, CheckCircle2, X, AlertTriangle,
} from 'lucide-react';
import { useState } from 'react';
import type { PracticeQueueResponse, Revision } from '@/types';

const REVISION_MINS = 5;
const DIFFICULTY_MINS: Record<string, number> = { Easy: 20, Medium: 35, Hard: 50 };

function formatTime(minutes: number) {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [remaining, setRemaining] = useState<number | null>(null);

  const { data, isLoading, error, refetch } = useQuery({
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
    },
    onError: (err) => setGenerateError(parseApiError(err)),
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'complete' | 'dismiss' }) =>
      action === 'complete'
        ? recommendationsApi.complete(id)
        : recommendationsApi.dismiss(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (err, _vars) => {
      setGenerateError(parseApiError(err));
    },
  });

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  if (error) {
    return <ApiErrorState error={error} onRetry={() => refetch()} />;
  }

  if (!data) {
    return <ApiErrorState error={new Error('The dashboard returned no data.')} onRetry={() => refetch()} />;
  }

  const kh = data.knowledgeHealth || {};
  const lc = data.leetcodeStats;
  const tp = data.targetProgress;
  const hasLeetcode = !!user?.leetcodeUsername;
  const dueRevs: Revision[] = data.revisionsDue || [];
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

  const ctaLabel = dueRevs.length > 0
    ? `Continue — Review ${dueRevs.length} topic${dueRevs.length !== 1 ? 's' : ''}`
    : queue.length > 0
      ? `Start — Solve ${queue.length} problem${queue.length !== 1 ? 's' : ''}`
      : 'Explore Practice';
  const ctaTo = dueRevs.length > 0 ? '/app/revision' : '/app/problems';

  const streak = lc?.streak || 0;
  const readiness = tp?.readinessScore ?? 0;

  return (
    <div className="space-y-6">
      {/* Today's Mission */}
      <HeroCard className="fade-in-up p-6 md:p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold uppercase tracking-widest text-primary">{data.greeting}</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Today's Mission</h1>
            <p className="mt-2 max-w-xl text-sm leading-relaxed text-muted-foreground md:text-base">
              {mission}
            </p>
            <div className="mt-5 flex flex-wrap items-center gap-3">
              <Button onClick={() => navigate(ctaTo)} className="group">
                {ctaLabel}
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </Button>
              {streak > 0 && (
                <Badge variant="warning">
                  <Flame className="mr-1 h-3 w-3" />
                  {streak}-day streak
                </Badge>
              )}
              {tp && (
                <Badge>
                  <Target className="mr-1 h-3 w-3" />
                  Level {tp.targetLevel} target
                </Badge>
              )}
            </div>
          </div>

          <div className="flex items-center gap-5">
            {tp && <ReadinessRing score={readiness} />}
            <div className="space-y-2 text-sm">
              {hasWork && (
                <p className="flex items-center gap-2 text-muted-foreground">
                  <Clock className="h-4 w-4 text-primary" />
                  Est. <span className="font-semibold text-foreground tabular-nums">{formatTime(queueMins)}</span>
                </p>
              )}
              {dueRevs.length > 0 && (
                <p className="flex items-center gap-2 text-muted-foreground">
                  <RefreshCw className="h-4 w-4 text-warning" />
                  <span className="font-semibold text-foreground tabular-nums">{dueRevs.length}</span> revisions due
                </p>
              )}
              {queue.length > 0 && (
                <p className="flex items-center gap-2 text-muted-foreground">
                  <Code2 className="h-4 w-4 text-success" />
                  <span className="font-semibold text-foreground tabular-nums">{queue.length}</span> problems queued
                </p>
              )}
              {!hasWork && (
                <p className="max-w-[180px] text-xs text-muted-foreground">All caught up. Your queue is clear for today.</p>
              )}
            </div>
          </div>
        </div>
      </HeroCard>

      {/* Today's Queue */}
      <section className="fade-in-up" style={{ animationDelay: '60ms' }}>
        <Card>
          <CardHeader>
            <SectionHeader
              title="Today's Queue"
              icon={<ListChecks className="h-4 w-4" />}
              action={hasWork && (
                <span className="rounded-full bg-secondary px-3 py-1 text-caption font-medium text-muted-foreground tabular-nums">
                  ~{formatTime(queueMins)} total
                </span>
              )}
            />
          </CardHeader>
          <CardContent>
            {!hasWork ? (
              <EmptyState
                title="Queue is clear"
                description="No revisions or problems waiting. Enjoy the moment — or practice something new."
                action={
                  <Button variant="secondary" size="sm" onClick={() => navigate('/app/problems')}>
                    Browse Practice <ArrowRight className="h-3 w-3" />
                  </Button>
                }
              />
            ) : (
              <div className="space-y-2">
                {dueRevs.slice(0, 4).map((rev) => (
                  <Link
                    key={rev.id}
                    to="/app/revision"
                    className="group flex items-center gap-3 rounded-xl bg-secondary/50 px-4 py-3 transition-colors hover:bg-secondary"
                  >
                    <RefreshCw className="h-4 w-4 shrink-0 text-warning" />
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
                    className="group flex items-center gap-3 rounded-xl bg-secondary/50 px-4 py-3 transition-colors hover:bg-secondary"
                  >
                    <Code2 className="h-4 w-4 shrink-0 text-success" />
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
              <SectionHeader
                title="This Week"
                icon={<CalendarCheck2 className="h-4 w-4" />}
              />
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-3">
                <StatTile label="Studied" value={`${weekly?.hoursStudied ? Number(weekly.hoursStudied).toFixed(1) : '0'}h`} />
                <StatTile label="Reviewed" value={weekly?.revisionsCompleted ?? 0} />
                <StatTile label="Logged" value={weekly?.journalEntries ?? 0} />
              </div>
              <Link
                to="/app/journal"
                className="mt-4 flex items-center gap-1.5 text-caption font-medium text-muted-foreground transition-colors hover:text-primary"
              >
                <NotebookPen className="h-3.5 w-3.5" />
                Log today in your journal
              </Link>
            </CardContent>
          </Card>

          <div className="grid grid-cols-2 gap-4 md:grid-cols-4 lg:col-span-2">
            <KpiCard
              icon={<Target className="h-5 w-5 text-muted-foreground" />}
              value={data.currentFocus}
              label="Current Focus"
              tooltip="Your weakest topic. Focus your study sessions here to close the gap."
            />
            <KpiCard
              icon={<RefreshCw className="h-5 w-5 text-muted-foreground" />}
              value={dueRevs.length}
              label="Revisions Due"
              tooltip="Number of topics scheduled for review today via SM-2 spaced repetition."
            />
            <KpiCard
              icon={<Brain className="h-5 w-5 text-muted-foreground" />}
              value={kh.averageRetention != null ? `${Math.round(kh.averageRetention)}%` : '-'}
              label="Avg Retention"
              tooltip="Estimated knowledge retention across all topics based on the Ebbinghaus curve."
            />
            <KpiCard
              icon={<Clock className={`h-5 w-5 ${(kh.overdueRevisions || 0) > 0 ? 'text-destructive' : 'text-muted-foreground'}`} />}
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
            <SectionHeader title="Knowledge Health" icon={<Brain className="h-4 w-4" />} />
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <StatTile label="Total Topics" value={kh.totalTopics || 0} tone="primary" />
              <StatTile label="Avg Mastery" value={`${kh.averageMastery || 0}%`} tone="success" />
              <StatTile label="Avg Confidence" value={`${kh.averageConfidence || 0}/10`} tone="warning" />
              <StatTile
                label="Avg Retention"
                value={kh.averageRetention != null ? `${Math.round(kh.averageRetention)}%` : '-'}
                tone="primary"
              />
            </div>

            <div className="mt-5">
              <div className="mb-2 flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Topic Status</span>
                <span className="font-medium tabular-nums">{kh.totalTopics || 0} total</span>
              </div>
              <div className="flex h-3 overflow-hidden rounded-full bg-secondary" role="img" aria-label="Topic status distribution">
                <div
                  className="bg-destructive transition-all"
                  style={{ width: `${kh.totalTopics ? ((kh.notStartedTopics || 0) / kh.totalTopics) * 100 : 0}%` }}
                />
                <div
                  className="bg-warning transition-all"
                  style={{ width: `${kh.totalTopics ? ((kh.inProgressTopics || 0) / kh.totalTopics) * 100 : 0}%` }}
                />
                <div
                  className="bg-success transition-all"
                  style={{ width: `${kh.totalTopics ? ((kh.masteredTopics || 0) / kh.totalTopics) * 100 : 0}%` }}
                />
              </div>
              <div className="mt-2.5 flex flex-wrap gap-x-4 gap-y-1 text-caption text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-destructive" /> Not Started ({kh.notStartedTopics || 0})
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-warning" /> In Progress ({kh.inProgressTopics || 0})
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-success" /> Mastered ({kh.masteredTopics || 0})
                </span>
                {(kh.overdueRevisions || 0) > 0 && (
                  <span className="flex items-center gap-1.5 text-destructive">
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
              <SectionHeader
                title={`Target Progress — Level ${tp.targetLevel}`}
                icon={<Target className="h-4 w-4" />}
                action={
                  <Badge variant="outline" className="tabular-nums">
                    {tp.totalSolved} / {tp.targetTotal} solved
                  </Badge>
                }
              />
            </CardHeader>
            <CardContent className="space-y-4">
              <ProgressBar
                value={(tp.totalSolved / tp.targetTotal) * 100}
                tone="primary"
                ariaLabel="Target progress"
              />
              <div className="grid grid-cols-3 gap-3">
                <StatTile
                  label={`Easy / ${tp.difficultyGap.targetEasy}`}
                  value={tp.difficultyGap.currentEasy}
                  tone="success"
                />
                <StatTile
                  label={`Medium / ${tp.difficultyGap.targetMedium}`}
                  value={tp.difficultyGap.currentMedium}
                  tone="warning"
                />
                <StatTile
                  label={`Hard / ${tp.difficultyGap.targetHard}`}
                  value={tp.difficultyGap.currentHard}
                  tone="danger"
                />
              </div>
            </CardContent>
          </Card>
        </section>
      )}

      {/* LeetCode Stats */}
      {lc ? (
        <section className="fade-in-up" style={{ animationDelay: '300ms' }}>
          <Card>
            <CardHeader>
              <SectionHeader
                title="LeetCode Progress"
                icon={<Code2 className="h-4 w-4" />}
                action={
                  lc.streak > 0 && (
                    <Badge variant="warning">
                      <Flame className="mr-1 h-3 w-3" />
                      {lc.streak}-day streak
                    </Badge>
                  )
                }
              />
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
                <StatTile label="Total" value={lc.totalSolved} tone="primary" />
                <StatTile label="Easy" value={lc.easySolved} tone="success" />
                <StatTile label="Medium" value={lc.mediumSolved} tone="warning" />
                <StatTile label="Hard" value={lc.hardSolved} tone="danger" />
                <StatTile label="Ranking" value={lc.ranking ? `#${lc.ranking.toLocaleString()}` : '-'} />
              </div>
            </CardContent>
          </Card>
        </section>
      ) : hasLeetcode ? (
        <section className="fade-in-up" style={{ animationDelay: '300ms' }}>
          <EmptyState
            icon={<Code2 className="h-5 w-5" />}
            title="LeetCode not synced"
            description="Sync your LeetCode profile to see your solving stats and streak here."
            action={
              <Link to="/app/profile" className={buttonVariants({ variant: 'secondary', size: 'sm' })}>
                Sync in Profile
              </Link>
            }
          />
        </section>
      ) : null}

      {/* Recommendations & Journal */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <SectionHeader
              title="Recommendations"
              icon={<Zap className="h-4 w-4" />}
              action={
                <div className="flex items-center gap-3">
                  {remaining != null && remaining >= 0 && (
                    <span className="hidden text-caption text-muted-foreground sm:inline">
                      {remaining} left today
                    </span>
                  )}
                  <Button
                    size="sm"
                    onClick={() => generateMutation.mutate()}
                    loading={generateMutation.isPending}
                  >
                    <Sparkles className="h-3.5 w-3.5" />
                    {generateMutation.isPending ? 'Generating…' : 'Generate'}
                  </Button>
                </div>
              }
            />
            {generateError && <p className="mt-2 text-caption text-destructive">{generateError}</p>}
          </CardHeader>
          <CardContent className="space-y-3">
            {(data.recommendations ?? []).length === 0 ? (
              <EmptyState
                dashed={false}
                title="All caught up"
                description="No recommendations right now. Generate a fresh set when you're ready to practice."
              />
            ) : (
              (data.recommendations ?? [])
                .slice(0, 5)
                .map((rec) => (
                  <div key={rec.id} className="rounded-xl bg-secondary/50 p-3.5">
                    <div className="flex items-center gap-2">
                      <p className="min-w-0 flex-1 truncate text-sm font-medium">{rec.title}</p>
                      {rec.priority <= 2 && (
                        <Badge variant={rec.priority === 1 ? 'destructive' : 'warning'}>
                          {rec.priority === 1 ? 'High' : 'Medium'}
                        </Badge>
                      )}
                    </div>
                    <p className="mt-1 text-caption text-muted-foreground">{rec.reason}</p>
                    {rec.scoreBreakdown?.items?.length ? (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {[...rec.scoreBreakdown.items]
                          .sort((a, b) => b.contribution - a.contribution)
                          .slice(0, 3)
                          .map((s) => (
                            <SignalChip
                              key={s.name}
                              name={s.name}
                              value={s.value}
                              weight={s.weight}
                              contribution={s.contribution}
                            />
                          ))}
                      </div>
                    ) : null}
                    <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
                      {rec.problemSlug && (
                        <a
                          href={`https://leetcode.com/problems/${rec.problemSlug}/`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className={buttonVariants({ variant: 'outline', size: 'sm' })}
                        >
                          <Code2 className="h-3 w-3" />
                          Solve on LeetCode
                        </a>
                      )}
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => resolveMutation.mutate({ id: rec.id, action: 'complete' })}
                        disabled={resolveMutation.isPending}
                      >
                        <CheckCircle2 className="h-3 w-3" />
                        Mark Solved
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => resolveMutation.mutate({ id: rec.id, action: 'dismiss' })}
                        disabled={resolveMutation.isPending}
                      >
                        <X className="h-3 w-3" />
                        Dismiss
                      </Button>
                    </div>
                  </div>
                ))
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <SectionHeader title="Today's Journal" icon={<BookOpen className="h-4 w-4" />} />
          </CardHeader>
          <CardContent>
            {data.recentJournal ? (
              <p className="text-sm leading-relaxed text-muted-foreground">{data.recentJournal}</p>
            ) : (
              <p className="text-sm leading-relaxed text-muted-foreground">
                No entry today. Reflect on what you learned in the Journal tab.
              </p>
            )}
            <Link
              to="/app/journal"
              className="mt-4 flex items-center gap-1.5 text-caption font-medium text-muted-foreground transition-colors hover:text-primary"
            >
              <NotebookPen className="h-3.5 w-3.5" />
              Open journal
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
