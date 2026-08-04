import { useQuery } from '@tanstack/react-query';
import { roadmapApi } from '@/api';
import { useAuth } from '@/contexts/AuthContext';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Brain, Target, ArrowRight, Lightbulb } from 'lucide-react';
import { Link } from 'react-router-dom';
import { SkeletonCard } from '@/components/ui/LoadingSkeleton';

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
  const targetLevel = user?.targetLevel ?? 5;

  const { data: analysis, isLoading } = useQuery({
    queryKey: ['roadmap-analysis'],
    queryFn: () => roadmapApi.getAnalysis().then((res) => res.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <SkeletonCard className="h-64" />
        <SkeletonCard className="h-24" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Roadmap</h1>

      {analysis && (
        <div className="rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/5 to-transparent p-6 shadow-soft">
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

          <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
            {(analysis.strongTags ?? []).length > 0 && (
              <div>
                <p className="mb-2 text-xs font-semibold text-green-400">Strong Areas</p>
                <div className="flex flex-wrap gap-2">
                  {(analysis.strongTags ?? []).map((t: any) => (
                    <span key={t.slug} className="rounded-full bg-green-500/10 px-2.5 py-1 text-xs text-green-400">
                      {t.name} ({t.solved})
                    </span>
                  ))}
                </div>
              </div>
            )}
            {(analysis.weakTags ?? []).length > 0 && (
              <div>
                <p className="mb-2 text-xs font-semibold text-red-400">Needs Work</p>
                <div className="flex flex-wrap gap-2">
                  {(analysis.weakTags ?? []).map((t: any) => (
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

      {!analysis && (
        <TeachingEmptyState
          icon={<Lightbulb className="h-6 w-6 text-primary" />}
          title="Your roadmap is generated from your data"
          description="This page reads your solved problems, topic mastery, and revision history to lay out the fastest path to your target level."
          steps={[
            'Sync LeetCode so your solved count and tag strengths are real.',
            'Review a few topics so mastery and retention signals are fresh.',
            'Generate recommendations on the Dashboard to feed the queue.',
          ]}
          action={
            <Link
              to="/app/profile"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              Set your target level
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
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

      <div className="rounded-xl border border-dashed border-border p-6 text-center">
        <p className="text-sm text-muted-foreground">
          Start your practice session to execute this plan.
        </p>
        <Link
          to="/app/problems"
          className="mt-3 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          Go to Practice Queue
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
}
