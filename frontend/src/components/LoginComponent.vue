<template>
  <div class="login-wrapper">
    <div class="login-container">
      <h1 class="login-title">登录</h1>
      <el-form 
        class="login-form"
        @submit.prevent="handleLogin"
        label-position="top"
      >
        <el-form-item label="账号">
          <el-input
            v-model="username"
            placeholder="请输入账号"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <div class="action-buttons">
          <el-button
            native-type="submit"
            type="primary"
            size="large"
            round
            style="flex: 1;"
            :loading="loading"
          >
            登录
          </el-button>
        </div>

        <div class="text-center mt-4">
          <span class="register-link" @click="goRegister">
            <!-- TODO -->
          </span>
        </div>

        <el-alert
          v-if="message"
          :title="message"
          :type="isSuccess ? 'success' : 'error'"
          class="message"
          show-icon
          :closable="false"
        />
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { User, Lock } from '@element-plus/icons-vue';

const router = useRouter();
const API_BASE = 'http:';

const username = ref("");
const password = ref("");
const message = ref("");
const isSuccess = ref(false);
const loading = ref(false);

const handleLogin = async () => {
  // 清空旧状态
  message.value = "";
  isSuccess.value = false;
  loading.value = true;

  // 前端验证
  if (!username.value.trim() || !password.value.trim()) {
    message.value = "账号和密码不能为空！";
    loading.value = false;
    return;
  }

  try {
    // 使用表单格式发送登录请求
    const formData = new URLSearchParams();
    formData.append('username', username.value);
    formData.append('password', password.value);

    // 发送登录请求
    const response = await axios.post(
      `${API_BASE}/api/auth/login`,
      formData,
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    );

    // 处理成功响应
    if (response.data && response.data.success) {
      isSuccess.value = true;
      message.value = "登录成功！";
      
      // 检查登录状态
      const statusResponse = await axios.get(`${API_BASE}/api/auth/status`);
      const userData = statusResponse.data;
      
      // 存储用户信息（仅存储用户名）
      if (userData.authenticated && userData.username) {
        localStorage.setItem('username', userData.username);
      }
      
      // 延迟 400ms 跳转，确保用户看到成功提示
      setTimeout(() => {
        router.push({ name: 'home' });
      }, 400);
    } else {
      isSuccess.value = false;
      message.value = response.data?.message || "登录失败，请重试";
    }
  } catch (error) {
    // 统一错误处理
    handleApiError(error);
  } finally {
    loading.value = false;
  }
};

const handleApiError = (error) => {
  isSuccess.value = false;
  
  if (error.response) {
    switch (error.response.status) {
      case 401:
        message.value = "账号或密码错误";
        break;
      case 403:
        message.value = "无访问权限";
        break;
      case 500:
        message.value = "服务器内部错误，请联系管理员";
        break;
      default:
        message.value = `请求错误：${error.response.status}`;
    }
    
    // 处理 Spring Security 返回的错误信息
    if (error.response.data && error.response.data.message) {
      message.value = error.response.data.message;
    }
  } else if (error.request) {
    message.value = "网络连接异常，请检查网络设置";
  } else {
    message.value = "请求失败，请重试";
  }
};

const goRegister = () => {
  // 暂时没有注册功能
  message.value = "注册功能尚未开放";
};
</script>

<style scoped>
.login-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2c3e50 0%, #4a6491 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 40px 35px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 10;
}

.login-title {
  color: #2c3e50;
  font-size: 24px;
  margin-bottom: 30px;
  font-weight: 600;
  letter-spacing: 1px;
  text-align: center;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #34495e;
  margin-bottom: 8px;
  display: block;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 2px 15px;
  height: 48px;
}

.login-form :deep(.el-input__inner) {
  height: 44px;
}

.action-buttons {
  margin-top: 25px;
  display: flex;
  justify-content: space-between;
}

.register-link {
  color: #409eff;
  cursor: pointer;
  transition: color 0.2s;
  font-size: 14px;
}

.register-link:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.message {
  margin-top: 20px;
}

/* 背景动画效果 */
.login-wrapper::before {
  content: "";
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  animation: rotate 20s linear infinite;
  transform-origin: center;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式调整 */
@media (max-width: 480px) {
  .login-container {
    padding: 30px 20px;
    margin: 0 15px;
  }
  
  .login-title {
    font-size: 20px;
    margin-bottom: 20px;
  }
  
  .login-form :deep(.el-input__wrapper) {
    height: 42px;
  }
  
  .login-form :deep(.el-input__inner) {
    height: 38px;
    font-size: 14px;
  }
  
  .el-button {
    height: 42px;
    font-size: 14px;
  }
}
</style>