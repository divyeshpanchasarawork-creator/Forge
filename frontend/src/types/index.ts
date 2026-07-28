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

export interface TopicRequest {
  title: string;
  description: string;
  category: string;
  confidence?: number;
  mastery?: number;
  notes?: string;
}

export interface Problem {
  id: string;
  title: string;
  leetcodeId: string | null;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  timeTaken: number | null;
  attempts: number;
  confidence: number;
  mistakes: string | null;
  summary: string | null;
  notes: string | null;
  solutionUrl: string | null;
  solvedAt: string | null;
  topics: { id: string; title: string; category: string }[];
  createdAt: string;
}

export interface ProblemRequest {
  title: string;
  leetcodeId?: string;
  difficulty: string;
  timeTaken?: number;
  attempts?: number;
  confidence?: number;
  mistakes?: string;
  summary?: string;
  notes?: string;
  solutionUrl?: string;
  topicIds?: string[];
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
  dismissed: boolean;
  createdAt: string;
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

export interface LoginResponse {
  token: string;
  tokenType: string;
  user: {
    id: string;
    username: string;
    displayName: string;
    email: string | null;
    leetcodeUsername: string | null;
  };
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
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
    totalTopics: number;
    masteredTopics: number;
  };
  weeklyProgress: {
    problemsSolved: number;
    topicsReviewed: number;
    hoursStudied: number;
    revisionsCompleted: number;
  };
  recentJournal: string;
  recentProblems: string[];
  leetcodeStats: LeetCodeStats | null;
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

export interface AnalyticsResponse {
  totalProblems: number;
  totalTopics: number;
  totalStudyHours: number;
  averageMastery: number;
  averageConfidence: number;
  problemsByDifficulty: { easy: number; medium: number; hard: number };
  masteryByCategory: { category: string; averageMastery: number }[];
  revisionCompletionRate: number;
  learningTrend: { date: string; problemsSolved: number; hoursStudied: number }[];
  weakestTopics: { title: string; confidence: number; mastery: number; category: string }[];
  strongestTopics: { title: string; confidence: number; mastery: number; category: string }[];
  currentStreak: number;
  leetcodeOverview: {
    totalSolved: number;
    easySolved: number;
    mediumSolved: number;
    hardSolved: number;
    ranking: number | null;
    streak: number;
    totalActiveDays: number;
    easyBeatsPct: number | null;
    mediumBeatsPct: number | null;
    hardBeatsPct: number | null;
  } | null;
}
