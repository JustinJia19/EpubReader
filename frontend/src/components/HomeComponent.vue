<template>
  <div class="app-container">
    <!-- 头部导航 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">
          <router-link to="/">EpubReader</router-link>
        </h1>
        <nav class="nav-menu">
          <router-link to="/about-us" class="nav-link">关于我们</router-link>
          <!-- 用户状态区域 -->
          <div class="user-status">
            <span v-if="isAuthenticated" class="welcome-msg">
              欢迎，{{ username }}
            </span>
            <el-button 
              v-if="isAuthenticated"
              @click="handleLogout" 
              class="logout-button">
              登出
            </el-button>
            <router-link 
              v-else 
              to="/login" 
              class="login-link">
              登录
            </router-link>
          </div>
        </nav>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="main-content">
      <!-- 登录状态提示 -->
      <div v-if="!isAuthenticated" class="auth-prompt">
        <el-alert title="提示" type="warning" :closable="false" show-icon>
          您当前处于<strong>游客模式</strong>，仅可阅读书籍。
          <router-link to="/login">立即登录</router-link>以使用完整功能。
        </el-alert>
      </div>
      
      <!-- 搜索和操作区域 -->
      <div class="action-bar">
        <el-form :model="searchForm" inline class="search-form">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8" :lg="6">
              <el-form-item label="书名">
                <el-input 
                  v-model="searchForm.title" 
                  placeholder="输入书名" 
                  clearable 
                  class="search-input"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6">
              <el-form-item label="作者">
                <el-input 
                  v-model="searchForm.author" 
                  placeholder="输入作者" 
                  clearable 
                  class="search-input"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6">
              <el-form-item label="分类" class="form-item-category">
                <el-select
                  v-model="searchForm.category"
                  placeholder="选择分类"
                  clearable
                  class="category-select"
                >
                  <el-option
                    v-for="item in categoryOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6">
              <el-form-item>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
                <el-button @click="resetSearch">重置</el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        
        <div class="action-buttons">
          <!-- 添加书籍按钮 - 仅登录用户可见 -->
          <el-button 
            v-if="isAuthenticated"
            type="primary" 
            icon="Plus" 
            @click="handleCreate">
            添加新书
          </el-button>
        </div>
      </div>

      <!-- 书籍表格 -->
      <el-table 
        :data="paginatedBookList" 
        border 
        style="width: 100%"
        class="data-table"
        v-loading="loading"
      >
        <el-table-column label="封面" width="120" align="center">
          <template #default="{ row }">
            <div class="cover-container">
              <el-image 
                :src="row.coverUrl" 
                fit="cover" 
                class="cover-image"
                @click="previewCover(row.coverUrl)"
              >
                <template #error>
                  <div class="cover-error">
                    <el-icon><Picture /></el-icon>
                    <span>封面加载失败</span>
                  </div>
                </template>
              </el-image>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="书名" min-width="150" />
        <el-table-column prop="author" label="作者" width="120" />
        <!-- 描述列添加固定高度和滚动 -->
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <div class="scrollable-description">
              {{ row.description }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            {{ categoryMap[row.category] }}
          </template>
        </el-table-column>
        <el-table-column label="电子书文件" min-width="200">
          <template #default="{ row }">
            <a target="_blank">
              {{ row.epubFileName }}
            </a>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <!-- 阅读按钮始终可见 -->
            <el-button 
              size="small" 
              type="primary" 
              @click="handleRead(row)"
              class="action-button"
            >
              阅读
            </el-button>
            
            <!-- 编辑按钮 - 仅登录用户可见 -->
            <el-button 
              v-if="isAuthenticated"
              size="small" 
              @click="handleEdit(row)"
              class="action-button"
            >
              编辑
            </el-button>
            
            <!-- 删除按钮 - 仅登录用户可见 -->
            <el-button 
              v-if="isAuthenticated"
              size="small" 
              type="danger" 
              @click="handleDelete(row.id)"
              class="action-button"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="bookList.length"
        v-model:current-page="currentPage"
        class="pagination-container"
      />

      <!-- 添加/编辑对话框 -->
      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑书籍' : '添加书籍'"
        width="600px"
        class="book-dialog"
      >
        <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
          <!-- 新增封面预览区域 -->
          <el-form-item label="封面">
            <div class="cover-preview-container">
              <el-image 
                :src="form.coverUrl" 
                fit="cover" 
                class="cover-preview"
              >
                <template #error>
                  <div class="cover-preview-error">
                    <el-icon><Picture /></el-icon>
                    <span>封面未上传</span>
                  </div>
                </template>
              </el-image>
            </div>
          </el-form-item>
          
          <el-form-item label="书名" prop="title">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="作者" prop="author">
            <el-input v-model="form.author" />
          </el-form-item>
          <el-form-item label="分类" prop="category" class="form-item-category">
            <el-select 
              v-model="form.category" 
              placeholder="请选择分类" 
              clearable
              class="category-select"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="form.description" type="textarea" />
          </el-form-item>
          <el-form-item label="电子书" prop="epubFileName">
            <el-upload
              class="upload-demo"
              :action="`${API_BASE}/upload`"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :show-file-list="false"
            >
              <el-button type="primary">
                {{ form.epubFileName ? '重新上传' : '点击上传EPUB' }}
              </el-button>
              <div class="el-upload__tip" v-if="form.epubFileName">
                已选文件：{{ form.epubFileName }}
              </div>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">确认</el-button>
          </span>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'

const router = useRouter()
const API_BASE = 'http://192.168.0.100:8082'

// 分类映射
const categoryMap = {
  FICTION: '小说',
  TECHNOLOGY: '科学技术',
  HISTORY: '历史人文',
  DETECTIVE: '推理小说',
  BIOGRAPHY: '人物传记',
  OTHER: '其他分类'
}

const categoryOptions = Object.keys(categoryMap).map((key) => ({
  value: key,
  label: categoryMap[key]
}))

// 数据
const bookList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const isAuthenticated = ref(false) // 登录状态标志
const username = ref('') // 当前登录用户名

// 分页相关数据
const currentPage = ref(1)
const pageSize = ref(5)

const searchForm = reactive({
  title: '',
  author: '',
  category: ''
})

const form = reactive({
  id: null,
  title: '',
  author: '',
  description: '',
  category: '',
  epubFileName: '',
  coverUrl: '' // 新增封面URL字段
})

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  epubFileName: [{ required: true, message: '请上传电子书文件', trigger: 'change' }]
}

