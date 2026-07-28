import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { revisionsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { CheckCircle, Clock } from 'lucide-react';

export default function RevisionPage() {
  const queryClient = useQueryClient();

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
