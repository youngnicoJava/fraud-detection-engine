import { useNavigate } from 'react-router-dom';
import { keycloak } from './oidc';
import styles from './LoginPage.module.css';

export function LoginPage() {
  const navigate = useNavigate();
  const signIn = async () => {
    if (keycloak.authenticated) {
      navigate('/assessments', { replace: true });
      return;
    }
    await keycloak.login({ redirectUri: `${window.location.origin}/assessments` });
  };

  return (
    <main className={styles.page}>
      <section className={styles.card}>
        <div className={styles.mark}>F</div>
        <div className={styles.kicker}>FRAUD DETECTION ENGINE</div>
        <h1>Analyst console</h1>
        <p>
          Review automated fraud assessments and investigate cases with a traceable decision
          history.
        </p>
        <button className={styles.signIn} onClick={signIn}>
          Sign in with Keycloak
        </button>
        <div className={styles.access}>
          <strong>Authorized roles</strong>
          <span>FRAUD_ANALYST · ADMIN</span>
        </div>
        <div className={styles.dev}>
          Local development accounts
          <br />
          <code>fraud-analyst / fraud-analyst</code>
          <br />
          <code>admin / admin</code>
        </div>
      </section>
    </main>
  );
}
