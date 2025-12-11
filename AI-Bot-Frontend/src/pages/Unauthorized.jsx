import { Link } from 'react-router-dom';
import ThemeToggle from '../components/ThemeToggle';
import './Auth.css';

const Unauthorized = () => {
  return (
    <div className="auth-container">
      <ThemeToggle />
      <div className="auth-card">
        <div className="auth-header">
          <h1>403 - Unauthorized</h1>
          <p>You don't have permission to access this resource</p>
        </div>
        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <Link to="/signin" className="auth-button" style={{ display: 'inline-block', textDecoration: 'none' }}>
            Go to Sign In
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Unauthorized;

