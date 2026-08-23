export interface Topic {
  id: string;
  title: string;
  description: string;
  category: string;
  confidence: number;
  mastery: number;
  notes: string;
  lastRevision: string | null;
  nextRevision: string | null;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'MASTERED';
  revisionCount: number;
  estimatedRetention: number;
  createdAt: string;
  updatedAt: string;
}

export interface Revision {
  id: string;
  topicId: string;
  topicTitle: string;
  topicCategory: string;
  scheduledDate: string;
  completed: boolean;
  priority: number;
  reason: string | null;
  completionDate: string | null;
}

export interface Recommendation {
  id: string;
  title: string;
  description: string;
  reason: string;
  priority: number;
  action: string;
  status: 'ACTIVE' | 'COMPLETED' | 'DISMISSED';
  completedAt: string | null;
  outcome: string | null;
  problemSlug: string | null;
  problemTitle: string | null;
  problemDifficulty: string | null;
  createdAt: string;
  score?: number | null;
  scoreBreakdown?: { total: number; items: ScoreItem[] } | null;
}

export interface EngineReport {
  sampleCount: number;
  minSamples: number;
  storedMse: number;
  storedLogLoss: number;
  storedAuc: number;
  liveMse: number;
  liveLogLoss: number;
  liveAuc: number;
  weights: Record<string, number>;
  version: number | null;
  lastMetricBefore: number | null;
  lastMetricAfter: number | null;
  lastCalibratedAt: string | null;
}

export interface CalibrationResult {
  status: 'SKIPPED' | 'APPLIED';
  message: string;
  sampleCount: number;
  minSamples: number;
  before: number | null;
  after: number | null;
  applied: boolean;
}

export interface Journal {
  id: string;
  entryDate: string;
  morningGoal: string | null;
  eveningReflection: string | null;
  energy: number | null;
  mood: number | null;
  hoursStudied: number | null;
  achievements: string | null;
  challenges: string | null;
  lessons: string | null;
}

export interface JournalRequest {
  entryDate?: string;
  morningGoal?: string;
  eveningReflection?: string;
  energy?: number;
  mood?: number;
  hoursStudied?: number;
  achievements?: string;
  challenges?: string;
  lessons?: string;
}

