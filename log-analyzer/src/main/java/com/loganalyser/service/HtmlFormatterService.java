package com.loganalyser.service;

import org.springframework.stereotype.Service;

/**
 * Service for formatting plain text log analysis responses into a nicely styled HTML page.
 * This keeps HTML generation under our control instead of relying on the LLM to emit HTML.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
@Service
public class HtmlFormatterService {

    /**
     * Wraps the raw text response from OpenAI into a styled HTML document.
     *
     * @param textResponse The plain text response (RCA, analysis, etc.)
     * @return A complete HTML5 document with basic styling
     */
    public String formatToHtml(String textResponse) {
        if (textResponse == null) {
            textResponse = "";
        }

        String escaped = escapeHtml(textResponse);
        String content =
                "<pre class=\"analysis-text\"><code>" + escaped + "</code></pre>";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Log Analysis Report</title>
                    <style>
                        * {
                            box-sizing: border-box;
                        }
                        body {
                            margin: 0;
                            padding: 0;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                            background: linear-gradient(135deg, #141e30 0%%, #243b55 100%%);
                            color: #f5f7fb;
                        }
                        .page-wrapper {
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 24px;
                        }
                        .card {
                            width: 100%%;
                            max-width: 1200px;
                            background: #0f172a;
                            border-radius: 16px;
                            box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6);
                            overflow: hidden;
                            border: 1px solid rgba(148, 163, 184, 0.3);
                        }
                        .card-header {
                            padding: 24px 28px;
                            background: radial-gradient(circle at top left, #22c55e 0%%, transparent 50%%),
                                        radial-gradient(circle at top right, #3b82f6 0%%, transparent 50%%),
                                        #020617;
                            border-bottom: 1px solid rgba(148, 163, 184, 0.4);
                        }
                        .card-header-title {
                            font-size: 1.8rem;
                            font-weight: 700;
                            letter-spacing: 0.03em;
                        }
                        .card-header-subtitle {
                            margin-top: 8px;
                            font-size: 0.95rem;
                            color: #e5e7eb;
                            opacity: 0.85;
                        }
                        .card-body {
                            padding: 24px 28px 28px 28px;
                            background: radial-gradient(circle at bottom, rgba(56, 189, 248, 0.12) 0%%, transparent 55%%),
                                        #020617;
                        }
                        .analysis-text {
                            margin: 0;
                            padding: 16px 20px;
                            background: rgba(15, 23, 42, 0.95);
                            border-radius: 12px;
                            border: 1px solid rgba(148, 163, 184, 0.4);
                            overflow-x: auto;
                            max-height: 75vh;
                            font-size: 0.9rem;
                            line-height: 1.6;
                            color: #e5e7eb;
                        }
                        .analysis-text code {
                            font-family: "JetBrains Mono", "Fira Code", "Source Code Pro", Menlo, Monaco, Consolas, "Courier New", monospace;
                            white-space: pre-wrap;
                        }
                        @media (max-width: 768px) {
                            .card-header {
                                padding: 18px 16px;
                            }
                            .card-body {
                                padding: 18px 16px 20px 16px;
                            }
                            .card-header-title {
                                font-size: 1.4rem;
                            }
                            .analysis-text {
                                padding: 12px 14px;
                                max-height: 70vh;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="page-wrapper">
                        <div class="card">
                            <div class="card-header">
                                <div class="card-header-title">Log Analysis Report</div>
                                <div class="card-header-subtitle">
                                    Generated by the Log Analyser using OpenAI RCA engine.
                                </div>
                            </div>
                            <div class="card-body">
                                %s
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(content);
    }

    /**
     * Escapes HTML special characters to avoid breaking the page.
     *
     * @param text Original text
     * @return Escaped text safe for HTML
     */
    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
