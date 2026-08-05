import { memo, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { practiceApi, leetcodeApi } from '@/api';
import { Badge } from '@/components/ui/Badge';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Code, RefreshCw, ExternalLink, CheckCircle2, ChevronDown, ChevronUp, Sparkles, Target, RotateCcw } from 'lucide-react';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import ApiErrorState from '@/components/ui/ApiErrorState';
import { parseApiError } from '@/lib/error';
import type { PracticeProblem, ProblemAttemptRequest } from '@/types';

const difficultyConfig: Record<string, { class: string }> = {
  Easy: { class: 'bg-green-500/10 text-green-400 border-green-500/20' },
  Medium: { class: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20' },
  Hard: { class: 'bg-red-500/10 text-red-400 border-red-500/20' },
};

const segmentConfig: Record<string, { label: string; dot: string; text: string; icon: React.ReactNode }> = {
  WARMUP: { label: 'Warm-up', dot: 'bg-green-400', text: 'text-green-400', icon: <Sparkles className="h-3.5 w-3.5" /> },
  REINFORCE: { label: 'Reinforce', dot: 'bg-primary', text: 'text-primary', icon: <Target className="h-3.5 w-3.5" /> },
  CHALLENGE: { label: 'Challenge', dot: 'bg-orange-400', text: 'text-orange-400', icon: <RotateCcw className="h-3.5 w-3.5" /> },
  REVISION: { label: 'Review', dot: 'bg-red-400', text: 'text-red-400', icon: <RotateCcw className="h-3.5 w-3.5" /> },
};

const SEGMENT_ORDER = ['WARMUP', 'REINFORCE', 'CHALLENGE', 'REVISION'] as const;

type Outcome = 'SOLVED' | 'PARTIAL' | 'FAILED' | 'SKIPPED';

const outcomeConfig: Record<Outcome, { label: string; class: string; active: string }> = {
  SOLVED: { label: 'Solved', class: 'bg-green-500/10 text-green-400', active: 'ring-2 ring-green-400' },
  PARTIAL: { label: 'Partial', class: 'bg-yellow-500/10 text-yellow-400', active: 'ring-2 ring-yellow-400' },
  FAILED: { label: 'Failed', class: 'bg-red-500/10 text-red-400', active: 'ring-2 ring-red-400' },
  SKIPPED: { label: 'Skipped', class: 'bg-secondary text-muted-foreground', active: 'ring-2 ring-muted-foreground' },
};

const ProblemRow = memo(function ProblemRow({ problem, index }: { problem: PracticeProblem; index: number }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [outcome, setOutcome] = useState<Outcome>('SOLVED');
  const [hints, setHints] = useState(0);
  const [time, setTime] = useState(0);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState('');

  const diff = difficultyConfig[problem.difficulty] || difficultyConfig.Medium;
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
    <div className="group rounded-xl border border-border bg-card/50 px-5 py-4 transition-all hover:border-primary/20 hover:bg-card">
      <div className="flex items-start gap-4">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-secondary/70 text-xs font-bold text-muted-foreground">
          {String(index + 1).padStart(2, '0')}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`inline-flex items-center gap-1 text-[11px] font-semibold ${segment.text}`}>
              <span className={`h-1.5 w-1.5 rounded-full ${segment.dot}`} />
              {segment.label}
            </span>
            <span className="text-sm font-medium truncate">{problem.title}</span>
            <Badge variant="outline" className={`shrink-0 border ${diff.class}`}>
              {problem.difficulty}
            </Badge>
            {problem.topicTag && (
              <span className="shrink-0 rounded-full bg-secondary px-2.5 py-0.5 text-[11px] font-medium text-muted-foreground">
                {problem.topicTag}
              </span>
            )}
            {typeof problem.score === 'number' && (
              <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
                {problem.score}
              </span>
            )}
            {problem.attempts != null && problem.attempts > 0 && (
              <span className="shrink-0 text-[11px] text-muted-foreground">
                {problem.solved}/{problem.attempts} solved
              </span>
            )}
          </div>
          <p className="mt-1 text-xs text-muted-foreground">{problem.reason}</p>
          {topSignals.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1.5">
              {topSignals.map((s) => (
                <span
                  key={s.name}
                  title={`${s.name}: ${s.value}/100 × weight ${s.weight}`}
                  className="rounded-full bg-secondary px-2 py-0.5 text-[10px] font-medium text-muted-foreground"
                >
                  {s.name} <span className="text-primary">+{s.contribution}</span>
                </span>
              ))}
            </div>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <a
            href={`https://leetcode.com/problems/${problem.titleSlug}/`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20 transition-colors"
          >
            <ExternalLink className="h-3 w-3" />
            Solve
          </a>
          <button
            onClick={() => setOpen(!open)}
            className="flex items-center gap-1 rounded-lg bg-secondary px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-secondary/70"
          >
            {open ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            Log result
          </button>
        </div>
      </div>

      {open && (
        <div className="mt-3 rounded-xl border border-border bg-secondary/30 p-4">
          <div className="flex flex-wrap items-center gap-2">
            {(Object.keys(outcomeConfig) as Outcome[]).map((o) => (
              <button
                key={o}
                onClick={() => setOutcome(o)}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${outcomeConfig[o].class} ${outcome === o ? outcomeConfig[o].active : 'opacity-70 hover:opacity-100'}`}
              >
                {outcomeConfig[o].label}
              </button>
            ))}
            <div className="ml-auto flex items-center gap-3 text-xs text-muted-foreground">
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
          {error && <p className="mt-2 text-xs text-destructive">{error}</p>}
          {feedback && (
            <p className="mt-2 flex items-center gap-1.5 text-xs text-green-400">
              <CheckCircle2 className="h-3.5 w-3.5" /> {feedback}
            </p>
          )}
          <button
            onClick={handleSubmit}
            disabled={submit.isPending}
            className="mt-3 rounded-lg bg-primary px-4 py-1.5 text-xs font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
          >
            {submit.isPending ? 'Saving...' : 'Save result'}
          </button>
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
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
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
    () =>
      SEGMENT_ORDER.map((seg) => ({
        seg,
        items: queue.filter((p) => (p.segment || 'REINFORCE') === seg),
      })).filter((g) => g.items.length > 0),
    [queue]
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Practice</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {data?.planMessage || 'Your queue is curated by the intelligence engine.'}
          </p>
        </div>
        <button
          onClick={handleSync}
          disabled={syncing}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          <RefreshCw className={`h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
          {syncing ? 'Syncing...' : 'Sync LeetCode'}
        </button>
      </div>

      {syncError && <p className="text-xs text-destructive">{syncError}</p>}

      {data?.revisitTopics && data.revisitTopics.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 rounded-xl border border-red-500/20 bg-red-500/5 px-4 py-3">
          <RotateCcw className="h-4 w-4 shrink-0 text-red-400" />
          <span className="text-xs font-medium text-red-400">Retention risk:</span>
          {data.revisitTopics.map((t) => (
            <span key={t} className="rounded-full bg-red-500/10 px-2.5 py-0.5 text-[11px] font-medium text-red-400">
              {t}
            </span>
          ))}
        </div>
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
                {segmentConfig[seg].icon}
                <h2 className={`text-sm font-semibold uppercase tracking-wider ${segmentConfig[seg].text}`}>
                  {segmentConfig[seg].label}
                </h2>
                <span className="text-xs text-muted-foreground">({items.length})</span>
              </div>
              <div className="space-y-2">
                {items.map((problem, i) => (
                  <ProblemRow key={problem.titleSlug} problem={problem} index={i} />
                ))}
              </div>
            </section>
          ))}
          <p className="pt-2 text-center text-xs text-muted-foreground">
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
            <button
              onClick={handleSync}
              disabled={syncing}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
              {syncing ? 'Syncing...' : 'Sync LeetCode now'}
            </button>
          }
        />
      )}
    </div>
  );
}
