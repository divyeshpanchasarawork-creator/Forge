import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi } from '@/api';

interface User {
  id: string;
  username: string;
  displayName: string;
  email: string | null;
  leetcodeUsername: string | null;
}

interface AuthContextType {
  isAuthenticated: boolean;
  token: string | null;
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  register: (data: { email: string; password: string }) => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem('forge_token'));
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      authApi.getProfile().then((res) => {
        setUser(res.data.data);
      }).catch(() => {
        localStorage.removeItem('forge_token');
        setToken(null);
      }).finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (username: string, password: string) => {
    const response = await authApi.login(username, password);
    const data = response.data.data;
    localStorage.setItem('forge_token', data.token);
    setToken(data.token);
    setUser(data.user);
  };

  const register = async (data: { email: string; password: string }) => {
    await authApi.register(data);
  };

  const logout = () => {
    localStorage.removeItem('forge_token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated: !!token, token, user, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
