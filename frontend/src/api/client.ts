import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('forge_token');
  if (token && !isLoggedOut) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (value: unknown) => void; reject: (reason: unknown) => void }> = [];
let skipAuthRedirect = false;
let isLoggedOut = false;

export const setSkipAuthRedirect = (value: boolean) => {
  skipAuthRedirect = value;
};

export const setLoggedOut = (value: boolean) => {
  isLoggedOut = value;
};

const isAuthEndpoint = (url?: string) =>
  !!url && (url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh'));

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

    // Cold-start retry: 502/503/504, at most one retry after a short backoff
    if (!originalRequest._retryCount) {
      originalRequest._retryCount = 0;
    }
    const status = error.response?.status;
    if (status >= 502 && status <= 504 && originalRequest._retryCount < 1) {
      originalRequest._retryCount++;
      await new Promise((resolve) => setTimeout(resolve, 5000));
      return api(originalRequest);
    }

    // 401 handling with refresh
    if (status === 401 && !originalRequest._retry) {
      if (isAuthEndpoint(originalRequest.url)) {
        return Promise.reject(error);
      }

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
        const refreshToken = sessionStorage.getItem('forge_refresh');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }
        const { data } = await axios.post(
          `${import.meta.env.VITE_API_URL || '/api'}/auth/refresh`,
          { refreshToken }
        );
        const newToken = data.data.token;
        const newRefreshToken = data.data.refreshToken;
        if (isLoggedOut) {
          processQueue(error, null);
          return Promise.reject(error);
        }
        sessionStorage.setItem('forge_token', newToken);
        sessionStorage.setItem('forge_refresh', newRefreshToken);
        processQueue(null, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch {
        processQueue(error, null);
        sessionStorage.removeItem('forge_token');
        sessionStorage.removeItem('forge_refresh');
        redirectToLogin();
        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    if (status === 401) {
      if (!isAuthEndpoint(originalRequest.url)) {
        sessionStorage.removeItem('forge_token');
        sessionStorage.removeItem('forge_refresh');
        redirectToLogin();
      }
    }

    return Promise.reject(error);
  }
);

export default api;
