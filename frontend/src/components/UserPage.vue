<template>
  <div class="user-page">
    <!-- 顶部导航栏 -->
    <el-header class="user-header">
      <div class="header-content">
        <div class="brand">
          <div class="mobile-menu-btn" @click="toggleDrawer">
            <el-icon size="24" color="#fff"><Menu /></el-icon>
          </div>
          <img src="../assets/icon.png" alt="XXG-KAMI-PRO" class="brand-icon">
          <span class="brand-text">XXG-KAMI-PRO 2.0</span>
          <el-tag type="success" size="small" class="user-badge" effect="plain">用户端</el-tag>
        </div>
        <div class="user-info">
          <el-dropdown>
            <span class="user-dropdown">
              <el-avatar :size="32" :src="localUserInfo.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ localUserInfo.username }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showProfile">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item @click="showSettings">
                  <el-icon><Setting /></el-icon>设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主体内容 -->
    <el-container class="main-container">
      <!-- 侧边栏 -->
      <el-aside class="sidebar" width="240px">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleMenuSelect"
          background-color="#ffffff"
          text-color="#606266"
          active-text-color="#409eff"
        >
          <el-menu-item index="dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="card-query">
            <el-icon><Search /></el-icon>
            <span>卡密查询</span>
          </el-menu-item>
          <el-menu-item index="usage-records">
            <el-icon><Document /></el-icon>
            <span>使用记录</span>
          </el-menu-item>
          <el-menu-item index="my-cards">
            <el-icon><CreditCard /></el-icon>
            <span>我的卡密</span>
          </el-menu-item>
          <el-menu-item index="purchase-cards">
            <el-icon><ShoppingCart /></el-icon>
            <span>购买卡密</span>
          </el-menu-item>
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区域 -->
      <el-main class="main-content">
        <!-- 仪表盘 -->
        <div v-if="activeMenu === 'dashboard'" class="content-section">
          <div class="section-header">
            <h2>仪表盘</h2>
            <p>欢迎回来，{{ localUserInfo.username }}！</p>
          </div>
          
          <!-- 统计卡片 -->
          <el-row :gutter="20" class="stats-row">
            <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
              <el-card class="stat-card" shadow="never">
                <div class="stat-content">
                  <div class="stat-icon total">
                    <el-icon><CreditCard /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-number">{{ stats.totalCards }}</div>
                    <div class="stat-label">总卡密数</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
              <el-card class="stat-card" shadow="never">
                <div class="stat-content">
                  <div class="stat-icon used">
                    <el-icon><Check /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-number">{{ stats.usedCards }}</div>
                    <div class="stat-label">已使用</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
              <el-card class="stat-card" shadow="never">
                <div class="stat-content">
                  <div class="stat-icon unused">
                    <el-icon><Clock /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-number">{{ stats.unusedCards }}</div>
                    <div class="stat-label">未使用</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
              <el-card class="stat-card" shadow="never">
                <div class="stat-content">
                  <div class="stat-icon expired">
                    <el-icon><Warning /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-number">{{ stats.expiredCards }}</div>
                    <div class="stat-label">已过期</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 最近使用记录 -->
          <el-card class="recent-records" shadow="never">
            <template #header>
              <div class="card-header">
                <span>最近使用记录</span>
                <el-button type="text" @click="activeMenu = 'usage-records'">查看全部</el-button>
              </div>
            </template>
            <el-table :data="recentRecords" style="width: 100%">
              <el-table-column prop="cardKey" label="卡密" width="200" show-overflow-tooltip />
              <el-table-column prop="useTime" label="使用时间" width="180" />
              <el-table-column prop="deviceId" label="设备ID" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'" size="small" effect="plain">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- 卡密查询 -->
        <div v-else-if="activeMenu === 'card-query'" class="content-section">
          <div class="section-header">
            <h2>卡密查询</h2>
            <p>查询卡密状态和详细信息</p>
          </div>
          
          <el-card shadow="never">
            <el-form :model="queryForm" label-width="100px">
              <el-form-item label="卡密">
                <el-input
                  v-model="queryForm.cardKey"
                  placeholder="请输入卡密"
                  clearable
                  style="width: 300px"
                />
                <el-button type="primary" @click="queryCard" style="margin-left: 10px">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
            
            <div v-if="queryResult" class="query-result">
              <el-descriptions title="卡密信息" :column="2" border>
                <el-descriptions-item label="卡密">{{ queryResult.cardKey }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusType(queryResult.status)" effect="plain">{{ getStatusText(queryResult.status) }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="类型">{{ queryResult.cardType === 'time' ? '时间卡' : '次数卡' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ queryResult.createTime }}</el-descriptions-item>
                <el-descriptions-item label="使用时间">{{ queryResult.useTime || '未使用' }}</el-descriptions-item>
                <el-descriptions-item label="过期时间">{{ queryResult.expireTime || '永久有效' }}</el-descriptions-item>
                <el-descriptions-item v-if="queryResult.cardType === 'time'" label="时长">{{ queryResult.duration }} 天</el-descriptions-item>
                <el-descriptions-item v-if="queryResult.cardType === 'count'" label="总次数">{{ queryResult.totalCount }}</el-descriptions-item>
                <el-descriptions-item v-if="queryResult.cardType === 'count'" label="剩余次数">{{ queryResult.remainingCount }}</el-descriptions-item>
                <el-descriptions-item label="机器码">{{ queryResult.machineCode || '未绑定' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </div>

        <!-- 使用记录 -->
        <div v-else-if="activeMenu === 'usage-records'" class="content-section">
          <div class="section-header">
            <h2>使用记录</h2>
            <p>查看所有卡密使用记录</p>
          </div>
          
          <el-card shadow="never">
            <el-table :data="paginatedRecords" style="width: 100%" v-loading="loading">
              <el-table-column prop="cardKey" label="卡密" width="200" show-overflow-tooltip />
              <el-table-column prop="useTime" label="使用时间" width="180" />
              <el-table-column prop="deviceId" label="设备ID" show-overflow-tooltip />
              <el-table-column prop="ipAddress" label="IP地址" width="140" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'" size="small" effect="plain">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            </el-table>
            
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </el-card>
        </div>

        <!-- 我的卡密 -->
        <div v-else-if="activeMenu === 'my-cards'" class="content-section">
          <div class="section-header">
            <h2>我的卡密</h2>
            <p>管理您拥有的所有卡密</p>
          </div>
          
          <el-card shadow="never">
            <el-table :data="myCards" style="width: 100%" v-loading="loading">
              <el-table-column prop="cardKey" label="卡密" width="200" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)" effect="plain">{{ getStatusText(scope.row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column prop="useTime" label="使用时间" width="180" />
              <el-table-column prop="expireTime" label="过期时间" width="180" />
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.status === 0"
                    type="primary"
                    size="small"
                    @click="useCard(scope.row)"
                  >
                    使用
                  </el-button>
                  <el-button
                    type="info"
                    size="small"
                    @click="viewCardDetail(scope.row)"
                  >
                    详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- 购买卡密页面 -->
        <div v-else-if="activeMenu === 'purchase-cards'" class="purchase-cards-page">
          <el-card class="page-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>购买卡密</span>
              </div>
            </template>
            
            <el-row :gutter="20">
              <!-- 时间卡购买 -->
              <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
                <el-card shadow="never" class="purchase-card">
                  <template #header>
                    <div class="purchase-card-header">
                      <el-icon><Timer /></el-icon>
                      <span>时间卡</span>
                    </div>
                  </template>
                  
                  <el-form label-width="80px">
                    <el-form-item label="时长">
                      <el-select v-model="selectedTimeCard" placeholder="请选择时长" style="width: 100%">
                        <el-option
                          v-for="option in timeCardOptions"
                          :key="option.id"
                          :label="option.description"
                          :value="option.id"
                        >
                          <span style="float: left">{{ option.description }}</span>
                          <span style="float: right; color: #8492a6; font-size: 13px">¥{{ option.price }}</span>
                        </el-option>
                      </el-select>
                    </el-form-item>
                    
                    <el-form-item label="数量">
                      <el-input-number 
                        v-model="timeCardQuantity" 
                        :min="1" 
                        :max="100"
                        style="width: 100%"
                      />
                    </el-form-item>
                    
                    <el-form-item label="总价">
                      <el-text size="large" type="danger">
                        ¥{{ selectedTimeCard ? (timeCardOptions.find(item => item.id === selectedTimeCard)?.price || 0) * timeCardQuantity : 0 }}
                      </el-text>
                    </el-form-item>
                    
                    <el-form-item label="支付方式">
                      <el-radio-group v-model="paymentMethod">
                        <el-radio label="alipay">支付宝</el-radio>
                        <el-radio label="wxpay">微信</el-radio>
                        <el-radio label="qqpay">QQ钱包</el-radio>
                      </el-radio-group>
                    </el-form-item>

                    


                    <el-form-item>
                      <el-button 
                        type="primary" 
                        :loading="purchaseLoading"
                        :disabled="!selectedTimeCard"
                        @click="purchaseCard('time')"
                        style="width: 100%"
                      >
                        <el-icon><ShoppingCart /></el-icon>
                        立即购买
                      </el-button>
                    </el-form-item>
                  </el-form>
                </el-card>
              </el-col>
              
              <!-- 次数卡购买 -->
              <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
                <el-card shadow="never" class="purchase-card">
                  <template #header>
                    <div class="purchase-card-header">
                      <el-icon><Tickets /></el-icon>
                      <span>次数卡</span>
                    </div>
                  </template>
                  
                  <el-form label-width="80px">
                    <el-form-item label="次数">
                      <el-select v-model="selectedCountCard" placeholder="请选择次数" style="width: 100%">
                        <el-option
                          v-for="option in countCardOptions"
                          :key="option.id"
                          :label="option.description"
                          :value="option.id"
                        >
                          <span style="float: left">{{ option.description }}</span>
                          <span style="float: right; color: #8492a6; font-size: 13px">¥{{ option.price }}</span>
                        </el-option>
                      </el-select>
                    </el-form-item>
                    
                    <el-form-item label="数量">
                      <el-input-number 
                        v-model="countCardQuantity" 
                        :min="1" 
                        :max="100"
                        style="width: 100%"
                      />
                    </el-form-item>
                    
                    <el-form-item label="总价">
                      <el-text size="large" type="danger">
                        ¥{{ selectedCountCard ? (countCardOptions.find(item => item.id === selectedCountCard)?.price || 0) * countCardQuantity : 0 }}
                      </el-text>
                    </el-form-item>
                    
                    <el-form-item label="支付方式">
                      <el-radio-group v-model="paymentMethod">
                        <el-radio label="alipay">支付宝</el-radio>
                        <el-radio label="wxpay">微信</el-radio>
                        <el-radio label="qqpay">QQ钱包</el-radio>
                      </el-radio-group>
                    </el-form-item>

                    <el-form-item>
                      <el-button 
                        type="primary" 
                        :loading="purchaseLoading"
                        :disabled="!selectedCountCard"
                        @click="purchaseCard('count')"
                        style="width: 100%"
                      >
                        <el-icon><ShoppingCart /></el-icon>
                        立即购买
                      </el-button>
                    </el-form-item>
                  </el-form>
                </el-card>
              </el-col>
            </el-row>
          </el-card>
          
          <!-- 购买记录 -->
          <el-card class="page-card" style="margin-top: 20px;" shadow="never">
            <template #header>
              <div class="card-header">
                <span>购买记录</span>
              </div>
            </template>
            
            <el-table :data="purchaseHistory" style="width: 100%">
              <el-table-column prop="orderNo" label="订单号" width="180" />
              <el-table-column prop="cardType" label="卡密类型" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.cardType === 'time' ? 'primary' : 'success'" effect="plain">
                    {{ scope.row.cardType === 'time' ? '时间卡' : '次数卡' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="specification" label="规格" width="120" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="totalPrice" label="总价" width="100">
                <template #default="scope">
                  <el-text type="danger">¥{{ scope.row.totalPrice }}</el-text>
                </template>
              </el-table-column>
              <el-table-column prop="purchaseTime" label="购买时间" width="160" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getOrderStatusType(scope.row.status)" effect="plain">
                    {{ getOrderStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="viewOrderDetail(scope.row)"
                  >
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- 个人信息 -->
        <div v-else-if="activeMenu === 'profile'" class="content-section">
          <div class="section-header">
            <h2>个人信息</h2>
            <p>管理您的个人资料和账户设置</p>
          </div>
          
          <el-row :gutter="20">
            <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
              <el-card shadow="never">
                <template #header>
                  <span>头像设置</span>
                </template>
                <div class="avatar-section">
                  <el-avatar :size="120" :src="localUserInfo.avatar">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                  <el-upload
                    class="avatar-uploader"
                    action="#"
                    :show-file-list="false"
                    :auto-upload="false"
                    :on-change="handleAvatarUpload"
                    style="margin-top: 20px;"
                  >
                    <el-button type="primary">更换头像</el-button>
                  </el-upload>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
              <el-card shadow="never">
                <template #header>
                  <span>基本信息</span>
                </template>
                <el-form :model="profileForm" label-width="100px">
                  <el-form-item label="用户名">
                    <el-input v-model="localUserInfo.username" disabled />
                  </el-form-item>
                  <el-form-item label="昵称">
                    <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
                  </el-form-item>
                  <el-form-item label="邮箱">
                    <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                  </el-form-item>
                  <el-form-item label="手机号">
                    <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="updateProfile">保存修改</el-button>
                  </el-form-item>
                </el-form>
              </el-card>

              <el-card shadow="never" style="margin-top: 20px;">
                <template #header>
                  <span>{{ localUserInfo.hasPassword ? '修改密码' : '设置密码' }}</span>
                </template>
                <el-form :model="passwordForm" label-width="100px">
                  <el-form-item label="旧密码" v-if="localUserInfo.hasPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
                  </el-form-item>
                  <el-form-item label="新密码">
                    <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
                  </el-form-item>
                  <el-form-item label="确认新密码">
                    <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">{{ localUserInfo.hasPassword ? '修改密码' : '设置密码' }}</el-button>
                  </el-form-item>
                </el-form>
              </el-card>

              <el-card shadow="never" style="margin-top: 20px;">
                <template #header>
                  <span>社交账号绑定</span>
                </template>
                <div v-if="loadingSocial" class="loading-text">加载中...</div>
                <div v-else>
                  <div class="social-list">
                    <!-- QQ -->
                     <div v-if="oauthLoginTypes.qq" class="social-item">
                        <div class="social-info">
                          <span class="social-name">QQ</span>
                        </div>
                        <div class="social-action">
                          <template v-if="isBound('qq')">
                             <el-tag type="success" style="margin-right: 10px;">已绑定</el-tag>
                             <el-button type="primary" size="small" @click="handleBindSocial('qq')">更换绑定</el-button>
                             <el-button type="danger" size="small" @click="handleUnbindSocial('qq')">解绑</el-button>
                          </template>
                          <el-button v-else type="primary" size="small" @click="handleBindSocial('qq')">绑定</el-button>
                        </div>
                     </div>
                      <!-- WeChat -->
                     <div v-if="oauthLoginTypes.wx" class="social-item">
                        <div class="social-info">
                          <span class="social-name">微信</span>
                        </div>
                        <div class="social-action">
                          <template v-if="isBound('wx')">
                             <el-tag type="success" style="margin-right: 10px;">已绑定</el-tag>
                             <el-button type="primary" size="small" @click="handleBindSocial('wx')">更换绑定</el-button>
                             <el-button type="danger" size="small" @click="handleUnbindSocial('wx')">解绑</el-button>
                          </template>
                          <el-button v-else type="primary" size="small" @click="handleBindSocial('wx')">绑定</el-button>
                        </div>
                     </div>
                      <!-- Alipay -->
                     <div v-if="oauthLoginTypes.alipay" class="social-item">
                        <div class="social-info">
                          <span class="social-name">支付宝</span>
                        </div>
                        <div class="social-action">
                          <template v-if="isBound('alipay')">
                             <el-tag type="success" style="margin-right: 10px;">已绑定</el-tag>
                             <el-button type="primary" size="small" @click="handleBindSocial('alipay')">更换绑定</el-button>
                             <el-button type="danger" size="small" @click="handleUnbindSocial('alipay')">解绑</el-button>
                          </template>
                          <el-button v-else type="primary" size="small" @click="handleBindSocial('alipay')">绑定</el-button>
                        </div>
                     </div>
                  </div>
                  <div v-if="!oauthLoginTypes.qq && !oauthLoginTypes.wx && !oauthLoginTypes.alipay" class="no-social">
                    暂无支持的社交登录方式
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 系统设置 -->
        <div v-else-if="activeMenu === 'settings'" class="content-section">
          <UserSettingsPage />
        </div>
      </el-main>
    </el-container>

    <!-- 移动端侧边栏抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="240px"
      :with-header="false"
      class="mobile-drawer"
      append-to-body
      :z-index="3000"
    >
      <div class="drawer-menu-container">
        <div class="drawer-brand">
          <img src="../assets/icon.png" alt="Logo" class="brand-icon">
          <span class="brand-text">XXG-KAMI-PRO</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="(index) => { handleMenuSelect(index); drawerVisible = false; }"
          background-color="#ffffff"
          text-color="#606266"
          active-text-color="#409eff"
        >
          <el-menu-item index="dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="card-query">
            <el-icon><Search /></el-icon>
            <span>卡密查询</span>
          </el-menu-item>
          <el-menu-item index="usage-records">
            <el-icon><Document /></el-icon>
            <span>使用记录</span>
          </el-menu-item>
          <el-menu-item index="my-cards">
            <el-icon><CreditCard /></el-icon>
            <span>我的卡密</span>
          </el-menu-item>
          <el-menu-item index="purchase-cards">
            <el-icon><ShoppingCart /></el-icon>
            <span>购买卡密</span>
          </el-menu-item>
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <!-- 卡密详情对话框 -->
    <el-dialog v-model="cardDetailVisible" title="卡密详情" width="500px">
      <el-descriptions :column="1" border v-if="selectedCard">
        <el-descriptions-item label="卡密">{{ selectedCard.cardKey }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(selectedCard.status)" effect="plain">{{ getStatusText(selectedCard.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="类型">{{ selectedCard.cardType === 'time' ? '时间卡' : '次数卡' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ selectedCard.createTime }}</el-descriptions-item>
        <el-descriptions-item label="使用时间">{{ selectedCard.useTime || '未使用' }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ selectedCard.expireTime || '无' }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedCard.cardType === 'time'" label="时长">{{ selectedCard.duration }} 天</el-descriptions-item>
        <el-descriptions-item v-if="selectedCard.cardType === 'count'" label="总次数">{{ selectedCard.totalCount }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedCard.cardType === 'count'" label="剩余次数">{{ selectedCard.remainingCount }}</el-descriptions-item>
        <el-descriptions-item label="机器码">{{ selectedCard.machineCode || '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="验证方式">{{ selectedCard.verifyMethod || 'web' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="cardDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userProfileApi, cardApi, pricingApi, orderApi, settingsApi } from '../services/api.js'
import UserSettingsPage from './UserSettingsPage.vue'
import logger from '../utils/logger'

// Props 和 Emits
const props = defineProps({
  userInfo: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['logout'])

// 响应式数据
const activeMenu = ref('dashboard')
const drawerVisible = ref(false)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
// 使用记录总数（响应式）
const total = computed(() => usageRecords.value.length)

// 用户信息 - 使用props传入的数据，并监听props变化保持同步
const localUserInfo = reactive({
  id: props.userInfo.id,
  username: props.userInfo.username || '普通用户',
  avatar: props.userInfo.avatar || '',
  email: props.userInfo.email || 'user@example.com',
  phone: props.userInfo.phone || '138****8888',
  hasPassword: props.userInfo.hasPassword
})

// 监听 props.userInfo 变化，同步到本地 reactive 对象
watch(() => props.userInfo, (newVal) => {
  if (newVal) {
    Object.assign(localUserInfo, {
      id: newVal.id,
      username: newVal.username || localUserInfo.username,
      avatar: newVal.avatar || localUserInfo.avatar,
      email: newVal.email || localUserInfo.email,
      phone: newVal.phone || localUserInfo.phone,
      hasPassword: newVal.hasPassword
    })
  }
}, { deep: true })

// 统计数据
const stats = reactive({
  totalCards: 0,
  usedCards: 0,
  unusedCards: 0,
  expiredCards: 0
})

// 查询表单
const queryForm = reactive({
  cardKey: ''
})

// 查询结果
const queryResult = ref(null)

// 卡密详情对话框
const cardDetailVisible = ref(false)
const selectedCard = ref(null)

// 个人资料表单
const profileForm = reactive({
  nickname: '',
  email: '',
  phone: ''
})

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

// 社交绑定
const socialBindings = ref([])
const loadingSocial = ref(false)
const oauthLoginTypes = reactive({
  qq: false,
  wx: false,
  alipay: false
})

// 最近使用记录
const recentRecords = ref([])

// 使用记录
const usageRecords = ref([])

// 我的卡密
const myCards = ref([])

// 购买卡密相关数据
const timeCardOptions = ref([])
const countCardOptions = ref([])
const selectedTimeCard = ref(null)
const selectedCountCard = ref(null)
const timeCardQuantity = ref(1)
const countCardQuantity = ref(1)
const paymentMethod = ref('alipay')
const purchaseHistory = ref([])
const purchaseLoading = ref(false)

// 轮询订单状态（保存 interval 引用以便组件卸载时清理）
let pollTimer = null

// 修改密码
const handleChangePassword = async () => {
  if (localUserInfo.hasPassword && !passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  passwordLoading.value = true
  try {
    const res = await userProfileApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    if (res.success) {
       ElMessage.success(localUserInfo.hasPassword ? '密码修改成功' : '密码设置成功')
       passwordForm.oldPassword = ''
       passwordForm.newPassword = ''
       passwordForm.confirmPassword = ''
       // Refresh profile to update hasPassword status
       fetchUserProfile();
    } else {
       ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    passwordLoading.value = false
  }
}

// 获取社交绑定列表
const fetchSocialBindings = async () => {
  loadingSocial.value = true
  try {
    const res = await userProfileApi.getSocialBindings()
    if (res.success) {
      socialBindings.value = res.data || []
    }
  } catch (e) {
    logger.error(e)
  } finally {
    loadingSocial.value = false
  }
}

// 获取OAuth配置
const fetchOAuthSettings = async () => {
   try {
     const res = await settingsApi.getAllSettings()
     if (res.success && res.data) {
         const types = res.data.oauth_login_types || ''
         oauthLoginTypes.qq = types.includes('qq')
         oauthLoginTypes.wx = types.includes('wx')
         oauthLoginTypes.alipay = types.includes('alipay')
     }
   } catch (e) {
     logger.error(e)
   }
}

const isBound = (type) => {
   return socialBindings.value.some(b => b.socialType === type)
}

const handleBindSocial = (type) => {
   sessionStorage.setItem('binding_mode', 'true');
   const apiUrl = import.meta.env.VITE_API_BASE_URL || '/api';
   window.location.href = `${apiUrl}/oauth/login/${type}`;
}

const handleUnbindSocial = async (type) => {
   try {
     await ElMessageBox.confirm('确定要解绑该账号吗？', '提示', {
       type: 'warning',
       confirmButtonText: '确定',
       cancelButtonText: '取消'
     })
     const res = await userProfileApi.unbindSocial(type)
     if (res.success) {
        ElMessage.success('解绑成功')
        fetchSocialBindings()
     } else {
        ElMessage.error(res.message || '解绑失败')
     }
   } catch (e) {
     if (e !== 'cancel') ElMessage.error(e.message || '解绑失败')
   }
}

// 获取用户资料
const fetchUserProfile = async () => {
    try {
        const result = await userProfileApi.getProfile();
        if (result && result.success && result.data) {
            Object.assign(localUserInfo, result.data);
            Object.assign(profileForm, {
                nickname: result.data.nickname,
                email: result.data.email,
                phone: result.data.phone
            });
        } else {
            ElMessage.error(result?.message || '获取个人信息失败');
        }
    } catch (error) {
        logger.error('Failed to fetch user profile:', error);
        ElMessage.error('获取个人信息失败');
    }
}

// 更新用户资料
const updateProfile = async () => {
    try {
        const result = await userProfileApi.updateProfile(profileForm);
        if (result.success) {
            ElMessage.success('个人信息更新成功');
            await fetchUserProfile();
        } else {
            ElMessage.error(result.message || '更新失败');
        }
    } catch (error) {
        logger.error('Failed to update profile:', error);
        ElMessage.error('更新失败');
    }
}

// 上传头像
const handleAvatarUpload = async (file) => {
    try {
        const isJPG = file.raw.type === 'image/jpeg' || file.raw.type === 'image/png';
        const isLt2M = file.raw.size / 1024 / 1024 < 2;

        if (!isJPG) {
            ElMessage.error('上传头像图片只能是 JPG/PNG 格式!');
            return false;
        }
        if (!isLt2M) {
            ElMessage.error('上传头像图片大小不能超过 2MB!');
            return false;
        }

        const result = await userProfileApi.uploadAvatar(file.raw);
        if (result.success) {
            ElMessage.success('头像上传成功');
            localUserInfo.avatar = result.url;
        } else {
            ElMessage.error(result.message || '头像上传失败');
        }
    } catch (error) {
        logger.error('Failed to upload avatar:', error);
        ElMessage.error('头像上传失败');
    }
}

const fetchOrders = async () => {
    try {
        const userId = localUserInfo.id || 1;
        const timestamp = new Date().getTime();

        const [ordersResult, cardsResult] = await Promise.all([
            orderApi.getOrders(),
            cardApi.getUserCards(userId)
        ]);

        if (Array.isArray(ordersResult)) {
            purchaseHistory.value = ordersResult.map(order => ({
                orderNo: order.order_id,
                cardType: order.card_type,
                specification: order.card_spec,
                quantity: order.quantity,
                totalPrice: order.total_price,
                purchaseTime: order.purchase_time,
                status: order.status,
                paymentMethod: order.payment_method,
                cardKeys: order.card_keys
            }));
        }

        if (cardsResult && cardsResult.success && Array.isArray(cardsResult.data)) {
            myCards.value = cardsResult.data.map(card => ({
                cardKey: card.card_key,
                status: card.status,
                createTime: card.create_time,
                useTime: card.use_time,
                expireTime: card.expire_time
            }));

            const usedCardsList = cardsResult.data
                .filter(card => card.status === 1);

            usageRecords.value = usedCardsList
                .map(card => ({
                    cardKey: card.card_key,
                    useTime: card.use_time,
                    deviceId: card.device_id || 'Unknown',
                    ipAddress: card.ip_address || '-',
                    status: '成功',
                    remark: '正常使用'
                }))
                .sort((a, b) => new Date(b.useTime) - new Date(a.useTime));

            stats.totalCards = cardsResult.data.length;
            stats.usedCards = usedCardsList.length;
            stats.unusedCards = cardsResult.data.filter(card => card.status === 0).length;
            stats.expiredCards = cardsResult.data.filter(card => card.status === 2).length;

            recentRecords.value = usageRecords.value.slice(0, 5);
        } else {
            logger.warn('Failed to fetch card details, falling back to orders');
            const cards = [];
            if (Array.isArray(ordersResult)) {
                ordersResult.forEach(order => {
                    if (order.card_keys) {
                        const keys = order.card_keys.split(',');
                        keys.forEach(key => {
                            cards.push({
                                cardKey: key,
                                status: 0,
                                createTime: order.purchase_time,
                                useTime: null,
                                expireTime: null
                            });
                        });
                    }
                });
                myCards.value = cards;
            }
        }
    } catch (e) {
        logger.error("Failed to fetch data", e);
        ElMessage.error("获取数据失败");
    }
};

const fetchPricing = async () => {
  try {
    const result = await pricingApi.getAllPricing();
    if (result.success && result.data) {
      timeCardOptions.value = result.data.timeCards || [];
      countCardOptions.value = result.data.countCards || [];
    }
  } catch (e) {
    logger.error("Failed to fetch pricing", e);
    ElMessage.error("获取定价信息失败");
  }
}

const pollOrderStatus = async (orderId) => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }

  await fetchOrders();

  const maxAttempts = 20;
  let attempts = 0;

  pollTimer = setInterval(async () => {
    attempts++;
    if (attempts > maxAttempts) {
      clearInterval(pollTimer);
      pollTimer = null;
      return;
    }

    await fetchOrders();

    const order = purchaseHistory.value.find(o => o.orderNo === orderId);
    if (order && order.status === 'completed') {
      clearInterval(pollTimer);
      pollTimer = null;
      ElMessageBox.alert(
        `订单号：${order.orderNo}\n购买成功！\n\n您可以在"我的卡密"中查看和使用卡密。`,
        '支付成功',
        {
          confirmButtonText: '查看卡密',
          type: 'success',
          callback: () => {
            activeMenu.value = 'my-cards';
          }
        }
      );
    }
  }, 3000);
}

// 组件卸载前清理轮询定时器，防止内存泄漏
onBeforeUnmount(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
})

onMounted(() => {
    fetchOrders();
    fetchUserProfile();
    fetchPricing();
    fetchSocialBindings();
    fetchOAuthSettings();

    let paymentStatus = null;

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('payment') === 'success') {
        paymentStatus = 'success';
    }

    if (!paymentStatus && window.location.hash.includes('?')) {
        const hashQuery = window.location.hash.split('?')[1];
        const hashParams = new URLSearchParams(hashQuery);
        if (hashParams.get('payment') === 'success') {
            paymentStatus = 'success';
        }
    }

    if (paymentStatus === 'success') {
         fetchOrders().then(() => {
             const latestOrder = purchaseHistory.value[0];
             if (latestOrder) {
                 if (latestOrder.status === 'completed') {
                     ElMessageBox.alert(
                        `订单号：${latestOrder.orderNo}\n购买成功！\n\n您可以在"我的卡密"中查看和使用卡密。`,
                        '支付成功',
                        {
                          confirmButtonText: '查看卡密',
                          type: 'success',
                          callback: () => {
                            activeMenu.value = 'my-cards';
                          }
                        }
                      );
                 } else if (latestOrder.status === 'pending') {
                     pollOrderStatus(latestOrder.orderNo);
                 }
             }
         });
    }
})

// 方法
const toggleDrawer = () => {
  drawerVisible.value = !drawerVisible.value
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
}

const queryCard = async () => {
  if (!queryForm.cardKey) {
    ElMessage.warning('请输入卡密')
    return
  }

  loading.value = true
  try {
    const result = await cardApi.queryCard(queryForm.cardKey)
    if (result.success) {
      queryResult.value = result.data
      ElMessage.success('查询成功')
    } else {
      ElMessage.error(result.message || '查询失败')
      queryResult.value = null
    }
  } catch (error) {
    logger.error(error)
    ElMessage.error(error.message || '查询失败')
    queryResult.value = null
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '未使用', 1: '已使用', 2: '已停用' }
  return texts[status] || '未知'
}

const useCard = (card) => {
  ElMessageBox.confirm('确定要使用这张卡密吗？', '确认使用', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const result = await cardApi.useCard(card.cardKey);
      if (result.success) {
        ElMessage.success('卡密使用成功');
        fetchOrders();
      } else {
        ElMessage.error(result.message || '使用失败');
      }
    } catch (error) {
      logger.error(error);
      ElMessage.error(error.message || '使用失败');
    }
  })
}

const viewCardDetail = (card) => {
  selectedCard.value = card
  cardDetailVisible.value = true
}

const showProfile = () => {
  activeMenu.value = 'profile'
}

const showSettings = () => {
  activeMenu.value = 'settings'
}

const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '确认退出', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('已退出登录')
    emit('logout')
  }).catch(() => {
    // 用户取消退出
  })
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 分页后的使用记录
const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return usageRecords.value.slice(start, start + pageSize.value)
})

// 购买卡密相关方法
const purchaseCard = async (cardType) => {
  const selectedOption = cardType === 'time' ? selectedTimeCard.value : selectedCountCard.value
  const quantity = cardType === 'time' ? timeCardQuantity.value : countCardQuantity.value

  if (!selectedOption) {
    ElMessage.warning('请选择卡密规格')
    return
  }

  const option = cardType === 'time'
    ? timeCardOptions.value.find(item => item.id === selectedOption)
    : countCardOptions.value.find(item => item.id === selectedOption)

  const totalPrice = option.price * quantity

  try {
    await ElMessageBox.confirm(
      `确认购买 ${quantity} 张 ${option.description}？\n总价：¥${totalPrice}`,
      '确认购买',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    purchaseLoading.value = true
    ElMessage.info('正在处理支付...')

    const createOrderData = {
      userId: localUserInfo.id || 1,
      username: localUserInfo.username,
      cardType: cardType,
      cardSpec: option.description,
      pricingId: option.id,
      quantity: quantity,
      paymentMethod: paymentMethod.value,
      email: localUserInfo.email
    }

    const result = await orderApi.createOrder(createOrderData)

    if (result.success) {
      if (result.paymentUrl) {
        const url = result.paymentUrl;
        const isRelativeUrl = url.startsWith('/') && !url.startsWith('//');
        let isTrustedUrl = isRelativeUrl;
        if (!isRelativeUrl) {
          try {
            const parsed = new URL(url);
            isTrustedUrl = parsed.origin === window.location.origin;
          } catch (e) {
            isTrustedUrl = false;
          }
        }
        if (!isTrustedUrl) {
          ElMessage.error('支付链接异常，请联系管理员');
          return;
        }
        ElMessage.success('订单创建成功，正在跳转支付...');
        pollOrderStatus(result.data.order_id);

        window.location.href = url;
        return;
      }

      ElMessage.success(result.message)
      await fetchOrders();

      if (cardType === 'time') {
        selectedTimeCard.value = null
        timeCardQuantity.value = 1
      } else {
        selectedCountCard.value = null
        countCardQuantity.value = 1
      }

      ElMessageBox.alert(
        `订单号：${result.data.order_id}\n购买成功！\n支付金额：¥${result.data.total_price}\n\n系统已发送订单通知邮件到您的邮箱：${localUserInfo.email}`,
        '购买成功',
        {
          confirmButtonText: '确定',
          type: 'success'
        }
      )
    } else {
      ElMessage.error(result.message || '购买失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      logger.error(error)
      ElMessage.error('购买过程中发生错误: ' + (error.message || '未知错误'))
    }
  } finally {
    purchaseLoading.value = false
  }
}

const getOrderStatusType = (status) => {
  const types = {
    'pending': 'warning',
    'paid': 'success',
    'completed': 'success',
    'cancelled': 'danger',
    'refunded': 'info'
  }
  return types[status] || 'info'
}

const getOrderStatusText = (status) => {
  const texts = {
    'pending': '待支付',
    'paid': '已支付',
    'completed': '已完成',
    'cancelled': '已取消',
    'refunded': '已退款'
  }
  return texts[status] || '未知'
}

const viewOrderDetail = (order) => {
  ElMessageBox.alert(
    `订单号：${order.orderNo}\n卡密类型：${order.cardType === 'time' ? '时间卡' : '次数卡'}\n规格：${order.specification}\n数量：${order.quantity}\n总价：¥${order.totalPrice}\n购买时间：${order.purchaseTime}`,
    '订单详情',
    {
      confirmButtonText: '确定'
    }
  )
}
</script>

<style scoped>
.user-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.user-header {
  background: #2c3e50; /* Flat dark color */
  color: white;
  padding: 0;
  height: 60px !important;
  line-height: 60px;
  border-bottom: 1px solid #1f2d3d;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.brand-text {
  font-size: 18px;
  font-weight: 600;
}

.user-badge {
  margin-left: 10px;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-dropdown:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.username {
  color: white;
  font-weight: 500;
}

/* 主容器 */
.main-container {
  flex: 1;
  height: calc(100vh - 60px);
}

/* 侧边栏 */
.sidebar {
  background-color: #ffffff;
  border-right: 1px solid #e6e6e6;
}

.sidebar-menu {
  border: none;
  height: 100%;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 6px; /* Small radius */
}

.sidebar-menu .el-menu-item:hover {
  background-color: #f5f7fa;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #ecf5ff;
  color: #409eff !important;
}

/* 主内容区域 */
.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

.content-section {
  max-width: 1200px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.section-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: #dcdfe6;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

/* Flat colors for stats */
.stat-icon.total {
  background-color: #409eff;
}

.stat-icon.used {
  background-color: #67c23a;
}

.stat-icon.unused {
  background-color: #909399;
}

.stat-icon.expired {
  background-color: #f56c6c;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

/* 最近记录卡片 */
.recent-records {
  border-radius: 6px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 查询结果 */
.query-result {
  margin-top: 20px;
}

/* 头像设置 */
.avatar-section {
  text-align: center;
  padding: 20px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}

/* 响应式设计 */
.mobile-menu-btn {
  display: none;
  margin-right: 15px;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: background 0.3s;
  z-index: 200; /* 确保在最上层 */
}

.mobile-menu-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.mobile-menu-btn:active {
  background: rgba(255, 255, 255, 0.2);
}

.drawer-menu-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.drawer-brand {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e6e6e6;
}

@media (max-width: 768px) {
  .sidebar {
    display: none !important;
  }
  
  .mobile-menu-btn {
    display: flex; /* 改为 flex 布局 */
    align-items: center;
    justify-content: center;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .stats-row .el-col {
    margin-bottom: 10px;
  }
  
  .header-content {
    padding: 0 10px;
  }
  
  .user-header .brand-text {
    display: none;
  }
  
  /* 
  .sidebar-menu .el-menu-item span {
    display: none;
  }
  */
}

@media (max-width: 480px) {
  .user-dropdown .username {
    display: none;
  }
}

.social-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.social-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.social-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.social-name {
  font-weight: 500;
}

.social-action {
  display: flex;
  align-items: center;
}

.no-social {
  color: #909399;
  text-align: center;
  padding: 20px;
}
</style>
