(() => {
  'use strict';

  const store = { token: 'demoToken', user: 'demoUser', session: 'demoSessionId' };
  const byId = (id) => document.getElementById(id);
  const text = (node, value) => { node.textContent = value == null ? '' : String(value); };
  const element = (tag, value) => { const node = document.createElement(tag); if (value != null) text(node, value); return node; };
  const user = () => { try { return JSON.parse(sessionStorage.getItem(store.user) || 'null'); } catch (_) { return null; } };

  async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = sessionStorage.getItem(store.token);
    if (token) headers.set('Authorization', `Bearer ${token}`);
    if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(path, { ...options, headers });
    let body;
    try { body = await response.json(); } catch (_) { throw new Error('COMMON_INTERNAL_ERROR'); }
    if (!response.ok || !body.success) throw new Error(body.code || 'COMMON_INTERNAL_ERROR');
    return body.data;
  }

  function setView() {
    const current = user();
    const login = byId('login-view'); const customer = byId('customer-view'); const supervisor = byId('supervisor-view'); const identity = byId('identity');
    login.hidden = !!current; customer.hidden = !current || current.role !== 'CUSTOMER'; supervisor.hidden = !current || current.role !== 'SUPERVISOR'; identity.hidden = !current;
    if (!current) { window.location.hash = '#login'; return; }
    text(byId('identity-name'), current.displayName); text(byId('identity-role'), current.role);
    window.location.hash = current.role === 'SUPERVISOR' ? '#supervisor' : '#customer';
    if (current.role === 'SUPERVISOR') loadSupervisorTickets(); else { ensureSession(); loadMyTickets(); }
  }

  async function login(event) {
    event.preventDefault(); text(byId('login-error'), '');
    try {
      const response = await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ account: byId('account').value.trim(), password: byId('password').value }) });
      const body = await response.json();
      if (!response.ok || !body.success) throw new Error(body.code || 'AUTH_INVALID_CREDENTIALS');
      sessionStorage.setItem(store.token, body.data.token);
      sessionStorage.setItem(store.user, JSON.stringify({ userId: body.data.userId, displayName: body.data.displayName, role: body.data.role }));
      sessionStorage.removeItem(store.session); setView();
    } catch (error) { text(byId('login-error'), `登录失败：${error.message}`); }
  }

  async function ensureSession() {
    let sessionId = sessionStorage.getItem(store.session);
    if (sessionId) return sessionId;
    try { sessionId = (await api('/api/sessions', { method: 'POST' })).sessionId; sessionStorage.setItem(store.session, sessionId); text(byId('session-label'), `会话 ${sessionId.slice(0, 8)}…`); return sessionId; }
    catch (error) { text(byId('session-label'), `会话创建失败：${error.message}`); throw error; }
  }

  async function sendChat(event) {
    event.preventDefault(); const message = byId('chat-message').value.trim(); if (!message) return;
    appendMessage('user', message); byId('chat-message').value = ''; clearNode(byId('timeline')); clearNode(byId('citations'));
    try {
      const sessionId = await ensureSession();
      const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${sessionStorage.getItem(store.token)}` };
      const response = await fetch('/api/chat/stream', { method: 'POST', headers, body: JSON.stringify({ sessionId, message }) });
      if (!response.ok || !response.body) { const body = await response.json(); throw new Error(body.code || 'COMMON_INTERNAL_ERROR'); }
      await readSse(response.body);
      loadMyTickets();
    } catch (error) { appendTimeline(`处理失败：${error.message}`, 'error'); }
  }

  async function readSse(stream) {
    const reader = stream.getReader(); const decoder = new TextDecoder(); let buffer = ''; let eventName = 'message'; let data = [];
    const dispatch = () => { if (!data.length) return; handleSse(eventName, data.join('\n')); eventName = 'message'; data = []; };
    while (true) {
      const result = await reader.read(); buffer += decoder.decode(result.value || new Uint8Array(), { stream: !result.done });
      const lines = buffer.split(/\r?\n/); buffer = lines.pop();
      lines.forEach((line) => { if (line === '') dispatch(); else if (line.startsWith('event:')) eventName = line.slice(6).trim(); else if (line.startsWith('data:')) data.push(line.slice(5).trim()); });
      if (result.done) { if (buffer) { if (buffer.startsWith('data:')) data.push(buffer.slice(5).trim()); } dispatch(); break; }
    }
  }

  function handleSse(type, raw) {
    if (type === 'status') { appendTimeline(raw, 'status'); return; }
    if (type === 'done') { appendTimeline('处理完成', 'done'); return; }
    let payload; try { payload = JSON.parse(raw); } catch (_) { payload = raw; }
    if (type === 'ticket') { appendTimeline(`已创建/关联工单 #${payload.ticketId}，状态：${payload.taskStatus}`, 'ticket'); return; }
    if (type === 'error') { appendTimeline(`错误：${payload.code || ''} ${payload.message || ''}`, 'error'); return; }
    if (type === 'message') { appendMessage('agent', payload.reply || raw); renderCitations(payload.citations || []); if (payload.ticketId) appendTimeline(`工单 #${payload.ticketId}：${payload.taskStatus}`, 'ticket'); }
  }

  function appendMessage(kind, value) { const node = element('div', value); node.className = `message ${kind}`; byId('conversation').append(node); node.scrollIntoView({ block: 'nearest' }); }
  function appendTimeline(value, kind) { const item = element('li', value); item.className = kind; byId('timeline').append(item); }
  function renderCitations(citations) { const list = byId('citations'); clearNode(list); citations.forEach((citation) => { const item = element('li'); item.append(element('strong', citation.sourceTitle)); item.append(element('small', citation.sourcePath)); item.append(element('span', citation.excerpt)); list.append(item); }); if (!citations.length) list.append(element('li', '本次回答没有可展示的知识库引用。')); }
  function clearNode(node) { while (node.firstChild) node.removeChild(node.firstChild); }

  async function loadMyTickets() { try { renderTicketList(byId('my-ticket-items'), (await api('/api/tickets/mine')).items || [], false, inspectMyTicket); } catch (error) { text(byId('my-ticket-items'), `加载失败：${error.message}`); } }
  async function loadSupervisorTickets() { try { renderTicketList(byId('supervisor-ticket-items'), (await api('/api/supervisor/tickets')).items || [], true, inspectSupervisorTicket); } catch (error) { text(byId('supervisor-ticket-items'), `加载失败：${error.message}`); } }
  function renderTicketList(container, items, showOwner, onClick) { clearNode(container); if (!items.length) { container.append(element('p', '暂无工单。')); return; } items.forEach((ticket) => { const button = element('button'); button.type = 'button'; button.className = 'ticket-row'; const title = element('strong'); title.append(element('span', `#${ticket.ticketId} ${ticket.ticketType}`)); const state = element('span', ticket.status); state.className = `status ${ticket.status}`; title.append(state); button.append(title); button.append(element('span', `${ticket.currentStep}/${ticket.totalSteps} 步 · ${ticket.priority}`)); if (showOwner) button.append(element('small', `归属演示用户：${ticket.userId}`)); button.addEventListener('click', () => onClick(ticket.ticketId)); container.append(button); }); }

  async function inspectMyTicket(ticketId) { await inspectTicket(`/api/tickets/${ticketId}`, byId('ticket-detail-content'), byId('ticket-trace-content')); }
  async function inspectSupervisorTicket(ticketId) { await inspectTicket(`/api/supervisor/tickets/${ticketId}`, byId('supervisor-ticket-detail'), byId('supervisor-ticket-trace')); }
  async function inspectTicket(basePath, detailNode, traceNode) { try { const [detail, trace] = await Promise.all([api(basePath), api(`${basePath}/trace`)]); renderDetail(detailNode, detail); renderTrace(traceNode, trace); } catch (error) { text(detailNode, `加载失败：${error.message}`); text(traceNode, ''); } }
  function renderDetail(container, detail) { clearNode(container); const list = element('div'); list.className = 'detail-list'; [['工单编号', `#${detail.ticketId}`], ['类型', detail.ticketType], ['状态', detail.status], ['进度', `${detail.currentStep}/${detail.totalSteps}`], ['结论', detail.resultSummary || '处理中']].forEach(([label, value]) => { const row = element('div'); row.append(element('span', label)); row.append(element('strong', value)); list.append(row); }); container.append(list); }
  function renderTrace(container, trace) { clearNode(container); const list = element('div'); list.className = 'trace-list'; (trace.steps || []).forEach((step) => { const item = element('div'); item.className = 'trace-item'; item.append(element('strong', `${step.stepNo}. ${step.stepName} · ${step.status}`)); item.append(element('small', `${step.inputSummary || ''} → ${step.outputSummary || ''}`)); list.append(item); }); (trace.toolCalls || []).forEach((tool) => { const item = element('div'); item.className = 'trace-item'; item.append(element('strong', `Tool: ${tool.toolName} · ${tool.success ? 'SUCCESS' : 'FAILED'}`)); item.append(element('small', `${tool.requestSummary || ''} → ${tool.responseSummary || ''}`)); list.append(item); }); if (!list.childNodes.length) list.append(element('p', '暂无已记录轨迹。')); container.append(list); }

  async function reindex() { text(byId('supervisor-notice'), '正在提交知识库重建任务…'); try { const result = await api('/api/knowledge/reindex', { method: 'POST' }); text(byId('supervisor-notice'), `重建完成：${JSON.stringify(result)}`); } catch (error) { text(byId('supervisor-notice'), `当前环境未启用真实知识库重建：${error.message}。可在 local,dashscope 配置 Qdrant 与 API Key 后使用。`); } }
  function logout() { Object.values(store).forEach((key) => sessionStorage.removeItem(key)); setView(); }

  byId('login-form').addEventListener('submit', login); byId('chat-form').addEventListener('submit', sendChat); byId('logout-button').addEventListener('click', logout); byId('refresh-my-tickets').addEventListener('click', loadMyTickets); byId('refresh-supervisor-tickets').addEventListener('click', loadSupervisorTickets); byId('reindex-button').addEventListener('click', reindex);
  document.querySelectorAll('[data-account]').forEach((button) => button.addEventListener('click', () => { byId('account').value = button.dataset.account; byId('password').value = button.dataset.password; }));
  document.querySelectorAll('[data-prompt]').forEach((button) => button.addEventListener('click', () => { byId('chat-message').value = button.dataset.prompt; byId('chat-message').focus(); }));
  setView();
})();
