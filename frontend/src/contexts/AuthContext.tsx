import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi } from '@/api';

interface User {
  id: string;
  username: string;
  displayName: string;
  email: string | null;
  leetcodeUsername: string | null;
  targetLevel: number;
  preferredAnalysisTime?: string;
  dailyGenerationsUsed?: number;
}

interface AuthContextType {
  isAuthenticated: boolean;
  token: string | null;
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  register: (data: { email: string; password: string }) => Promise<void>;
  logout: () => void;
  loading: boolean;
  setUser: (user: User | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function init() {
      const storedToken = sessionStorage.getItem('forge_token');

      if (storedToken) {
        setToken(storedToken);
        try {
          const res = await authApi.getProfile();
          if (cancelled) return;
          setUser(res.data.data);
        } catch {
          if (cancelled) return;
          sessionStorage.removeItem('forge_token');
          setToken(null);
          await tryRefreshToken();
        }
      } else {
        await tryRefreshToken();
      }

      if (!cancelled) setLoading(false);
    }

    async function tryRefreshToken() {
      try {
        const res = await authApi.refresh();
        if (cancelled) return;
        const data = res.data.data;
        sessionStorage.setItem('forge_token', data.token);
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
    const response = await authApi.login(username, password);
    const data = response.data.data;
    sessionStorage.setItem('forge_token', data.token);
    setToken(data.token);
    setUser(data.user);
  };

  const register = async (data: { email: string; password: string }) => {
    await authApi.register(data);
  };

  const logout = () => {
    authApi.logout().catch(() => {});
    sessionStorage.removeItem('forge_token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated: !!token, token, user, login, register, logout, loading, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
