import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { recommendationsApi, roadmapApi } from '@/api';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { useAuth } from '@/contexts/AuthContext';
import { Lightbulb, X, RefreshCw, AlertTriangle, TrendingUp, Target, Sparkles, ExternalLink, Brain, BookOpen } from 'lucide-react';

const priorityConfig: Record<number, { label: string; class: string }> = {
  1: { label: 'High', class: 'bg-red-500/10 text-red-400 border-red-500/20' },
  2: { label: 'Medium', class: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  3: { label: 'Low', class: 'bg-green-500/10 text-green-400 border-green-500/20' },
};

const actionIcons: Record<string, React.ElementType> = {
  REVIEW: AlertTriangle,
  ADVANCE: TrendingUp,
  SYNC_LEETCODE: RefreshCw,
  PRACTICE_TAG: Target,
  LEVEL_UP: TrendingUp,
  TRY_HARD: Target,
  MAINTAIN_STREAK: Sparkles,
  START_STREAK: Sparkles,
  TRY_CONTEST: Target,
  MILESTONE: Target,
};

function getDefaultIcon(action: string) {
  return actionIcons[action] || Lightbulb;
}

const readinessColor = (score: number) => {
  if (score >= 70) return 'text-green-400';
  if (score >= 40) return 'text-yellow-400';
  return 'text-red-400';
};

const readinessBg = (score: number) => {
  if (score >= 70) return 'bg-green-500/10';
  if (score >= 40) return 'bg-yellow-500/10';
  return 'bg-red-500/10';
};

export default function RoadmapPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const targetLevel = user?.targetLevel ?? 5;

  const { data: analysis, isLoading: analysisLoading } = useQuery({
    queryKey: ['roadmap-analysis'],
    queryFn: () => roadmapApi.getAnalysis().then((res) => res.data.data),
  });

  const { data: recommendations, isLoading: recsLoading } = useQuery({
    queryKey: ['recommendations'],
    queryFn: () => recommendationsApi.getActive().then((res) => res.data.data || []),
  });

  const generateMutation = useMutation({
    mutationFn: () => recommendationsApi.generate(),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['roadmap-analysis'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const dismissMutation = useMutation({
    mutationFn: recommendationsApi.dismiss,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
  });

  const isGenerating = generateMutation.isPending;
  const sorted = [...(recommendations || [])].sort((a, b) => a.priority - b.priority);

  if (analysisLoading || recsLoading) {
    return (
      <div className="space-y-4">
        <div className="h-32 animate-pulse rounded-2xl bg-secondary" />
        <div className="h-24 animate-pulse rounded-2xl bg-secondary" />
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-28 animate-pulse rounded-xl bg-secondary" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Roadmap</h1>
        <button
          onClick={() => generateMutation.mutate()}
          disabled={isGenerating}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          <RefreshCw className={`h-4 w-4 ${isGenerating ? 'animate-spin' : ''}`} />
          {isGenerating ? 'Generating...' : 'Generate New Plan'}
        </button>
      </div>

      {/* Personalized Analysis Hero Card */}
      {analysis && (
        <div className="rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/5 to-transparent p-6">
          <div className="mb-4 flex items-center gap-2">
            <Brain className="h-5 w-5 text-primary" />
            <h2 className="font-semibold">Your Personal Analysis</h2>
          </div>
          <p className="text-sm leading-relaxed text-muted-foreground">{analysis.paragraph}</p>

          <div className="mt-5 grid grid-cols-2 gap-4 md:grid-cols-4">
            <div className="rounded-xl bg-secondary/50 p-3 text-center">
              <p className="text-xl font-bold text-primary">Level {analysis.currentLevel}</p>
              <p className="text-xs text-muted-foreground">Target</p>
            </div>
            <div className={`rounded-xl ${readinessBg(analysis.readinessScore)} p-3 text-center`}>
              <p className={`text-xl font-bold ${readinessColor(analysis.readinessScore)}`}>{analysis.readinessScore}%</p>
              <p className="text-xs text-muted-foreground">Readiness</p>
            </div>
            <div className="rounded-xl bg-secondary/50 p-3 text-center">
              <p className="text-xl font-bold truncate" title={analysis.focusArea}>{analysis.focusArea}</p>
              <p className="text-xs text-muted-foreground">Focus Area</p>
            </div>
            <div className="rounded-xl bg-secondary/50 p-3 text-center">
              <p className="text-xl font-bold">{analysis.estimatedTimeToNextLevel}</p>
              <p className="text-xs text-muted-foreground">To Next Level</p>
            </div>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-4 text-xs">
            <span className="rounded-full bg-secondary px-3 py-1 text-muted-foreground">
              Next milestone: {analysis.nextMilestone}
            </span>
            <span className="rounded-full bg-secondary px-3 py-1 text-muted-foreground">
              Suggested split: {analysis.recommendedDifficultySplit}
            </span>
          </div>

          {/* Strong & Weak Tags */}
          <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
            {analysis.strongTags.length > 0 && (
              <div>
                <p className="mb-2 text-xs font-semibold text-green-400">Strong Areas</p>
                <div className="flex flex-wrap gap-2">
                  {analysis.strongTags.map((t) => (
                    <span key={t.slug} className="rounded-full bg-green-500/10 px-2.5 py-1 text-xs text-green-400">
                      {t.name} ({t.solved})
                    </span>
                  ))}
                </div>
              </div>
            )}
            {analysis.weakTags.length > 0 && (
              <div>
                <p className="mb-2 text-xs font-semibold text-red-400">Needs Work</p>
                <div className="flex flex-wrap gap-2">
                  {analysis.weakTags.map((t) => (
                    <span key={t.slug} className="rounded-full bg-red-500/10 px-2.5 py-1 text-xs text-red-400">
                      {t.name} ({t.solved})
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
        <div className="flex items-center gap-2">
          <Target className="h-4 w-4 text-primary" />
          <p className="text-sm">
            <span className="font-medium text-primary">Level {targetLevel} plan</span>
            <span className="text-muted-foreground"> — Recommendations are tailored for your target.</span>
          </p>
        </div>
      </div>

      {sorted.length === 0 && !isGenerating ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Lightbulb className="mx-auto mb-4 h-10 w-10 text-muted-foreground/40" />
            <p className="text-lg font-medium">All caught up!</p>
            <p className="text-sm text-muted-foreground">Click "Generate New Plan" to get fresh recommendations.</p>
          </CardContent>
        </Card>
      ) : sorted.length === 0 && isGenerating ? (
        <div className="space-y-3">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-24 animate-pulse rounded-xl bg-secondary" />
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {sorted.map((rec) => {
            const config = priorityConfig[rec.priority] || priorityConfig[3];
            const Icon = getDefaultIcon(rec.action);
            return (
              <div
                key={rec.id}
                className="group relative rounded-xl border border-border bg-card/50 px-6 py-5 transition-all hover:border-primary/20 hover:bg-card"
              >
                <div className="flex items-start gap-4">
                  <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <Icon className="h-4 w-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-3">
                      <h3 className="font-semibold">{rec.title}</h3>
                      <span className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${config.class}`}>
                        {config.label}
                      </span>
                      {rec.problemDifficulty && (
                        <Badge variant={rec.problemDifficulty === 'EASY' ? 'success' : rec.problemDifficulty === 'HARD' ? 'destructive' : 'warning'}>
                          {rec.problemDifficulty}
                        </Badge>
                      )}
                    </div>
                    <p className="mt-1 text-sm text-muted-foreground">{rec.description}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground/60">{rec.reason}</p>
                    {rec.problemSlug && (
                      <a
                        href={`https://leetcode.com/problems/${rec.problemSlug}/`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:text-primary/80 transition-colors"
                      >
                        <ExternalLink className="h-3 w-3" />
                        Solve on LeetCode
                      </a>
                    )}
                  </div>
                  <button
                    onClick={() => dismissMutation.mutate(rec.id)}
                    className="shrink-0 rounded-lg p-2 text-muted-foreground/40 opacity-0 transition-all hover:bg-destructive/10 hover:text-destructive group-hover:opacity-100"
                    title="Dismiss"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