export interface GenerateResponse {
  recommendations: Recommendation[];
  remainingGenerations: number;
  dailyLimit: number;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  user: {
    id: string;
    username: string;
    displayName: string;
    email: string | null;
    leetcodeUsername: string | null;
    targetLevel: number;
    preferredAnalysisTime: string | null;
    timezone: string | null;
    dailyGenerationsUsed: number;
    lastGenerationDate: string | null;
  };
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface DashboardResponse {
  greeting: string;
  currentFocus: string;
  todayMission: string;
  revisionsDue: Revision[];
  recommendations: Recommendation[];
  weakTopics: Topic[];
  strongTopics: Topic[];
  knowledgeHealth: {
    averageMastery: number;
    averageConfidence: number;
    averageRetention: number;
    totalTopics: number;
    masteredTopics: number;
    inProgressTopics: number;
    notStartedTopics: number;
    overdueRevisions: number;
  };
  recentJournal: string;
  recentProblems: string[];
  leetcodeStats: LeetCodeStats | null;
  targetProgress: {
    targetLevel: number;
    readinessScore: number;
    totalSolved: number;
    targetTotal: number;
    difficultyGap: {
      currentEasy: number;
      currentMedium: number;
      currentHard: number;
      targetEasy: number;
      targetMedium: number;
      targetHard: number;
    };
  };
  knowledgeMap: {
    category: string;
    topics: { id: string; title: string; mastery: number; confidence: number; status: string }[];
    averageMastery: number;
    averageConfidence: number;
  }[];
}

export interface LeetCodeStats {
  totalSolved: number;
  easySolved: number;
  mediumSolved: number;
  hardSolved: number;
  ranking: number | null;
  streak: number;
  totalActiveDays: number;
  contestRating: number | null;
  contestRanking: number | null;
  contestAttendedCount: number | null;
  lastSyncedAt: string | null;
  tags?: LeetCodeTagStat[];
}

export interface LeetCodeTagStat {
  tagName: string;
  tagSlug: string;
  problemsSolved: number;
  skillLevel: string;
}

export interface PendingSolve {
  id: string;
  title: string | null;
  titleSlug: string;
  difficulty: string;
  topicTagSlug: string;
  solvedAt: string | null;
}

export interface PracticeProblem {
  title: string;
  titleSlug: string;
  difficulty: string;
  topicTag: string | null;
  reason: string;
  segment?: 'WARMUP' | 'REINFORCE' | 'CHALLENGE' | 'REVISION';
  score?: number;
  breakdown?: ScoreItem[];
  attempts?: number;
  solved?: number;
}

export interface ScoreItem {
  name: string;
  weight: number;
  value: number;
  contribution: number;
}

export interface PracticeQueueResponse {
  profile: string;
  planMessage: string;
  queue: PracticeProblem[];
  revisitTopics: string[];
}

export interface ProblemAttempt {
  id: string;
  problemTitle: string;
  problemSlug: string;
  difficulty: string;
  topicTagSlug: string | null;
  topicTagName: string | null;
  outcome: 'SOLVED' | 'FAILED' | 'PARTIAL' | 'SKIPPED';
  hintsUsed: number;
  timeTakenSeconds: number | null;
  quality: number;
  attemptedAt: string;
}

export interface ProblemAttemptRequest {
  problemTitle: string;
  problemSlug: string;
  difficulty: string;
  topicTagSlug?: string | null;
  topicTagName?: string | null;
  outcome: 'SOLVED' | 'FAILED' | 'PARTIAL' | 'SKIPPED';
  hintsUsed?: number;
  timeTakenSeconds?: number;
}

export interface ProblemAttemptResponse {
  attempt: ProblemAttempt;
  topicsUpdated: string[];
  feedback: string;
}

export interface LearningCurvePoint {
  date: string;
  mastery: number;
  confidence: number;
  retention: number;
  skillRating: number;
  consistency: number;
  solved: number;
  revisions: number;
  milestones: string[];
}

export interface CurveMilestone {
  date: string;
  type: string;
  label: string;
}

export interface LearningCurveResponse {
  points: LearningCurvePoint[];
  milestones: CurveMilestone[];
}

export interface MemoryResponse {
  fadingConcepts: {
    topicId: string;
    title: string;
    category: string;
    confidence: number;
    mastery: number;
    daysSinceRevision: number;
    estimatedRetention: number | null;
    suggestedProblemTitle: string | null;
    suggestedProblemSlug: string | null;
    suggestedProblemDifficulty: string | null;
  }[];
}

export interface RoadmapAnalysis {
  paragraph: string;
  currentLevel: number;
  focusArea: string;
  estimatedTimeToNextLevel: string;
  strongTags: { name: string; slug: string; solved: number; confidence: number }[];
  weakTags: { name: string; slug: string; solved: number; confidence: number }[];
  nextMilestone: string;
  readinessScore: number;
  recommendedDifficultySplit: string;
}

export interface AnalyticsResponse {
  totalProblems: number;
  totalTopics: number;
  averageMastery: number;
  problemsByDifficulty: { easy: number; medium: number; hard: number };
  masteryByCategory: { category: string; averageMastery: number }[];
  weakestTopics: { title: string; confidence: number; mastery: number; category: string }[];
  strongestTopics: { title: string; confidence: number; mastery: number; category: string }[];
  currentStreak: number;
  targetLevel: number;
  readinessScore: number;
  insights?: Insight[];
}

export interface Insight {
  type: string;
  title: string;
  message: string;
  metric: number | null;
  delta: number | null;
  display?: string;
}

export interface WeeklyProgress {
  problemsSolved: number;
  topicsReviewed: number;
  hoursStudied: number;
  revisionsCompleted: number;
  journalEntries: number;
}

export interface ActivityDay {
  date: string;
  active: boolean;
  hours: number;
  attempts: number;
  revisions: number;
}
