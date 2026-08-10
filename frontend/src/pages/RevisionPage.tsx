import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { revisionsApi } from '@/api';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import KpiCard from '@/components/ui/KpiCard';
import { SectionHeader } from '@/components/ui/SectionHeader';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { EmptyState } from '@/components/ui/EmptyState';
import { Callout } from '@/components/ui/Callout';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { HeroCard } from '@/components/ui/HeroCard';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import ApiErrorState from '@/components/ui/ApiErrorState';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import type { Revision } from '@/types';
import {
  CheckCircle, Clock, Calendar, ListTodo, TrendingUp, Brain, Target, PartyPopper,
} from 'lucide-react';

const REVISION_MINS = 5;

function CelebrationOverlay({ onLogJournal, onClose }: { onLogJournal: () => void; onClose: () => void }) {
  return (
    <div className="fade-in fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={onClose}>
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        {Array.from({ length: 32 }).map((_, i) => {
          const left = (i * 37 + 13) % 100;
          const delay = (i % 9) * 0.1;
          const duration = 1.6 + (i % 6) * 0.25;
          const color = ['#6d5dfc', '#22c55e', '#f59e0b', '#ef4444', '#ec4899'][i % 5];
          return (
            <span
              key={i}
              className="absolute top-[-20px] block h-2.5 w-1.5 rounded-sm"
              style={{ left: `${left}%`, backgroundColor: color, animation: `confetti-fall ${duration}s ${delay}s linear both` }}
            />
          );
        })}
      </div>

      <div
        className="fade-in-up relative w-full max-w-md rounded-2xl border border-border bg-card p-8 text-center"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-primary/15">
          <PartyPopper className="h-8 w-8 text-primary" />
        </div>
        <h2 className="mt-5 text-xl font-semibold tracking-tight">Mission Complete</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          All of today's revisions are done. Your long-term retention is locked in — this is how streaks are built.
        </p>
        <div className="mt-6 flex flex-col gap-2">
          <Button onClick={onLogJournal}>
            Log a journal entry
          </Button>
          <Button variant="outline" onClick={onClose}>
            Back to queue
          </Button>
        </div>
      </div>
    </div>
  );
}

