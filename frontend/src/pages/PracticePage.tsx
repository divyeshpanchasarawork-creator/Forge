import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { recommendationsApi, dashboardApi, leetcodeApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { useAuth } from '@/contexts/AuthContext';
import { Code, Target, ExternalLink, X, RefreshCw, Sparkles, Layers } from 'lucide-react';
import { useState } from 'react';

export default function PracticePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const targetLevel = user?.targetLevel ?? 5;
  const [syncing, setSyncing] = useState(false);
  const [filterTag, setFilterTag] = useState<string | null>(null);

  const { data: dashData } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get().then((res) => res.data.data),
  });

  const { data: recs } = useQuery({
    queryKey: ['recommendations'],
    queryFn: () => recommendationsApi.getActive().then((res) => res.data.data || []),
  });

  const dismissMutation = useMutation({
    mutationFn: recommendationsApi.dismiss,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recommendations'] }),
  });

  const generateMutation = useMutation({
    mutationFn: () => recommendationsApi.generate(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const handleSync = async () => {
    setSyncing(true);
    try {
      await leetcodeApi.sync();
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } finally {
      setSyncing(false);
    }
  };

  const problemRecs = (recs || []).filter(r => r.problemSlug);
  const filteredRecs = filterTag
    ? problemRecs.filter(r => r.description?.toLowerCase().includes(filterTag.toLowerCase()))
    : problemRecs;

  const knowledgeMap = dashData?.knowledgeMap || [];
  const tp = dashData?.targetProgress;

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

      {!dashData?.leetcodeStats && (
        <Card className="border-amber-500/20 bg-amber-500/5">
          <CardContent className="p-4 text-sm">
            Sync your LeetCode profile to get personalized problem recommendations based on your weak areas.
          </CardContent>
        </Card>
      )}

      {problemRecs.length > 0 && (
        <div>
          <div className="flex items-center gap-2 mb-3">
            <Target className="h-4 w-4 text-primary" />
            <h2 className="text-lg font-semibold">Your Practice Queue</h2>
            <span className="text-xs text-muted-foreground">({problemRecs.length} problem{problemRecs.length !== 1 ? 's' : ''})</span>
          </div>
          <div className="space-y-2">
            {filteredRecs.map((rec) => (
              <div
                key={rec.id}
                className="group flex items-center gap-4 rounded-xl border border-border bg-card/50 px-5 py-4 transition-all hover:border-primary/20 hover:bg-card"
              >
                <Code className="h-5 w-5 shrink-0 text-primary" />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium truncate">{rec.problemTitle || rec.title}</span>
                    {rec.problemDifficulty && (
                      <Badge variant={rec.problemDifficulty === 'EASY' ? 'success' : rec.problemDifficulty === 'HARD' ? 'destructive' : 'warning'} className="shrink-0">
                        {rec.problemDifficulty}
                      </Badge>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground mt-0.5">{rec.description}</p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {rec.problemSlug && (
                    <a
                      href={`https://leetcode.com/problems/${rec.problemSlug}/`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-1 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20 transition-colors"
                    >
                      <ExternalLink className="h-3 w-3" />
                      Solve
                    </a>
                  )}
                  <button
                    onClick={() => dismissMutation.mutate(rec.id)}
                    className="rounded-lg p-1.5 text-muted-foreground/40 opacity-0 group-hover:opacity-100 hover:text-destructive transition-all"
                    title="Dismiss"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {dashData?.leetcodeStats && problemRecs.length === 0 && (
        <Card>
          <CardContent className="py-10 text-center">
            <Sparkles className="mx-auto mb-3 h-8 w-8 text-muted-foreground/40" />
            <p className="font-medium">Queue is clear</p>
            <p className="text-sm text-muted-foreground mb-4">Generate fresh problem suggestions based on your weak areas.</p>
            <button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
            >
              <RefreshCw className={`h-4 w-4 ${generateMutation.isPending ? 'animate-spin' : ''}`} />
              {generateMutation.isPending ? 'Generating...' : 'Generate Queue'}
            </button>
          </CardContent>
        </Card>
      )}

      {knowledgeMap.length > 0 && (
        <div>
          <div className="flex items-center gap-2 mb-3">
            <Layers className="h-4 w-4 text-primary" />
            <h2 className="text-lg font-semibold">Browse by Topic</h2>
          </div>
          <div className="flex flex-wrap gap-2 mb-3">
            {filterTag && (
              <button
                onClick={() => setFilterTag(null)}
                className="rounded-full border border-border px-3 py-1 text-xs hover:bg-secondary transition-colors"
              >
                Clear filter
              </button>
            )}
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
            {knowledgeMap.map((cat) => (
              <Card key={cat.category} className="hover:border-primary/20 transition-colors">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm">{cat.category}</CardTitle>
                  <p className="text-xs text-muted-foreground">{cat.averageMastery}% avg mastery</p>
                </CardHeader>
                <CardContent className="space-y-1.5">
                  {cat.topics.slice(0, 5).map((topic) => (
                    <button
                      key={topic.id}
                      onClick={() => setFilterTag(topic.title === filterTag ? null : topic.title)}
                      className={`w-full flex items-center justify-between rounded-lg px-3 py-2 text-xs transition-colors ${
                        filterTag === topic.title
                          ? 'bg-primary/10 text-primary'
                          : 'hover:bg-secondary/50 text-muted-foreground'
                      }`}
                    >
                      <span>{topic.title}</span>
                      <Badge variant={topic.confidence < 4 ? 'destructive' : topic.confidence >= 7 ? 'success' : 'default'}>
                        {topic.confidence}/10
                      </Badge>
                    </button>
                  ))}
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">{problemRecs.length}</p>
            <p className="text-xs text-muted-foreground">In Queue</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">{tp?.totalSolved || '-'}</p>
            <p className="text-xs text-muted-foreground">Solved</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">{tp?.readinessScore != null ? `${tp.readinessScore}%` : '-'}</p>
            <p className="text-xs text-muted-foreground">Readiness</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">{dashData?.leetcodeStats?.streak || 0}</p>
            <p className="text-xs text-muted-foreground">Streak</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
