import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { User, Code2, Save, RefreshCw, Trophy, Flame, TrendingUp } from 'lucide-react';
import { authApi, leetcodeApi } from '@/api';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export default function ProfilePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [email, setEmail] = useState(user?.email || '');
  const [leetcodeUsername, setLeetcodeUsername] = useState(user?.leetcodeUsername || '');
  const [saved, setSaved] = useState(false);

  const profileMutation = useMutation({
    mutationFn: () => authApi.updateProfile({ displayName, email, leetcodeUsername }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    },
  });

  const syncMutation = useMutation({
    mutationFn: () => leetcodeApi.sync(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leetcode-stats'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
    },
  });

  const { data: lcData } = useQuery({
    queryKey: ['leetcode-stats'],
    queryFn: () => leetcodeApi.getStats().then((res) => res.data.data),
    enabled: !!user?.leetcodeUsername,
  });

  const lcStats = lcData;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Profile</h1>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-4 w-4" />
            Account Information
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Username</label>
            <input
              type="text"
              value={user?.username || ''}
              disabled
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground opacity-60"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Display Name</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Code2 className="h-4 w-4" />
            LeetCode Integration
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">LeetCode Username</label>
            <input
              type="text"
              value={leetcodeUsername}
              onChange={(e) => setLeetcodeUsername(e.target.value)}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="your_leetcode_username"
            />
          </div>

          {user?.leetcodeUsername && (
            <button
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-sm font-medium text-primary hover:bg-primary/20 disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
              {syncMutation.isPending ? 'Syncing...' : 'Sync LeetCode Data'}
            </button>
          )}

          {syncMutation.isError && (
            <p className="text-sm text-red-400">Sync failed. Check your LeetCode username and try again.</p>
          )}

          {lcStats && (
            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <p className="text-2xl font-bold text-primary">{lcStats.totalSolved}</p>
                  <p className="text-xs text-muted-foreground">Total Solved</p>
                </div>
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <Flame className="h-4 w-4 text-orange-400" />
                    <p className="text-2xl font-bold text-orange-400">{lcStats.streak}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">Day Streak</p>
                </div>
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <Trophy className="h-4 w-4 text-yellow-400" />
                    <p className="text-2xl font-bold text-yellow-400">{lcStats.ranking ? `#${lcStats.ranking.toLocaleString()}` : '-'}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">Ranking</p>
                </div>
                <div className="rounded-lg bg-secondary/50 px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <TrendingUp className="h-4 w-4 text-green-400" />
                    <p className="text-2xl font-bold text-green-400">{lcStats.totalActiveDays}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">Active Days</p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="flex-1 rounded-lg bg-green-500/10 px-3 py-2 text-center">
                  <p className="text-lg font-bold text-green-400">{lcStats.easySolved}</p>
                  <p className="text-xs text-green-400/70">Easy</p>
                </div>
                <div className="flex-1 rounded-lg bg-yellow-500/10 px-3 py-2 text-center">
                  <p className="text-lg font-bold text-yellow-400">{lcStats.mediumSolved}</p>
                  <p className="text-xs text-yellow-400/70">Medium</p>
                </div>
                <div className="flex-1 rounded-lg bg-red-500/10 px-3 py-2 text-center">
                  <p className="text-lg font-bold text-red-400">{lcStats.hardSolved}</p>
                  <p className="text-xs text-red-400/70">Hard</p>
                </div>
              </div>

              {lcStats.lastSyncedAt && (
                <p className="text-xs text-muted-foreground">
                  Last synced: {new Date(lcStats.lastSyncedAt).toLocaleString()}
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center gap-4">
        <button
          onClick={() => profileMutation.mutate()}
          disabled={profileMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          <Save className="h-4 w-4" />
          {profileMutation.isPending ? 'Saving...' : 'Save Changes'}
        </button>
        {saved && <span className="text-sm text-green-400">Profile updated!</span>}
      </div>
    </div>
  );
}
