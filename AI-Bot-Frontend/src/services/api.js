import axios from 'axios';

// Read API base URL from Vite env for flexible deployments; fall back to localhost for dev
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Important: allows cookies to be sent
});

// Store accessToken in memory (and mirror to localStorage for persistence)
let accessToken = null;

// Initialize accessToken from localStorage if available (browser only)
if (typeof window !== 'undefined') {
  const storedToken = window.localStorage.getItem('access_token');
  if (storedToken) {
    accessToken = storedToken;
  }
}

// Token management functions
const setAccessToken = (token) => {
  accessToken = token;
  if (typeof window !== 'undefined') {
    if (token) {
      window.localStorage.setItem('access_token', token);
    } else {
      window.localStorage.removeItem('access_token');
    }
  }
};

const getAccessToken = () => {
  return accessToken;
};

const clearAccessToken = () => {
  accessToken = null;
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem('access_token');
  }
};

// Request interceptor: Add accessToken to headers
apiClient.interceptors.request.use(
  (config) => {
    // Add accessToken if available
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor: Handle auth errors (no refresh endpoint in current backend)
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || '';

    // For login and register, let the caller handle 401 so we can show proper error messages
    const isAuthLogin = requestUrl.includes('/auth/login');
    const isAuthRegister = requestUrl.includes('/auth/register');

    if (status === 401 && !isAuthLogin && !isAuthRegister) {
      // Clear token and redirect to login on unauthorized for protected resources
      clearAccessToken();
      if (typeof window !== 'undefined') {
        window.location.href = '/signin';
      }
    }
    return Promise.reject(error);
  }
);

// Export token management functions
export { setAccessToken, getAccessToken, clearAccessToken };

// Initialize: Restore accessToken from localStorage if present
export const initializeAuth = async () => {
  if (typeof window === 'undefined') {
    return { success: false };
  }

  const storedToken = window.localStorage.getItem('access_token');
  if (storedToken) {
    setAccessToken(storedToken);
    return {
      success: true,
      accessToken: storedToken,
      user: null,
    };
  }

  return { success: false };
};

export default apiClient;

