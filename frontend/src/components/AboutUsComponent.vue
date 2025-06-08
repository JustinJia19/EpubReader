<template>
  <div class="app-container">
    <!-- 头部导航 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">EpubReader</h1>
        <nav class="nav-menu">
          <router-link to="/home" class="nav-link">首页</router-link>
        </nav>
      </div>
    </header>

    <div class="about-container">
      <div class="left-panel">
        <img src="../resource/logo.png" alt="logo" class="avatar" />
        <div class="info">
          <p><strong>About us</strong></p>
          <p>一个基于Web的轻量级epub阅读器</p>
          <p>无需下载，点击即可体验</p>
          <p>还有很多功能仍待拓展</p>
          <p>如果对这个demo感兴趣，请联系我！</p>
          <br>
          <p><strong>GitHub：</strong>https://github.com/JustinJia19/EpubReader</p>
          <p><strong>WeChat：</strong>Justin_Jia_</p>
          <p><strong>Mailbox：</strong>1662855715@qq.com</p>
        </div>
      </div>

      <div class="right-content">
        <div class="statistics-container">
          <div class="statistics-card">
            <div class="statistics-header">
              <h3>统计信息</h3>
            </div>
            <div class="statistics-content">
              <div class="total-count">
                <el-statistic :value="bookTotal" title="书籍总数" />
              </div>
              <div v-if="loading" class="chart-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                加载统计数据...
              </div>
              <div v-else class="chart-container">
                <!-- 使用 ref 获取 DOM 元素 -->
                <div ref="chartContainer" style="width: 100%; height: 400px;"></div>
                <div v-if="!hasData" class="no-data">
                  <el-empty description="暂无书籍数据" />
                </div>
              </div>
            </div>
            <div class = "publish-header">
              <h3>版权声明</h3>
            </div>
            <div class="publish-content">
              <p>欢迎使用 EpubReader 在线图书馆服务。为保障知识产权和各方合法权益，请您在使用本网站服务前仔细阅读以下声明：</p>
              <p><strong>用户上传内容责任自负</strong></p>
              <p>本网站为用户提供存储空间及在线阅读功能，所有书籍资源均由用户自行上传。上传者应对所上传内容的合法性、版权有效性及完整性负责，不得上传侵犯他人知识产权、隐私权或其他合法权益的内容。</p>
              <p><strong>平台不承担审核义务</strong></p>
              <p>本网站不主动审查用户上传的内容，但保留对上传内容进行适当管理与筛选的权利。同时，平台保证不在平台外使用用户上传的内容及书籍资料。</p>
              <p><strong>版权投诉机制</strong></p>
              <p>若您认为本网站上展示的内容侵犯了您的合法权益，请通过左侧的联系方式联系我们，我们将及时处理。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { onMounted, onUnmounted, ref, nextTick } from 'vue';
import axios from 'axios';
import { ElStatistic, ElIcon, ElEmpty, ElMessage } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';

let echarts = null;

