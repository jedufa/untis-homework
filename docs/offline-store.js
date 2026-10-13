// Local synthetic adapter. It never calls fetch, a socket, or a school service.
const OFFLINE_KEY = 'untis-oss-demo-v1';
function freshDemo() {
  return {
    homework: JSON.parse(JSON.stringify(DEMO_HOMEWORK)),
    notes: {child1: [{id: 'welcome', title: 'Ready for tomorrow', text: 'Pack the sketchbook and a water bottle.', category: 'remember', date: '2026-10-14', color: '#fff7cc', icon: '★'}], child2: []},
    appearances: {}, styles: {}, icons: {}, nextNote: 1,
  };
}
let offlineState;
try {
  offlineState = JSON.parse(localStorage.getItem(OFFLINE_KEY));
  if (!offlineState?.homework?.children?.child1 || !offlineState?.notes) offlineState = freshDemo();
} catch { offlineState = freshDemo(); }

function resetOfflineDemo() {
  for (const key of Object.keys(localStorage)) {
    if (key.startsWith('untis-oss-')) localStorage.removeItem(key);
  }
  location.reload();
}

async function demoRequest(path, options = {}) {
  const method = options.method || 'GET';
  if (!['GET', 'POST'].includes(method)) throw new Error('Unsupported demo operation');
  const body = options.body ? JSON.parse(options.body) : {};
  if (body.child && !Object.hasOwn(DEMO_HOMEWORK.children, body.child)) throw new Error('Unknown fictional child');
  const save = () => localStorage.setItem(OFFLINE_KEY, JSON.stringify(offlineState));
  let result;
  switch (path) {
    case '/api/homeworks': result = offlineState.homework; break;
    case '/api/homework-state': {
      if (method !== 'POST' || !['open', 'completed', 'not_done'].includes(body.status)) throw new Error('Invalid status');
      const homework = offlineState.homework.children[body.child]?.homeworks.find(item => String(item.id) === String(body.id));
      if (!homework) throw new Error('Unknown homework');
      homework.status = body.status; homework.isArchived = body.status !== 'open';
      homework.completedAt = body.status === 'completed' ? '2026-10-13' : '';
      save(); result = {ok: true}; break;
    }
    case '/api/notes':
    case '/api/notes/delete': {
      if (method === 'POST') {
        const notes = offlineState.notes[body.child];
        if (!notes) throw new Error('Unknown fictional child');
        if (path.endsWith('/delete')) offlineState.notes[body.child] = notes.filter(note => note.id !== body.id);
        else {
          const input = body.note || {};
          const note = {
            id: input.id || `demo-note-${offlineState.nextNote++}`,
            title: String(input.title || '').trim().slice(0, 80), text: String(input.text || '').trim().slice(0, 2000),
            category: ['idea', 'school', 'remember', 'personal'].includes(input.category) ? input.category : 'personal',
            date: /^\d{4}-\d{2}-\d{2}$/.test(input.date) ? input.date : '2026-10-13',
            color: /^#[a-f\d]{6}$/i.test(input.color) ? input.color : '#fff7cc', icon: String(input.icon || '★').slice(0, 4),
          };
          if (!note.title) throw new Error('A title is required');
          const index = notes.findIndex(existing => existing.id === note.id);
          if (index >= 0) notes[index] = note; else notes.push(note);
        }
        save();
      }
      result = {ok: true, children: offlineState.notes}; break;
    }
    case '/api/appearance':
      if (method === 'POST') { offlineState.appearances[body.child] = body.appearance; save(); }
      result = {ok: true, children: offlineState.appearances}; break;
    case '/api/domain-styles':
      if (method === 'POST') { offlineState.styles = body.children; save(); }
      result = {ok: true, children: offlineState.styles}; break;
    case '/api/domain-icons':
      if (method === 'POST') {
        if (!['M', 'E', 'SCI', 'ART', 'GEO', 'MUS', 'm', 'e', 'sci', 'art', 'geo', 'mus'].includes(body.subjectKey)) throw new Error('Unknown subject');
        if (body.image && (!String(body.image).startsWith('data:image/') || body.image.length > 700000)) throw new Error('Invalid image');
        offlineState.icons[body.child] ||= {};
        offlineState.icons[body.child][body.subjectKey] = body.image; save();
      }
      result = {ok: true, icons: {}, children: offlineState.icons}; break;
    case '/api/office-hours': result = {ok: true, hours: [], settings: {showEmail: false, showPhone: false}}; break;
    case '/api/cancelled-lessons': result = {ok: true, children: {child1: {lessons: []}, child2: {lessons: []}}}; break;
    default: throw new Error('No such offline operation');
  }
  return {ok: true, json: async () => JSON.parse(JSON.stringify(result))};
}
