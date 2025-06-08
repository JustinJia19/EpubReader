<template>
  <div class="reader-container">
    <!-- 左侧面板：目录和搜索 -->
    <div class="sidebar">
      <div class="tabs">
        <div class="tab" :class="{ active: activeTab === 'toc' }" @click="activeTab = 'toc'">
          <el-icon><Collection /></el-icon> 目录
        </div>
        <div class="tab" :class="{ active: activeTab === 'search' }" @click="activeTab = 'search'">
          <el-icon><Search /></el-icon> 搜索
        </div>
      </div>

      <!-- 目录内容 -->
      <div v-if="activeTab === 'toc'" class="tab-content">
        <h3>图书目录</h3>
        <ul class="toc-list">
          <li v-for="(item, index) in toc" 
              :key="index"
              :class="[
                'toc-item',
                { active: activeTocItem === item.href },
                `level-${item.level}`
              ]"
              @click="goToChapter(item)">
            {{ item.label }}
          </li>
        </ul>
      </div>

      <!-- 搜索内容 -->
      <div v-if="activeTab === 'search'" class="tab-content">
        <div class="search-container">
          <div class="search-box">
            <el-input
              v-model="searchQuery"
              placeholder="输入关键词搜索全书..."
              @keyup.enter="performSearch"
              clearable
            >
              <template #append>
                <el-button @click="performSearch" :loading="searching">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          
          <div v-if="searching" class="search-progress">
            <el-progress 
              :percentage="searchProgress"
              :stroke-width="4"
              :show-text="false"
            />
            <div class="progress-text">
              正在搜索: {{ currentSearchChapter }} ({{ searchProgress }}%)
              <el-button 
                size="small" 
                @click="cancelSearch"
                v-if="searching"
              >
                取消
              </el-button>
            </div>
          </div>
          
          <div class="search-stats" v-if="!searching && searchResults.length">
            找到 {{ searchResults.length }} 个结果
          </div>
        </div>

        <div class="search-results">
          <div v-if="searching" class="no-results">
            <el-icon class="is-loading"><Loading /></el-icon> 搜索中...
          </div>
          
          <div v-else-if="searchResults.length === 0 && searchQuery" class="no-results">
            <el-icon><Search /></el-icon> 未找到相关结果
          </div>
          
          <div v-else-if="!searchQuery" class="no-results">
            <el-icon><Search /></el-icon> 输入关键词开始搜索
          </div>
          
          <div 
            v-for="(result, index) in searchResults" 
            :key="index"
            class="result-item"
            :class="{ active: currentResultIndex === index }"
            @click="goToSearchResult(result, index)"
          >
            <div class="result-title">
              <el-icon><Document /></el-icon> 结果 #{{ index + 1 }} · {{ result.chapterTitle }}
            </div>
            <div class="result-excerpt" v-html="result.excerpt"></div>
          </div>
        </div>

        <div class="search-controls" v-if="searchResults.length > 0 && !searching">
          <el-button 
            circle 
            :disabled="currentResultIndex <= 0"
            @click="prevResult"
          >
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <span class="result-counter">{{ currentResultIndex + 1 }} / {{ searchResults.length }}</span>
          <el-button 
            circle 
            :disabled="currentResultIndex >= searchResults.length - 1"
            @click="nextResult"
          >
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 顶部进度条和章节信息 -->
    <div class="reader-header">
      <router-link to="/home" class="nav-link">
        <el-icon class="header-icon">
          <HomeFilled />
        </el-icon>
      </router-link>
      <div class="header-content">
        <div class="progress-container">
          <div class="progress-bar" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="chapter-title">{{ currentChapter }}</div>
      </div>
    </div>

    <!-- 章节导航按钮 -->
    <div class="navigation-buttons">
      <button class="nav-button prev-button" @click="goPrevChapter">
        <el-icon color="white"><ArrowLeftBold /></el-icon>
      </button>
      <button class="nav-button next-button" @click="goNextChapter">
        <el-icon color="white"><ArrowRightBold /></el-icon>
      </button>
    </div>
   
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loader"></div>
      <p>电子书加载中...</p>
    </div>

    <!-- 阅读器主体 -->
    <div 
      id="viewer"
      ref="viewerEl"
      tabindex="0"
      @keydown.left.stop.prevent="goPrevChapter"
      @keydown.right.stop.prevent="goNextChapter"
    >
      <!-- 错误提示 -->
      <div v-if="error" class="error-message">
        {{ error }}
        <button @click="reloadReader">重试</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElNotification,
  ElIcon,
  ElButton,
  ElInput,
  ElProgress
} from 'element-plus'
import {
  HomeFilled,
  ArrowLeftBold,
  ArrowRightBold,
  Collection,
  Search,
  Loading,
  ArrowLeft,
  ArrowRight,
  Document
} from '@element-plus/icons-vue'

