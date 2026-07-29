import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { problemsApi, dashboardApi } from '@/api';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import KpiCard from '@/components/ui/KpiCard';
import { useAuth } from '@/contexts/AuthContext';
import { Plus, Trash2, Code, CircleCheck, Zap, Shield, Target, Lightbulb, TrendingUp } from 'lucide-react';
import { useState } from 'react';

const levelAdvice = {
  1: { focus: 'Build foundational DSA. Focus on Arrays, Strings, Basic Math.', difficulty: 'Mostly Easy' },
  2: { focus: 'Strengthen Arrays, Strings, Hash Maps. Add Linked Lists.', difficulty: 'Easy + some Medium' },
  3: { focus: 'Cover Trees, Stacks, Queues. Add Binary Search.', difficulty: 'Mix Easy/Medium' },
  4: { focus: 'Deepen Trees, Graphs. Start DP basics.', difficulty: 'Medium > Easy' },
  5: { focus: 'DP, Graphs, Backtracking. Add System Design basics.', difficulty: 'Medium + 25% Hard' },
  6: { focus: 'Advanced DP, Graphs, Tries. Mock interviews.', difficulty: 'Medium/Heavy Hard' },
  7: { focus: 'All DSA in depth. System Design for interviews.', difficulty: '40% Medium, 50% Hard' },
  8: { focus: 'Hard problems daily. DP, Graphs, Advanced Trees.', difficulty: 'Mostly Hard' },
  9: { focus: 'Every Hard tag. Contest performance. ICC topics.', difficulty: '70%+ Hard' },
  10: { focus: 'Elite DSA. Advanced algorithms. Quant-style prep.', difficulty: '80% Hard' },
};

export default function ProblemsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState({ title: '', difficulty: 'EASY', leetcodeId: '', solutionUrl: '' });

  const targetLevel = user?.targetLevel ?? 5;
  const advice = levelAdvice[targetLevel as keyof typeof levelAdvice] || levelAdvice[5];

  const { data, isLoading } = useQuery({
    queryKey: ['problems'],
    queryFn: () => problemsApi.getAll().then((res) => res.data.data),
  });

  const { data: dashData } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get().then((res) => res.data.data),
  });

  const createMutation = useMutation({
    mutationFn: problemsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['problems'] });
      setShowAdd(false);
      setForm({ title: '', difficulty: 'EASY', leetcodeId: '', solutionUrl: '' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: problemsApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['problems'] }),
  });

  const problems = data?.content || [];
  const tp = dashData?.targetProgress;

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-16 animate-pulse rounded-2xl bg-secondary" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Practice</h1>
        <button
          onClick={() => setShowAdd(!showAdd)}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" /> Add Problem
        </button>
      </div>

      {/* Practice Insights — Target-driven guidance */}
      <Card className="border-primary/20 bg-primary/5">
        <CardContent className="p-5 space-y-3">
          <div className="flex items-center gap-2">
            <Target className="h-4 w-4 text-primary" />
            <span className="text-sm font-semibold text-primary">Level {targetLevel} Practice Plan</span>
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
            <div className="rounded-lg bg-secondary/50 p-3">
              <p className="text-xs text-muted-foreground font-medium mb-1">Focus Areas</p>
              <p className="text-sm">{advice.focus}</p>
            </div>
            <div className="rounded-lg bg-secondary/50 p-3">
              <p className="text-xs text-muted-foreground font-medium mb-1">Difficulty Mix</p>
              <p className="text-sm">{advice.difficulty}</p>
            </div>
            <div className="rounded-lg bg-secondary/50 p-3">
              <p className="text-xs text-muted-foreground font-medium mb-1">Target Progress</p>
              {tp ? (
                <p className="text-sm">
                  {tp.totalSolved} / {tp.targetTotal} solved
                  <span className="text-xs text-muted-foreground ml-1">({tp.readinessScore}% ready)</span>
                </p>
              ) : (
                <p className="text-sm text-muted-foreground">Sync LeetCode to track</p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Problem KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <KpiCard icon={<Code className="h-5 w-5 text-primary" />} value={data?.totalElements || 0} label="Total Problems" tooltip="Total number of LeetCode problems." />
        <KpiCard icon={<Zap className="h-5 w-5 text-green-400" />} value={problems.filter((p: any) => p.difficulty === 'EASY').length} label="Easy" tooltip="Problems with EASY difficulty." />
        <KpiCard icon={<Shield className="h-5 w-5 text-yellow-400" />} value={problems.filter((p: any) => p.difficulty === 'MEDIUM').length} label="Medium" tooltip="Problems with MEDIUM difficulty." />
        <KpiCard icon={<CircleCheck className="h-5 w-5 text-red-400" />} value={problems.filter((p: any) => p.difficulty === 'HARD').length} label="Hard" tooltip="Problems with HARD difficulty." />
      </div>

      {showAdd && (
        <Card>
          <CardContent className="p-6">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <input
                placeholder="Problem title"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="rounded-lg border border-input bg-secondary px-4 py-2 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              />
              <select
                value={form.difficulty}
                onChange={(e) => setForm({ ...form, difficulty: e.target.value })}
                className="rounded-lg border border-input bg-secondary px-4 py-2 text-foreground focus:border-primary focus:outline-none"
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
              <input
                placeholder="LeetCode ID"
                value={form.leetcodeId}
                onChange={(e) => setForm({ ...form, leetcodeId: e.target.value })}
                className="rounded-lg border border-input bg-secondary px-4 py-2 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <button
              onClick={() => createMutation.mutate({ ...form, topicIds: [] })}
              disabled={!form.title || createMutation.isPending}
              className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Problem'}
            </button>
          </CardContent>
        </Card>
      )}

      <div className="rounded-2xl border border-border bg-card overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border">
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground">Title</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground">Difficulty</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground">Topics</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground">Date</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-muted-foreground"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {problems.map((problem) => (
              <tr key={problem.id} className="hover:bg-secondary/30 transition-colors">
                <td className="px-6 py-4">
                  <p className="text-sm font-medium">{problem.title}</p>
                  {problem.leetcodeId && <p className="text-xs text-muted-foreground">#{problem.leetcodeId}</p>}
                </td>
                <td className="px-6 py-4">
                  <Badge variant={problem.difficulty === 'EASY' ? 'success' : problem.difficulty === 'HARD' ? 'destructive' : 'warning'}>
                    {problem.difficulty}
                  </Badge>
                </td>
                <td className="px-6 py-4">
                  <div className="flex gap-1">
                    {problem.topics?.slice(0, 2).map((t) => (
                      <Badge key={t.id} variant="outline">{t.title}</Badge>
                    ))}
                  </div>
                </td>
                <td className="px-6 py-4 text-sm text-muted-foreground">
                  {problem.solvedAt ? new Date(problem.solvedAt).toLocaleDateString() : '-'}
                </td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => deleteMutation.mutate(problem.id)}
                    className="text-muted-foreground hover:text-destructive transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
