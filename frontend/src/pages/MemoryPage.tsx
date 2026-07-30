import { useQuery } from '@tanstack/react-query';
import { memoryApi } from '@/api';
import { Card, CardContent } from '@/components/ui/Card';
import { Brain, ExternalLink, Clock, AlertTriangle } from 'lucide-react';

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
  const { data, isLoading } = useQuery({
    queryKey: ['memory'],
    queryFn: () => memoryApi.get().then((res) => res.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-28 animate-pulse rounded-2xl bg-secondary" />
        ))}
      </div>
    );
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

      {fading.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Brain className="mx-auto mb-4 h-10 w-10 text-muted-foreground/40" />
            <p className="text-lg font-medium">All topics fresh</p>
            <p className="text-sm text-muted-foreground mt-1">
              No fading concepts found. Your spaced repetition schedule is working well.
            </p>
          </CardContent>
        </Card>
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