const route = useRoute()

// EPUB实例引用
const book = ref(null)
const rendition = ref(null)
const viewerEl = ref(null)

// 响应式状态
const loading = ref(true)
const error = ref(null)
const progress = ref(0)
const currentChapter = ref('')
const baseURL = process.env.VUE_APP_API_BASE || 'http:'

// 目录相关
const toc = ref([])
const activeTocItem = ref(null)

// 搜索相关
const activeTab = ref('toc')
const searchQuery = ref('')
const searchResults = ref([])
const searching = ref(false)
// const searchCancelled = ref(false)
const searchProgress = ref(0)
const currentSearchChapter = ref('')
const currentResultIndex = ref(-1)

let searchWorker = null // Web Worker 实例

// 初始化阅读器
const initReader = async () => {
  try {
    loading.value = true
    const Epub = (await import('epubjs')).default;
    const bookUrl = `${baseURL}/uploads/${encodeURIComponent(route.query.file)}`

    book.value = new Epub(bookUrl)

    rendition.value = book.value.renderTo(viewerEl.value, {
      width: '100%',
      height: '100%',
      spread: 'none',
      flow: 'scrolled',
      scroll: 'continuous',
      allowScriptedContent: true
    })

    await rendition.value.themes.default({
      'body': {
        'margin': '0 auto',
        'padding': '2rem',
        'line-height': 1.8,
        'font-size': '18px',
        'max-width': '800px',
        'text-align': 'justify'
      }
    })

    await book.value.ready
    await book.value.locations.generate(1600)
    await fetchToc()
    rendition.value.on('relocated', updateProgress)
    rendition.value.on('relocated', updateChapter)
    setupScrollHandler()
    setupFocus()

    await rendition.value.display()
    loading.value = false
  } catch (err) {
    error.value = `加载失败: ${err.message}`
    console.error('EPUB Error:', err)
    loading.value = false
  }
}

const setupFocus = () => {
  viewerEl.value.focus()
  viewerEl.value.setAttribute('tabindex', 0)
}

const setupScrollHandler = () => {
  viewerEl.value.addEventListener('wheel', (e) => {
    if (Math.abs(e.deltaY) < Math.abs(e.deltaX)) return
    e.preventDefault()
    viewerEl.value.scrollBy({ top: e.deltaY * 1.5, behavior: 'smooth' })
  }, { passive: false })
}

const updateProgress = (location) => {
  progress.value = Math.floor(location.start.percentage * 100)
}

const updateChapter = (location) => {
  currentChapter.value = location?.chapter?.title || ' ';
  try {
    const currentLocation = rendition.value.currentLocation();
    if (currentLocation && currentLocation.start) {
      activeTocItem.value = currentLocation.start.href;
    }
  } catch (e) {
    console.error('Failed to get current location:', e);
  }
};

const reloadReader = () => {
  destroyReader()
  error.value = null
  initReader()
}

const destroyReader = () => {
  if (rendition.value) rendition.value.destroy()
  if (book.value) book.value.destroy()
  if (viewerEl.value) {
    viewerEl.value.removeEventListener('wheel', setupScrollHandler)
  }
}

onMounted(initReader)
onUnmounted(destroyReader)

const fetchToc = async () => {
  if (!book.value) return
  const nav = book.value?.navigation
  const navItems = nav && Array.isArray(nav.toc) ? nav.toc : []
  toc.value = flattenToc(navItems)
}

function flattenToc(items, level = 0, result = []) {
  for (let item of items) {
    result.push({ ...item, level })
    if (item.subitems && item.subitems.length > 0) {
      flattenToc(item.subitems, level + 1, result)
    }
  }
  return result
}

