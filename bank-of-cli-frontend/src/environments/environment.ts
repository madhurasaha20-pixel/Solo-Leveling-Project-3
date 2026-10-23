export const environment = {
  production: false,
  apiBase: '/api',
  // true  = requests are answered by core/interceptors/mock-backend.interceptor.ts
  // false = requests go to a real server at apiBase (Part 3)
  useMockBackend: false,
  // Fake server delay so loading spinners/skeletons are visible
  mockLatencyMs: 700
};
