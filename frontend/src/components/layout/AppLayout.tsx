import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function AppLayout() {
  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <main className="min-h-screen p-4 pt-20 transition-all lg:ml-64 lg:p-8 lg:pt-8">
        <Outlet />
      </main>
    </div>
  );
}
