# Food Delivery System Frontend

A modern React application with signup and signin pages featuring dark/light mode toggle and integrated authentication API.

## Features

- ✨ Beautiful, modern UI design
- 🌓 Dark and Light mode support
- 📱 Responsive design
- 🔐 Signin and Signup pages with API integration
- 🔄 Automatic token refresh on expiry
- 🍪 Cookie-based refresh token management
- 💾 Memory-only access token storage
- ⚡ Built with Vite for fast development

## Getting Started

### Prerequisites

- Node.js (v16 or higher)
- npm or yarn

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

3. Open your browser and navigate to `http://localhost:5173`

### Available Routes

- `/signin` - Sign in page
- `/signup` - Sign up page
- `/dashboard` - Dashboard page (protected)
- `/` - Redirects to signin page

### API Configuration

The app is configured to connect to the backend API at `http://localhost:8081/api`. Make sure your backend server is running on this port.

**Important:** The app uses cookies for refresh tokens. Ensure your backend sets the refresh token as an HTTP-only cookie and that CORS is properly configured to allow credentials.

## Project Structure

```
src/
├── components/
│   ├── ThemeToggle.jsx      # Theme toggle button component
│   └── ThemeToggle.css      # Theme toggle styles
├── context/
│   ├── ThemeContext.jsx     # Theme context provider
│   └── AuthContext.jsx      # Authentication context provider
├── services/
│   └── api.js               # Axios instance with interceptors
├── pages/
│   ├── Signin.jsx           # Sign in page with API integration
│   ├── Signup.jsx           # Sign up page
│   ├── Dashboard.jsx        # Dashboard page
│   ├── Auth.css             # Shared auth page styles
│   └── Dashboard.css        # Dashboard styles
├── App.jsx                   # Main app component with routing
├── App.css                   # Global styles and theme variables
└── main.jsx                  # Application entry point
```

## Authentication Flow

### Token Management

1. **Access Token**: Stored in memory only (not in localStorage or cookies)
2. **Refresh Token**: Stored as HTTP-only cookie by the backend
3. **Automatic Refresh**: When an API request receives a 401 error, the app automatically:
   - Calls `/api/auth/refresh` endpoint
   - Backend reads refreshToken from cookie
   - Returns new accessToken
   - Retries the original failed request

### Login Flow

1. User submits credentials on signin page
2. Frontend calls `/api/auth/login` with:
   - `username` (email)
   - `password`
   - `rememberMe` (boolean as string)
   - `deviceInfo` (IP and userAgent)
3. Backend returns:
   - `accessToken` (stored in memory)
   - `refreshToken` (set as HTTP-only cookie)
   - User information
4. User is redirected to dashboard

### Page Reload Behavior

- On app load, the app automatically calls `/api/auth/refresh`
- If refresh token exists in cookie, new access token is obtained
- If no refresh token, user must login again

### Logout

- Clears access token from memory
- Clears localStorage
- Calls `/api/auth/logout` endpoint
- Redirects to signin page

## Theme System

The app uses CSS variables for theming. The theme preference is stored in localStorage and persists across sessions. Toggle the theme using the button in the top-right corner of any page.

## Build for Production

```bash
npm run build
```

The built files will be in the `dist` directory.

## Preview Production Build

```bash
npm run preview
```

