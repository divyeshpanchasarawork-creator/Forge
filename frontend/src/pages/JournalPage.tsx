import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { journalsApi, unwrap } from '@/api';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { SectionHeader } from '@/components/ui/SectionHeader';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import ApiErrorState from '@/components/ui/ApiErrorState';
import { SkeletonList } from '@/components/ui/LoadingSkeleton';
import { Input } from '@/components/ui/Input';
import { useToast } from '@/contexts/ToastContext';
import { parseApiError } from '@/lib/error';
import { NotebookPen, ListChecks } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';

const inputClass =
  'w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary';

const DEFAULT_FORM = {
  morningGoal: '',
  eveningReflection: '',
  energy: 3,
  mood: 3,
  hoursStudied: 0,
  achievements: '',
  challenges: '',
  lessons: '',
};

const todayISO = (() => {
  const d = new Date();
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
})();

export default function JournalPage() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [form, setForm] = useState(DEFAULT_FORM);
  const hydratedRef = useRef(false);

  const [page, setPage] = useState(0);

  const { data: journalPage, error, refetch, isLoading } = useQuery({
    queryKey: ['journal', 'all', page],
    queryFn: () => journalsApi.getAll(page, 20).then(unwrap),
  });

  const entries = journalPage?.content ?? [];

  useEffect(() => {
    if (hydratedRef.current || isLoading) return;
    const todaysEntry = entries.find((entry) => entry.entryDate === todayISO);
    if (!todaysEntry) return;
    hydratedRef.current = true;
    setForm({
      morningGoal: todaysEntry.morningGoal ?? '',
      eveningReflection: todaysEntry.eveningReflection ?? '',
      energy: todaysEntry.energy ?? 3,
      mood: todaysEntry.mood ?? 3,
      hoursStudied: todaysEntry.hoursStudied ?? 0,
      achievements: todaysEntry.achievements ?? '',
      challenges: todaysEntry.challenges ?? '',
      lessons: todaysEntry.lessons ?? '',
    });
  }, [entries, isLoading]);

  const saveMutation = useMutation({
    mutationFn: journalsApi.save,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['journal'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      queryClient.invalidateQueries({ queryKey: ['memory'] });
      hydratedRef.current = true;
      setForm(DEFAULT_FORM);
      toast({ title: 'Journal entry saved', tone: 'success' });
    },
    onError: (err: unknown) => {
      toast({ title: 'Could not save entry', description: parseApiError(err), tone: 'danger' });
    },
  });

  const moodEmojis = ['', '😞', '😐', '🙂', '😊', '🤩'];
  const energyLabels = ['', 'Very Low', 'Low', 'Medium', 'High', 'Very High'];

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold tracking-tight">Journal</h1>

      {/* Today's Entry */}
      <Card>
        <CardHeader>
          <SectionHeader title="Today's Entry" icon={<NotebookPen className="h-4 w-4" />} />
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Morning Goal</label>
            <textarea
              value={form.morningGoal}
              onChange={(e) => setForm({ ...form, morningGoal: e.target.value })}
              placeholder="What do you want to accomplish today?"
              className={inputClass}
              rows={3}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Energy: {energyLabels[form.energy]}</label>
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
              <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Mood: {moodEmojis[form.mood]}</label>
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
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Hours Studied</label>
            <Input
              type="number"
              min={0}
              max={24}
              step={0.5}
              value={form.hoursStudied}
              onChange={(e) => setForm({ ...form, hoursStudied: Number(e.target.value) })}
            />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Achievements</label>
            <textarea
              value={form.achievements}
              onChange={(e) => setForm({ ...form, achievements: e.target.value })}
              placeholder="What did you achieve today?"
              className={inputClass}
              rows={2}
            />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Evening Reflection</label>
            <textarea
              value={form.eveningReflection}
              onChange={(e) => setForm({ ...form, eveningReflection: e.target.value })}
              placeholder="How was your day? What did you learn?"
              className={inputClass}
              rows={3}
            />
          </div>

          <Button onClick={() => saveMutation.mutate(form)} disabled={saveMutation.isPending} loading={saveMutation.isPending}>
            {saveMutation.isPending ? 'Saving…' : 'Save Journal Entry'}
          </Button>
        </CardContent>
      </Card>

      {/* All Entries */}
      {isLoading ? (
        <Card>
          <CardHeader>
            <SectionHeader title="All Entries" icon={<ListChecks className="h-4 w-4" />} />
          </CardHeader>
          <CardContent>
            <SkeletonList rows={5} />
          </CardContent>
        </Card>
      ) : error ? (
        <ApiErrorState error={error} onRetry={() => refetch()} />
      ) : entries.length > 0 ? (
        <Card>
          <CardHeader>
            <SectionHeader title="All Entries" icon={<ListChecks className="h-4 w-4" />} />
          </CardHeader>
          <CardContent className="space-y-3">
            {entries.map((journal) => (
              <div key={journal.id} className="rounded-xl bg-secondary/50 px-5 py-4">
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-sm font-medium">{new Date(`${journal.entryDate.slice(0, 10)}T00:00:00`).toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' })}</p>
                  <div className="flex items-center gap-2 text-lg">
                    <span>{moodEmojis[journal.mood || 3]}</span>
                    <span className="text-caption text-muted-foreground">Energy: {journal.energy}/5</span>
                  </div>
                </div>
                {journal.morningGoal && <p className="text-sm text-muted-foreground">Goal: {journal.morningGoal}</p>}
                {journal.achievements && <p className="text-sm text-success">Achieved: {journal.achievements}</p>}
              </div>
            ))}
          </CardContent>
          <CardContent className="flex items-center justify-between border-t border-border pt-4">
            <Button variant="outline" size="sm" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
              Previous
            </Button>
            <span className="text-caption text-muted-foreground tabular-nums">
              Page {journalPage ? journalPage.page + 1 : 1} of {journalPage?.totalPages ?? 1}
            </span>
            <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={!journalPage || journalPage.last}>
              Next
            </Button>
          </CardContent>
        </Card>
      ) : (
        <EmptyState
          icon={<NotebookPen className="h-5 w-5" />}
          title="Your journal powers the rest of Forge"
          description="Every entry fuels your Memory page (patterns, mistakes, insights), the Analytics heatmap, and your weekly study hours. One honest line a day is enough — consistency compounds."
        />
      )}
    </div>
  );
}
