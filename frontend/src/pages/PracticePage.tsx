import { memo, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { practiceApi, leetcodeApi } from '@/api';
import { Badge } from '@/components/ui/Badge';
import { Button, buttonVariants } from '@/components/ui/Button';
import { SignalChip } from '@/components/ui/SignalChip';
import { Callout } from '@/components/ui/Callout';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Code, RefreshCw, ExternalLink, CheckCircle2, ChevronDown, ChevronUp, Sparkles, Target, RotateCcw } from 'lucide-react';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import ApiErrorState from '@/components/ui/ApiErrorState';
import { parseApiError } from '@/lib/error';
import type { PracticeProblem, ProblemAttemptRequest } from '@/types';

const segmentConfig: Record<string, { label: string; tone: 'primary' | 'success' | 'warning' | 'danger'; icon: React.ReactNode }> = {
  WARMUP: { label: 'Warm-up', tone: 'success', icon: <Sparkles className="h-4 w-4" /> },
  REINFORCE: { label: 'Reinforce', tone: 'primary', icon: <Target className="h-4 w-4" /> },
  CHALLENGE: { label: 'Challenge', tone: 'warning', icon: <RotateCcw className="h-4 w-4" /> },
  REVISION: { label: 'Review', tone: 'danger', icon: <RotateCcw className="h-4 w-4" /> },
};

const toneText: Record<string, string> = {
  primary: 'text-primary',
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-destructive',
};

const toneDot: Record<string, string> = {
  primary: 'bg-primary',
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-destructive',
};

const SEGMENT_ORDER = ['WARMUP', 'REINFORCE', 'CHALLENGE', 'REVISION'] as const;

type Outcome = 'SOLVED' | 'PARTIAL' | 'FAILED' | 'SKIPPED';

const outcomeConfig: Record<Outcome, { label: string; base: string; active: string }> = {
  SOLVED: { label: 'Solved', base: 'bg-success/10 text-success', active: 'ring-2 ring-success' },
  PARTIAL: { label: 'Partial', base: 'bg-warning/10 text-warning', active: 'ring-2 ring-warning' },
  FAILED: { label: 'Failed', base: 'bg-destructive/10 text-destructive', active: 'ring-2 ring-destructive' },
  SKIPPED: { label: 'Skipped', base: 'bg-secondary text-muted-foreground', active: 'ring-2 ring-muted-foreground' },
};

