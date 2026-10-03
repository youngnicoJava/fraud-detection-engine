import Keycloak from 'keycloak-js';

const authority =
  import.meta.env.VITE_OIDC_AUTHORITY || 'http://localhost:8182/realms/fraud-detection';
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID || 'fraud-detection-console';
const realmUrl = new URL(authority);

export const keycloak = new Keycloak({
  url: `${realmUrl.origin}${realmUrl.pathname.split('/realms/')[0]}`,
  realm: realmUrl.pathname.split('/realms/')[1]?.split('/')[0] || 'fraud-detection',
  clientId,
});

let currentToken: string | undefined;
let initialization: Promise<boolean> | undefined;

export function initializeOidc(): Promise<boolean> {
  initialization ??= keycloak
    .init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    })
    .then((authenticated) => {
      currentToken = keycloak.token;
      keycloak.onAuthSuccess = () => {
        currentToken = keycloak.token;
      };
      keycloak.onAuthRefreshSuccess = () => {
        currentToken = keycloak.token;
      };
      keycloak.onAuthLogout = () => {
        currentToken = undefined;
      };
      return authenticated;
    });
  return initialization;
}

export async function getAccessToken(): Promise<string | undefined> {
  if (!keycloak.authenticated) return undefined;
  try {
    await keycloak.updateToken(30);
    currentToken = keycloak.token;
    return currentToken;
  } catch {
    currentToken = undefined;
    await keycloak.login({ redirectUri: window.location.href });
    return undefined;
  }
}

export function roleNames(): string[] {
  return keycloak.tokenParsed?.realm_access?.roles ?? [];
}
