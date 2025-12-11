## Conversational RCA with Context Reuse

### Core idea
- Maintain a per-session “RCA context” (issue, filters, evidence, trace IDs) and decide each turn whether to continue that context or start fresh.

### Data structures (session store)
- `issue_summary`: initial/last confirmed issue text.
- `time_window`: start/end or relative; `last_cursor` for incremental pulls.
- `scopes`: services, regions, env, version/deploy, severity.
- `request_ids` / `trace_ids`: top correlated IDs.
- `evidence`: cited log/trace snippets with timestamps/source.
- `hypotheses`: candidate root causes plus confidence.
- `last_queries`: structured and semantic queries used.
- `status`: active / resolved / switched.

### Tool APIs (adapters)
- `search_logs(filters, text, limit, cursor?)`
- `get_traces(ids)`
- `get_metrics(expr, range)`
- `list_deploys(time_window)` (optional)

### Turn-by-turn algorithm
1) **Classify intent** — is this a follow-up on the same issue or a new one? Heuristics: overlap in services/time/symptoms; ask to confirm if ambiguous.  
2) **Update context (if same issue)** — merge new constraints (time, service, symptom keywords); keep previous scopes unless explicitly changed; if time advanced, use `cursor = last_cursor` to fetch only new logs.  
3) **Plan queries** — structured filters first (service, level ≥ error, time window, region/version if known); semantic search text = `issue_summary + latest prompt`; if request/trace IDs exist, fetch newest spans/logs; if metrics available, pull error-rate deltas around the window.  
4) **Retrieve** — hybrid search (structured + semantic); cluster/group by request_id/trace_id/stack signature/host; incremental if `cursor` exists (only newer logs).  
5) **Analyze** — refresh timeline (first/last occurrence, spikes, deploy proximity); identify dominant patterns (repeating stack, specific host/region/version); update `hypotheses` with confidence; mark evidence used.  
6) **Respond** — cite evidence snippets (timestamp + source); state confidence and alternative hypotheses if weak; offer next actions (“Want failing trace?” “Compare with previous deploy?”); store a concise response summary back into `issue_summary`.  
7) **Persist state** — save updated `time_window`, `last_cursor`, `request_ids`, `hypotheses`, `evidence_refs`.

### Prompt template (system)
“You are an SRE copilot. Use tools to search logs/traces/metrics. Prefer structured filters, then semantic search. Cite evidence with timestamps. If data is insufficient, ask what to query. Don’t invent log lines.”

### Controller pseudocode
```
on_user_message(msg, session_id):
    ctx = load_session(session_id)
    intent = classify(msg, ctx.issue_summary)

    if intent == "new_issue":
        ctx = new_context()
        ctx.issue_summary = summarize_issue(msg)
    else:
        ctx.issue_summary = merge_summary(ctx.issue_summary, msg)

    filters = merge_filters(ctx.scopes, msg)
    window = update_time_window(ctx.time_window, msg)
    text_query = build_semantic_query(ctx.issue_summary, msg)

    cursor = ctx.last_cursor
    logs = search_logs(filters, text_query, limit=200, cursor=cursor)
    clusters = cluster_logs(logs, by=["request_id", "trace_id", "stack", "host"])
    traces = fetch_top_traces(clusters, ctx.trace_ids)
    metrics = maybe_get_metrics(filters, window)

    findings, hypotheses = analyze(clusters, traces, metrics)
    response = compose_response(findings, hypotheses, ctx.evidence)

    ctx = update_ctx(ctx, logs, clusters, traces, hypotheses, window)
    save_session(session_id, ctx)

    return response
```

### Clustering heuristic
- Prefer request_id/trace_id. Otherwise hash stack trace signature + service + endpoint.
- Aggregate counts, first/last timestamps, sample messages.

### Incremental retrieval
- Store `last_cursor` (timestamp or log offset); use it to fetch only new logs to reduce cost and latency.

### Guardrails
- Grounding: respond only with cited evidence; no fabricated log lines.
- Ambiguity: ask for confirmation if filters/scope are unclear or changed.
- Privacy: mask secrets/PII.
- Performance: cap results, sample top-N, paginate evidence.

### MVP slice
- Session store plus single tool `search_logs` (hybrid: filters + semantic).
- Context fields: `issue_summary`, `time_window`, `scopes`, `last_cursor`, `evidence_refs`.
- Prompt enforcing evidence and uncertainty.
- Follow-up handling: reuse context, refine filters, fetch deltas.

----
1. Create a new Chat and each chat should be unique as per clientTxnId -> Samyak
2. himanshu -> Take Recent 5 chats and basis that give response
3. Every time we fetch the logs persist in disk, every response should be stored in db against clientTxnId