// 计算属性 - 分页后的数据
const paginatedBookList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return bookList.value.slice(start, end)
})

// 生命周期钩子
onMounted(() => {
  checkAuthStatus().then(() => {
    fetchBooks();
  }).catch(() => {
    fetchBooks(); // 即使认证失败也允许游客访问书籍列表
  });
})

// 检查登录状态
const checkAuthStatus = async () => {
  try {
    const response = await axios.get(`${API_BASE}/api/auth/status`)
    
    // 确保后端返回格式正确
    if (response.data && typeof response.data.authenticated === 'boolean') {
      isAuthenticated.value = response.data.authenticated
      username.value = response.data.username || ''
    } else {
      console.error('无效的认证状态响应:', response.data)
      isAuthenticated.value = false
    }
  } catch (error) {
    // 处理 401 错误
    if (error.response && error.response.status === 401) {
      isAuthenticated.value = false
    } else {
      console.error('认证检查失败:', error)
      // ElMessage.error('检查登录状态失败')
    }
  }
}

// 登出处理
const handleLogout = async () => {
  try {
    await axios.post(`${API_BASE}/api/auth/logout`)
    isAuthenticated.value = false
    username.value = ''
    ElMessage.success('已成功登出')
    
    // 刷新页面以清除状态
    router.go(0)
  } catch (error) {
    ElMessage.error('登出失败: ' + error.message)
  }
}

