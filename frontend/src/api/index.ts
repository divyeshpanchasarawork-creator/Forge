import api from './client';
import type { ApiResponse, LoginResponse, GenerateResponse, DashboardResponse, Revision, Journal, JournalRequest, AnalyticsResponse, WeeklyProgress, LeetCodeStats, MemoryResponse, PracticeQueueResponse, RoadmapAnalysis, PagedResponse, ProblemAttempt, ProblemAttemptRequest, ProblemAttemptResponse, LearningCurveResponse } from '@/types';

export const authApi = {
  login: (username: string, password: string) =>
    api.post<ApiResponse<LoginResponse>>('/auth/login', { username, password }),
  register: (data: { email: string; password: string }) =>
    api.post<ApiResponse<void>>('/auth/register', data),
  refresh: () =>
    api.post<ApiResponse<LoginResponse>>('/auth/refresh'),
  logout: () =>
    api.post<ApiResponse<void>>('/auth/logout'),
  getProfile: () =>
    api.get<ApiResponse<LoginResponse['user']>>('/auth/profile'),
  updateProfile: (data: { displayName?: string; email?: string; leetcodeUsername?: string; targetLevel?: number; preferredAnalysisTime?: string }) =>
    api.put<ApiResponse<LoginResponse['user']>>('/auth/profile', data),
};

export const dashboardApi = {
  get: () => api.get<ApiResponse<DashboardResponse>>('/dashboard'),
};

export const revisionsApi = {
  getToday: () => api.get<ApiResponse<Revision[]>>('/revisions/today'),
  getPending: () => api.get<ApiResponse<Revision[]>>('/revisions/pending'),
  complete: (id: string, quality = 4) => api.post<ApiResponse<Revision>>(`/revisions/${id}/complete?quality=${quality}`),
};

export const recommendationsApi = {
  generate: () => api.post<ApiResponse<GenerateResponse>>('/recommendations/generate'),
};

export const journalsApi = {
  getRecent: () => api.get<ApiResponse<Journal[]>>('/journals/recent'),
  getAll: (page = 0, size = 200) => api.get<ApiResponse<PagedResponse<Journal>>>(`/journals?page=${page}&size=${size}`),
  save: (data: JournalRequest) => api.post<ApiResponse<Journal>>('/journals', data),
};

export const analyticsApi = {
  get: () => api.get<ApiResponse<AnalyticsResponse>>('/analytics'),
  getWeekly: () => api.get<ApiResponse<WeeklyProgress>>('/analytics/weekly'),
  getLearningCurve: (days = 30) => api.get<ApiResponse<LearningCurveResponse>>(`/analytics/learning-curve?days=${days}`),
};

export const practiceApi = {
  getQueue: () => api.get<ApiResponse<PracticeQueueResponse>>('/practice/queue'),
  submitAttempt: (data: ProblemAttemptRequest) => api.post<ApiResponse<ProblemAttemptResponse>>('/practice/attempts', data),
  getAttempts: (limit = 20) => api.get<ApiResponse<ProblemAttempt[]>>(`/practice/attempts?limit=${limit}`),
};

export const memoryApi = {
  get: () => api.get<ApiResponse<MemoryResponse>>('/memory'),
};

export const roadmapApi = {
  getAnalysis: () => api.get<ApiResponse<RoadmapAnalysis>>('/roadmap/analysis'),
};

export const leetcodeApi = {
  sync: () => api.post<ApiResponse<LeetCodeStats>>('/leetcode/sync'),
  getStats: () => api.get<ApiResponse<LeetCodeStats>>('/leetcode/stats'),
};

export interface SearchProblem {
  title: string;
  titleSlug: string;
  difficulty: string;
  tags: string[];
}

export const searchApi = {
  problems: (q: string) =>
    api.get<ApiResponse<SearchProblem[]>>(`/search/problems?q=${encodeURIComponent(q)}`),
};