const goToChapter = async (item) => {
  try {
    loading.value = true
    await rendition.value.display(item.href)
    viewerEl.value.scrollTop = 0
    activeTocItem.value = item.href
    loading.value = false
  } catch (err) {
    error.value = `跳转失败: ${err.message}`
    loading.value = false
  }
}

// 预加载所有章节文本内容
const loadAllChapterTexts = async () => {
  const chapterContents = []
  const spineItems = book.value.spine.spineItems

  for (let i = 0; i < spineItems.length; i++) {
    const item = spineItems[i]
    if (!item.href) continue

    try {
      const content = await book.value.load(item.href)

      let textContent = ''
      if (typeof content === 'string') {
        const parser = new DOMParser()
        const doc = parser.parseFromString(content, 'text/html')
        textContent = doc.body?.textContent || ''
      } else if (content && typeof content.querySelector === 'function') {
        textContent = content.querySelector('body')?.textContent || ''
      }

      chapterContents.push({
        href: item.href,
        title: item.title || item.href,
        text: textContent
      })

    } catch (err) {
      console.warn(`加载章节失败: ${item.href}`, err)
    }
  }

  return chapterContents
}

// 启动搜索 Worker
const performSearch = async () => {
  const query = searchQuery.value.trim()
  if (!query) {
    ElNotification.warning({ title: '提示', message: '请输入搜索关键词' })
    return
  }

  if (searchWorker) {
    searchWorker.terminate()
  }

  searchWorker = new Worker(new URL('@/workers/epubSearchWorker.js', import.meta.url))

  // 预加载章节内容
  searching.value = true
  searchResults.value = []
  searchProgress.value = 0

  const chapters = await loadAllChapterTexts()
  searchWorker.postMessage({ chapters, query })

  searchWorker.onmessage = (e) => {
    const data = e.data

    if (data.type === 'progress') {
      searchProgress.value = data.percent
      currentSearchChapter.value = data.chapter
    }

    if (data.type === 'result') {
      searchResults.value.push(...data.results)
    }

    if (data.type === 'done') {
      searching.value = false
      if (!searchResults.value.length) {
        ElNotification.info({ title: '搜索完成', message: '未找到相关内容' })
      } else {
        ElNotification.success({ title: '搜索完成', message: `共找到 ${searchResults.value.length} 个结果` })
      }
    }

    if (data.type === 'error') {
      searching.value = false
      ElNotification.error({ title: '搜索错误', message: data.message })
    }
  }

  searchWorker.onerror = (err) => {
    searching.value = false
    console.error('Worker error:', err)
    ElNotification.error({ title: '搜索错误', message: '搜索过程中发生异常' })
  }
}

// 取消搜索
const cancelSearch = () => {
  if (searchWorker) {
    searchWorker.postMessage({ type: 'cancel' })
    searchWorker.terminate()
    searchWorker = null
  }
  searching.value = false
  ElNotification.info({ title: '提示', message: '已取消搜索' })
}

// 跳转到搜索结果
const goToSearchResult = async (result, index) => {
  try {
    clearHighlights()
    await rendition.value.display(result.href)
    currentResultIndex.value = index
    viewerEl.value.scrollTop = 0
  } catch (err) {
    console.error('跳转失败:', err)
    ElNotification.error({ title: '导航错误', message: '无法跳转到搜索结果' })
  }
}

const clearHighlights = () => {
  // TODO 添加 highlight 插件支持
}

const prevResult = () => {
  if (currentResultIndex.value > 0) {
    const newIndex = currentResultIndex.value - 1
    goToSearchResult(searchResults.value[newIndex], newIndex)
  }
}

const nextResult = () => {
  if (currentResultIndex.value < searchResults.value.length - 1) {
    const newIndex = currentResultIndex.value + 1
    goToSearchResult(searchResults.value[newIndex], newIndex)
  }
}

// 章节导航方法
const goNextChapter = async () => {
  try {
    await rendition.value.next()
    viewerEl.value.scrollTop = 0
  } catch (err) {
  ElNotification.error({
    title: '错误',
    message: '已经是最后一章'
  });
    console.log('已经是最后一章')
  }
}

