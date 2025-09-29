export const API_PREFIX = process.env.NEXT_PUBLIC_API_PREFIX || '/api/v1';
export const HOME_ROUTE = process.env.NEXT_PUBLIC_HOME_ROUTE || '/login';

// helperfunction - concatenated use of the API prefix
export const api = (p: string) =>
  p.startsWith('/') ? `${API_PREFIX}${p}` : `${API_PREFIX}/${p}`;