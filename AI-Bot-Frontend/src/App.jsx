import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Signin from './pages/Signin';
import Signup from './pages/Signup';
import Chatbot from './pages/Chatbot';
import './App.css';

function App() {
  return (
    <ThemeProvider>
      <Router>
        <AuthProvider>
          <Routes>
            <Route path="/signin" element={<Signin />} />
            <Route path="/signup" element={<Signup />} />

            {/* Single protected chatbot page */}
            <Route
              path="/chat"
              element={
                <ProtectedRoute>
                  <Chatbot />
                </ProtectedRoute>
              }
            />

            {/* Default route: if authenticated, go to chat; otherwise to signin */}
            <Route path="/" element={<Navigate to="/chat" replace />} />
          </Routes>
        </AuthProvider>
      </Router>
    </ThemeProvider>
  );
}

export default App;

