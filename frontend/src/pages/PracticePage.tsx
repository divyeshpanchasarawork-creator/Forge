import { useQuery } from '@tanstack/react-query';
import { practiceApi, leetcodeApi } from '@/api';
import { Badge } from '@/components/ui/Badge';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Code, RefreshCw, ExternalLink } from 'lucide-react';
import { useState } from 'react';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';

const difficultyConfig: Record<string, { class: string }> = {
  Easy: { class: 'bg-green-500/10 text-green-400 border-green-500/20' },
  Medium: { class: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20' },
  Hard: { class: 'bg-red-500/10 text-red-400 border-red-500/20' },
};

export default function PracticePage() {
  const [syncing, setSyncing] = useState(false);

  const { data: queue, isLoading } = useQuery({
    queryKey: ['practice-queue'],
    queryFn: () => practiceApi.getQueue().then((res) => res.data.data || []),
  });

  const handleSync = async () => {
    setSyncing(true);
    try {
      await leetcodeApi.sync();
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Practice</h1>
        <button
          onClick={handleSync}
          disabled={syncing}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          <RefreshCw className={`h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
          {syncing ? 'Syncing...' : 'Sync LeetCode'}
        </button>
      </div>

      {isLoading ? (
        <SkeletonList rows={5} />
      ) : queue && queue.length > 0 ? (
        <div className="space-y-2">
          {queue.map((problem) => {
            const diff = difficultyConfig[problem.difficulty] || difficultyConfig.Medium;
            return (
              <div
                key={problem.titleSlug}
                className="group flex items-center gap-4 rounded-xl border border-border bg-card/50 px-5 py-4 transition-all hover:border-primary/20 hover:bg-card"
              >
                <Code className="h-5 w-5 shrink-0 text-primary" />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium truncate">{problem.title}</span>
                    <Badge variant="outline" className={`shrink-0 border ${diff.class}`}>
                      {problem.difficulty}
                    </Badge>
                    {problem.topicTag && (
                      <span className="shrink-0 rounded-full bg-secondary px-2.5 py-0.5 text-[11px] font-medium text-muted-foreground">
                        {problem.topicTag}
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{problem.reason}</p>
                </div>
                <a
                  href={`https://leetcode.com/problems/${problem.titleSlug}/`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex shrink-0 items-center gap-1 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20 transition-colors"
                >
                  <ExternalLink className="h-3 w-3" />
                  Solve
                </a>
              </div>
            );
          })}
          <p className="text-xs text-muted-foreground text-center pt-2">{queue.length} problem{queue.length !== 1 ? 's' : ''} in queue</p>
        </div>
      ) : (
        <TeachingEmptyState
          icon={<Code className="h-6 w-6 text-primary" />}
          title="Your queue is built from your weaknesses"
          description="Forge curates problems from two signals: your LeetCode history and your weakest topics. An empty queue means the engine doesn't have enough signal yet."
          steps={[
            'Sync your LeetCode profile once to unlock difficulty-gap analysis.',
            'Add or review topics so the engine knows your weak areas.',
            'Generate recommendations on the Dashboard to fill the queue.',
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