const goPrevChapter = async () => {
  try {
    await rendition.value.prev()
    viewerEl.value.scrollTop = 0
  } catch (err) {
    ElNotification.error({
    title: '错误',
    message: '已经是第一章'
  });
    console.log('已经是第一章')
  }
}
</script>

<style scoped>
.reader-container {
  position: relative;
  height: 100vh;
  background: #f5f5f5;
  display: grid;
  grid-template-columns: 250px 1fr;
  grid-template-rows: auto 1fr;
  grid-template-areas: 
    "sidebar header"
    "sidebar viewer";
}

/* 侧边栏样式 */
.sidebar {
  grid-area: sidebar;
  background-color: #ffffff;
  border-right: 1px solid #eaeaea;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #eaeaea;
  background: #f9f9f9;
}

.tab {
  flex: 1;
  padding: 14px 0;
  text-align: center;
  cursor: pointer;
  font-weight: 500;
  color: #666;
  transition: all 0.2s ease;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tab.active {
  color: #1a73e8;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 10%;
  right: 10%;
  height: 3px;
  background: #1a73e8;
  border-radius: 2px;
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
}

/* 搜索区域样式 */
.search-container {
  margin-bottom: 15px;
}

.search-box {
  margin-bottom: 10px;
}

.search-stats {
  font-size: 13px;
  color: #666;
  margin-bottom: 15px;
  text-align: center;
}

.search-results {
  flex: 1;
  overflow-y: auto;
}

.result-item {
  padding: 12px 15px;
  margin-bottom: 8px;
  background: #f9f9f9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.result-item:hover {
  background: #edf5ff;
  transform: translateX(3px);
  border-left-color: #1a73e8;
}

.result-item.active {
  background: #e6f7ff;
  border-left-color: #1a73e8;
  font-weight: 500;
}

.result-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 5px;
  color: #1a1a1a;
}

.result-excerpt {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.highlight {
  background-color: #ffeb3b;
  font-weight: 500;
  padding: 0 2px;
  border-radius: 2px;
}
::v-deep .highlight {
  background-color: #ffeb3b;
  color: #000;
  font-weight: bold;
}

.no-results {
  text-align: center;
  padding: 20px;
  color: #999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

/* 增强样式 */
.search-highlight {
  background-color: rgba(255, 235, 59, 0.5);
  border-bottom: 2px solid #ffc107;
}

.current-highlight {
  background-color: #ff9800 !important;
  border-bottom: 2px solid #e65100;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { background-color: rgba(255, 152, 0, 0.5); }
  50% { background-color: rgba(255, 152, 0, 0.8); }
  100% { background-color: rgba(255, 152, 0, 0.5); }
}

.search-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 15px;
  padding: 10px 0;
  border-top: 1px solid #eee;
}

.result-counter {
  font-size: 14px;
  color: #666;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-results {
    max-height: calc(100vh - 180px);
  }
}

/* 保留原有样式 */
.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-item {
  cursor: pointer;
  padding: 0.5rem 0.8rem;
  margin-bottom: 0.25rem;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 0.95rem;
  color: #444;
  position: relative;
  overflow: hidden;
}

.toc-item:hover {
  background-color: #f0f5ff;
  transform: translateX(3px);
}

.toc-item.active {
  background: linear-gradient(90deg, #e6f7ff, #d1ecff);
  font-weight: 600;
  color: #1a73e8;
  box-shadow: 0 2px 6px rgba(25, 118, 210, 0.15);
}

.toc-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #1a73e8;
  border-radius: 2px;
}

.level-0 {
  font-weight: 600;
  margin-top: 0.6rem;
  font-size: 1.05rem;
}

.level-1 {
  padding-left: 1.2rem;
}

.level-2 {
  padding-left: 2rem;
  font-size: 0.9rem;
  color: #666;
}

.reader-header {
  grid-area: header;
  background: rgba(255, 255, 255, 0.95);
  padding: 0.8rem 1.5rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 1.2rem;
  z-index: 100;
  position: sticky;
  top: 0;
}

.header-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.progress-container {
  width: 100%;
  height: 6px;
  background: #e9ecef;
  border-radius: 3px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05);
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #4dabf7, #1a73e8);
  border-radius: 3px;
  transition: width 0.3s cubic-bezier(0.34, 0.69, 0.1, 1);
  box-shadow: 0 2px 4px rgba(25, 118, 210, 0.2);
}

