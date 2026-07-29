import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { revisionsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import KpiCard from '@/components/ui/KpiCard';
import { useAuth } from '@/contexts/AuthContext';
import { CheckCircle, Clock, Calendar, ListTodo, TrendingUp, Brain } from 'lucide-react';

export default function RevisionPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const targetLevel = user?.targetLevel ?? 5;
  const overdueThreshold = targetLevel >= 7 ? '7 days' : targetLevel >= 4 ? '14 days' : '21 days';

  const { data: todayRevisions, isLoading: loadingToday } = useQuery({
    queryKey: ['revisions', 'today'],
    queryFn: () => revisionsApi.getToday().then((res) => res.data.data),
  });

  const { data: pendingRevisions, isLoading: loadingPending } = useQuery({
    queryKey: ['revisions', 'pending'],
    queryFn: () => revisionsApi.getPending().then((res) => res.data.data),
  });

  const completeMutation = useMutation({
    mutationFn: revisionsApi.complete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['revisions'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  if (loadingToday || loadingPending) {
    return (
      <div className="space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-24 animate-pulse rounded-2xl bg-secondary" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Revision</h1>

      {/* SM-2 context */}
      <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
        <div className="flex items-start gap-3">
          <Brain className="h-5 w-5 text-primary shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-primary">Spaced Repetition (SM-2)</p>
            <p className="text-xs text-muted-foreground mt-1">
              Revisions are scheduled using the SM-2 algorithm. Topics you review well (quality 4-5) get longer gaps.
              At your target level (<strong>Level {targetLevel}</strong>), topics overdue beyond <strong>{overdueThreshold}</strong> need immediate attention.
            </p>
          </div>
        </div>
      </div>

      {/* Revision KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <KpiCard icon={<Clock className="h-5 w-5 text-orange-400" />} value={todayRevisions?.length || 0} label="Due Today" tooltip="Revisions scheduled for today." />
        <KpiCard icon={<ListTodo className="h-5 w-5 text-yellow-400" />} value={pendingRevisions?.length || 0} label="Total Pending" tooltip="All pending revisions not yet completed." />
        <KpiCard icon={<Calendar className="h-5 w-5 text-blue-400" />} value={todayRevisions?.filter((r: any) => r.completed).length || 0} label="Completed Today" tooltip="Revisions completed today." />
        <KpiCard icon={<TrendingUp className="h-5 w-5 text-green-400" />} value={todayRevisions && todayRevisions.length > 0 ? 'Due' : 'Clear'} label="Status" tooltip={todayRevisions?.length > 0 ? 'Revisions are due today.' : 'No revisions due — you are on track.'} />
      </div>

      {/* Today's Revisions */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5 text-orange-400" />
            Today's Revisions
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {todayRevisions?.length === 0 && (
            <p className="text-sm text-muted-foreground">No revisions due today. Well done!</p>
          )}
          {todayRevisions?.map((rev) => (
            <div key={rev.id} className="flex items-center justify-between rounded-xl bg-secondary/50 px-5 py-4">
              <div className="flex-1">
                <p className="font-medium">{rev.topicTitle}</p>
                <p className="text-sm text-muted-foreground">{rev.topicCategory} &middot; {rev.reason || 'Spaced repetition review'}</p>
              </div>
              <button
                onClick={() => completeMutation.mutate(rev.id)}
                disabled={completeMutation.isPending}
                className="flex items-center gap-2 rounded-lg bg-green-500/10 px-4 py-2 text-sm font-medium text-green-400 hover:bg-green-500/20 transition-colors"
              >
                <CheckCircle className="h-4 w-4" />
                Complete
              </button>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Pending Revisions */}
      {pendingRevisions && pendingRevisions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>All Pending Revisions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {pendingRevisions.map((rev) => (
              <div key={rev.id} className="flex items-center justify-between rounded-xl bg-secondary/30 px-5 py-3">
                <div>
                  <p className="text-sm font-medium">{rev.topicTitle}</p>
                  <p className="text-xs text-muted-foreground">Scheduled: {new Date(rev.scheduledDate).toLocaleDateString()}</p>
                </div>
                <button
                  onClick={() => completeMutation.mutate(rev.id)}
                  className="text-sm text-muted-foreground hover:text-green-400 transition-colors"
                >
                  Complete
                </button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
