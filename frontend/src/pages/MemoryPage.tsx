import { useQuery } from '@tanstack/react-query';
import { memoryApi } from '@/api';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Callout } from '@/components/ui/Callout';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { buttonVariants } from '@/components/ui/Button';
import { scoreTone, toneText, toneBg } from '@/lib/score';
import { Brain, ExternalLink, Clock, AlertTriangle, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import ApiErrorState from '@/components/ui/ApiErrorState';

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
        <h1 className="text-xl font-semibold tracking-tight">Memory</h1>
      </div>

      <p className="text-sm text-muted-foreground">
        Topics that are fading from your long-term memory based on spaced repetition data.
        Review them before they're forgotten.
      </p>

      <Callout tone="primary" icon={<Brain className="h-4 w-4" />}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm">
            Fading concepts appear in your <span className="font-medium text-foreground">Practice queue</span> for review.
          </p>
          <Link to="/app/problems" className={buttonVariants({ variant: 'secondary', size: 'sm' })}>
            View Queue
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>
      </Callout>

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
            <Link to="/app/revision" className={buttonVariants()}>
              Review today's topics
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      ) : (
        <div className="space-y-3">
          {fading.map((concept) => {
            const retentionTone = concept.estimatedRetention != null
              ? scoreTone(concept.estimatedRetention, { good: 80, fair: 60 })
              : null;
            const masteryTone = scoreTone(concept.mastery, { good: 80, fair: 50 });
            return (
              <div
                key={concept.topicId}
                className="rounded-xl border border-border bg-card/50 px-5 py-4 transition-all hover:border-primary/20 hover:bg-card"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-semibold">{concept.title}</h3>
                      {concept.estimatedRetention != null && concept.estimatedRetention < 40 && (
                        <AlertTriangle className="h-4 w-4 shrink-0 text-destructive" />
                      )}
                    </div>
                    {concept.category && (
                      <p className="mt-0.5 text-caption text-muted-foreground">{concept.category}</p>
                    )}

                    <div className="mt-3 flex flex-wrap items-center gap-4">
                      <span
                        className={`rounded-lg px-2.5 py-1 text-caption font-medium ${
                          retentionTone ? `${toneBg[retentionTone]} ${toneText[retentionTone]}` : 'bg-secondary text-muted-foreground'
                        }`}
                      >
                        Retention: {concept.estimatedRetention != null ? `${Math.round(concept.estimatedRetention)}%` : 'N/A'}
                      </span>
                      <span className="flex items-center gap-1 text-caption text-muted-foreground">
                        <Brain className="h-3 w-3" />
                        Confidence: {concept.confidence}/10
                      </span>
                      {concept.daysSinceRevision >= 0 && (
                        <span className="flex items-center gap-1 text-caption text-muted-foreground">
                          <Clock className="h-3 w-3" />
                          {concept.daysSinceRevision}d since review
                        </span>
                      )}
                      <div className="flex items-center gap-2">
                        <ProgressBar value={concept.mastery} tone={masteryTone} className="w-24" />
                        <span className="text-caption text-muted-foreground tabular-nums">{concept.mastery}% mastery</span>
                      </div>
                    </div>
                  </div>

                  {concept.suggestedProblemSlug && (
                    <a
                      href={`https://leetcode.com/problems/${concept.suggestedProblemSlug}/`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={buttonVariants({ variant: 'secondary', size: 'sm' })}
                    >
                      <ExternalLink className="h-3 w-3" />
                      {concept.suggestedProblemDifficulty === 'Easy' ? 'Easy' : concept.suggestedProblemDifficulty === 'Hard' ? 'Hard' : 'Med'}
                    </a>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