const ProblemRow = memo(function ProblemRow({ problem, index }: { problem: PracticeProblem; index: number }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [outcome, setOutcome] = useState<Outcome>('SOLVED');
  const [hints, setHints] = useState(0);
  const [time, setTime] = useState(0);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState('');

  const segment = segmentConfig[problem.segment || 'REINFORCE'];
  const topSignals = useMemo(
    () => [...(problem.breakdown || [])].sort((a, b) => b.contribution - a.contribution).slice(0, 3),
    [problem.breakdown]
  );

  const submit = useMutation({
    mutationFn: (payload: ProblemAttemptRequest) => practiceApi.submitAttempt(payload).then((res) => res.data),
    onSuccess: (res) => {
      setFeedback(res.data.feedback);
      setOpen(false);
      setError('');
      queryClient.invalidateQueries({ queryKey: ['practice', 'queue'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['memory'] });
    },
    onError: (err: unknown) => setError(parseApiError(err)),
  });

  const handleSubmit = () => {
    submit.mutate({
      problemTitle: problem.title,
      problemSlug: problem.titleSlug,
      difficulty: problem.difficulty,
      topicTagSlug: problem.topicTag,
      topicTagName: problem.topicTag,
      outcome,
      hintsUsed: hints,
      timeTakenSeconds: time > 0 ? time * 60 : undefined,
    });
  };

  return (
    <div className="group rounded-xl border border-border bg-card px-5 py-4 transition-all hover:border-primary/20">
      <div className="flex items-start gap-4">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-secondary/70 text-xs font-bold text-muted-foreground tabular-nums">
          {String(index + 1).padStart(2, '0')}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`inline-flex items-center gap-1.5 text-caption font-semibold ${toneText[segment.tone]}`}>
              <span className={`h-1.5 w-1.5 rounded-full ${toneDot[segment.tone]}`} />
              {segment.label}
            </span>
            <span className="truncate text-sm font-medium">{problem.title}</span>
            <Badge
              variant={problem.difficulty === 'Easy' ? 'success' : problem.difficulty === 'Hard' ? 'destructive' : 'warning'}
              className="shrink-0"
            >
              {problem.difficulty}
            </Badge>
            {problem.topicTag && (
              <span className="shrink-0 rounded-full bg-secondary px-2.5 py-0.5 text-micro font-medium text-muted-foreground">
                {problem.topicTag}
              </span>
            )}
            {typeof problem.score === 'number' && (
              <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-micro font-medium text-primary tabular-nums">
                {problem.score}
              </span>
            )}
            {problem.attempts != null && problem.attempts > 0 && (
              <span className="shrink-0 text-caption text-muted-foreground tabular-nums">
                {problem.solved}/{problem.attempts} solved
              </span>
            )}
          </div>
          <p className="mt-1 text-caption text-muted-foreground">{problem.reason}</p>
          {topSignals.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1.5">
              {topSignals.map((s) => (
                <SignalChip key={s.name} name={s.name} value={s.value} weight={s.weight} contribution={s.contribution} />
              ))}
            </div>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <a
            href={`https://leetcode.com/problems/${problem.titleSlug}/`}
            target="_blank"
            rel="noopener noreferrer"
            className={buttonVariants({ variant: 'secondary', size: 'sm' })}
          >
            <ExternalLink className="h-3 w-3" />
            Solve
          </a>
          <Button size="sm" variant="secondary" onClick={() => setOpen(!open)}>
            {open ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            Log result
          </Button>
        </div>
      </div>

      {open && (
        <div className="mt-3 rounded-xl border border-border bg-secondary/30 p-4">
          <div className="flex flex-wrap items-center gap-2">
            {(Object.keys(outcomeConfig) as Outcome[]).map((o) => (
              <button
                key={o}
                onClick={() => setOutcome(o)}
                className={`rounded-lg px-3 py-1.5 text-caption font-medium transition-all ${outcomeConfig[o].base} ${
                  outcome === o ? outcomeConfig[o].active : 'opacity-70 hover:opacity-100'
                }`}
              >
                {outcomeConfig[o].label}
              </button>
            ))}
            <div className="ml-auto flex items-center gap-3 text-caption text-muted-foreground">
              <label className="flex items-center gap-1.5">
                Hints
                <select
                  value={hints}
                  onChange={(e) => setHints(Number(e.target.value))}
                  className="rounded-md border border-border bg-card px-2 py-1 text-xs"
                >
                  {[0, 1, 2, 3].map((h) => (
                    <option key={h} value={h}>{h}</option>
                  ))}
                </select>
              </label>
              <label className="flex items-center gap-1.5">
                Time (min)
                <select
                  value={time}
                  onChange={(e) => setTime(Number(e.target.value))}
                  className="rounded-md border border-border bg-card px-2 py-1 text-xs"
                >
                  {[0, 5, 10, 15, 20, 30, 45].map((t) => (
                    <option key={t} value={t}>{t === 0 ? '—' : t}</option>
                  ))}
                </select>
              </label>
            </div>
          </div>
          {error && <p className="mt-2 text-caption text-destructive">{error}</p>}
          {feedback && (
            <p className="mt-2 flex items-center gap-1.5 text-caption text-success">
              <CheckCircle2 className="h-3.5 w-3.5" /> {feedback}
            </p>
          )}
          <Button size="sm" className="mt-3" onClick={handleSubmit} loading={submit.isPending} disabled={submit.isPending}>
            {submit.isPending ? 'Saving…' : 'Save result'}
          </Button>
        </div>
      )}
    </div>
  );
});

export default function PracticePage() {
  const [syncing, setSyncing] = useState(false);
  const [syncError, setSyncError] = useState('');
  const queryClient = useQueryClient();

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['practice', 'queue'],
    queryFn: () => practiceApi.getQueue().then((res) => res.data.data),
    staleTime: 20_000,
  });

  const handleSync = async () => {
    setSyncing(true);
    setSyncError('');
    try {
      await leetcodeApi.sync();
      queryClient.invalidateQueries({ queryKey: ['practice', 'queue'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      queryClient.invalidateQueries({ queryKey: ['memory'] });
      queryClient.invalidateQueries({ queryKey: ['roadmap-analysis'] });
      queryClient.invalidateQueries({ queryKey: ['leetcode-stats'] });
    } catch (err: unknown) {
      setSyncError(parseApiError(err));
    } finally {
      setSyncing(false);
    }
  };

  const queue = data?.queue ?? [];
  const grouped = useMemo(
    () => {
      const q = data?.queue ?? [];
      return SEGMENT_ORDER.map((seg) => ({
        seg,
        items: q.filter((p) => (p.segment || 'REINFORCE') === seg),
      })).filter((g) => g.items.length > 0);
    },
    [data]
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Practice</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {data?.planMessage || 'Your queue is curated by the intelligence engine.'}
          </p>
        </div>
        <Button onClick={handleSync} disabled={syncing} loading={syncing}>
          <RefreshCw className="h-4 w-4" />
          {syncing ? 'Syncing…' : 'Sync LeetCode'}
        </Button>
      </div>

      {syncError && (
        <Callout tone="danger">
          <p className="text-caption text-destructive">{syncError}</p>
        </Callout>
      )}

      {data?.revisitTopics && data.revisitTopics.length > 0 && (
        <Callout tone="danger" icon={<RotateCcw className="h-4 w-4" />} title="Retention risk">
          <div className="flex flex-wrap items-center gap-2">
            {data.revisitTopics.map((t) => (
              <span key={t} className="rounded-full bg-destructive/10 px-2.5 py-0.5 text-caption font-medium text-destructive">
                {t}
              </span>
            ))}
          </div>
        </Callout>
      )}

      {error ? (
        <ApiErrorState error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <SkeletonList rows={5} />
      ) : queue.length > 0 ? (
        <div className="space-y-8">
          {grouped.map(({ seg, items }) => (
            <section key={seg} className="space-y-2">
              <div className="flex items-center gap-2">
                <span className={toneText[segmentConfig[seg].tone]}>{segmentConfig[seg].icon}</span>
                <h2 className={`text-caption font-semibold uppercase tracking-widest ${toneText[segmentConfig[seg].tone]}`}>
                  {segmentConfig[seg].label}
                </h2>
                <span className="text-caption text-muted-foreground tabular-nums">({items.length})</span>
              </div>
              <div className="space-y-2">
                {items.map((problem, i) => (
                  <ProblemRow key={problem.titleSlug} problem={problem} index={i} />
                ))}
              </div>
            </section>
          ))}
          <p className="pt-2 text-center text-caption text-muted-foreground">
            {queue.length} problem{queue.length !== 1 ? 's' : ''} in queue · {data?.profile === 'beginner' ? 'warm-up' : 'engine'} profile
          </p>
        </div>
      ) : (
        <TeachingEmptyState
          icon={<Code className="h-6 w-6 text-primary" />}
          title="Your queue is built from your weaknesses"
          description="Forge curates problems from your LeetCode history, topic mastery, retention decay, and skill rating. An empty queue means the engine needs more signal."
          steps={[
            'Sync your LeetCode profile once to unlock difficulty-gap analysis.',
            'Add or review topics so the engine knows your weak areas.',
            'Log your result after each problem to sharpen the queue.',
          ]}
          action={
            <Button onClick={handleSync} disabled={syncing} loading={syncing}>
              <RefreshCw className="h-4 w-4" />
              {syncing ? 'Syncing…' : 'Sync LeetCode now'}
            </Button>
          }
        />
      )}
    </div>
  );
}
