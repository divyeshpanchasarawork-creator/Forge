import api from './client';
import type { ApiResponse, LoginResponse, DashboardResponse, Topic, TopicRequest, PagedResponse, Problem, ProblemRequest, Revision, Recommendation, Journal, JournalRequest, AnalyticsResponse, LeetCodeStats } from '@/types';

export const authApi = {
  login: (username: string, password: string) =>
    api.post<ApiResponse<LoginResponse>>('/auth/login', { username, password }),
  register: (data: { email: string; password: string }) =>
    api.post<ApiResponse<void>>('/auth/register', data),
  getProfile: () =>
    api.get<ApiResponse<LoginResponse['user']>>('/auth/profile'),
  updateProfile: (data: { displayName?: string; email?: string; leetcodeUsername?: string }) =>
    api.put<ApiResponse<LoginResponse['user']>>('/auth/profile', data),
};

export const dashboardApi = {
  get: () => api.get<ApiResponse<DashboardResponse>>('/dashboard'),
};

export const topicsApi = {
  getAll: (page = 0, size = 20, category?: string, status?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (category) params.append('category', category);
    if (status) params.append('status', status);
    return api.get<ApiResponse<PagedResponse<Topic>>>(`/topics?${params}`);
  },
  getById: (id: string) => api.get<ApiResponse<Topic>>(`/topics/${id}`),
  create: (data: TopicRequest) => api.post<ApiResponse<Topic>>('/topics', data),
  update: (id: string, data: TopicRequest) => api.put<ApiResponse<Topic>>(`/topics/${id}`, data),
  delete: (id: string) => api.delete(`/topics/${id}`),
  getWeak: () => api.get<ApiResponse<Topic[]>>('/topics/weak'),
  getStrong: () => api.get<ApiResponse<Topic[]>>('/topics/strong'),
};

export const problemsApi = {
  getAll: (page = 0, size = 20, difficulty?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (difficulty) params.append('difficulty', difficulty);
    return api.get<ApiResponse<PagedResponse<Problem>>>(`/problems?${params}`);
  },
  create: (data: ProblemRequest) => api.post<ApiResponse<Problem>>('/problems', data),
  update: (id: string, data: ProblemRequest) => api.put<ApiResponse<Problem>>(`/problems/${id}`, data),
  delete: (id: string) => api.delete(`/problems/${id}`),
};

export const revisionsApi = {
  getToday: () => api.get<ApiResponse<Revision[]>>('/revisions/today'),
  getPending: () => api.get<ApiResponse<Revision[]>>('/revisions/pending'),
  complete: (id: string) => api.post<ApiResponse<Revision>>(`/revisions/${id}/complete`),
};

export const recommendationsApi = {
  getActive: () => api.get<ApiResponse<Recommendation[]>>('/recommendations'),
  dismiss: (id: string) => api.put<ApiResponse<Recommendation>>(`/recommendations/${id}/dismiss`),
};

export const journalsApi = {
  getAll: (page = 0, size = 20) =>
    api.get<ApiResponse<PagedResponse<Journal>>>(`/journals?page=${page}&size=${size}`),
  getToday: () => api.get<ApiResponse<Journal>>('/journals/today'),
  getRecent: () => api.get<ApiResponse<Journal[]>>('/journals/recent'),
  save: (data: JournalRequest) => api.post<ApiResponse<Journal>>('/journals', data),
};

export const analyticsApi = {
  get: () => api.get<ApiResponse<AnalyticsResponse>>('/analytics'),
  getWeekly: () => api.get<ApiResponse<any>>('/analytics/weekly'),
};

export const leetcodeApi = {
  sync: () => api.post<ApiResponse<LeetCodeStats>>('/leetcode/sync'),
  getStats: () => api.get<ApiResponse<LeetCodeStats>>('/leetcode/stats'),
};
