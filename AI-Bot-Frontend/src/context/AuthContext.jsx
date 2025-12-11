import { createContext, useContext, useState, useEffect } from 'react';
import apiClient, { setAccessToken, clearAccessToken, initializeAuth } from '../services/api';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Fetch user info from backend
  const fetchUserInfo = async () => {
    try {
      const response = await apiClient.get('/auth/me');

      // Backend returns a flat object: { id, username, email, role }
      const me = response.data;
      if (!me || !me.username) {
        return null;
      }

      const userData = {
        id: me.id,
        username: me.username,
        email: me.email,
        // Normalize role into an array to work with existing helpers
        roles: me.role
          ? [me.role, `ROLE_${me.role}`]
          : [],
      };

      setUser(userData);
      // Store minimal user data in localStorage as fallback
      localStorage.setItem('user_cache', JSON.stringify({
        email: userData.email,
        userId: userData.id,
        roles: userData.roles || [],
      }));

      return userData;
    } catch (error) {
      console.error('Failed to fetch user info:', error);
      return null;
    }
  };

  // Initialize auth on mount (try to refresh token)
  useEffect(() => {
    const initAuth = async () => {
      try {
        const result = await initializeAuth();
        
        if (result.success && result.accessToken) {
          // We have a token restored from localStorage, try to fetch user info
          const userData = await fetchUserInfo();
          
          // If API call fails, try to restore from localStorage cache
          if (!userData) {
            const cachedUser = localStorage.getItem('user_cache');
            if (cachedUser) {
              try {
                const parsed = JSON.parse(cachedUser);
                // Only restore if we have essential fields
                if (parsed.email || parsed.userId) {
                  setUser(parsed);
                  console.log('Restored user from cache');
                }
              } catch (e) {
                console.error('Failed to parse cached user:', e);
              }
            }
          }
        } else {
          // No valid token, clear any cached user data
          localStorage.removeItem('user_cache');
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        // On error, try to restore from cache as last resort
        const cachedUser = localStorage.getItem('user_cache');
        if (cachedUser) {
          try {
            const parsed = JSON.parse(cachedUser);
            if (parsed.email || parsed.userId) {
              setUser(parsed);
            }
          } catch (e) {
            // Ignore parse errors
          }
        }
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (username, password) => {
    try {
      const response = await apiClient.post('/auth/login', {
        username,
        password,
      });

      const { accessToken } = response.data || {};

      if (!accessToken) {
        return {
          success: false,
          message: 'Login failed: access token missing from response.',
        };
      }

      // Store accessToken (in memory + localStorage)
      setAccessToken(accessToken);

      // Fetch user info using the new token
      const userData = await fetchUserInfo();

      return {
        success: true,
        data: {
          accessToken,
          user: userData,
        },
      };
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || error.message || 'Login failed',
      };
    }
  };

  const logout = async () => {
    try {
      // Call logout endpoint
      // The apiClient automatically includes:
      // - Authorization header with Bearer token (if available)
      // - idempotency-key header
      // - withCredentials: true (for cookies)
      await apiClient.post('/auth/logout', {});
    } catch (error) {
      // Even if logout fails (e.g., token expired, network error),
      // we still clear local state and cookies will be cleared by backend
      console.error('Logout API error:', error);
      
      // If it's a 401, the token might be expired, which is fine
      // The backend will still clear cookies if the request reaches it
      if (error.response?.status === 401) {
        console.log('Token expired during logout - cookies will still be cleared by backend');
      }
    } finally {
      // Always clear state regardless of API response
      // This ensures the user is logged out on the frontend even if API call fails
      
      // Clear access token from memory
      clearAccessToken();
      
      // Clear user state
      setUser(null);
      
      // Clear user cache from localStorage
      localStorage.removeItem('user_cache');
      
      // Note: We only clear user_cache, not all localStorage
      // This preserves theme preference and other non-sensitive data
      
      // Note: Navigation should be handled by the component calling logout
      // This allows components to handle redirect logic
    }
  };

  // Helper functions to check user roles
  const hasRole = (role) => {
    if (!user || !user.roles) return false;
    // Check if user has the role (roles can be array of strings or array of objects with authority property)
    return user.roles.some((r) => {
      if (typeof r === 'string') {
        return r === role || r === `ROLE_${role}`;
      }
      return r.authority === role || r.authority === `ROLE_${role}`;
    });
  };

  const isAdmin = () => {
    return hasRole('ADMIN') || hasRole('ROLE_ADMIN');
  };

  const isCustomer = () => {
    return hasRole('CUSTOMER') || hasRole('ROLE_CUSTOMER');
  };

  const getUserRole = () => {
    if (!user || !user.roles || user.roles.length === 0) return null;
    
    // Return the first role, normalized
    const firstRole = user.roles[0];
    if (typeof firstRole === 'string') {
      return firstRole.replace('ROLE_', '');
    }
    return firstRole.authority?.replace('ROLE_', '') || null;
  };

  const value = {
    user,
    isLoading,
    login,
    logout,
    isAuthenticated: !!user,
    hasRole,
    isAdmin,
    isCustomer,
    getUserRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

