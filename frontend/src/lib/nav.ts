import type { ComponentType } from 'react';
import { BarChart3, Brain, Code2, LayoutDashboard, Lightbulb, PenLine, RefreshCw, User } from 'lucide-react';

export type NavIcon = ComponentType<{ className?: string }>;

export interface NavItem {
  to: string;
  label: string;
  shortcut: string;
  icon: NavIcon;
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/app', label: 'Dashboard', shortcut: '1', icon: LayoutDashboard },
  { to: '/app/roadmap', label: 'Roadmap', shortcut: '2', icon: Lightbulb },
  { to: '/app/problems', label: 'Practice', shortcut: '3', icon: Code2 },
  { to: '/app/revision', label: 'Revision', shortcut: '4', icon: RefreshCw },
  { to: '/app/journal', label: 'Journal', shortcut: '5', icon: PenLine },
  { to: '/app/memory', label: 'Memory', shortcut: '6', icon: Brain },
  { to: '/app/analytics', label: 'Analytics', shortcut: '7', icon: BarChart3 },
  { to: '/app/profile', label: 'Profile', shortcut: '8', icon: User },
];

export const NAV_SECTIONS: { label: string; items: NavItem[] }[] = [
  { label: 'Navigate', items: NAV_ITEMS.slice(0, 4) },
  { label: 'Insights', items: NAV_ITEMS.slice(4, 7) },
  { label: 'Account', items: NAV_ITEMS.slice(7) },
];

export const NAV_SHORTCUTS: Record<string, string> = Object.fromEntries(
  NAV_ITEMS.map((item) => [item.shortcut, item.to])
);
