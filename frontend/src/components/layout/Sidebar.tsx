import { NavLink } from 'react-router-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Logo } from '@/components/brand/Logo';
import { NAV_SECTIONS } from '@/lib/nav';

export default function Sidebar({
  collapsed,
  onToggle,
  mobileOpen,
  onMobileClose,
}: {
  collapsed: boolean;
  onToggle: () => void;
  mobileOpen: boolean;
  onMobileClose: () => void;
}) {
  const panel = (
    <div className="flex h-full flex-col">
      <div className={cn('flex shrink-0 items-center pt-5', collapsed ? 'justify-center px-2 pb-4' : 'px-6 pb-4')}>
        <Logo size="sm" variant="flame" withText={!collapsed} />
      </div>
      <nav className={cn('flex-1 overflow-y-auto px-2.5', collapsed ? 'mt-3' : '')}>
        {NAV_SECTIONS.map((section) => (
          <div key={section.label}>
            {!collapsed && (
              <p className="px-3 pb-1.5 pt-4 text-micro font-semibold uppercase tracking-[0.14em] text-muted-foreground/40">
                {section.label}
              </p>
            )}
            <div className="space-y-1">
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/app'}
                  onClick={onMobileClose}
                  className={({ isActive }) =>
                    cn(
                      'group relative flex h-12 items-center rounded-xl text-sm transition-colors lg:h-11',
                      collapsed ? 'justify-center' : 'gap-3 px-3',
                      isActive
                        ? 'bg-primary/8 text-primary'
                        : 'text-muted-foreground hover:bg-secondary/60 hover:text-foreground'
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      {isActive && !collapsed && (
                        <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-primary" />
                      )}
                      <item.icon
                        className={cn(
                          'h-4 w-4 shrink-0',
                          isActive ? 'text-primary' : 'text-muted-foreground group-hover:text-foreground'
                        )}
                      />
                      {!collapsed && (
                        <>
                          <span className={cn('flex-1 truncate', isActive && 'font-semibold')}>{item.label}</span>
                          <kbd className="rounded border border-border px-1.5 py-0.5 text-micro text-muted-foreground opacity-0 transition-opacity group-hover:opacity-60">
                            {item.shortcut}
                          </kbd>
                        </>
                      )}
                      {collapsed && (
                        <span className="pointer-events-none absolute left-full z-50 ml-2 whitespace-nowrap rounded-lg border border-border bg-card px-2.5 py-1.5 text-xs font-medium text-foreground opacity-0 transition-opacity duration-150 group-hover:opacity-100">
                          {item.label}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>

      <div className="px-2.5 py-3">
        <button
          type="button"
          onClick={onToggle}
          className={cn(
            'flex h-9 items-center rounded-lg text-xs font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground',
            collapsed ? 'w-full justify-center' : 'w-full gap-2 px-3'
          )}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          {!collapsed && <span className="flex-1 text-left">Collapse</span>}
        </button>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop floating sidebar */}
      <aside
        className={cn(
          'fixed left-3 top-3 bottom-3 z-40 hidden flex-col rounded-2xl border border-border bg-sidebar/90 transition-all duration-200 lg:flex',
          collapsed ? 'w-16' : 'w-60'
        )}
      >
        {panel}
      </aside>

      {/* Mobile floating drawer */}
      <div
        onClick={onMobileClose}
        className={cn(
          'fixed inset-0 z-40 bg-black/50 transition-opacity duration-200 lg:hidden',
          mobileOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
        )}
      />
      <aside
        inert={!mobileOpen}
        className={cn(
          'fixed left-3 top-3 bottom-3 z-50 flex w-64 flex-col rounded-2xl border border-border bg-sidebar transition-transform duration-200 lg:hidden',
          mobileOpen ? 'translate-x-0' : 'pointer-events-none -translate-x-[110%]'
        )}
      >
        {panel}
      </aside>
    </>
  );
}
