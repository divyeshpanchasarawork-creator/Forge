import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { revisionsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import KpiCard from '@/components/ui/KpiCard';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import { useAuth } from '@/contexts/AuthContext';
import { AnimatePresence, motion } from 'framer-motion';
import { useState } from 'react';
import {
  CheckCircle, Clock, Calendar, ListTodo, TrendingUp, Brain, Target, PartyPopper,
} from 'lucide-react';

const REVISION_MINS = 5;

function CelebrationOverlay({ onLogJournal, onClose }: { onLogJournal: () => void; onClose: () => void }) {
  return (
    <motion.div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        {Array.from({ length: 32 }).map((_, i) => {
          const left = (i * 37 + 13) % 100;
          const delay = (i % 9) * 0.1;
          const duration = 1.6 + (i % 6) * 0.25;
          const color = ['#6d5dfc', '#22c55e', '#f59e0b', '#ef4444', '#ec4899'][i % 5];
          return (
            <motion.span
              key={i}
              className="absolute top-[-20px] block h-2.5 w-1.5 rounded-sm"
              style={{ left: `${left}%`, backgroundColor: color }}
              initial={{ y: -20, opacity: 0, rotate: 0 }}
              animate={{ y: '110vh', opacity: [0, 1, 1, 0], rotate: 360 + (i % 3) * 180 }}
              transition={{ delay, duration, ease: 'easeIn' }}
            />
          );
        })}
      </div>

      <motion.div
        className="relative w-full max-w-md rounded-2xl border border-primary/30 bg-card p-8 text-center shadow-2xl"
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.9, opacity: 0 }}
        transition={{ type: 'spring', damping: 20, stiffness: 260 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-primary/15">
          <PartyPopper className="h-8 w-8 text-primary" />
        </div>
        <h2 className="mt-5 text-2xl font-bold">Mission Complete</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          All of today's revisions are done. Your long-term retention is locked in — this is how streaks are built.
        </p>
        <div className="mt-6 flex flex-col gap-2">
          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={onLogJournal}
            className="rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-colors hover:brightness-110"
          >
            Log a journal entry
          </motion.button>
          <button
            onClick={onClose}
            className="rounded-xl border border-border px-4 py-2.5 text-sm text-muted-foreground transition-colors hover:bg-secondary"
          >
            Back to queue
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}

export default function RevisionPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [celebrate, setCelebrate] = useState(false);
  const targetLevel = user?.targetLevel ?? 5;
  const overdueThreshold = targetLevel >= 7 ? '7 days' : targetLevel >= 4 ? '14 days' : '21 days';

  const { data: todayRevisions, isLoading: loadingToday } = useQuery({
    queryKey: ['revisions', 'today'],
    queryFn: () => revisionsApi.getToday().then((res) => res.data.data),
  });

  const { data: pendingRevisions, isLoading: loadingPending } = useQuery({
    queryKey: ['revisions', 'pending'],
    queryFn: () => revisionsApi.getPending().then((res) => res.data.data),
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => revisionsApi.complete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['revisions'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      const allToday = todayRevisions || [];
      const completedBefore = allToday.filter((r) => r.completed).length;
      if (completedBefore + 1 >= allToday.length && allToday.length > 0) {
        setCelebrate(true);
      }
    },
  });

  if (loadingToday) {
    return <SkeletonList rows={4} />;
  }

  const totalDue = todayRevisions?.length || 0;
  const doneCount = todayRevisions?.filter((r) => r.completed).length || 0;
  const pct = totalDue ? Math.round((doneCount / totalDue) * 100) : 0;
  const missionDone = totalDue > 0 && doneCount >= totalDue;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Revision</h1>

      {/* Today's Mission — hero */}
      <section className="fade-in-up rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/10 via-card to-card p-6 shadow-soft">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-widest text-primary">Today's Mission</p>
            <h2 className="mt-1 text-xl font-bold tracking-tight">
              {totalDue > 0 ? `Review ${totalDue} topic${totalDue !== 1 ? 's' : ''} to lock in retention` : 'No revisions due today'}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {totalDue > 0
                ? `${doneCount} of ${totalDue} done · ~${totalDue * REVISION_MINS} min · SM-2 widens the gap when you review well.`
                : 'Your SM-2 schedule is clear. Enjoy the headroom — or practice something new.'}
            </p>
          </div>
          <span
            className={`inline-flex shrink-0 items-center gap-1.5 self-start rounded-full px-3 py-1.5 text-xs font-medium sm:self-auto ${
              missionDone
                ? 'bg-green-500/10 text-green-400'
                : totalDue > 0
                  ? 'bg-primary/10 text-primary'
                  : 'bg-secondary text-muted-foreground'
            }`}
          >
            {missionDone ? <PartyPopper className="h-3.5 w-3.5" /> : <Target className="h-3.5 w-3.5" />}
            {missionDone ? 'Mission complete' : totalDue > 0 ? `${pct}% complete` : 'Queue clear'}
          </span>
        </div>
        {totalDue > 0 && (
          <div className="mt-4 h-2.5 overflow-hidden rounded-full bg-secondary">
            <motion.div
              className="h-2.5 rounded-full bg-primary"
              initial={false}
              animate={{ width: `${pct}%` }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            />
          </div>
        )}
      </section>

      {/* SM-2 context */}
      <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
        <div className="flex items-start gap-3">
          <Brain className="h-5 w-5 text-primary shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-primary">Spaced Repetition (SM-2)</p>
            <p className="text-xs text-muted-foreground mt-1">
              Revisions are scheduled using the SM-2 algorithm. Topics you review well (quality 4-5) get longer gaps.
              At your target level (<strong>Level {targetLevel}</strong>), topics overdue beyond <strong>{overdueThreshold}</strong> need immediate attention.
            </p>
          </div>
        </div>
      </div>

      {/* Revision KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <KpiCard icon={<Clock className="h-5 w-5 text-orange-400" />} value={totalDue} label="Due Today" tooltip="Revisions scheduled for today." />
        <KpiCard icon={<ListTodo className="h-5 w-5 text-yellow-400" />} value={loadingPending ? '…' : pendingRevisions?.length || 0} label="Total Pending" tooltip="All pending revisions not yet completed." />
        <KpiCard icon={<Calendar className="h-5 w-5 text-blue-400" />} value={doneCount} label="Completed Today" tooltip="Revisions completed today." />
        <KpiCard icon={<TrendingUp className="h-5 w-5 text-green-400" />} value={totalDue ? 'Due' : 'Clear'} label="Status" tooltip={totalDue > 0 ? 'Revisions are due today.' : 'No revisions due — you are on track.'} />
      </div>

      {/* Today's Revisions — missions */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="h-5 w-5 text-primary" />
            Today's Missions
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {totalDue === 0 && (
            <div className="rounded-xl border border-dashed border-border py-10 text-center">
              <PartyPopper className="mx-auto mb-2 h-8 w-8 text-muted-foreground/50" />
              <p className="text-sm font-medium">Queue is clear</p>
              <p className="mt-1 text-xs text-muted-foreground">
                No revisions scheduled for today. Your retention is well-maintained — come back when SM-2 flags a topic.
              </p>
            </div>
          )}
          {todayRevisions?.map((rev, i) => (
            <div
              key={rev.id}
              className={`flex items-center justify-between gap-3 rounded-xl px-5 py-4 transition-colors ${
                rev.completed ? 'bg-green-500/5' : 'bg-secondary/50'
              }`}
            >
              <div className="flex min-w-0 flex-1 items-center gap-4">
                <span
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-xs font-bold ${
                    rev.completed ? 'bg-green-500/15 text-green-400' : 'bg-primary/10 text-primary'
                  }`}
                >
                  {rev.completed ? <CheckCircle className="h-4 w-4" /> : String(i + 1).padStart(2, '0')}
                </span>
                <div className="min-w-0">
                  <p className={`truncate font-medium ${rev.completed ? 'line-through opacity-60' : ''}`}>
                    {rev.topicTitle}
                  </p>
                  <p className="truncate text-sm text-muted-foreground">
                    {rev.topicCategory} &middot; {rev.reason || 'Spaced repetition review'}
                  </p>
                </div>
              </div>
              {rev.completed ? (
                <span className="shrink-0 text-xs font-medium text-green-400">Done</span>
              ) : (
                <motion.button
                  whileTap={{ scale: 0.97 }}
                  onClick={() => completeMutation.mutate(rev.id)}
                  disabled={completeMutation.isPending}
                  className="flex shrink-0 items-center gap-2 rounded-lg bg-green-500/10 px-4 py-2 text-sm font-medium text-green-400 transition-colors hover:bg-green-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <CheckCircle className="h-4 w-4" />
                  Complete
                </motion.button>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Pending Revisions */}
      {!loadingPending && pendingRevisions && pendingRevisions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>All Pending Revisions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {pendingRevisions.map((rev) => (
              <div key={rev.id} className="flex items-center justify-between rounded-xl bg-secondary/30 px-5 py-3">
                <div>
                  <p className="text-sm font-medium">{rev.topicTitle}</p>
                  <p className="text-xs text-muted-foreground">Scheduled: {new Date(rev.scheduledDate).toLocaleDateString()}</p>
                </div>
                <button
                  onClick={() => completeMutation.mutate(rev.id)}
                  className="text-sm text-muted-foreground hover:text-green-400 transition-colors"
                >
                  Complete
                </button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <AnimatePresence>
        {celebrate && (
          <CelebrationOverlay
            onLogJournal={() => {
              setCelebrate(false);
              navigate('/app/journal');
            }}
            onClose={() => setCelebrate(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
