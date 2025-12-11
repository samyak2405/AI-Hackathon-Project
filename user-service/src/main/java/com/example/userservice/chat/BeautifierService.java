package com.example.userservice.chat;

import org.springframework.stereotype.Service;

@Service
public class BeautifierService {

    // Scoped, minimal styling for agent responses
    private static final String EMBEDDED_CSS = """
      <style>
        .agent-card {
          font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
          color: #0f172a;
          background: linear-gradient(135deg, rgba(59,130,246,0.04), rgba(16,185,129,0.03));
          border: 1px solid rgba(148,163,184,0.35);
          border-radius: 14px;
          padding: 14px 16px;
          box-shadow: 0 6px 20px rgba(15,23,42,0.18);
        }
        [data-theme='dark'] .agent-card {
          color: #e5e7eb;
          background: linear-gradient(135deg, rgba(59,130,246,0.08), rgba(16,185,129,0.06));
          border: 1px solid rgba(148,163,184,0.4);
          box-shadow: 0 8px 24px rgba(0,0,0,0.45);
        }
        .agent-card h1, .agent-card h2, .agent-card h3, .agent-card h4 {
          margin: 0 0 8px;
          color: #0f172a;
          letter-spacing: -0.01em;
        }
        [data-theme='dark'] .agent-card h1,
        [data-theme='dark'] .agent-card h2,
        [data-theme='dark'] .agent-card h3,
        [data-theme='dark'] .agent-card h4 {
          color: #f8fafc;
        }
        .agent-card p {
          margin: 0 0 8px;
          line-height: 1.6;
          color: #1f2937;
        }
        [data-theme='dark'] .agent-card p {
          color: #e2e8f0;
        }
        .agent-card ul, .agent-card ol {
          padding-left: 20px;
          margin: 6px 0 10px;
        }
        .agent-card table {
          width: 100%;
          border-collapse: collapse;
          margin: 10px 0;
          font-size: 14px;
        }
        .agent-card th, .agent-card td {
          border: 1px solid rgba(148,163,184,0.5);
          padding: 8px 10px;
          text-align: left;
        }
        .agent-card thead {
          background: rgba(59,130,246,0.08);
        }
        .agent-card code, .agent-card pre {
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
        }
        .agent-card pre {
          background: #0b1220;
          color: #e5e7eb;
          padding: 10px;
          border-radius: 10px;
          overflow-x: auto;
          margin: 10px 0;
        }
        .agent-card a {
          color: #2563eb;
          text-decoration: none;
        }
        .agent-card a:hover {
          text-decoration: underline;
        }
      </style>
    """;

    public String beautify(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return EMBEDDED_CSS + "<div class=\"agent-card\">" + html + "</div>";
    }
}


