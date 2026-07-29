import { useQuery } from '@tanstack/react-query';
import { memoryApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Brain, AlertTriangle, Lightbulb, RefreshCw } from 'lucide-react';

export default function MemoryPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['memory'],
    queryFn: () => memoryApi.get().then((res) => res.data.data),
  });

  if (isLoading) return <div className="p-6 text-muted-foreground">Loading memory data...</div>;
  if (error) return <div className="p-6 text-red-400">Failed to load memory data.</div>;
  if (!data) return null;

  const { fadingConcepts, patternsDiscovered, pastMistakes, insights } = data;

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center gap-3">
        <Brain className="h-6 w-6 text-primary" />
        <h1 className="text-xl font-bold">Memory</h1>
      </div>

      {/* Fading Concepts */}
      <div className="space-y-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
          <AlertTriangle className="h-4 w-4 text-yellow-400" />
          Fading Concepts ({fadingConcepts.length})
        </h2>
        {fadingConcepts.length === 0 ? (
          <p className="text-sm text-muted-foreground">No fading concepts right now.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {fadingConcepts.map((concept) => (
              <Card key={concept.topicId}>
                <CardContent className="p-4">
                  <p className="font-medium text-sm">{concept.title}</p>
                  <p className="text-xs text-muted-foreground">{concept.category}</p>
                  <div className="mt-2 flex flex-wrap gap-2 text-xs">
                    <span className="rounded-md bg-red-500/10 px-2 py-0.5 text-red-400">
                      Confidence {concept.confidence}/10
                    </span>
                    {concept.daysSinceRevision >= 0 && (
                      <span className="rounded-md bg-yellow-500/10 px-2 py-0.5 text-yellow-400">
                        {concept.daysSinceRevision}d ago
                      </span>
                    )}
                    {concept.estimatedRetention != null && (
                      <span className="rounded-md bg-blue-500/10 px-2 py-0.5 text-blue-400">
                        {concept.estimatedRetention.toFixed(0)}% retention
                      </span>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Insights */}
      <div className="space-y-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
          <Lightbulb className="h-4 w-4 text-amber-400" />
          Key Insights ({insights.length})
        </h2>
        {insights.length === 0 ? (
          <p className="text-sm text-muted-foreground">No insights recorded yet.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {insights.map((entry, i) => (
              <Card key={i}>
                <CardContent className="p-4">
                  <p className="text-xs text-muted-foreground">{entry.date}</p>
                  <p className="mt-1 text-sm">{entry.content}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Patterns Discovered */}
      <div className="space-y-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
          <RefreshCw className="h-4 w-4 text-green-400" />
          Patterns Discovered ({patternsDiscovered.length})
        </h2>
        {patternsDiscovered.length === 0 ? (
          <p className="text-sm text-muted-foreground">No patterns identified yet.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {patternsDiscovered.map((entry, i) => (
              <Card key={i}>
                <CardContent className="p-4">
                  <p className="text-xs text-muted-foreground">{entry.date}</p>
                  <p className="mt-1 text-sm">{entry.content}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Past Mistakes */}
      <div className="space-y-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
          <AlertTriangle className="h-4 w-4 text-red-400" />
          Past Mistakes ({pastMistakes.length})
        </h2>
        {pastMistakes.length === 0 ? (
          <p className="text-sm text-muted-foreground">No mistakes logged yet.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {pastMistakes.map((entry, i) => (
              <Card key={i}>
                <CardContent className="p-4">
                  <p className="text-xs text-muted-foreground">{entry.date}</p>
                  <p className="mt-1 text-sm">{entry.content}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
