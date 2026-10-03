import { useEffect, useState } from 'react';
import { Link, Navigate, NavLink, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { CasesPage } from '../features/cases/CasesPage';
import { FraudCasePage } from '../features/cases/FraudCasePage';
import { LoginPage } from '../features/auth/LoginPage';
import { AssessmentsPage } from '../features/assessments/AssessmentsPage';
import { AssessmentPage } from '../features/assessments/AssessmentPage';
import { initializeOidc, keycloak, roleNames } from '../features/auth/oidc';
import { Message } from '../shared/components/Primitives';
import styles from './App.module.css';

export function App() {
  const [ready, setReady] = useState(false);
  const [failure, setFailure] = useState<string>();

  useEffect(() => {
    initializeOidc()
      .then(() => setReady(true))
      .catch((error: unknown) => {
        setFailure(
          error instanceof Error ? error.message : 'Identity provider could not be reached.',
        );
        setReady(true);
      });
  }, []);

  if (!ready) return <main className={styles.boot}>Connecting to identity provider…</main>;
  if (failure)
    return (
      <main className={styles.boot}>
        <Message kind="error">{failure}</Message>
      </main>
    );

  const roles = roleNames();
  const permitted = roles.includes('FRAUD_ANALYST') || roles.includes('ADMIN');
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={<AccessGate authenticated={Boolean(keycloak.authenticated)} allowed={permitted} />}
      >
        <Route element={<AppShell />}>
          <Route index element={<Navigate to="/assessments" replace />} />
          <Route path="/assessments" element={<AssessmentsPage />} />
          <Route path="/assessments/:id" element={<AssessmentPage />} />
          <Route path="/cases" element={<CasesPage />} />
          <Route path="/cases/:id" element={<FraudCasePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function AccessGate({ authenticated, allowed }: { authenticated: boolean; allowed: boolean }) {
  const location = useLocation();
  if (!authenticated) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (!allowed)
    return (
      <main className={styles.boot}>
        <Message kind="error">
          <strong>Access denied</strong>
          <span>This console is restricted to FRAUD_ANALYST and ADMIN.</span>
        </Message>
      </main>
    );
  return <Outlet />;
}

function AppShell() {
  const location = useLocation();
  const profile = keycloak.tokenParsed as { preferred_username?: string } | undefined;
  const active = (path: string) =>
    `${styles.navLink} ${location.pathname.startsWith(path) ? styles.active : ''}`;
  return (
    <div className={styles.frame}>
      <aside className={styles.sidebar}>
        <Link to="/assessments" className={styles.brand}>
          <span className={styles.brandIcon}>F</span>
          <span>
            <strong>Fraud Detection</strong>
            <small>ANALYST CONSOLE</small>
          </span>
        </Link>
        <div className={styles.navCaption}>INVESTIGATIONS</div>
        <nav className={styles.nav} aria-label="Main navigation">
          <NavLink to="/assessments" className={active('/assessments')}>
            Assessment history
          </NavLink>
          <NavLink to="/cases" className={active('/cases')}>
            Case queue
          </NavLink>
        </nav>
        <div className={styles.sidebarFoot}>
          <span className={styles.online} /> Local risk operations
        </div>
      </aside>
      <div className={styles.main}>
        <header className={styles.topbar}>
          <div className={styles.mobileBrand}>Fraud Detection</div>
          <div className={styles.user}>
            <span className={styles.userIcon}>
              {profile?.preferred_username?.slice(0, 1).toUpperCase() ?? 'A'}
            </span>
            <span>{profile?.preferred_username ?? 'Analyst'}</span>
            <button
              onClick={() => keycloak.logout({ redirectUri: `${window.location.origin}/login` })}
            >
              Sign out
            </button>
          </div>
        </header>
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
