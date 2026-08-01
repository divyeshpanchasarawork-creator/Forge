import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('forge_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (value: unknown) => void; reject: (reason: unknown) => void }> = [];
let skipAuthRedirect = false;

export const setSkipAuthRedirect = (value: boolean) => {
  skipAuthRedirect = value;
};

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  failedQueue = [];
};

const redirectToLogin = () => {
  if (skipAuthRedirect) return;
  window.location.href = '/';
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Cold-start retry: 502/503/504 with exponential backoff
    if (!originalRequest._retryCount) {
      originalRequest._retryCount = 0;
    }
    const status = error.response?.status;
    if (status >= 502 && status <= 504 && originalRequest._retryCount < 3) {
      originalRequest._retryCount++;
      const delay = [5000, 10000, 15000][originalRequest._retryCount - 1];
      await new Promise((resolve) => setTimeout(resolve, delay));
      return api(originalRequest);
    }

    // 401 handling with refresh
    if (status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post(
          `${import.meta.env.VITE_API_URL || '/api'}/auth/refresh`,
          {},
          { withCredentials: true }
        );
        const newToken = data.data.token;
        sessionStorage.setItem('forge_token', newToken);
        processQueue(null, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch {
        processQueue(error, null);
        sessionStorage.removeItem('forge_token');
        redirectToLogin();
        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    if (status === 401) {
      sessionStorage.removeItem('forge_token');
      redirectToLogin();
    }

    return Promise.reject(error);
  }
);

export default api;
