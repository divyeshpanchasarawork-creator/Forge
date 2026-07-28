import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Flame, Target, RefreshCw, TrendingUp, TrendingDown, BookOpen, Zap } from 'lucide-react';

export default function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get().then((res) => res.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="h-24 animate-pulse rounded-2xl bg-secondary" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-40 animate-pulse rounded-2xl bg-secondary" />
          ))}
        </div>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="space-y-6">
      {/* Greeting */}
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
            <Flame className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">{data.greeting}</h1>
            <p className="text-muted-foreground">{data.todayMission}</p>
          </div>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Current Focus</CardTitle>
            <Target className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <p className="text-lg font-semibold">{data.currentFocus}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Revisions Due</CardTitle>
            <RefreshCw className="h-4 w-4 text-orange-400" />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">{data.revisionsDue?.length || 0}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Avg Mastery</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-400" />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">{data.knowledgeHealth?.averageMastery || 0}%</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Avg Confidence</CardTitle>
            <Zap className="h-4 w-4 text-yellow-400" />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">{data.knowledgeHealth?.averageConfidence || 0}/10</p>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Revisions Due */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <RefreshCw className="h-4 w-4" />
              Today's Revisions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.revisionsDue?.length === 0 && (
              <p className="text-sm text-muted-foreground">No revisions due today. Great job!</p>
            )}
            {data.revisionsDue?.slice(0, 5).map((rev: any) => (
              <div key={rev.id} className="flex items-center justify-between rounded-lg bg-secondary/50 px-4 py-3">
                <div>
                  <p className="text-sm font-medium">{rev.topicTitle}</p>
                  <p className="text-xs text-muted-foreground">{rev.topicCategory}</p>
                </div>
                <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                  Priority {rev.priority}
                </span>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Recommendations */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Zap className="h-4 w-4" />
              Recommendations
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.recommendations?.length === 0 && (
              <p className="text-sm text-muted-foreground">No recommendations right now.</p>
            )}
            {data.recommendations?.slice(0, 5).map((rec: any) => (
              <div key={rec.id} className="rounded-lg bg-secondary/50 px-4 py-3">
                <p className="text-sm font-medium">{rec.title}</p>
                <p className="mt-1 text-xs text-muted-foreground">{rec.reason}</p>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Weak Topics */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingDown className="h-4 w-4 text-red-400" />
              Weakest Topics
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.weakTopics?.slice(0, 5).map((topic: any) => (
              <div key={topic.id} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">{topic.title}</p>
                  <p className="text-xs text-muted-foreground">{topic.category}</p>
                </div>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-20 rounded-full bg-secondary">
                    <div
                      className="h-2 rounded-full bg-red-400"
                      style={{ width: `${topic.confidence * 10}%` }}
                    />
                  </div>
                  <span className="text-xs text-muted-foreground">{topic.confidence}/10</span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Strong Topics */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-green-400" />
              Strongest Topics
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.strongTopics?.slice(0, 5).map((topic: any) => (
              <div key={topic.id} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">{topic.title}</p>
                  <p className="text-xs text-muted-foreground">{topic.category}</p>
                </div>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-20 rounded-full bg-secondary">
                    <div
                      className="h-2 rounded-full bg-green-400"
                      style={{ width: `${topic.confidence * 10}%` }}
                    />
                  </div>
                  <span className="text-xs text-muted-foreground">{topic.confidence}/10</span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>

      {/* Knowledge Health & Journal */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BookOpen className="h-4 w-4" />
              Knowledge Health
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-4">
              <div className="text-center">
                <p className="text-3xl font-bold text-primary">{data.knowledgeHealth?.totalTopics || 0}</p>
                <p className="text-sm text-muted-foreground">Total Topics</p>
              </div>
              <div className="text-center">
                <p className="text-3xl font-bold text-green-400">{data.knowledgeHealth?.masteredTopics || 0}</p>
                <p className="text-sm text-muted-foreground">Mastered</p>
              </div>
              <div className="text-center">
                <p className="text-3xl font-bold text-yellow-400">{data.knowledgeHealth?.averageMastery || 0}%</p>
                <p className="text-sm text-muted-foreground">Avg Mastery</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Today's Journal</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{data.recentJournal || 'No entry yet today.'}</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