export default function RevisionPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [celebrate, setCelebrate] = useState(false);
  const targetLevel = user?.targetLevel ?? 5;
  const overdueThreshold = targetLevel >= 7 ? '7 days' : targetLevel >= 4 ? '14 days' : '21 days';

  const { data: todayRevisions, isLoading: loadingToday, error: todayError, refetch: refetchToday } = useQuery({
    queryKey: ['revisions', 'today'],
    queryFn: () => revisionsApi.getTodayActivity().then((res) => res.data.data),
  });

  const { data: pendingRevisions, isLoading: loadingPending } = useQuery({
    queryKey: ['revisions', 'pending'],
    queryFn: () => revisionsApi.getPending().then((res) => res.data.data),
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => revisionsApi.complete(id),
    onSuccess: async () => {
      const completedBefore = (todayRevisions || []).filter((r) => r.completed).length;
      await queryClient.invalidateQueries({ queryKey: ['revisions'] });
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      const latest = queryClient.getQueryData<Revision[]>(['revisions', 'today']);
      if (latest && latest.length > 0 && completedBefore < latest.length && latest.every((r) => r.completed)) {
        setCelebrate(true);
      }
    },
  });

  if (todayError) {
    return <ApiErrorState error={todayError} onRetry={() => refetchToday()} />;
  }

  if (loadingToday) {
    return <SkeletonList rows={4} />;
  }

  const totalDue = todayRevisions?.length || 0;
  const doneCount = todayRevisions?.filter((r) => r.completed).length || 0;
  const pct = totalDue ? Math.round((doneCount / totalDue) * 100) : 0;
  const missionDone = totalDue > 0 && doneCount >= totalDue;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold tracking-tight">Revision</h1>

      {/* Today's Mission — hero */}
      <HeroCard className="fade-in-up p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-widest text-primary">Today's Mission</p>
            <h2 className="mt-1 text-xl font-semibold tracking-tight">
              {totalDue > 0 ? `Review ${totalDue} topic${totalDue !== 1 ? 's' : ''} to lock in retention` : 'No revisions due today'}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {totalDue > 0
                ? `${doneCount} of ${totalDue} done · ~${totalDue * REVISION_MINS} min · SM-2 widens the gap when you review well.`
                : 'Your SM-2 schedule is clear. Enjoy the headroom — or practice something new.'}
            </p>
          </div>
          <Badge
            variant={missionDone ? 'success' : totalDue > 0 ? 'default' : 'outline'}
            className="w-fit shrink-0"
          >
            {missionDone ? <PartyPopper className="mr-1 h-3.5 w-3.5" /> : <Target className="mr-1 h-3.5 w-3.5" />}
            {missionDone ? 'Mission complete' : totalDue > 0 ? `${pct}% complete` : 'Queue clear'}
          </Badge>
        </div>
        {totalDue > 0 && (
          <div className="mt-4">
            <ProgressBar value={pct} ariaLabel="Today's revision progress" />
          </div>
        )}
      </HeroCard>

      {/* SM-2 context */}
      <Callout tone="primary" icon={<Brain className="h-5 w-5" />} title="Spaced Repetition (SM-2)">
        Revisions are scheduled using the SM-2 algorithm. Topics you review well (quality 4-5) get longer gaps.
        At your target level (<strong>Level {targetLevel}</strong>), topics overdue beyond <strong>{overdueThreshold}</strong> need immediate attention.
      </Callout>

      {/* Revision KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <KpiCard icon={<Clock className="h-5 w-5 text-muted-foreground" />} value={totalDue} label="Due Today" tooltip="Revisions scheduled for today." />
        <KpiCard icon={<ListTodo className="h-5 w-5 text-muted-foreground" />} value={loadingPending ? '…' : pendingRevisions?.length || 0} label="Total Pending" tooltip="All pending revisions not yet completed." />
        <KpiCard icon={<Calendar className={`h-5 w-5 ${doneCount > 0 ? 'text-success' : 'text-muted-foreground'}`} />} value={doneCount} label="Completed Today" tooltip="Revisions completed today." />
        <KpiCard icon={<TrendingUp className={`h-5 w-5 ${missionDone ? 'text-success' : 'text-muted-foreground'}`} />} value={totalDue ? 'Due' : 'Clear'} label="Status" tooltip={totalDue > 0 ? 'Revisions are due today.' : 'No revisions due — you are on track.'} />
      </div>

      {/* Today's Revisions — missions */}
      <Card>
        <CardHeader>
          <SectionHeader title="Today's Missions" icon={<Target className="h-4 w-4" />} />
        </CardHeader>
        <CardContent className="space-y-3">
          {totalDue === 0 && (
            <EmptyState
              icon={<PartyPopper className="h-5 w-5" />}
              title="Queue is clear"
              description="No revisions scheduled for today. Your retention is well-maintained — come back when SM-2 flags a topic."
            />
          )}
          {todayRevisions?.map((rev, i) => (
            <div
              key={rev.id}
              className={`flex items-center justify-between gap-3 rounded-xl px-5 py-4 transition-colors ${
                rev.completed ? 'bg-secondary/30' : 'bg-secondary/50'
              }`}
            >
              <div className="flex min-w-0 flex-1 items-center gap-4">
                <span
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-xs font-bold ${
                    rev.completed ? 'bg-success/10 text-success' : 'bg-primary/10 text-primary'
                  }`}
                >
                  {rev.completed ? <CheckCircle className="h-4 w-4" /> : String(i + 1).padStart(2, '0')}
                </span>
                <div className="min-w-0">
                  <p className={`truncate text-sm font-medium ${rev.completed ? 'line-through opacity-60' : ''}`}>
                    {rev.topicTitle}
                  </p>
                  <p className="truncate text-caption text-muted-foreground">
                    {rev.topicCategory} &middot; {rev.reason || 'Spaced repetition review'}
                  </p>
                </div>
              </div>
              {rev.completed ? (
                <Badge variant="success">
                  <CheckCircle className="mr-1 h-3 w-3" />
                  Done
                </Badge>
              ) : (
                <Button
                  size="sm"
                  onClick={() => completeMutation.mutate(rev.id)}
                  disabled={completeMutation.isPending}
                  loading={completeMutation.isPending && completeMutation.variables === rev.id}
                >
                  <CheckCircle className="h-4 w-4" />
                  Complete
                </Button>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Pending Revisions */}
      {!loadingPending && pendingRevisions && pendingRevisions.length > 0 && (
        <Card>
          <CardHeader>
            <SectionHeader title="All Pending Revisions" icon={<ListTodo className="h-4 w-4" />} />
          </CardHeader>
          <CardContent className="space-y-3">
            {pendingRevisions.map((rev) => (
              <div key={rev.id} className="flex items-center justify-between rounded-xl bg-secondary/30 px-5 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{rev.topicTitle}</p>
                  <p className="text-caption text-muted-foreground">Scheduled: {new Date(rev.scheduledDate).toLocaleDateString()}</p>
                </div>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => completeMutation.mutate(rev.id)}
                  disabled={completeMutation.isPending}
                >
                  <CheckCircle className="h-3.5 w-3.5" />
                  Complete
                </Button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {celebrate && (
        <CelebrationOverlay
          onLogJournal={() => {
            setCelebrate(false);
            navigate('/app/journal');
          }}
          onClose={() => setCelebrate(false)}
        />
      )}
    </div>
  );
}
