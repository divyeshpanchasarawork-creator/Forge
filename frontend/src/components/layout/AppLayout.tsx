import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopHeader from './TopHeader';

export default function AppLayout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    const saved = localStorage.getItem('forge-sidebar-collapsed');
    return saved === 'true';
  });

  const toggleSidebar = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('forge-sidebar-collapsed', String(next));
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <TopHeader sidebarCollapsed={sidebarCollapsed} />
      <Sidebar collapsed={sidebarCollapsed} onToggle={toggleSidebar} />
      <main
        className={`min-h-screen pt-14 transition-all ${
          sidebarCollapsed ? 'lg:ml-16' : 'lg:ml-64'
        }`}
      >
        <div className="p-4 lg:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
