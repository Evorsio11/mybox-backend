# MyBox API Postman Collection

这是一个基于 OpenAPI 3.1.0 文档自动生成的 Postman 测试集合。

## 📁 文件结构

```
postman/
├── collections/
│   └── mybox.postman_collection.json    # API 接口集合
├── environments/
│   └── MyBox Environment.postman_environment.json  # 开发环境配置
├── openapi.json                          # OpenAPI 3.1.0 文档
└── README.md                             # 本文件
```

## 🚀 快速开始

### 1. 导入 Collection

1. 打开 Postman
2. 点击 "Import" 按钮
3. 选择 `collections/mybox.postman_collection.json`
4. 点击导入

### 2. 导入环境变量

1. 在 Postman 中点击右上角的 "Manage Environments" 图标
2. 点击 "Import"
3. 选择 `environments/MyBox Environment.postman_environment.json`
4. 选择 "MyBox 开发环境" 作为当前活动环境

## 📋 API 接口分组

### 用户认证 (3个接口)
- **用户注册** - POST /api/auth/register
- **用户登录** - POST /api/auth/login
- **刷新令牌** - POST /api/auth/refresh

### 用户管理 (1个接口)
- **获取当前用户信息** - GET /api/users/me

### 文件管理 (8个接口)
- **列出文件** - GET /api/files
- **列出已删除文件** - GET /api/files/deleted
- **上传文件** - POST /api/files/upload
- **下载文件** - POST /api/files/download
- **删除文件** - POST /api/files/delete
- **恢复文件** - POST /api/files/restore
- **获取文件配置** - GET /api/files/config
- **更新文件配置** - PUT /api/files/config

### 分片上传 (6个接口)
- **初始化分片上传** - POST /api/files/chunk/init
- **上传分片** - POST /api/files/chunk/upload
- **合并分片** - POST /api/files/chunk/merge
- **恢复上传** - GET /api/files/chunk/resume/{uploadId}
- **获取上传进度** - GET /api/files/chunk/progress/{uploadId}
- **取消上传** - DELETE /api/files/chunk/cancel

## 🔑 环境变量说明

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| baseUrl | API 基础URL | http://localhost:8080 |
| AccessToken | 访问令牌（登录后自动保存） | - |
| RefreshToken | 刷新令牌（登录后自动保存） | - |
| devUserName | 开发用户名 | devUser |
| devUserPassword | 开发用户密码 | 123abc |
| fileId | 文件ID（运行时动态设置） | - |
| uploadId | 上传会话ID（运行时动态设置） | - |
| chunkSize | 分片大小（运行时动态设置） | - |
| totalChunks | 总分片数（运行时动态设置） | - |

## 🎯 使用流程

### 认证流程

1. **注册新用户**（可选）
   ```
   POST /api/auth/register
   ```

2. **用户登录**
   ```
   POST /api/auth/login
   ```
   - 登录成功后，`AccessToken` 和 `RefreshToken` 会自动保存到环境变量
   - 所有需要认证的接口都会自动使用 `{{AccessToken}}` 作为 Bearer Token

3. **刷新令牌**（当 Access Token 过期时）
   ```
   POST /api/auth/refresh
   ```

### 文件操作流程

1. **上传文件**
   ```
   POST /api/files/upload
   ```
   - 选择一个或多个文件
   - 上传成功后记录返回的 `fileId`

2. **列出文件**
   ```
   GET /api/files
   ```
   - 查看所有已上传的文件

3. **下载文件**
   ```
   POST /api/files/download
   ```
   - 使用 `{{fileId}}` 变量或手动输入文件ID

4. **删除文件**
   ```
   POST /api/files/delete
   ```
   - 软删除，文件可以恢复

5. **恢复文件**
   ```
   POST /api/files/restore
   ```
   - 从回收站恢复已删除的文件

### 分片上传流程（用于大文件）

1. **初始化上传**
   ```
   POST /api/files/chunk/init
   ```
   - 提供文件名、大小和类型
   - 成功后 `uploadId`、`chunkSize`、`totalChunks` 会自动保存到环境变量

2. **上传分片**（重复多次）
   ```
   POST /api/files/chunk/upload
   ```
   - 每次上传一个分片
   - 需要提供 `uploadId` 和 `chunkNumber`

3. **合并分片**
   ```
   POST /api/files/chunk/merge
   ```
   - 所有分片上传完成后调用
   - 合并后会生成完整的文件

4. **监控进度**（可选）
   ```
   GET /api/files/chunk/progress/{uploadId}
   ```
   - 实时查看上传进度

5. **取消上传**（如果需要）
   ```
   DELETE /api/files/chunk/cancel
   ```

## 🧪 测试脚本

Collection 中已包含自动化测试脚本：

### 用户登录测试
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (response.success && response.data) {
        pm.environment.set("AccessToken", response.data.accessToken);
        pm.environment.set("RefreshToken", response.data.refreshToken);
        console.log("登录成功，Token 已保存到环境变量");
    }
}
```

### 分片上传初始化测试
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (response.success && response.data) {
        pm.environment.set("uploadId", response.data.uploadId);
        pm.environment.set("chunkSize", response.data.chunkSize);
        pm.environment.set("totalChunks", response.data.totalChunks);
        console.log("上传初始化成功，uploadId:", response.data.uploadId);
    }
}
```

## 📝 注意事项

1. **认证**：除了注册和登录接口，所有其他接口都需要在 Header 中携带 `Authorization: Bearer {{AccessToken}}`

2. **设备信息**：登录和注册时建议提供设备信息，用于设备管理和多端登录控制

3. **文件配置**：更新文件配置接口仅对 ADMIN 用户开放

4. **分片上传**：
   - 适用于大文件上传（默认 >10MB）
   - 支持断点续传
   - 支持并发上传（最多3个并发）

5. **响应格式**：所有接口返回统一的 `ApiResponse<T>` 格式
   ```json
   {
     "success": true,
     "code": "SUCCESS",
     "message": "操作成功",
     "data": { ... },
     "timestamp": 1234567890
   }
   ```

## 🔗 相关链接

- [OpenAPI 文档](./openapi.json)
- [项目源码](../)
- [SpringDoc UI](http://localhost:8080/swagger-ui.html)

## 📅 更新记录

- **2026-01-01**: 基于 OpenAPI 3.1.0 文档更新
  - 新增 18 个 API 接口
  - 优化环境变量配置
  - 添加自动化测试脚本
  - 完善接口分组和描述
