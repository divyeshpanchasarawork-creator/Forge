import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { problemsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';

export default function ProblemsPage() {
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState({ title: '', difficulty: 'EASY', leetcodeId: '', solutionUrl: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['problems'],
    queryFn: () => problemsApi.getAll().then((res) => res.data.data),
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
        <h1 className="text-2xl font-bold">Problems</h1>
        <button
          onClick={() => setShowAdd(!showAdd)}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" /> Add Problem
        </button>
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