export default {
  components: {
    ElStatistic,
    ElIcon,
    ElEmpty,
    Loading
  },
  setup() {
    const bookTotal = ref(0);
    const loading = ref(true);
    const hasData = ref(false);
    const chartContainer = ref(null);
    let chartInstance = null;
    let resizeObserver = null;

    // 加载 ECharts
    const loadECharts = async () => {
      if (!echarts) {
        try {
          const module = await import('echarts');
          echarts = module.default || module;
          console.log('ECharts 已加载', echarts);
        } catch (error) {
          console.error('加载 ECharts 失败:', error);
          ElMessage.error('加载图表库失败，请刷新页面重试');
          throw error;
        }
      }
      return echarts;
    };

    // 获取统计数据
    const fetchBookStatistics = async () => {
      try {
        loading.value = true;
        console.log('开始获取统计数据...');

        const response = await axios.get('/statistics', {
          baseURL: 'http://192.168.0.100:8082'
        });

        console.log('统计数据:', response.data);
        bookTotal.value = response.data.total;

        if (response.data.categories && response.data.categories.length > 0) {
          hasData.value = true;

          await nextTick();
          setTimeout(async () => {
            const echartsLib = await loadECharts();
            renderChart(response.data.categories, echartsLib);
          }, 100); // 延迟 100ms 等待 DOM 完全渲染

          // 确保 ECharts 已加载
          const echartsLib = await loadECharts();

          // 渲染图表
          renderChart(response.data.categories, echartsLib);
        } else {
          hasData.value = false;
          console.log('没有分类数据');
        }
      } catch (error) {
        console.error('获取书籍统计数据失败:', error);
        ElMessage.error('获取统计数据失败: ' + error.message);
        hasData.value = false;
      } finally {
        loading.value = false;
      }
    };

    // 渲染图表
const renderChart = async (categories, echartsLib) => {
        try {
          console.log('开始渲染图表');

          // 销毁已有实例
          if (chartInstance) {
            chartInstance.dispose();
            chartInstance = null;
          }

          // 异步获取容器，最多重试 10 次，每次间隔 500ms
          const getContainerWithRetry = async (retries = 10, delay = 500) => {
            let attempts = 0;
            while (attempts < retries) {
              const container = chartContainer.value;
              if (container) {
                return container;
              }
              attempts++;
              console.warn(`容器未就绪，第 ${attempts} 次重试...`);
              await new Promise(resolve => setTimeout(resolve, delay));
            }
            throw new Error('图表容器始终未找到');
          };

          // 获取容器（带重试）
          const container = await getContainerWithRetry().catch(err => {
            console.error(err);
            return null;
          });

          if (!container) {
            console.error('无法获取图表容器，停止渲染');
            return;
          }

          // 初始化图表
          chartInstance = echartsLib.init(container);
          console.log('图表实例已创建');

          // 配置项
          const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#3498db','#FFA500'];

          const option = {
            tooltip: { trigger: 'item' },
            legend: {
              top: '5%',
              left: 'center'
            },
            series: [{
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              data: categories.map((item, index) => ({
                value: item.count,
                name: item.name,
                itemStyle: { color: colors[index % colors.length] }
              })),
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }]
          };

          // 设置配置
          chartInstance.setOption(option);

          // 设置自适应大小
          setupResizeObserver(container);
        } catch (error) {
          console.error('图表渲染失败:', error);
          ElMessage.error('图表渲染失败');
        }
      };

    // 响应式调整
    const setupResizeObserver = (container) => {
      if (resizeObserver) {
        resizeObserver.disconnect();
      }

      resizeObserver = new ResizeObserver(() => {
        if (chartInstance) {
          setTimeout(() => chartInstance.resize(), 100);
        }
      });

      resizeObserver.observe(container);
    };

    // 组件卸载时清理资源
    onUnmounted(() => {
      if (chartInstance) {
        chartInstance.dispose();
        chartInstance = null;
      }

      if (resizeObserver) {
        resizeObserver.disconnect();
        resizeObserver = null;
      }
    });

    // 组件挂载
    onMounted(() => {
      console.log('组件已挂载');

      // 预加载 ECharts
      loadECharts().then(() => {
        console.log('ECharts 预加载完成');
      }).catch(err => {
        console.error('ECharts 预加载失败:', err);
      });

      // 获取统计数据
      fetchBookStatistics();
    });

    return {
      bookTotal,
      loading,
      hasData,
      chartContainer
    };
  }
};
</script>

<style scoped lang="scss">
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
  }
}

.about-container {
  flex: 1;
  display: flex;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;

  .left-panel {
    flex: 0 0 20%;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    border-right: 1px solid #eee;
    margin-bottom: 24px;
    background: #fff;
    padding: 24px;
    border-radius: 8px;
    box-shadow: var(--el-box-shadow-light);
  }

  .avatar {
    width: 240px;
    height: 100px;
    object-fit: cover;
    margin-bottom: 20px;
  }

  .info {
    font-size: 14px;
    line-height: 1.6;
  }

  .right-content {
    flex: 1;
    padding-left: 24px;
  }
}

.statistics-container {
  height: 100%;
  padding: 0 12px;
  
  .statistics-card {
    background: #fff;
    border-radius: 8px;
    box-shadow: var(--el-box-shadow-light);
    height: 100%;
    display: flex;
    flex-direction: column;
    
    .statistics-header {
      padding: 16px 24px;
      border-bottom: 1px solid #eee;
      
      h3 {
        margin: 0;
        font-size: 18px;
        color: var(--el-text-color-primary);
      }
    }

    .publish-header {
      padding: 16px 24px;
      border-bottom: 1px solid #eee;
      
      h3 {
        margin: 0;
        font-size: 18px;
        color: var(--el-text-color-primary);
      }
    }
    
    .statistics-content {
      flex: 1;
      padding: 24px;
      display: flex;
      flex-direction: column;
      position: relative; /* 为绝对定位容器提供参考 */
      
      .total-count {
        text-align: center;
        margin-bottom: 24px;
        
        :deep(.el-statistic__content) {
          font-size: 32px;
          font-weight: bold;
          color: var(--el-color-primary);
        }
      }

      
            
      .chart-loading {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        flex: 1;
        color: var(--el-color-info);
        
        .el-icon {
          font-size: 40px;
          margin-bottom: 10px;
          animation: rotating 2s linear infinite;
        }
      }
      
      .no-data {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative; /* 确保在容器上层 */
        z-index: 1; /* 确保在图表容器上层 */
      }
      
      .chart-container {
        flex: 1;
        min-height: 400px;
        position: relative; /* 为绝对定位容器提供参考 */
      }
    }
  }
}

.publish-content {
      flex: 1;
      padding: 24px;
      display: flex;
      flex-direction: column;
      position: relative; /* 为绝对定位容器提供参考 */
      color: rgb(89, 86, 86); 
    }

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
