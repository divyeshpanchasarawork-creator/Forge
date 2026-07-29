import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { topicsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import KpiCard from '@/components/ui/KpiCard';
import { Plus, Trash2, Layers, CheckCircle, RefreshCw, Circle } from 'lucide-react';
import { useState } from 'react';

const categories = ['Data Structures', 'Algorithms', 'System Design', 'Backend', 'Frontend', 'DevOps'];

export default function TopicsPage() {
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState({ title: '', description: '', category: 'Data Structures', confidence: 0, mastery: 0, notes: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['topics'],
    queryFn: () => topicsApi.getAll().then((res) => res.data.data),
  });

  const createMutation = useMutation({
    mutationFn: topicsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['topics'] });
      setShowAdd(false);
      setForm({ title: '', description: '', category: 'Data Structures', confidence: 0, mastery: 0, notes: '' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: topicsApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['topics'] }),
  });

  const topics = data?.content || [];

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-20 animate-pulse rounded-2xl bg-secondary" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Topics</h1>
        <button
          onClick={() => setShowAdd(!showAdd)}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" /> Add Topic
        </button>
      </div>

      {/* Topic KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <KpiCard icon={<Layers className="h-5 w-5 text-primary" />} value={data?.totalElements || 0} label="Total Topics" tooltip="Total number of topics across all categories." />
        <KpiCard icon={<CheckCircle className="h-5 w-5 text-green-400" />} value={topics.filter((t: any) => t.status === 'MASTERED').length} label="Mastered (page)" tooltip="Topics with mastery >= 80% shown on this page." />
        <KpiCard icon={<RefreshCw className="h-5 w-5 text-yellow-400" />} value={topics.filter((t: any) => t.status === 'IN_PROGRESS').length} label="In Progress" tooltip="Topics actively being worked on." />
        <KpiCard icon={<Circle className="h-5 w-5 text-muted-foreground" />} value={topics.filter((t: any) => !t.status || t.status === 'NOT_STARTED').length} label="Not Started" tooltip="Topics not yet begun." />
      </div>

      {showAdd && (
        <Card>
          <CardContent className="p-6">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <input
                placeholder="Topic title"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="rounded-lg border border-input bg-secondary px-4 py-2 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              />
              <select
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                className="rounded-lg border border-input bg-secondary px-4 py-2 text-foreground focus:border-primary focus:outline-none"
              >
                {categories.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
              <input
                placeholder="Description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="col-span-2 rounded-lg border border-input bg-secondary px-4 py-2 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <button
              onClick={() => createMutation.mutate(form)}
              disabled={!form.title || createMutation.isPending}
              className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Topic'}
            </button>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {topics.map((topic) => (
          <Card key={topic.id} className="group hover:border-primary/30 transition-colors">
            <CardHeader className="flex flex-row items-start justify-between pb-2">
              <div>
                <CardTitle className="text-base">{topic.title}</CardTitle>
                <p className="text-xs text-muted-foreground">{topic.category}</p>
              </div>
              <button
                onClick={() => deleteMutation.mutate(topic.id)}
                className="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-destructive transition-opacity"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <div className="flex items-center justify-between text-xs text-muted-foreground mb-1">
                  <span>Mastery</span>
                  <span>{topic.mastery}%</span>
                </div>
                <div className="h-2 rounded-full bg-secondary">
                  <div className="h-2 rounded-full bg-primary" style={{ width: `${topic.mastery}%` }} />
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">Confidence</span>
                <Badge variant={topic.confidence < 4 ? 'destructive' : topic.confidence >= 7 ? 'success' : 'default'}>
                  {topic.confidence}/10
                </Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">Status</span>
                <Badge variant={topic.status === 'MASTERED' ? 'success' : topic.status === 'IN_PROGRESS' ? 'warning' : 'default'}>
                  {topic.status.replace('_', ' ')}
                </Badge>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
