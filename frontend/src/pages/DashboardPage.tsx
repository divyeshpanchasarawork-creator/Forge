import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import KpiCard from '@/components/ui/KpiCard';
import { useAuth } from '@/contexts/AuthContext';
import {
  Flame, Target, RefreshCw, TrendingUp, BookOpen, Zap,
  Code2, AlertTriangle, Clock, Brain
} from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get().then((res) => res.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="h-32 animate-pulse rounded-2xl bg-secondary" />
        <div className="h-64 animate-pulse rounded-2xl bg-secondary" />
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-2xl bg-secondary" />
          ))}
        </div>
      </div>
    );
  }

  if (!data) return null;

  const kh = data.knowledgeHealth || {};
  const lc = data.leetcodeStats;
  const tp = data.targetProgress;
  const hasLeetcode = !!user?.leetcodeUsername;

  return (
    <div className="space-y-6">
      {/* Greeting + Readiness */}
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
              <Flame className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">{data.greeting}</h1>
              <p className="text-muted-foreground">{data.todayMission}</p>
            </div>
          </div>
          {tp && (
            <div className="flex items-center gap-4">
              <div className="text-right">
                <p className="text-xs text-muted-foreground">Target Level</p>
                <p className="text-lg font-bold text-primary">{tp.targetLevel}/10</p>
              </div>
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
                <div className="text-center">
                  <p className="text-xl font-bold text-primary">{tp.readinessScore}</p>
                  <p className="text-[10px] leading-tight text-muted-foreground">Ready</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Knowledge Health — simplified */}
      <Card className="border-primary/10">
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

      {/* Target Progress Section */}
      {tp && tp.targetTotal > 0 && (
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
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <KpiCard
          icon={<Target className="h-5 w-5 text-primary" />}
          value={data.currentFocus}
          label="Current Focus"
          tooltip="Your weakest topic. Focus your study sessions here to close the gap."
        />
        <KpiCard
          icon={<RefreshCw className="h-5 w-5 text-orange-400" />}
          value={data.revisionsDue?.length || 0}
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

      {/* LeetCode Stats */}
      {lc ? (
        <div className="rounded-2xl border border-primary/20 bg-primary/5 p-5">
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
        </div>
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
            <CardTitle className="flex items-center gap-2">
              <Zap className="h-4 w-4" />
              Recommendations
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.recommendations?.length === 0 && (
              <p className="text-sm text-muted-foreground">All caught up. No recommendations right now.</p>
            )}
            {data.recommendations?.slice(0, 5).map((rec: any) => (
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

      {/* Today's Revisions */}
      {data.revisionsDue && data.revisionsDue.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <RefreshCw className="h-4 w-4" />
              Today's Revisions ({data.revisionsDue.length})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {data.revisionsDue.slice(0, 5).map((rev: any) => (
              <div key={rev.id} className="flex items-center justify-between rounded-lg bg-secondary/50 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium truncate">{rev.topicTitle}</p>
                  <p className="text-xs text-muted-foreground">{rev.topicCategory}</p>
                </div>
                <span className="shrink-0 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                  P{rev.priority}
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
