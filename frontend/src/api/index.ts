import api from './client';
import type { ApiResponse, LoginResponse, GenerateResponse, DashboardResponse, Revision, Recommendation, Journal, JournalRequest, AnalyticsResponse, WeeklyProgress, LeetCodeStats, MemoryResponse, PracticeQueueResponse, RoadmapAnalysis, PagedResponse, ProblemAttempt, ProblemAttemptRequest, ProblemAttemptResponse, LearningCurveResponse, ActivityDay, EngineReport, CalibrationResult } from '@/types';

export const authApi = {
  login: (username: string, password: string) =>
    api.post<ApiResponse<LoginResponse>>('/auth/login', { username, password }),
  register: (data: { email: string; password: string }) =>
    api.post<ApiResponse<void>>('/auth/register', data),
  refresh: (refreshToken: string) =>
    api.post<ApiResponse<LoginResponse>>('/auth/refresh', { refreshToken }),
  logout: (refreshToken: string) =>
    api.post<ApiResponse<void>>('/auth/logout', { refreshToken }),
  getProfile: () =>
    api.get<ApiResponse<LoginResponse['user']>>('/auth/profile'),
  updateProfile: (data: { displayName?: string; email?: string; leetcodeUsername?: string; targetLevel?: number; preferredAnalysisTime?: string; timezone?: string }) =>
    api.put<ApiResponse<LoginResponse['user']>>('/auth/profile', data),
};

export const dashboardApi = {
  get: () => api.get<ApiResponse<DashboardResponse>>('/dashboard'),
};

export const revisionsApi = {
  getTodayActivity: () => api.get<ApiResponse<Revision[]>>('/revisions/today-activity'),
  getPending: () => api.get<ApiResponse<Revision[]>>('/revisions/pending'),
  complete: (id: string, quality = 4) => api.post<ApiResponse<Revision>>(`/revisions/${id}/complete?quality=${quality}`),
};

export const recommendationsApi = {
  generate: () => api.post<ApiResponse<GenerateResponse>>('/recommendations/generate'),
  complete: (id: string, outcome = 'SOLVED') => api.put<ApiResponse<Recommendation>>(`/recommendations/${id}/complete`, { outcome }),
  dismiss: (id: string) => api.put<ApiResponse<Recommendation>>(`/recommendations/${id}/dismiss`),
};

export const journalsApi = {
  getAll: (page = 0, size = 200) => api.get<ApiResponse<PagedResponse<Journal>>>(`/journals?page=${page}&size=${size}`),
  save: (data: JournalRequest) => api.post<ApiResponse<Journal>>('/journals', data),
};

export const analyticsApi = {
  get: () => api.get<ApiResponse<AnalyticsResponse>>('/analytics'),
  getWeekly: () => api.get<ApiResponse<WeeklyProgress>>('/analytics/weekly'),
  getLearningCurve: (days = 30) => api.get<ApiResponse<LearningCurveResponse>>(`/analytics/learning-curve?days=${days}`),
  getHeatmap: (weeks = 28) => api.get<ApiResponse<ActivityDay[]>>(`/analytics/heatmap?weeks=${weeks}`),
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

export const calibrationApi = {
  getReport: () => api.get<ApiResponse<EngineReport>>('/internal/engine-report'),
  runCalibration: () => api.post<ApiResponse<CalibrationResult>>('/internal/calibration/run'),
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