.chapter-title {
  font-size: 0.95rem;
  color: #495057;
  font-weight: 500;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-icon {
  font-size: 1.4rem;
  color: #1a73e8;
  transition: all 0.3s;
  background: #ffffff;
  padding: 0.5rem;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon:hover {
  color: white;
  background: #1a73e8;
  transform: scale(1.05);
}

#viewer {
  grid-area: viewer;
  height: 100%;
  overflow-y: auto;
  scroll-behavior: smooth;
  overscroll-behavior: contain;
  background: white;
  outline: none;
  position: relative;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 999;
  backdrop-filter: blur(4px);
}

.loader {
  border: 4px solid rgba(52, 152, 219, 0.2);
  border-radius: 50%;
  border-top: 4px solid #3498db;
  width: 50px;
  height: 50px;
  animation: spin 1s cubic-bezier(0.5, 0.1, 0.4, 0.9) infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-overlay p {
  margin-top: 1.2rem;
  font-size: 1.1rem;
  color: #2c3e50;
  font-weight: 500;
}

.error-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 1.8rem;
  text-align: center;
  background: white;
  border-radius: 12px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 5px 25px rgba(0, 0, 0, 0.15);
  border-left: 4px solid #dc3545;
}

.error-message button {
  margin-top: 1.2rem;
  padding: 0.7rem 1.5rem;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
  box-shadow: 0 4px 8px rgba(220, 53, 69, 0.25);
}

.error-message button:hover {
  background: #bb2d3b;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(220, 53, 69, 0.3);
}

/* 增强EPUB内容样式 */
:deep(.epub-container) {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  box-sizing: border-box;
}

:deep(body) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, 
               Ubuntu, Cantarell, "Open Sans", "Helvetica Neue", sans-serif;
  line-height: 1.8;
  color: #333;
  font-size: 18px;
  background: white;
}

:deep(h1) {
  color: #1a73e8;
  border-bottom: 2px solid #e6f7ff;
  padding-bottom: 0.5rem;
}

:deep(h2) {
  color: #2c3e50;
}

:deep(a) {
  color: #1a73e8;
  text-decoration: none;
}

:deep(a:hover) {
  text-decoration: underline;
}

.navigation-buttons {
  position: fixed;
  top: 50%;
  left: 250px;
  right: 0;
  transform: translateY(-50%);
  pointer-events: none;
  z-index: 999;
}

.nav-button {
  position: absolute;
  width: 60px;
  height: 120px;
  background: var(--el-color-primary);
  border: none;
  cursor: pointer;
  pointer-events: all;
  transition: all 0.3s;
  opacity: 0.7;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.nav-button:hover {
  opacity: 1;
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.prev-button {
  left: 0;
  border-radius: 0 8px 8px 0;
}

.next-button {
  right: 0;
  border-radius: 8px 0 0 8px;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .reader-container {
    grid-template-columns: 200px 1fr;
  }
  
  .navigation-buttons {
    left: 200px;
  }
}

@media (max-width: 768px) {
  .reader-container {
    grid-template-columns: 1fr;
    grid-template-areas: 
      "header"
      "viewer";
  }
  
  .sidebar {
    position: fixed;
    left: -250px;
    top: 0;
    bottom: 0;
    width: 250px;
    transition: transform 0.3s ease;
    z-index: 1000;
  }
  
  .sidebar.active {
    transform: translateX(250px);
    box-shadow: 0 0 25px rgba(0, 0, 0, 0.2);
  }
  
  .navigation-buttons {
    left: 0;
  }
  
  :deep(.epub-container) {
    padding: 1.2rem 1rem;
    max-width: 100%;
  }
  
  :deep(body) {
    font-size: 16px;
    line-height: 1.6;
  }
  
  .nav-button {
    width: 50px;
    height: 100px;
  }
  
  .chapter-title {
    font-size: 0.85rem;
  }
}

@media (max-width: 480px) {
  .reader-header {
    padding: 0.7rem 1rem;
  }
  
  .header-icon {
    font-size: 1.2rem;
    padding: 0.4rem;
    width: 32px;
    height: 32px;
  }
  
  .nav-button {
    width: 40px;
    height: 80px;
  }
  
  .progress-container {
    height: 4px;
  }
}
</style>