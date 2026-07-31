import api from './client';
import type { ApiResponse, LoginResponse, GenerateResponse, DashboardResponse, Revision, Recommendation, Journal, JournalRequest, AnalyticsResponse, WeeklyProgress, LeetCodeStats, MemoryResponse, PracticeProblem, RoadmapAnalysis, PagedResponse } from '@/types';

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
  getActive: () => api.get<ApiResponse<Recommendation[]>>('/recommendations'),
  generate: () => api.post<ApiResponse<GenerateResponse>>('/recommendations/generate'),
  dismiss: (id: string) => api.put<ApiResponse<Recommendation>>(`/recommendations/${id}/dismiss`),
};

export const journalsApi = {
  getRecent: () => api.get<ApiResponse<Journal[]>>('/journals/recent'),
  getAll: (page = 0, size = 200) => api.get<ApiResponse<PagedResponse<Journal>>>(`/journals?page=${page}&size=${size}`),
  save: (data: JournalRequest) => api.post<ApiResponse<Journal>>('/journals', data),
};

export const analyticsApi = {
  get: () => api.get<ApiResponse<AnalyticsResponse>>('/analytics'),
  getWeekly: () => api.get<ApiResponse<WeeklyProgress>>('/analytics/weekly'),
};

export const practiceApi = {
  getQueue: () => api.get<ApiResponse<PracticeProblem[]>>('/practice/queue'),
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