// 方法
const fetchBooks = async () => {
  try {
    loading.value = true
    const response = await axios.get(`${API_BASE}/getall`, {
      params: searchForm
    })

    bookList.value = response.data.map(book => ({
      ...book,
      coverUrl: book.coverImagePath 
        ? `${API_BASE}/covers/${book.coverImagePath}?t=${Date.now()}` 
        : `${API_BASE}/covers/default-cover.jpg`
    }))

    console.log('书籍列表:', bookList.value)

  } catch (error) {
    ElMessage.error('获取书籍失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchBooks()
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.author = ''
  searchForm.category = ''
  currentPage.value = 1
  fetchBooks()
}

const handleCreate = () => {
  // 双重验证：确保用户已登录
  if (!isAuthenticated.value) {
    ElMessage.warning('请登录后再操作')
    router.push('/login')
    return
  }
  
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleRead = (row) => {
  const encodedName = encodeURIComponent(row.epubFileName)
  window.location.href = `/reader?file=${encodedName}`
}



const handleEdit = (row) => {
  // 双重验证：确保用户已登录
  if (!isAuthenticated.value) {
    ElMessage.warning('请登录后再操作')
    router.push('/login')
    return
  }
  
  isEdit.value = true
  Object.assign(form, {
    ...row,
    coverUrl: row.coverUrl // 确保封面URL也被复制
  })
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    // 双重验证：确保用户已登录
    if (!isAuthenticated.value) {
      ElMessage.warning('请登录后再操作')
      return
    }
    
    await ElMessageBox.confirm('确认删除该书籍吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await axios.delete(`${API_BASE}/delebook?id=${id}`)
    ElMessage.success('删除成功')
    fetchBooks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const submitForm = async () => {
  try {
    if (!isAuthenticated.value) {
      ElMessage.warning('请登录后再操作')
      return
    }
    
    await formRef.value.validate()

    // 添加会话验证
    await validateSession();
    
    let response
    if (isEdit.value) {
      response = await axios.put(`${API_BASE}/upbook`, form, {
        headers: {
          'X-Session-Validate': 'true'
        }
      })
      console.log('API 响应:', response.data)
    } else {
      response = await axios.post(`${API_BASE}/addbook`, form, {
        headers: {
          'X-Session-Validate': 'true'
        }
      })
      console.log('API 响应:', response.data)
    }

    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
    dialogVisible.value = false
    fetchBooks()
  } catch (error) {
    handleApiError(error);
  }
  
}

// 新增加的会话验证方法
const validateSession = async () => {
  try {
    await axios.get(`${API_BASE}/api/auth/status`, {
      timeout: 5000
    });
  } catch (error) {
    if (error.response && error.response.status === 401) {
      isAuthenticated.value = false;
      throw new Error('会话已过期，请重新登录');
    }
    throw error;
  }
}

// 统一的错误处理方法
const handleApiError = (error) => {
  console.error('操作错误:', error)
  
  if (error.message.includes('会话已过期')) {
    ElMessage.warning(error.message);
    router.push('/login');
    return;
  }
  
  if (error.response) {
    switch (error.response.status) {
      case 401:
        ElMessage.warning('会话已过期，请重新登录')
        isAuthenticated.value = false
        router.push('/login');
        break
      case 403:
        ElMessage.error('权限不足，无法执行此操作')
        break
      case 500:
        ElMessage.error('服务器错误: ' + (error.response.data?.message || '内部错误'))
        break
      default:
        ElMessage.error('操作失败: ' + error.message)
    }
  } else {
    ElMessage.error('网络错误: ' + error.message)
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    title: '',
    author: '',
    description: '',
    category: '',
    epubFileName: '',
    coverUrl: ''
  })
}

const formatTime = (timeStr) => {
  return new Date(timeStr).toLocaleString()
}

const handleUploadSuccess = (res) => {
  try {
    console.log('上传响应:', res)
    
    if (!res || !res.fileName) {
      throw new Error('服务器响应无效')
    }
    
    form.epubFileName = res.fileName
    
    const metadata = res.metadata || {}
    
    if (!form.title.trim()) form.title = metadata.title || '未命名书籍'
    if (!form.author.trim()) form.author = metadata.author || '未知作者'
    if (!form.description.trim()) form.description = metadata.description || ''
    
    // 设置封面URL（用于预览）
    if (metadata.coverImagePath) {
      form.coverUrl = `${API_BASE}/covers/${metadata.coverImagePath}`
      
      // 设置 coverImagePath 字段，直接传给后端
      form.coverImagePath = metadata.coverImagePath
    } else {
      form.coverUrl = `${API_BASE}/covers/default-cover.jpg`
      form.coverImagePath = "default-cover.jpg"
    }
    
    ElMessage.success('EPUB解析成功，已自动填充信息')
  } catch (error) {
    console.error('上传处理错误:', error)
    ElMessage.error(`处理上传结果失败: ${error.message}`)
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败')
}

const previewCover = (coverUrl) => {
  // 防止重复创建预览窗口
  const existingPreview = document.querySelector('.custom-cover-preview')
  if (existingPreview) {
    return
  }

  // 创建遮罩层
  const mask = document.createElement('div')
  mask.className = 'custom-cover-preview'
  mask.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.7);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999;
    transition: opacity 0.3s ease;
    opacity: 0;
  `

  // 创建图片元素
  const img = document.createElement('img')
  img.src = coverUrl
  img.alt = '封面预览'
  img.style.cssText = `
    max-width: 90%;
    max-height: 90%;
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    transition: opacity 0.3s ease;
    opacity: 0;
  `

  // 图片加载完成后渐显
  img.onload = () => {
    setTimeout(() => {
      mask.style.opacity = '1'
      img.style.opacity = '1'
    }, 50)
  }

  // 点击遮罩或图片关闭预览
  const closePreview = () => {
    mask.style.opacity = '0'
    img.style.opacity = '0'
    setTimeout(() => {
      if (document.body.contains(mask)) {
        document.body.removeChild(mask)
      }
    }, 300)
  }

  mask.appendChild(img)
  document.body.appendChild(mask)

  // 添加点击关闭事件
  mask.addEventListener('click', closePreview)
  img.addEventListener('click', (e) => e.stopPropagation()) // 阻止冒泡到 mask
}
</script>

<style scoped lang="scss">
/* 分页样式 */
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 限制描述列高度并添加滚动条 */
.scrollable-description {
  max-height: 150px; /* 固定高度 */
  overflow-y: auto;  /* 垂直滚动 */
  padding-right: 8px; /* 避免滚动条遮挡文字 */
  word-break: break-word; /* 允许单词内断行 */
}

/* 优化滚动条样式 */
.scrollable-description::-webkit-scrollbar {
  width: 6px;
}
.scrollable-description::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.app-header {
  background-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
  padding: 0 24px;
  
  .header-content {
    max-width: 1400px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 64px;
  }

  .logo {
    margin: 0;
    font-size: 24px;
    color: #fff;
    
    a {
      color: #fff;
      text-decoration: none;
      transition: opacity 0.3s;

      &:hover {
        opacity: 0.8;
      }
    }
  }

  .nav-menu {
    display: flex;
    align-items: center;
    gap: 32px;

    .nav-link {
      color: rgba(255,255,255,0.9);
      text-decoration: none;
      font-size: 14px;
      transition: color 0.3s;

      &:hover {
        color: #fff;
      }

      &.router-link-active {
        color: #fff;
        font-weight: 500;
      }
    }
    
    /* 用户状态样式 */
    .user-status {
      margin-left: 20px;
      display: flex;
      align-items: center;
      
      .welcome-msg {
        margin-right: 15px;
        color: rgba(255,255,255,0.9);
        font-weight: 500;
      }
      
      .login-link, .logout-button {
        padding: 6px 12px;
        border-radius: 4px;
        transition: all 0.3s;
      }
      
      .login-link {
        color: rgba(255,255,255,0.9);
        border: 1px solid rgba(255,255,255,0.7);
        text-decoration: none;
        
        &:hover {
          background-color: rgba(255,255,255,0.9);
          color: var(--el-color-primary);
        }
      }
      
      .logout-button {
        background-color: transparent;
        color: rgba(255,255,255,0.9);
        border: 1px solid rgba(255,255,255,0.7);
        cursor: pointer;
        
        &:hover {
          background-color: #e74c3c;
          border-color: #e74c3c;
          color: #fff;
        }
      }
    }
  }
}

.main-content {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.action-bar {
  margin-bottom: 24px;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
  
  // 搜索表单统一控制
  .search-form {
    :deep(.el-select) {
      width: 100%;  // 强制撑满栅格列
      min-width: 180px;  // 设置最小宽度
    }

    // 针对分类选择框的特定调整
    .el-col:nth-child(3) { // 第三个栅格列（分类所在位置）
      :deep(.el-select) {
        width: 100%;
        min-width: 200px; // 比输入框略宽
      }
    }

    // 保证所有表单元素宽度一致
    :deep(.el-input) {
      width: 100%;
    }
    
    .search-input {
      :deep(.el-input__inner) {
        height: 40px;
      }
    }
  }
}

.search-form {
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
  
  .category-select {
    :deep(.el-input__inner) {
      height: 40px;
    }
  }
}

.action-buttons {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.data-table {
  margin-top: 24px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--el-box-shadow-light);
  
  :deep(th) {
    background-color: var(--el-color-primary-light-9);
  }
  
  a {
    color: var(--el-color-primary);
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
  
  .action-button {
    margin: 0 4px;
  }
  
  /* 新增封面样式 */
  .cover-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 120px;
    cursor: pointer;
    transition: transform 0.3s ease;
    
    &:hover {
      transform: scale(1.05);
    }
  }
  
  .cover-image {
    width: 80px;
    height: 110px;
    border-radius: 4px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.1);
    background-color: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    border: 1px solid var(--el-border-color-light);
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .cover-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    padding: 5px;
    
    .el-icon {
      font-size: 24px;
      margin-bottom: 5px;
      color: var(--el-text-color-placeholder);
    }
  }
}

.auth-prompt {
  margin-bottom: 20px;
  
  .el-alert {
    border-radius: 8px;
    background-color: #fdf6ec;
  }
  
  a {
    color: var(--el-color-primary);
    text-decoration: none;
    margin-left: 5px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.form-item-category {
  :deep(.el-select) {
    width: 240px;
  }
}

/* 书籍对话框样式 */
.book-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
  }
  
  .cover-preview-container {
    display: flex;
    justify-content: center;
    margin-bottom: 20px;
  }
  
  .cover-preview {
    width: 120px;
    height: 170px;
    border-radius: 4px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.1);
    background-color: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--el-border-color-light);
    overflow: hidden;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .cover-preview-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    padding: 10px;
    
    .el-icon {
      font-size: 32px;
      margin-bottom: 8px;
      color: var(--el-text-color-placeholder);
    }
  }
}

@media (max-width: 768px) {
  .app-header {
    padding: 0 16px;
    
    .logo {
      font-size: 20px;
    }
    
    .nav-menu {
      gap: 16px;
      
      .user-status {
        margin-left: 10px;
        
        .welcome-msg {
          font-size: 12px;
          margin-right: 8px;
        }
        
        .login-link, .logout-button {
          padding: 4px 8px;
          font-size: 12px;
        }
      }
    }
  }
  
  .main-content {
    padding: 16px;
  }
  
  .action-bar {
    padding: 16px;
  }
  
  .el-col {
    margin-bottom: 12px;
  }
  
  .form-item-category {
    :deep(.el-select) {
      width: 100%;
      min-width: auto;
    }
  }
  
  /* 移动端封面调整 */
  .data-table {
    .cover-container {
      height: 90px;
    }
    
    .cover-image {
      width: 60px;
      height: 85px;
    }
  }
  
  .book-dialog {
    .cover-preview {
      width: 100px;
      height: 140px;
    }
  }
}

/* 封面预览模态框 */
.cover-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.85);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  cursor: pointer;
  
  img {
    max-width: 80%;
    max-height: 80%;
    border-radius: 4px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  }
}
</style>
