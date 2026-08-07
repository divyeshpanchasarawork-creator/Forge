import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { authApi } from '@/api';
import { setLoggedOut, setSkipAuthRedirect } from '@/api/client';

interface User {
  id: string;
  username: string;
  displayName: string;
  email: string | null;
  leetcodeUsername: string | null;
  targetLevel: number;
  preferredAnalysisTime?: string | null;
  timezone?: string | null;
  dailyGenerationsUsed?: number | null;
  lastGenerationDate?: string | null;
}

interface AuthContextType {
  isAuthenticated: boolean;
  token: string | null;
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  register: (data: { email: string; password: string }) => Promise<void>;
  logout: () => void;
  loading: boolean;
  loggingOut: boolean;
  setUser: (user: User | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const TOKEN_KEY = 'forge_token';
const REFRESH_TOKEN_KEY = 'forge_refresh';

function persistTokens(data: { token: string; refreshToken: string }) {
  sessionStorage.setItem(TOKEN_KEY, data.token);
  sessionStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
}

function clearTokens() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const queryClient = useQueryClient();

  useEffect(() => {
    let cancelled = false;

    async function init() {
      setLoggedOut(false);
      setSkipAuthRedirect(true);
      try {
        const storedToken = sessionStorage.getItem(TOKEN_KEY);

        if (storedToken) {
          setToken(storedToken);
          try {
            const res = await authApi.getProfile();
            if (cancelled) return;
            setUser(res.data.data);
          } catch (error) {
            if (cancelled) return;
            const status = (error as AxiosError).response?.status;
            if (status === 401) {
              clearTokens();
              setToken(null);
              await tryRefreshToken();
            } else {
              setUser(null);
            }
          }
        } else {
          await tryRefreshToken();
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
          setSkipAuthRedirect(false);
        }
      }
    }

    async function tryRefreshToken() {
      try {
        const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
        if (!refreshToken) {
          if (cancelled) return;
          setToken(null);
          setUser(null);
          return;
        }
        const res = await authApi.refresh(refreshToken);
        if (cancelled) return;
        const data = res.data.data;
        persistTokens(data);
        setToken(data.token);
        setUser(data.user);
      } catch {
        if (cancelled) return;
        setToken(null);
        setUser(null);
      }
    }

    init();

    return () => { cancelled = true; };
  }, []);

  const login = async (username: string, password: string) => {
    setLoggedOut(false);
    setLoggingOut(false);
    setSkipAuthRedirect(false);
    const response = await authApi.login(username, password);
    const data = response.data.data;
    persistTokens(data);
    setToken(data.token);
    setUser(data.user);
  };

  const register = async (data: { email: string; password: string }) => {
    setLoggedOut(false);
    setLoggingOut(false);
    setSkipAuthRedirect(false);
    await authApi.register(data);
    const response = await authApi.login(data.email, data.password);
    const loginData = response.data.data;
    persistTokens(loginData);
    setToken(loginData.token);
    setUser(loginData.user);
  };

  const logout = () => {
    setLoggingOut(true);
    setLoggedOut(true);
    setSkipAuthRedirect(true);
    setToken(null);
    setUser(null);
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    clearTokens();
    queryClient.clear();
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => {});
    }
    setTimeout(() => setLoggingOut(false), 500);
  };

  return (
    <AuthContext.Provider
      value={{ isAuthenticated: !!token, token, user, login, register, logout, loading, loggingOut, setUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
