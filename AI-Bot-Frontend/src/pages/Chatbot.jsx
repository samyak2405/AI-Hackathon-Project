import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import DOMPurify from 'dompurify';
import { marked } from 'marked';
import ThemeToggle from '../components/ThemeToggle';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import apiClient from '../services/api';
import PayULogoLime from '../../assests/images/payu_logo/PAYU_LOGO_SQUARE_LIME-ai.png';
import PayULogoWhite from '../../assests/images/payu_logo/PAYU_LOGO_SQUARE_WHITE-ai.png';
import './Dashboard.css';

const toSafeHtml = (markdownOrHtml) => {
  if (!markdownOrHtml) return '';
  // If it looks like HTML, keep it; otherwise convert Markdown to HTML.
  const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(markdownOrHtml);
  const rawHtml = looksLikeHtml ? markdownOrHtml : marked.parse(markdownOrHtml);
  return DOMPurify.sanitize(rawHtml);
};

const TypewriterHtml = ({ html, speed = 18 }) => {
  const [displayed, setDisplayed] = useState('');

  useEffect(() => {
    if (!html) {
      setDisplayed('');
      return;
    }

    let index = 0;
    let timeoutId;

    const totalLength = html.length;
    const stepSize = 3; // reveal a few characters at a time for smoother typing

    const tick = () => {
      index += stepSize;
      setDisplayed(html.slice(0, index));

      if (index < totalLength) {
        timeoutId = setTimeout(tick, speed);
      }
    };

    // start animation
    setDisplayed('');
    tick();

    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [html, speed]);

  return (
    <div
      className="chatbot-message-content"
      // progressively reveal the HTML response like a typing animation
      dangerouslySetInnerHTML={{ __html: displayed }}
    />
  );
};

const Chatbot = () => {
  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState([]);
  const [conversations, setConversations] = useState([]);
  const [currentChatId, setCurrentChatId] = useState(null);
  const [hasStartedChat, setHasStartedChat] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { theme } = useTheme();

  const payuLogoSrc = theme === 'dark' ? PayULogoWhite : PayULogoLime;
  const recommendedQuestions = [
    'Show last 1 hour transactions.',
    'Investigate failed payments in the last 1 hour.',
    'Give insights on top customer issues this week.',
  ];
  const formatUpdatedAt = (ts) => {
    if (!ts) return '';
    try {
      return new Date(ts).toLocaleString();
    } catch {
      return ts;
    }
  };

  const fetchConversations = useCallback(
    async (preserveSelection = true) => {
    try {
      const res = await apiClient.get('/chat/conversations');
      const list = Array.isArray(res.data) ? res.data : [];
      setConversations(list);
        if (!preserveSelection || !currentChatId) {
          if (list.length > 0) {
            setCurrentChatId(list[0].chatId);
            return list[0].chatId;
          }
          setCurrentChatId(null);
        }
        return currentChatId;
    } catch {
      // ignore; conversations are optional
      setConversations([]);
        if (!preserveSelection) {
          setCurrentChatId(null);
        }
        return null;
    }
    },
    [currentChatId]
  );

  const loadHistory = useCallback(async (chatIdParam = null) => {
    try {
      const res = await apiClient.get('/chat/history', {
        params: chatIdParam ? { chatId: chatIdParam } : {},
      });
      const historyResp = res.data || {};
      const resolvedChatId = historyResp.chatId || chatIdParam || null;
      const history = Array.isArray(historyResp.messages) ? historyResp.messages : [];
      if (Array.isArray(history) && history.length > 0) {
        const mapped = history.map((m) => ({
          id: m.id,
          from: m.role === 'USER' ? 'user' : 'ai',
          type: 'html',
          content: toSafeHtml(m.content),
          animated: false, // historical messages render immediately
        }));
        setMessages(mapped);
        setHasStartedChat(true);
        setCurrentChatId(resolvedChatId);
      } else {
        setMessages([]);
        setHasStartedChat(false);
        setCurrentChatId(resolvedChatId);
      }
    } catch {
      // ignore history load errors; chat still works
    }
  }, []);

  useEffect(() => {
    (async () => {
      const initialChatId = await fetchConversations();
      await loadHistory(initialChatId);
    })();
  }, [fetchConversations, loadHistory]);

  const handlePromptChange = (e) => {
    const value = e.target.value;
    setPrompt(value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isLoading) return; // prevent new prompt while previous is processing
    const trimmed = prompt.trim();
    if (!trimmed) return;

    let chatIdToUse = currentChatId;
      setHasStartedChat(true);

    if (!chatIdToUse) {
      try {
        const newChatRes = await apiClient.post('/chat/conversations');
        chatIdToUse = newChatRes.data?.chatId || null;
        setCurrentChatId(chatIdToUse);
        setConversations((prev) => [
          { chatId: chatIdToUse, title: 'New chat', updatedAt: new Date().toISOString() },
          ...prev,
        ]);
      } catch (creationErr) {
        setError('Could not start a new chat. Please try again.');
        setIsLoading(false);
        return;
      }
    }

    // Add user message to the conversation
    const userMessage = {
      id: Date.now(),
      from: 'user',
      type: 'text',
      content: trimmed,
      animated: false,
    };
    setMessages((prev) => [...prev, userMessage]);
    setPrompt('');
    setError(null);
    setIsLoading(true);

    try {
      const response = await apiClient.post('/auth/prompt', { prompt: trimmed, chatId: chatIdToUse });
      const aiMessage = {
        id: Date.now() + 1,
        from: 'ai',
        type: 'html',
        content: toSafeHtml(
          typeof response.data === 'string' ? response.data : JSON.stringify(response.data)
        ),
        animated: false, // render HTML immediately
      };

      setMessages((prev) => [...prev, aiMessage]);
      if (!currentChatId && chatIdToUse) {
        setCurrentChatId(chatIdToUse);
      }
      await fetchConversations(true);
    } catch (err) {
      const fallbackMessage =
        typeof err?.response?.data === 'string'
          ? err.response.data
          : err?.response?.data?.message || 'Something went wrong while contacting the assistant.';
      setError(fallbackMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleNewChat = async () => {
    // Prevent creating multiple empty chats; if current has no messages, just keep it selected.
    if (!messages || messages.length === 0) {
      await loadHistory(currentChatId);
      return;
    }
    setIsLoading(true);
    try {
      const res = await apiClient.post('/chat/conversations');
      const newChatId = res.data?.chatId || null;
      if (!newChatId) {
        setIsLoading(false);
        return;
      }
      setCurrentChatId(newChatId);
      setMessages([]);
      setHasStartedChat(false);
      setConversations((prev) => {
        const withoutDuplicate = prev.filter((c) => c.chatId !== newChatId);
        return [
          { chatId: newChatId, title: res.data?.title || 'New chat', updatedAt: res.data?.updatedAt },
          ...withoutDuplicate,
        ];
      });
    } catch {
      setError('Could not create a new chat. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectConversation = async (chatId) => {
    if (!chatId || chatId === currentChatId) return;
    setIsLoading(true);
    setError(null);
    setMessages([]);
    await loadHistory(chatId);
    setIsLoading(false);
  };

  const handleLogout = async () => {
    await logout();
    navigate('/signin', { replace: true });
  };

  return (
    <div className="dashboard-container chatbot-page">
      <div className="chatbot-shell">
        <header className="chatbot-header">
          <div className="chatbot-top-row">
          <div className="chatbot-payu-top">
            <img src={payuLogoSrc} alt="PayU logo" className="chatbot-payu-logo" />
            </div>
            <div className="chatbot-header-actions">
              <button
                type="button"
                className="chatbot-new-chat-button"
                onClick={handleNewChat}
                title="Start a new chat"
                disabled={isLoading}
              >
                +
              </button>
              <ThemeToggle />
              <button type="button" className="chatbot-logout-button" onClick={handleLogout}>
                Logout
              </button>
            </div>
          </div>

          <div className="chatbot-header-main">
            <div className="chatbot-brand">
              <div>
                <h1>PayU Sensei 🧠</h1>
                <p>Your smart assistant for instant debugging, insights, and clarity.</p>
              </div>
            </div>
          </div>
        </header>

        <div className={`chatbot-layout ${isSidebarCollapsed ? 'collapsed' : ''}`}>
          <aside className={`chatbot-sidebar ${isSidebarCollapsed ? 'collapsed' : ''}`}>
            <div className="chatbot-sidebar-header">
              <h3>{isSidebarCollapsed ? 'Chats' : 'Previous chats'}</h3>
              <button
                type="button"
                className="chatbot-sidebar-toggle"
                onClick={() => setIsSidebarCollapsed((v) => !v)}
                aria-label={isSidebarCollapsed ? 'Expand chat list' : 'Collapse chat list'}
              >
                {isSidebarCollapsed ? '»' : '«'}
              </button>
            </div>
            <div className="chatbot-convo-list">
              {conversations.length === 0 && (
                <p className="chatbot-convo-empty">No chats yet. Start a new one!</p>
              )}
              {conversations.map((c) => (
                <button
                  type="button"
                  key={c.chatId}
                  className={`chatbot-convo-item ${currentChatId === c.chatId ? 'active' : ''}`}
                  onClick={() => handleSelectConversation(c.chatId)}
                >
                  <span className="chatbot-convo-title">{c.title || 'New chat'}</span>
                  <span className="chatbot-convo-meta">{formatUpdatedAt(c.updatedAt)}</span>
                </button>
              ))}
            </div>
          </aside>

          <div className="chatbot-content">
        <main className="chatbot-main">
              <div
                className={`chatbot-messages ${
                  !hasStartedChat && messages.length === 0 ? 'empty' : ''
                }`}
              >
              <div className="chatbot-intro">
                <p className="chatbot-intro-title">Hi, I'm PayU Sensei 🧠</p>
              {!hasStartedChat && messages.length === 0 && (
                <p className="chatbot-intro-subtitle">
                  Ask me anything about your transactions, production issues, or insights and I&apos;ll guide you.
                </p>
              )}
              </div>
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`chatbot-message ${
                  msg.from === 'user' ? 'chatbot-message-user' : 'chatbot-message-ai'
                }`}
              >
                {msg.type === 'html' ? (
                  <div
                    className="chatbot-message-content"
                    dangerouslySetInnerHTML={{ __html: msg.content }}
                  />
                ) : (
                  <p className="chatbot-message-content">{msg.content}</p>
                )}
              </div>
            ))}

            {isLoading && (
              <div className="chatbot-message chatbot-message-ai chatbot-message-loading">
                <p className="chatbot-message-content">Thinking…</p>
              </div>
            )}
          </div>
        </main>

        <form onSubmit={handleSubmit} className="chatbot-form">
              {!hasStartedChat && messages.length === 0 && (
                <div className="chatbot-recommended">
                  <p className="chatbot-recommended-title">Try asking:</p>
                  <div className="chatbot-recommended-list">
                    {recommendedQuestions.map((q) => (
                      <button
                        type="button"
                        key={q}
                        className="chatbot-recommended-chip"
                        onClick={() => setPrompt(q)}
                      >
                        {q}
                      </button>
                    ))}
                  </div>
                </div>
              )}
          <div className="chatbot-input-wrapper">
            <input
              type="text"
              className="chatbot-input"
              placeholder="Type your prompt here..."
              value={prompt}
              onChange={handlePromptChange}
              disabled={isLoading}
            />
            <button type="submit" className="chatbot-button" disabled={isLoading}>
              Search
            </button>
          </div>
          {error && <p className="chatbot-error">{error}</p>}
          <p className="chatbot-hint">AI can make mistakes. Verify important information.</p>
        </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Chatbot;


