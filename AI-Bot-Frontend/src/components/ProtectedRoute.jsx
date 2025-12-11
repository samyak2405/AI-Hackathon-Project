import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children, requiredRole = null, allowedRoles = [] }) => {
  const { isAuthenticated, isLoading, hasRole, isAdmin, isCustomer } = useAuth();

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading...</div>
      </div>
    );
  }

  // Redirect to signin if not authenticated
  if (!isAuthenticated) {
    return <Navigate to="/signin" replace />;
  }

  // Check role-based access
  if (requiredRole) {
    if (requiredRole === 'ADMIN' && !isAdmin()) {
      return <Navigate to="/unauthorized" replace />;
    }
    if (requiredRole === 'CUSTOMER' && !isCustomer()) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // Check if user has any of the allowed roles
  if (allowedRoles.length > 0) {
    const hasAllowedRole = allowedRoles.some((role) => {
      if (role === 'ADMIN') return isAdmin();
      if (role === 'CUSTOMER') return isCustomer();
      return hasRole(role);
    });

    if (!hasAllowedRole) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;

