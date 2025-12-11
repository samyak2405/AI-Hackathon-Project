import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import { getAccessToken } from '../services/api';
import ThemeToggle from '../components/ThemeToggle';
import './Dashboard.css';

const AdminDashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [testData, setTestData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchAdminTest = async () => {
    setLoading(true);
    setError('');
    setTestData(null); // Clear previous data
    
    try {
      // Note: Using port 8082 as specified in the API endpoint
      // Using axios directly with full URL since it's on a different port
      const accessToken = getAccessToken();
      
      if (!accessToken) {
        setError('No access token available. Please login again.');
        return;
      }

      const response = await axios.get('http://localhost:8082/api/admin/test', {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        withCredentials: true,
        timeout: 5000, // 5 second timeout
      });
      
      setTestData(response.data);
      setError(''); // Clear any previous errors on success
    } catch (err) {
      // Handle different types of errors
      if (err.code === 'ERR_NETWORK' || err.message === 'Network Error') {
        setError('Cannot connect to server. Please ensure the backend server is running on port 8082.');
      } else if (err.code === 'ECONNREFUSED' || err.message.includes('CONNECTION_REFUSED')) {
        setError('Connection refused. Please ensure the backend server is running on port 8082.');
      } else if (err.response) {
        // Server responded with error status
        const status = err.response.status;
        if (status === 401) {
          setError('Unauthorized. Please login again.');
        } else if (status === 403) {
          setError('Access forbidden. You do not have permission to access this resource.');
        } else {
          setError(err.response?.data?.error || err.response?.data?.message || `Server error (${status})`);
        }
      } else if (err.request) {
        setError('No response from server. Please check if the backend is running.');
      } else {
        setError(err.message || 'Failed to fetch admin data');
      }
      
      // Only log to console, don't show technical details to user
      console.error('Admin test error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/signin');
  };

  return (
    <div className="dashboard-container">
      <ThemeToggle />
      <div className="dashboard-content">
        <h1>Admin Dashboard</h1>
        <div className="user-info">
          <h2>Welcome, {user?.email}!</h2>
          {user && (
            <div className="user-details">
              <p><strong>Email:</strong> {user.email}</p>
              <p><strong>User ID:</strong> {user.userId}</p>
              <p><strong>Roles:</strong> {user.roles?.length > 0 ? user.roles.map(r => typeof r === 'string' ? r : r.authority).join(', ') : 'None'}</p>
            </div>
          )}
        </div>

        <div className="test-section">
          <h3>Admin Test Endpoint</h3>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
            Click the button below to test the admin endpoint (requires backend on port 8082)
          </p>
          <button 
            onClick={fetchAdminTest} 
            className="test-button"
            disabled={loading}
          >
            {loading ? 'Loading...' : 'Test Admin Endpoint'}
          </button>

          {error && (
            <div className="error-message" style={{ marginTop: '16px' }}>
              {error}
            </div>
          )}

          {testData && (
            <div className="test-result">
              <h4>Response:</h4>
              <pre>{JSON.stringify(testData, null, 2)}</pre>
            </div>
          )}
        </div>

        <button onClick={handleLogout} className="logout-button">
          Logout
        </button>
      </div>
    </div>
  );
};

export default AdminDashboard;

