import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { journalsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import ApiErrorState from '@/components/ui/ApiErrorState';
import { useState } from 'react';

export default function JournalPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    morningGoal: '',
    eveningReflection: '',
    energy: 3,
    mood: 3,
    hoursStudied: 0,
    achievements: '',
    challenges: '',
    lessons: '',
  });

  const [page, setPage] = useState(0);

  const { data: journalPage, error, refetch } = useQuery({
    queryKey: ['journal', 'all', page],
    queryFn: () => journalsApi.getAll(page, 20).then((res) => res.data.data),
  });

  const entries = journalPage?.content ?? [];

  const saveMutation = useMutation({
    mutationFn: journalsApi.save,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['journal'] });
      queryClient.invalidateQueries({ queryKey: ['journals'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const moodEmojis = ['', '😞', '😐', '🙂', '😊', '🤩'];
  const energyLabels = ['', 'Very Low', 'Low', 'Medium', 'High', 'Very High'];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Journal</h1>

      {/* Today's Entry */}
      <Card>
        <CardHeader>
          <CardTitle>Today's Entry</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Morning Goal</label>
            <textarea
              value={form.morningGoal}
              onChange={(e) => setForm({ ...form, morningGoal: e.target.value })}
              placeholder="What do you want to accomplish today?"
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              rows={3}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-1.5">Energy: {energyLabels[form.energy]}</label>
              <input
                type="range"
                min={1}
                max={5}
                value={form.energy}
                onChange={(e) => setForm({ ...form, energy: Number(e.target.value) })}
                className="w-full accent-primary"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-1.5">Mood: {moodEmojis[form.mood]}</label>
              <input
                type="range"
                min={1}
                max={5}
                value={form.mood}
                onChange={(e) => setForm({ ...form, mood: Number(e.target.value) })}
                className="w-full accent-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Hours Studied</label>
            <input
              type="number"
              min={0}
              max={24}
              step={0.5}
              value={form.hoursStudied}
              onChange={(e) => setForm({ ...form, hoursStudied: Number(e.target.value) })}
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground focus:border-primary focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Achievements</label>
            <textarea
              value={form.achievements}
              onChange={(e) => setForm({ ...form, achievements: e.target.value })}
              placeholder="What did you achieve today?"
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              rows={2}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-1.5">Evening Reflection</label>
            <textarea
              value={form.eveningReflection}
              onChange={(e) => setForm({ ...form, eveningReflection: e.target.value })}
              placeholder="How was your day? What did you learn?"
              className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              rows={3}
            />
          </div>

          <button
            onClick={() => saveMutation.mutate(form)}
            disabled={saveMutation.isPending}
            className="rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {saveMutation.isPending ? 'Saving...' : 'Save Journal Entry'}
          </button>
        </CardContent>
      </Card>

      {/* All Entries */}
      {error ? (
        <ApiErrorState error={error} onRetry={() => refetch()} />
      ) : entries.length > 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>All Entries</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {entries.map((journal) => (
              <div key={journal.id} className="rounded-xl bg-secondary/30 px-5 py-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-sm font-medium">{new Date(journal.entryDate).toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' })}</p>
                  <div className="flex gap-2 text-lg">
                    <span>{moodEmojis[journal.mood || 3]}</span>
                    <span className="text-xs text-muted-foreground">Energy: {journal.energy}/5</span>
                  </div>
                </div>
                {journal.morningGoal && <p className="text-sm text-muted-foreground">Goal: {journal.morningGoal}</p>}
                {journal.achievements && <p className="text-sm text-green-400">Achieved: {journal.achievements}</p>}
              </div>
            ))}
          </CardContent>
          <CardContent className="flex items-center justify-between border-t border-border pt-4">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-secondary disabled:opacity-40"
            >
              Previous
            </button>
            <span className="text-xs text-muted-foreground">
              Page {journalPage ? journalPage.page + 1 : 1} of {journalPage?.totalPages ?? 1}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={!journalPage || journalPage.last}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-secondary disabled:opacity-40"
            >
              Next
            </button>
          </CardContent>
        </Card>
      ) : (
        <div className="rounded-xl border border-dashed border-border px-6 py-8 text-center">
          <p className="text-sm font-medium">Your journal powers the rest of Forge</p>
          <p className="mx-auto mt-1 max-w-lg text-xs leading-relaxed text-muted-foreground">
            Every entry fuels your Memory page (patterns, mistakes, insights), the Analytics heatmap, and your weekly
            study hours. One honest line a day is enough — consistency compounds.
          </p>
        </div>
      )}
    </div>
  );
}
