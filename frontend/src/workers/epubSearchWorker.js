self.onmessage = function(e) {
  const { chapters, query } = e.data;

  if (!chapters || !Array.isArray(chapters) || !query) {
    self.postMessage({ type: 'error', message: '参数不完整' });
    return;
  }

  const results = [];
  const MAX_RESULTS = 100;
  const MIN_DISTANCE = query.length * 2; // 避免相邻重复匹配

  let lastMatchIndex = -Infinity;

  for (let i = 0; i < chapters.length; i++) {
    const chapter = chapters[i];
    const { href, title, text } = chapter;

    self.postMessage({
      type: 'progress',
      percent: Math.floor((i / chapters.length) * 100),
      chapter: href
    });

    if (e.data.type === 'cancel') break;

    const localRegex = new RegExp(query, 'gi');
    let match;
    while ((match = localRegex.exec(text)) !== null && results.length < MAX_RESULTS) {
      const position = match.index;

      // 防止距离太近的重复匹配
      if (position - lastMatchIndex < MIN_DISTANCE) {
        continue;
      }

      lastMatchIndex = position;

      const start = Math.max(0, position - 40);
      const end = Math.min(text.length, position + query.length + 80);
      let excerpt = text.substring(start, end);

      // 高亮关键词
      excerpt = excerpt.replace(new RegExp(query, 'gi'), `<span class="highlight">$&</span>`);
      if (start > 0) excerpt = '...' + excerpt;
      if (end < text.length) excerpt += '...';

      results.push({
        chapterTitle: title,
        excerpt: excerpt,
        href: href
      });
    }
  }

  self.postMessage({ type: 'result', results });
  self.postMessage({ type: 'done' });
};