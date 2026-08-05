import { useQuery } from '@tanstack/react-query';
import { memoryApi } from '@/api';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Brain, ExternalLink, Clock, AlertTriangle, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import ApiErrorState from '@/components/ui/ApiErrorState';

const retentionColor = (r: number | null) => {
  if (r == null) return 'text-muted-foreground';
  if (r >= 80) return 'text-green-400';
  if (r >= 60) return 'text-yellow-400';
  return 'text-red-400';
};

const retentionBg = (r: number | null) => {
  if (r == null) return 'bg-secondary';
  if (r >= 80) return 'bg-green-500/10';
  if (r >= 60) return 'bg-yellow-500/10';
  return 'bg-red-500/10';
};

export default function MemoryPage() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['memory'],
    queryFn: () => memoryApi.get().then((res) => res.data.data),
  });

  if (error) {
    return <ApiErrorState error={error} onRetry={() => refetch()} />;
  }

  if (isLoading) {
    return <SkeletonList rows={4} />;
  }

  const fading = data?.fadingConcepts || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Brain className="h-6 w-6 text-primary" />
        <h1 className="text-2xl font-bold">Memory</h1>
      </div>

      <p className="text-sm text-muted-foreground">
        Topics that are fading from your long-term memory based on spaced repetition data.
        Review them before they're forgotten.
      </p>

      <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm">
            <Brain className="h-4 w-4 text-primary" />
            <span className="text-muted-foreground">
              Fading concepts appear in your <span className="font-medium text-foreground">Practice queue</span> for review.
            </span>
          </div>
          <Link
            to="/app/problems"
            className="flex shrink-0 items-center gap-1 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20 transition-colors"
          >
            View Queue
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>
      </div>

      {fading.length === 0 ? (
        <TeachingEmptyState
          icon={<Brain className="h-6 w-6 text-primary" />}
          title="Nothing is fading — that's a good sign"
          description="This page surfaces topics whose retention is dropping, using your SM-2 revision history. An empty list means every topic is still well inside your memory window."
          steps={[
            'Keep reviewing on the Revision page so gaps keep widening.',
            'The moment a topic starts to fade, it lands here with a suggested problem.',
            'Maintain a daily journal streak — consistent study beats cramming.',
          ]}
          action={
            <Link
              to="/app/revision"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              Review today's topics
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      ) : (
        <div className="space-y-3">
          {fading.map((concept) => (
            <div
              key={concept.topicId}
              className="rounded-xl border border-border bg-card/50 px-5 py-4 transition-all hover:border-primary/20 hover:bg-card"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold">{concept.title}</h3>
                    {concept.estimatedRetention != null && concept.estimatedRetention < 40 && (
                      <AlertTriangle className="h-4 w-4 text-red-400 shrink-0" />
                    )}
                  </div>
                  {concept.category && (
                    <p className="mt-0.5 text-xs text-muted-foreground">{concept.category}</p>
                  )}

                  <div className="mt-3 flex flex-wrap items-center gap-4">
                    <div className={`rounded-lg px-2.5 py-1 text-xs font-medium ${retentionBg(concept.estimatedRetention)} ${retentionColor(concept.estimatedRetention)}`}>
                      Retention: {concept.estimatedRetention != null ? `${Math.round(concept.estimatedRetention)}%` : 'N/A'}
                    </div>
                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Brain className="h-3 w-3" />
                      Confidence: {concept.confidence}/10
                    </span>
                    {concept.daysSinceRevision >= 0 && (
                      <span className="flex items-center gap-1 text-xs text-muted-foreground">
                        <Clock className="h-3 w-3" />
                        {concept.daysSinceRevision}d since review
                      </span>
                    )}
                    <div className="h-2 w-24 rounded-full bg-secondary">
                      <div
                        className="h-2 rounded-full"
                        style={{
                          width: `${concept.mastery}%`,
                          backgroundColor: concept.mastery >= 80 ? '#22c55e' : concept.mastery >= 50 ? '#eab308' : '#ef4444',
                        }}
                      />
                    </div>
                    <span className="text-xs text-muted-foreground">{concept.mastery}% mastery</span>
                  </div>
                </div>

                {concept.suggestedProblemSlug && (
                  <a
                    href={`https://leetcode.com/problems/${concept.suggestedProblemSlug}/`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex shrink-0 items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-2 text-xs font-medium text-primary hover:bg-primary/20 transition-colors"
                  >
                    <ExternalLink className="h-3 w-3" />
                    {concept.suggestedProblemDifficulty === 'Easy' ? 'Easy' : concept.suggestedProblemDifficulty === 'Hard' ? 'Hard' : 'Med'}
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
