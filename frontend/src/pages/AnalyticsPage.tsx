import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis } from 'recharts';
import { TrendingUp, BookOpen, Flame, Target, Zap } from 'lucide-react';

const levelLabels = [
  'Service', 'Service+', 'Mid Product', 'Product', 'Good Product',
  'Strong Product', 'Top Tech', 'Big Tech', 'Elite', 'God Tier',
];

const levelColors = [
  'text-green-400', 'text-green-400', 'text-lime-400', 'text-yellow-400', 'text-yellow-400',
  'text-amber-400', 'text-orange-400', 'text-orange-400', 'text-red-400', 'text-purple-400',
];

const readinessColor = (score: number) => {
  if (score >= 80) return 'text-green-400';
  if (score >= 50) return 'text-yellow-400';
  if (score >= 30) return 'text-orange-400';
  return 'text-red-400';
};

const readinessBg = (score: number) => {
  if (score >= 80) return 'bg-green-500';
  if (score >= 50) return 'bg-yellow-500';
  if (score >= 30) return 'bg-orange-500';
  return 'bg-red-500';
};

export default function AnalyticsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['analytics'],
    queryFn: () => analyticsApi.get().then((res) => res.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-48 animate-pulse rounded bg-secondary" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-32 animate-pulse rounded-2xl bg-secondary" />
          ))}
        </div>
      </div>
    );
  }

  if (!data) return null;

  const difficultyData = [
    { name: 'Easy', count: data.problemsByDifficulty?.easy || 0, fill: '#22c55e' },
    { name: 'Medium', count: data.problemsByDifficulty?.medium || 0, fill: '#f59e0b' },
    { name: 'Hard', count: data.problemsByDifficulty?.hard || 0, fill: '#ef4444' },
  ];

  const masteryData = data.masteryByCategory?.map((c) => ({
    category: c.category.substring(0, 10),
    mastery: c.averageMastery,
    fullMark: 100,
  })) || [];

  const tl = data.targetLevel || 5;
  const rs = data.readinessScore || 0;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Analytics</h1>

      {/* Company Readiness – Hero */}
      <Card className="border-primary/20">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="h-4 w-4 text-primary" />
            Company Readiness
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {/* Readiness gauge */}
            <div className="rounded-xl bg-secondary/50 p-6 text-center">
              <p className={`text-5xl font-bold ${readinessColor(rs)}`}>{rs}</p>
              <p className="mt-1 text-sm text-muted-foreground">Readiness Score</p>
              <p className="text-xs text-muted-foreground mt-2">
                for <span className="font-medium text-primary">Level {tl} — {levelLabels[tl - 1]}</span>
              </p>
              <div className="mt-4 h-3 overflow-hidden rounded-full bg-secondary">
                <div
                  className={`h-3 rounded-full transition-all ${readinessBg(rs)}`}
                  style={{ width: `${rs}%` }}
                />
              </div>
            </div>

            {/* Level ladder */}
            <div className="space-y-1">
              {levelLabels.map((label, i) => {
                const lvl = i + 1;
                const isCurrent = lvl === tl;
                const isReached = lvl <= tl && lvl <= Math.ceil(tl * (rs / 100));
                return (
                  <div
                    key={lvl}
                    className={`flex items-center gap-3 rounded-lg px-3 py-1.5 text-sm transition-all ${
                      isCurrent ? 'bg-primary/10 border border-primary/20' : ''
                    } ${isReached ? 'opacity-100' : 'opacity-40'}`}
                  >
                    <span className={`w-6 text-xs font-bold ${levelColors[i]}`}>
                      L{lvl}
                    </span>
                    <span className={`flex-1 ${isCurrent ? 'font-medium text-primary' : ''}`}>
                      {label}
                    </span>
                    {isCurrent && <Zap className="h-3 w-3 text-primary" />}
                    {isReached && !isCurrent && (
                      <span className="text-xs text-green-400">✓</span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Problems Solved</CardTitle>
            <Code2 className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{data.totalProblems}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Topics Learned</CardTitle>
            <BookOpen className="h-4 w-4 text-green-400" />
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{data.totalTopics}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Avg Mastery</CardTitle>
            <TrendingUp className="h-4 w-4 text-yellow-400" />
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{Math.round(data.averageMastery)}%</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Day Streak</CardTitle>
            <Flame className="h-4 w-4 text-orange-400" />
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{data.currentStreak}</p>
          </CardContent>
        </Card>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Problems by Difficulty</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={difficultyData}>
                <XAxis dataKey="name" stroke="#a0a0a0" fontSize={12} />
                <YAxis stroke="#a0a0a0" fontSize={12} />
                <Tooltip contentStyle={{ backgroundColor: '#1c1c1f', border: '1px solid #2a2a2d', borderRadius: '8px' }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Mastery by Category</CardTitle>
          </CardHeader>
          <CardContent>
            {masteryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <RadarChart data={masteryData}>
                  <PolarGrid stroke="#2a2a2d" />
                  <PolarAngleAxis dataKey="category" stroke="#a0a0a0" fontSize={10} />
                  <PolarRadiusAxis angle={30} domain={[0, 100]} stroke="#a0a0a0" fontSize={10} />
                  <Radar name="Mastery" dataKey="mastery" stroke="#6d5dfc" fill="#6d5dfc" fillOpacity={0.3} />
                </RadarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[250px] items-center justify-center text-sm text-muted-foreground">
                No topic data yet. Add topics or sync LeetCode.
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Weakest & Strongest */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-red-400">Weakest Topics</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.weakestTopics?.length === 0 && (
              <p className="text-sm text-muted-foreground">No weak topics identified.</p>
            )}
            {data.weakestTopics?.map((t, i) => (
              <div key={i} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">{t.title}</p>
                  <p className="text-xs text-muted-foreground">{t.category}</p>
                </div>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-16 rounded-full bg-secondary">
                    <div className="h-2 rounded-full bg-red-400" style={{ width: `${t.confidence * 10}%` }} />
                  </div>
                  <span className="text-xs">{t.confidence}/10</span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-green-400">Strongest Topics</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.strongestTopics?.length === 0 && (
              <p className="text-sm text-muted-foreground">No strong topics yet.</p>
            )}
            {data.strongestTopics?.map((t, i) => (
              <div key={i} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">{t.title}</p>
                  <p className="text-xs text-muted-foreground">{t.category}</p>
                </div>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-16 rounded-full bg-secondary">
                    <div className="h-2 rounded-full bg-green-400" style={{ width: `${t.confidence * 10}%` }} />
                  </div>
                  <span className="text-xs">{t.confidence}/10</span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
