import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from '../components/ThemeToggle';
import './Dashboard.css';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/signin');
  };

  return (
    <div className="dashboard-container">
      <ThemeToggle />
      <div className="dashboard-content">
        <h1>Dashboard</h1>
        <div className="user-info">
          <h2>Welcome!</h2>
          {user && (
            <div className="user-details">
              <p><strong>Email:</strong> {user.email}</p>
              <p><strong>User ID:</strong> {user.userId}</p>
              <p><strong>Roles:</strong> {user.roles?.length > 0 ? user.roles.join(', ') : 'None'}</p>
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

export default Dashboard;

