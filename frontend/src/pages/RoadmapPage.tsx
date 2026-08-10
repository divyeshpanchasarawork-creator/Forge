import { useQuery } from '@tanstack/react-query';
import { roadmapApi } from '@/api';
import { useAuth } from '@/contexts/AuthContext';
import TeachingEmptyState from '@/components/ui/TeachingEmptyState';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { SectionHeader } from '@/components/ui/SectionHeader';
import { StatTile } from '@/components/ui/StatTile';
import { Badge } from '@/components/ui/Badge';
import { Callout } from '@/components/ui/Callout';
import { EmptyState } from '@/components/ui/EmptyState';
import { buttonVariants } from '@/components/ui/Button';
import { scoreTone, toneText } from '@/lib/score';
import { Brain, Target, ArrowRight, Lightbulb } from 'lucide-react';
import { Link } from 'react-router-dom';
import { SkeletonCard } from '@/components/ui/LoadingSkeleton';
import ApiErrorState from '@/components/ui/ApiErrorState';

export default function RoadmapPage() {
  const { user } = useAuth();
  const targetLevel = user?.targetLevel ?? 5;

  const { data: analysis, isLoading, error, refetch } = useQuery({
    queryKey: ['roadmap-analysis'],
    queryFn: () => roadmapApi.getAnalysis().then((res) => res.data.data),
  });

  if (error) {
    return <ApiErrorState error={error} onRetry={() => refetch()} />;
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <SkeletonCard className="h-64" />
        <SkeletonCard className="h-24" />
      </div>
    );
  }

  const readinessTone = scoreTone(analysis?.readinessScore ?? 0, { good: 70, fair: 40 });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold tracking-tight">Roadmap</h1>

      {analysis && (
        <Card className="border-primary/20">
          <CardHeader>
            <SectionHeader title="Your Personal Analysis" icon={<Brain className="h-4 w-4" />} />
          </CardHeader>
          <CardContent className="space-y-5">
            <p className="text-sm leading-relaxed text-muted-foreground">{analysis.paragraph}</p>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <StatTile label="Target" value={`Level ${analysis.currentLevel}`} tone="primary" />
              <StatTile
                label="Readiness"
                value={`${analysis.readinessScore}%`}
                tone={readinessTone}
              />
              <StatTile label="Focus Area" value={analysis.focusArea} hint={analysis.focusArea.length > 20 ? analysis.focusArea : undefined} />
              <StatTile label="To Next Level" value={analysis.estimatedTimeToNextLevel} />
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="outline">Next milestone: {analysis.nextMilestone}</Badge>
              <Badge variant="outline">Suggested split: {analysis.recommendedDifficultySplit}</Badge>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {(analysis.strongTags ?? []).length > 0 && (
                <div>
                  <p className={`mb-2 text-caption font-semibold ${toneText.success}`}>Strong Areas</p>
                  <div className="flex flex-wrap gap-2">
                    {(analysis.strongTags ?? []).map((t) => (
                      <Badge key={t.slug} variant="success">
                        {t.name} ({t.solved})
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
              {(analysis.weakTags ?? []).length > 0 && (
                <div>
                  <p className={`mb-2 text-caption font-semibold ${toneText.danger}`}>Needs Work</p>
                  <div className="flex flex-wrap gap-2">
                    {(analysis.weakTags ?? []).map((t) => (
                      <Badge key={t.slug} variant="destructive">
                        {t.name} ({t.solved})
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
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
            <Link to="/app/profile" className={buttonVariants()}>
              Set your target level
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      )}

      <Callout tone="primary" icon={<Target className="h-4 w-4" />}>
        <p className="text-sm">
          <span className="font-medium text-primary">Level {targetLevel} plan</span>
          <span className="text-muted-foreground"> — Recommendations are tailored for your target.</span>
        </p>
      </Callout>

      <EmptyState
        title="Start your practice session to execute this plan"
        action={
          <Link to="/app/problems" className={buttonVariants()}>
            Go to Practice Queue
            <ArrowRight className="h-4 w-4" />
          </Link>
        }
      />
    </div>
  );
}
