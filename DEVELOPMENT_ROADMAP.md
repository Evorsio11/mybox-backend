# MyBox 个人云盘开发路线图

> **项目定位**: 个人云盘应用 - 用户在各设备上访问存储在其中一个设备上的文件
>
> **核心目标**: 实现跨设备文件同步、设备管理、文件共享等云盘核心功能

---

## 目录
- [1. 项目概述](#1-项目概述)
- [2. 总体架构规划](#2-总体架构规划)
- [3. 开发阶段规划](#3-开发阶段规划)
- [4. P0阶段：核心云盘功能](#4-p0阶段核心云盘功能)
- [5. P1阶段：用户体验提升](#5-p1阶段用户体验提升)
- [6. P2阶段：安全与稳定性](#6-p2阶段安全与稳定性)
- [7. P3阶段：性能优化与高级功能](#7-p3阶段性能优化与高级功能)
- [8. 技术选型建议](#8-技术选型建议)
- [9. 测试策略](#9-测试策略)
- [10. 部署与上线](#10-部署与上线)

---

## 1. 项目概述

### 1.1 当前状态评估

**已实现功能** ✅
- 基础文件CRUD（上传、下载、删除、恢复）
- 用户认证系统（JWT + Spring Security）
- 文件去重（SHA-256）
- 分片上传（大文件支持）
- Range断点续传
- 文件大小限制
- 用户隔离（ownerId）

**核心差距** ❌
1. **无设备管理** - 无法识别和追踪设备
2. **无文件同步** - 设备间无法自动更新
3. **无文件夹** - 文件平铺存储
4. **无分享功能** - 无法与他人分享
5. **无版本控制** - 多设备编辑可能丢失数据
6. **性能问题** - 无分页、无索引

### 1.2 开发目标

**短期目标（1-2个月）**
- 实现基础云盘能力（设备管理、文件夹、分享）
- 完善基础功能（分页、搜索、索引）
- 系统稳定运行

**中期目标（3-4个月）**
- 实现文件同步机制
- 实现版本控制
- 提升用户体验

**长期目标（6个月+）**
- 实现离线支持
- 性能优化
- 企业级功能

---

## 2. 总体架构规划

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         前端层                                │
│  Web应用 / 桌面客户端 / 移动应用 / 浏览器扩展                 │
└─────────────────────────────────────────────────────────────┘
                              │
                         HTTP/HTTPS
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         网关层                                │
│  API Gateway / 负载均衡 / 限流 / 鉴权                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       应用服务层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │文件服务  │  │设备服务  │  │同步服务  │  │分享服务  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │用户服务  │  │通知服务  │  │搜索服务  │                 │
│  └──────────┘  └──────────┘  └──────────┘                 │
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   PostgreSQL │    │     Redis    │    │    MinIO     │
│  (元数据存储) │    │   (缓存/队列) │    │  (文件存储)  │
└──────────────┘    └──────────────┘    └──────────────┘
```

### 2.2 核心模块划分

**新增模块**
1. **设备管理模块** (`device`)
   - 设备注册、认证、状态管理
   - 设备配额管理

2. **文件夹模块** (`folder`)
   - 目录树管理
   - 文件夹操作（CRUD、移动）

3. **文件同步模块** (`sync`)
   - 文件变更通知（WebSocket）
   - 增量同步
   - 冲突检测与解决

4. **文件分享模块** (`share`)
   - 分享链接生成
   - 权限控制
   - 访问统计

5. **版本控制模块** (`version`)
   - 文件版本管理
   - 版本对比
   - 版本回退

### 2.3 数据库设计

**核心表结构**

```sql
-- 设备表
CREATE TABLE devices (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(20) NOT NULL, -- mobile, desktop, tablet
    device_id VARCHAR(200) UNIQUE NOT NULL, -- 设备唯一标识
    status VARCHAR(20) DEFAULT 'OFFLINE', -- ONLINE, OFFLINE
    last_sync_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_owner_device (owner_id, device_id)
);

-- 文件夹表
CREATE TABLE folders (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    parent_id UUID, -- 父文件夹ID，NULL表示根目录
    name VARCHAR(255) NOT NULL,
    path VARCHAR(1000) NOT NULL, -- 完整路径，如 "/Documents/Photos"
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (owner_id, parent_id, name),
    INDEX idx_owner_parent (owner_id, parent_id),
    INDEX idx_path (path)
);

-- 文件表（修改）
ALTER TABLE files ADD COLUMN folder_id UUID;
ALTER TABLE files ADD COLUMN device_id UUID;
ALTER TABLE files ADD INDEX idx_folder (folder_id);
ALTER TABLE files ADD INDEX idx_device (device_id);

-- 文件版本表
CREATE TABLE file_versions (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    version_number INT NOT NULL,
    object_name VARCHAR(500) NOT NULL,
    size BIGINT NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    INDEX idx_file_version (file_id, version_number)
);

-- 文件分享表
CREATE TABLE file_shares (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    folder_id UUID, -- 支持文件夹分享
    created_by UUID NOT NULL,
    share_token VARCHAR(100) UNIQUE NOT NULL,
    permission VARCHAR(20) NOT NULL, -- READ, WRITE
    expire_at TIMESTAMP,
    access_limit INT,
    access_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_token (share_token),
    INDEX idx_file (file_id)
);

-- 文件访问日志表
CREATE TABLE file_access_logs (
    id UUID PRIMARY KEY,
    file_id UUID,
    owner_id UUID NOT NULL,
    device_id UUID,
    access_type VARCHAR(20) NOT NULL, -- UPLOAD, DOWNLOAD, VIEW
    access_time TIMESTAMP NOT NULL,
    INDEX idx_owner_time (owner_id, access_time),
    INDEX idx_file_time (file_id, access_time)
);

-- 同步状态表
CREATE TABLE sync_states (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL,
    last_sync_time TIMESTAMP NOT NULL,
    sync_cursor VARCHAR(500), -- 同步游标
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (device_id)
);
```

---

## 3. 开发阶段规划

### 3.1 阶段划分

| 阶段 | 优先级 | 目标 | 预估工期 |
|------|--------|------|----------|
| **P0** | 核心功能 | 实现基础云盘能力 | 4-6周 |
| **P1** | 体验提升 | 提升跨设备体验 | 3-4周 |
| **P2** | 安全稳定 | 保证系统稳定运行 | 2-3周 |
| **P3** | 性能优化 | 企业级功能 | 4-6周 |

### 3.2 里程碑

- **M1 (P0完成)**: 用户可以在多个设备上管理文件和文件夹
- **M2 (P1完成)**: 设备间可以自动同步文件
- **M3 (P2完成)**: 系统安全稳定，可以小范围试运行
- **M4 (P3完成)**: 性能优化完成，可以正式上线

---

## 4. P0阶段：核心云盘功能

**目标**: 实现基础云盘能力，让用户可以在多设备上管理文件

### 4.1 任务清单

#### 任务1: 设备管理功能 (1周)

**子任务**:
1. 创建Device实体和Repository
2. 实现设备注册API
3. 实现设备列表查询API
4. 实现设备删除/撤销API
5. 实现设备在线状态管理（心跳机制）
6. 前端设备管理页面

**技术要点**:
```java
// Device实体
@Entity
@Table(name = "devices")
@Data
public class Device {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @Column(unique = true, nullable = false)
    private String deviceId; // 客户端生成的唯一标识

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    private LocalDateTime lastSyncTime;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// 设备注册接口
POST /api/devices/register
{
    "deviceName": "My iPhone",
    "deviceType": "mobile",
    "deviceId": "unique-device-uuid"
}

// 设备列表
GET /api/devices

// 设备删除
DELETE /api/devices/{deviceId}

// 设备心跳（保持在线状态）
POST /api/devices/heartbeat
```

**验收标准**:
- [ ] 用户可以注册多个设备
- [ ] 可以查看所有已注册设备
- [ ] 可以删除设备
- [ ] 设备在线状态准确（5分钟内无心跳为离线）

---

#### 任务2: 文件夹功能 (1.5周)

**子任务**:
1. 创建Folder实体和Repository
2. 实现文件夹CRUD API
3. 实现文件夹移动API
4. 实现文件夹树查询API
5. 修改File实体，添加folder_id字段
6. 数据迁移（现有文件关联到根目录）
7. 前端文件夹树展示

**技术要点**:
```java
// Folder实体
@Entity
@Table(name = "folders",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ownerId", "parentId", "name"})
    }
)
@Data
public class Folder {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    private UUID parentId; // NULL表示根目录

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path; // "/Documents/Photos"

    private Integer sortOrder = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// 文件夹创建
POST /api/folders
{
    "name": "Photos",
    "parentId": "folder-uuid" // null表示根目录
}

// 文件夹树
GET /api/folders/tree

// 文件夹移动
PUT /api/folders/{folderId}/move
{
    "targetParentId": "new-parent-uuid"
}

// 文件上传到指定文件夹
POST /api/files/upload?folderId={folderId}
```

**验收标准**:
- [ ] 可以创建文件夹（支持嵌套）
- [ ] 可以移动文件夹
- [ ] 可以删除文件夹（级联删除或阻止）
- [ ] 文件可以上传到指定文件夹
- [ ] 前端可以展示文件夹树

---

#### 任务3: 文件分享功能 (1周)

**子任务**:
1. 创建FileShare实体和Repository
2. 实现分享链接生成API
3. 实现分享链接访问API
4. 实现分享链接管理（查询、删除）
5. 实现分享权限控制
6. 前端分享管理页面

**技术要点**:
```java
// FileShare实体
@Entity
@Table(name = "file_shares")
@Data
public class FileShare {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID fileId;

    private UUID folderId; // 支持文件夹分享

    @Column(nullable = false)
    private UUID createdBy;

    @Column(unique = true, nullable = false)
    private String shareToken; // 随机生成

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermission permission;

    private LocalDateTime expireAt;

    private Integer accessLimit;

    @Column(nullable = false)
    private Integer accessCount = 0;

    @CreatedDate
    private LocalDateTime createdAt;
}

// 创建分享
POST /api/shares
{
    "fileId": "file-uuid",
    "permission": "READ",
    "expireDays": 7,
    "accessLimit": 10
}

// 访问分享（无需登录）
GET /api/shares/{shareToken}

// 查询我的分享
GET /api/shares

// 取消分享
DELETE /api/shares/{shareId}
```

**验收标准**:
- [ ] 可以为文件/文件夹生成分享链接
- [ ] 分享链接可以设置过期时间和访问次数
- [ ] 分享链接可以设置权限（只读/读写）
- [ ] 可以查看和管理所有分享
- [ ] 可以取消分享

---

#### 任务4: 分页和搜索 (1周)

**子任务**:
1. 实现文件列表分页API
2. 实现文件搜索API（按名称）
3. 实现文件排序功能
4. 实现文件筛选功能（按类型、大小）
5. 添加数据库索引
6. 前端分页和搜索UI

**技术要点**:
```java
// 分页查询
GET /api/files?page=0&size=20&sort=createdAt,desc

// 搜索
GET /api/files/search?q=photo&folderId={uuid}

// 排序和筛选
GET /api/files?sortBy=size&sortOrder=asc&fileType=image

// 存储统计API
GET /api/files/stats
{
    "totalFiles": 100,
    "totalSize": 1073741824,
    "usedStorage": "1.0 GB",
    "maxStorage": "10.0 GB"
}
```

**数据库索引**:
```java
@Table(name = "files", indexes = {
    @Index(name = "idx_owner_status", columnList = "ownerId, status"),
    @Index(name = "idx_owner_folder", columnList = "ownerId, folderId"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_name", columnList = "originalFileName")
})
```

**验收标准**:
- [ ] 文件列表支持分页（每页20/50/100）
- [ ] 可以按文件名搜索
- [ ] 可以按名称、大小、时间排序
- [ ] 可以按文件类型筛选
- [ ] 可以查看存储统计
- [ ] 性能测试：10000个文件查询时间<500ms

---

#### 任务5: 事务保护和异常处理 (0.5周)

**子任务**:
1. 为文件删除操作添加@Transactional
2. 实现本地事务消息表（补偿机制）
3. 统一异常处理（使用FileException）
4. 文件恢复时检查MinIO对象存在性

**技术要点**:
```java
// 事务保护
@Transactional(rollbackFor = Exception.class)
public void deleteFile(UUID ownerId, UUID fileId) {
    // 1. 数据库更新
    file.setStatus(FileStatus.DELETED);
    fileRepository.save(file);

    // 2. MinIO删除
    try {
        minioStorageService.delete(file.getBucket(), file.getObjectName());
    } catch (Exception e) {
        // 3. 补偿机制：记录到事务消息表，异步重试
        transactionMessageService.save(new TransactionMessage(
            fileId, "DELETE_MINIO", file.getObjectName()
        ));
        throw e;
    }
}

// 文件恢复检查
public void restoreFile(UUID ownerId, UUID fileId) {
    File file = fileRepository.findByIdAndOwnerIdAndStatus(fileId, ownerId, FileStatus.DELETED)
        .orElseThrow(() -> new FileException(ErrorCode.FILE_NOT_FOUND));

    // 检查MinIO对象是否存在
    if (!minioStorageService.exists(file.getBucket(), file.getObjectName())) {
        throw new FileException(ErrorCode.FILE_NOT_FOUND_IN_STORAGE);
    }

    file.setStatus(FileStatus.ACTIVE);
    fileRepository.save(file);
}
```

**验收标准**:
- [ ] 删除操作失败时可以回滚
- [ ] 文件恢复前检查MinIO对象存在性
- [ ] 所有异常统一使用FileException
- [ ] 异常信息清晰友好

---

### 4.2 P0阶段总结

**交付成果**:
- 用户可以注册和管理多个设备
- 用户可以创建文件夹组织文件
- 用户可以生成分享链接
- 文件列表支持分页、搜索、排序
- 系统具备基本的事务保护

**后续行动**:
- 进行内部测试
- 收集用户反馈
- 准备进入P1阶段

---

## 5. P1阶段：用户体验提升

**目标**: 提升跨设备使用的便利性

### 5.1 任务清单

#### 任务1: 文件同步机制 (2周)

**子任务**:
1. 设计同步协议（增量同步）
2. 实现文件变更事件系统
3. 实现WebSocket推送服务
4. 实现增量同步API
5. 实现冲突检测与解决
6. 前端实时同步UI

**技术要点**:
```java
// 文件变更事件
@Entity
@Table(name = "file_events")
@Data
public class FileEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private UUID fileId;

    private UUID folderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileEventType eventType; // CREATED, UPDATED, DELETED

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false)
    private Long version; // 乐观锁版本号

    @Index(name = "idx_owner_time", columnList = "ownerId, eventTime")
}

// WebSocket推送
@Controller
public class SyncWebSocketController {

    @MessageMapping("/sync/subscribe")
    public void subscribeToUpdates(Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        // 订阅该用户的文件变更频道
        simpMessagingService.subscribeToUser(userId);
    }
}

// 增量同步API
GET /api/sync/changes?since={timestamp}&deviceId={uuid}
{
    "changes": [
        {
            "type": "CREATED",
            "fileId": "uuid",
            "timestamp": "2025-01-01T10:00:00"
        }
    ],
    "hasMore": false,
    "cursor": "next-cursor"
}

// 冲突解决API
POST /api/sync/resolve-conflict
{
    "fileId": "uuid",
    "strategy": "KEEP_LOCAL" // or "KEEP_REMOTE", "KEEP_BOTH"
}
```

**同步协议设计**:
```
1. 客户端启动时调用 /api/sync/changes 获取自上次同步以来的变更
2. 服务端返回变更列表（按时间排序）
3. 客户端应用变更
4. 客户端上传本地变更到服务端
5. 服务端检测冲突，使用"最后写入胜"策略或提示用户
6. WebSocket实时推送其他设备的变更
```

**验收标准**:
- [ ] 设备A上传文件，设备B可以在5秒内看到
- [ ] 支持增量同步（只传输变更部分）
- [ ] 可以检测文件冲突
- [ ] 可以手动解决冲突
- [ ] WebSocket连接稳定

---

#### 任务2: 文件版本控制 (1.5周)

**子任务**:
1. 创建FileVersion实体和Repository
2. 修改上传逻辑，保存新版本而非直接覆盖
3. 实现版本列表查询API
4. 实现版本回退API
5. 实现版本对比API（可选）
6. 前端版本历史页面

**技术要点**:
```java
// 文件上传时创建版本
public File uploadFile(UUID ownerId, MultipartFile file, UUID folderId) {
    // 计算hash
    String fileHash = calculateHash(file);

    // 查找现有文件
    File existingFile = fileRepository.findByOwnerIdAndFileHashAndStatus(
        ownerId, fileHash, FileStatus.ACTIVE
    );

    if (existingFile != null) {
        // 创建新版本
        FileVersion version = FileVersion.builder()
            .fileId(existingFile.getId())
            .ownerId(ownerId)
            .versionNumber(getNextVersionNumber(existingFile.getId()))
            .objectName(uploadToMinIO(file))
            .size(file.getSize())
            .comment("自动保存")
            .build();

        fileVersionRepository.save(version);

        // 更新文件记录
        existingFile.setObjectName(version.getObjectName());
        existingFile.setSize(file.getSize());
        existingFile.setUpdatedAt(LocalDateTime.now());
        return fileRepository.save(existingFile);
    }

    // 新文件...
}

// 版本列表
GET /api/files/{fileId}/versions
{
    "versions": [
        {
            "versionNumber": 3,
            "size": 1024000,
            "createdAt": "2025-01-03T10:00:00",
            "comment": "自动保存"
        },
        {
            "versionNumber": 2,
            "size": 1023000,
            "createdAt": "2025-01-02T10:00:00",
            "comment": "自动保存"
        }
    ],
    "currentVersion": 3
}

// 版本回退
POST /api/files/{fileId}/versions/{versionNumber}/restore
```

**版本清理策略**:
- 每个文件最多保留10个版本
- 版本超过30天自动删除
- 可配置

**验收标准**:
- [ ] 上传同名文件会创建新版本
- [ ] 可以查看文件的所有版本
- [ ] 可以回退到任意版本
- [ ] 版本列表显示创建时间和大小
- [ ] 版本自动清理机制正常工作

---

#### 任务3: 批量操作 (0.5周)

**子任务**:
1. 实现批量删除API
2. 实现批量移动API
3. 实现批量下载（打包为ZIP）
4. 前端批量选择UI

**技术要点**:
```java
// 批量删除
POST /api/files/batch-delete
{
    "fileIds": ["uuid1", "uuid2", "uuid3"]
}

// 批量移动
POST /api/files/batch-move
{
    "fileIds": ["uuid1", "uuid2"],
    "targetFolderId": "folder-uuid"
}

// 批量下载
GET /api/files/batch-download?fileIds=uuid1,uuid2,uuid3
// 返回ZIP文件
```

**验收标准**:
- [ ] 可以批量选择文件
- [ ] 批量删除可以正确处理
- [ ] 批量移动到指定文件夹
- [ ] 批量下载生成ZIP文件
- [ ] 批量操作有进度提示

---

#### 任务4: 最近访问记录 (0.5周)

**子任务**:
1. 创建FileAccessLog实体和Repository
2. 实现AOP切面记录访问日志
3. 实现最近文件查询API
4. 前端最近文件页面

**技术要点**:
```java
// AOP记录访问
@Aspect
@Component
public class FileAccessAspect {

    @AfterReturning(
        "execution(* com.evorsio.mybox.file.service.FileService.downloadFile(..)) && args(ownerId, fileId)"
    )
    public void logDownload(UUID ownerId, UUID fileId) {
        FileAccessLog log = FileAccessLog.builder()
            .fileId(fileId)
            .ownerId(ownerId)
            .deviceId(getCurrentDeviceId())
            .accessType(AccessType.DOWNLOAD)
            .accessTime(LocalDateTime.now())
            .build();

        fileAccessLogRepository.save(log);
    }
}

// 最近文件
GET /api/files/recent?limit=20
{
    "files": [
        {
            "fileId": "uuid",
            "fileName": "photo.jpg",
            "lastAccessTime": "2025-01-01T10:00:00",
            "accessDevice": "My iPhone"
        }
    ]
}
```

**验收标准**:
- [ ] 访问文件时自动记录
- [ ] 可以查看最近访问的文件
- [ ] 显示访问时间和设备
- [ ] 支持跨设备同步

---

#### 任务5: 存储统计接口 (0.5周)

**子任务**:
1. 实现存储统计API
2. 实现文件类型分布统计
3. 前端存储空间可视化

**技术要点**:
```java
// 存储统计
GET /api/files/stats
{
    "totalSize": 10737418240,
    "usedSize": 5368709120,
    "fileCount": 500,
    "usagePercent": 50,
    "byType": {
        "image": 3221225472,
        "video": 1073741824,
        "document": 536870912,
        "other": 536870912
    },
    "byDevice": {
        "My iPhone": 3221225472,
        "My Laptop": 2147483648
    }
}
```

**验收标准**:
- [ ] 显示总存储空间和已用空间
- [ ] 按文件类型统计
- [ ] 按设备统计
- [ ] 存储空间不足时提示

---

### 5.2 P1阶段总结

**交付成果**:
- 设备间可以实时同步文件
- 文件支持版本控制
- 支持批量操作
- 可以查看最近访问记录
- 可以查看存储统计

**后续行动**:
- 进行内部测试
- 邀请用户试用
- 收集反馈并优化

---

## 6. P2阶段：安全与稳定性

**目标**: 保证系统稳定运行

### 6.1 任务清单

#### 任务1: 安全增强 (1周)

**子任务**:
1. 修复认证方式（使用自定义UserDetails）
2. 实现API速率限制
3. 实现文件类型深度验证（magic bytes）
4. 实现设备配额管理
5. 文件名安全处理

**技术要点**:
```java
// 自定义UserDetails
public class MyboxUserDetails implements UserDetails {
    private UUID userId;
    private String username;
    private UserRole role;

    // 从authentication中获取
    public static MyboxUserDetails fromAuthentication(Authentication auth) {
        return (MyboxUserDetails) auth.getPrincipal();
    }
}

// 速率限制（使用Redis + 注解）
@RateLimit(key = "#userId", time = 60, maxRequests = 100)
@PostMapping("/upload")
public ApiResponse<File> uploadFile(...) {
    // ...
}

// 文件类型验证
public void validateFileType(MultipartFile file, String declaredType) {
    // 读取文件头（magic bytes）
    byte[] header = new byte[8];
    file.getInputStream().read(header);

    // 检查真实类型
    String realType = detectFileTypeFromMagicBytes(header);

    if (!realType.equals(declaredType)) {
        throw new FileException(ErrorCode.FILE_TYPE_MISMATCH);
    }
}

// 设备配额检查
public void registerDevice(UUID ownerId, RegisterDeviceRequest request) {
    long deviceCount = deviceRepository.countByOwnerId(ownerId);

    if (deviceCount >= MAX_DEVICES_PER_USER) {
        throw new DeviceException(ErrorCode.DEVICE_LIMIT_EXCEEDED);
    }

    // ...
}
```

**验收标准**:
- [ ] 使用安全的认证方式
- [ ] API调用速率限制生效
- [ ] 文件类型伪造被拦截
- [ ] 每个用户最多5台设备
- [ ] 文件名路径遍历攻击被阻止

---

#### 任务2: 性能优化 (1周)

**子任务**:
1. 添加所有缺失的数据库索引
2. 优化N+1查询问题
3. 实现查询结果缓存（Redis）
4. 实现CDN加速（预签名URL）

**技术要点**:
```java
// 缓存文件列表
@Cacheable(value = "fileList", key = "#ownerId + '_' + #folderId")
public List<File> listFiles(UUID ownerId, UUID folderId) {
    return fileRepository.findByOwnerIdAndFolderId(ownerId, folderId);
}

// 预签名URL下载
public String getPresignedDownloadUrl(UUID fileId, UUID ownerId) {
    File file = getActiveFileById(ownerId, fileId);

    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .bucket(file.getBucket())
            .object(file.getObjectName())
            .expiry(3600) // 1小时
            .build()
    );
}

// 批量查询优化（避免N+1）
@Query("SELECT f FROM File f LEFT JOIN FETCH f.folder WHERE f.ownerId = :ownerId")
List<File> findByOwnerIdWithFolder(@Param("ownerId") UUID ownerId);
```

**验收标准**:
- [ ] 所有常用查询字段都有索引
- [ ] 文件列表查询时间<200ms
- [ ] 缓存命中率>50%
- [ ] 下载不经过应用服务器（使用预签名URL）

---

#### 任务3: 自动清理机制 (0.5周)

**子任务**:
1. 实现DELETED文件自动清理定时任务
2. 实现过期分享链接清理
3. 实现旧版本文件清理
4. 实现访问日志清理

**技术要点**:
```java
// 定时清理DELETED文件
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
public void cleanupDeletedFiles() {
    LocalDateTime expireTime = LocalDateTime.now().minusDays(30);

    List<File> deletedFiles = fileRepository
        .findByStatusAndDeletedAtBefore(FileStatus.DELETED, expireTime);

    for (File file : deletedFiles) {
        try {
            minioStorageService.delete(file.getBucket(), file.getObjectName());
        } catch (Exception e) {
            log.error("清理MinIO文件失败: {}", file.getObjectName(), e);
        }

        fileRepository.delete(file);
    }
}

// 清理过期分享
@Scheduled(cron = "0 0 * * * ?") // 每小时
public void cleanupExpiredShares() {
    List<FileShare> expiredShares = fileShareRepository
        .findByExpireAtBefore(LocalDateTime.now());

    fileShareRepository.deleteAll(expiredShares);
}
```

**验收标准**:
- [ ] DELETED文件30天后自动删除
- [ ] 过期分享链接自动清理
- [ ] 超过10个版本的旧版本自动删除
- [ ] 90天前的访问日志自动清理

---

#### 任务4: 数据对账工具 (0.5周)

**子任务**:
1. 实现数据库与MinIO对账脚本
2. 实现孤儿文件清理
3. 实现缺失文件检测
4. 提供手动修复API

**技术要点**:
```java
// 对账服务
@Service
public class DataReconciliationService {

    public ReconciliationResult reconcile() {
        // 1. 检查数据库中的文件在MinIO中是否存在
        List<File> dbFiles = fileRepository.findAll();
        List<String> missingInMinio = new ArrayList<>();

        for (File file : dbFiles) {
            if (!minioStorageService.exists(file.getBucket(), file.getObjectName())) {
                missingInMinio.add(file.getId().toString());
            }
        }

        // 2. 检查MinIO中的文件在数据库中是否存在
        List<String> orphanObjects = minioStorageService.listAllObjects();
        orphanObjects.removeAll(dbFiles.stream()
            .map(File::getObjectName)
            .collect(Collectors.toList()));

        return new ReconciliationResult(missingInMinio, orphanObjects);
    }

    @Transactional
    public void fixMissingFiles(List<UUID> fileIds) {
        for (UUID fileId : fileIds) {
            File file = fileRepository.findById(fileId).orElse(null);
            if (file != null) {
                file.setStatus(FileStatus.ERROR);
                fileRepository.save(file);
            }
        }
    }
}

// 管理员API
POST /api/admin/reconcile
POST /api/admin/reconcile/fix
```

**验收标准**:
- [ ] 可以运行对账检测
- [ ] 可以发现孤儿文件和缺失文件
- [ ] 可以一键修复问题
- [ ] 对账报告清晰

---

### 6.2 P2阶段总结

**交付成果**:
- 系统安全加固
- 性能优化完成
- 自动清理机制运行
- 数据一致性保障

**后续行动**:
- 进行压力测试
- 进行安全测试
- 准备上线

---

## 7. P3阶段：性能优化与高级功能

**目标**: 提供企业级功能

### 7.1 任务清单

#### 任务1: 离线支持 (2周)

**子任务**:
1. 设计离线存储方案（IndexedDB）
2. 实现离线文件索引
3. 实现离线队列
4. 实现冲突解决策略
5. 前端离线模式UI

**技术要点**:
```java
// 离线同步API
POST /api/sync/offline-changes
{
    "changes": [
        {
            "type": "UPLOAD",
            "fileName": "photo.jpg",
            "localPath": "/local/path/photo.jpg",
            "timestamp": "2025-01-01T10:00:00"
        }
    ]
}

// 冲突解决
POST /api/sync/resolve-offline-conflict
{
    "conflictId": "uuid",
    "localVersion": {...},
    "remoteVersion": {...},
    "strategy": "KEEP_REMOTE" // or "KEEP_LOCAL", "MERGE", "KEEP_BOTH"
}
```

**验收标准**:
- [ ] 离线时可以查看已缓存的文件
- [ ] 离线时可以上传文件（加入队列）
- [ ] 网络恢复后自动同步
- [ ] 可以正确处理冲突

---

#### 任务2: 并发控制 (1周)

**子任务**:
1. 实现上传并发限制（信号量）
2. 实现下载并发限制
3. 实现分片上传并发控制
4. 资源池管理

**技术要点**:
```java
// 并发控制
@Service
public class ConcurrencyControlService {

    private final Semaphore uploadSemaphore = new Semaphore(10); // 最多10个并发上传
    private final Semaphore downloadSemaphore = new Semaphore(50); // 最多50个并发下载

    public <T> T uploadWithLimit(Callable<T> task) throws Exception {
        if (!uploadSemaphore.tryAcquire(5, TimeUnit.SECONDS)) {
            throw new FileException(ErrorCode.UPLOAD_CONCURRENT_LIMIT);
        }

        try {
            return task.call();
        } finally {
            uploadSemaphore.release();
        }
    }
}

// 使用
public File uploadFile(...) {
    return concurrencyControlService.uploadWithLimit(() -> {
        // 实际上传逻辑
    });
}
```

**验收标准**:
- [ ] 并发上传不超过10个
- [ ] 并发下载不超过50个
- [ ] 超过限制时友好提示
- [ ] 资源正确释放

---

#### 任务3: 流量控制 (0.5周)

**子任务**:
1. 实现下载速率限制（令牌桶算法）
2. 实现上传速率限制
3. 按用户等级分配带宽

**技术要点**:
```java
// 速率限制
@Service
public class RateLimitingService {

    private final Map<UUID, RateLimiter> userLimiters = new ConcurrentHashMap<>();

    public InputStream limitDownloadSpeed(UUID userId, InputStream inputStream, long bytesPerSecond) {
        RateLimiter limiter = userLimiters.computeIfAbsent(
            userId, k -> RateLimiter.create(bytesPerSecond)
        );

        return new RateLimitedInputStream(inputStream, limiter);
    }
}

// 使用
public InputStream downloadFile(UUID ownerId, UUID fileId) {
    File file = getActiveFileById(ownerId, fileId);
    InputStream stream = minioStorageService.download(file.getBucket(), file.getObjectName());

    // 限制下载速度（如 5MB/s）
    return rateLimitingService.limitDownloadSpeed(ownerId, stream, 5_000_000);
}
```

**验收标准**:
- [ ] 普通用户下载速度限制5MB/s
- [ ] VIP用户下载速度限制20MB/s
- [ ] 限流生效且稳定

---

#### 任务4: 高级同步功能 (1.5周)

**子任务**:
1. 实现选择性同步（按文件夹）
2. 实现WiFi only同步选项
3. 实现设备同步优先级
4. 实现智能同步（常用文件优先）

**技术要点**:
```java
// 选择性同步配置
@Entity
@Table(name = "sync_preferences")
@Data
public class SyncPreferences {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private UUID deviceId;

    @Column(columnDefinition = "json")
    private List<UUID> selectedFolderIds; // 只同步这些文件夹

    @Column(nullable = false)
    private Boolean wifiOnly = false;

    @Column(nullable = false)
    private Integer priority = 0; // 同步优先级

    @Column(nullable = false)
    private SyncMode syncMode = SyncMode.AUTO; // AUTO, SELECTIVE, PAUSED
}

// 更新同步偏好
PUT /api/sync/preferences
{
    "selectedFolderIds": ["uuid1", "uuid2"],
    "wifiOnly": true,
    "syncMode": "SELECTIVE"
}
```

**验收标准**:
- [ ] 可以选择只同步特定文件夹
- [ ] 可以设置WiFi only同步
- [ ] 可以设置设备优先级
- [ ] 可以暂停同步

---

#### 任务5: 定期对账自动化 (0.5周)

**子任务**:
1. 实现定时自动对账
2. 异常自动告警
3. 自动修复机制

**技术要点**:
```java
// 定时对账
@Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点
public void autoReconcile() {
    ReconciliationResult result = reconciliationService.reconcile();

    if (!result.isConsistent()) {
        // 发送告警
        alertService.sendAlert("数据不一致", result);

        // 自动修复（可选）
        if (autoFixEnabled) {
            reconciliationService.autoFix(result);
        }
    }
}
```

**验收标准**:
- [ ] 每天自动运行对账
- [ ] 发现问题自动告警
- [ ] 可以自动修复轻微问题

---

### 7.2 P3阶段总结

**交付成果**:
- 支持离线访问
- 并发控制完善
- 流量控制实现
- 高级同步功能
- 自动对账机制

**后续行动**:
- 进行全面测试
- 性能压测
- 准备正式上线

---

## 8. 技术选型建议

### 8.1 WebSocket实时通信

**推荐**: Spring WebSocket + STOMP

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**配置**:
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 8.2 缓存方案

**推荐**: Redis + Spring Cache

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**配置**:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

### 8.3 限流方案

**推荐**: Spring Cloud Gateway 或 Bucket4j

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

### 8.4 任务调度

**推荐**: Spring @Scheduled

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

### 8.5 搜索引擎（可选）

**推荐**: Elasticsearch 或 PostgreSQL Full Text Search

```sql
-- PostgreSQL全文搜索
CREATE INDEX idx_files_name_fts ON files USING gin(to_tsvector('english', originalFileName));

-- 查询
SELECT * FROM files
WHERE to_tsvector('english', originalFileName) @@ to_tsquery('photo');
```

---

## 9. 测试策略

### 9.1 单元测试

**目标**: 代码覆盖率 > 70%

```java
@SpringBootTest
class FileServiceTest {

    @Autowired
    private FileService fileService;

    @MockBean
    private MinioStorageService minioStorageService;

    @Test
    void testUploadFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "test.jpg", "test.jpg", "image/jpeg", "test".getBytes()
        );

        // When
        File uploadedFile = fileService.uploadFile(...);

        // Then
        assertNotNull(uploadedFile);
        assertEquals("test.jpg", uploadedFile.getOriginalFileName());
    }
}
```

### 9.2 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "test@example.com")
    void testUploadAndDownload() throws Exception {
        // Upload
        MockMultipartFile file = new MockMultipartFile(
            "files", "test.jpg", "image/jpeg", "test".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload")
                .file(file))
            .andExpect(status().isOk());

        // Download
        mockMvc.perform(post("/api/files/download")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fileId\": \"...\"}"))
            .andExpect(status().isOk());
    }
}
```

### 9.3 性能测试

**工具**: JMeter 或 Gatling

```scala
// Gatling测试脚本
class FileUploadSimulation extends Simulation {

    val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")

    val scn = scenario("File Upload")
        .exec(http("upload_file")
            .post("/api/files/upload")
            .body(RawFileBody("upload.json")))

    setUp(
        scn.inject(
            rampUsers(100) during (60 seconds)
        )
    ).protocols(httpProtocol)
}
```

**性能指标**:
- 文件上传: > 100次/秒
- 文件下载: > 500次/秒
- 文件列表查询: < 200ms
- 并发用户: > 1000

### 9.4 安全测试

**工具**: OWASP ZAP

**测试项**:
- SQL注入
- XSS攻击
- CSRF攻击
- 路径遍历
- 越权访问

---

## 10. 部署与上线

### 10.1 开发环境

**架构**:
```
前端 (localhost:3000)
  ↓
后端 (localhost:8080)
  ↓
PostgreSQL (localhost:5432)
Redis (localhost:6379)
MinIO (localhost:9000)
```

### 10.2 生产环境

**架构**:
```
用户
  ↓
CDN (静态资源)
  ↓
负载均衡 (Nginx)
  ↓
应用服务器集群 (Spring Boot)
  ↓
PostgreSQL集群 + Redis集群 + MinIO集群
```

**部署清单**:
- [ ] 数据库迁移脚本
- [ ] 配置文件加密（数据库密码、JWT密钥）
- [ ] 日志收集（ELK）
- [ ] 监控告警（Prometheus + Grafana）
- [ ] 备份策略（数据库备份 + MinIO备份）
- [ ] HTTPS证书
- [ ] 防火墙配置

### 10.3 上线检查清单

**功能检查**:
- [ ] 所有P0功能完成并通过测试
- [ ] 所有P1功能完成并通过测试
- [ ] 所有P2安全项完成
- [ ] 性能测试通过
- [ ] 安全测试通过

**运维检查**:
- [ ] 监控系统正常运行
- [ ] 告警系统配置完成
- [ ] 备份系统正常运行
- [ ] 日志系统正常收集
- [ ] 数据库连接池配置合理
- [ ] Redis连接池配置合理

**应急方案**:
- [ ] 回滚方案准备
- [ ] 应急联系人列表
- [ ] 故障处理手册
- [ ] 数据恢复流程

---

## 11. 附录

### 11.1 参考文档

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [MinIO Java SDK](https://docs.min.io/docs/java-client-quickstart-guide.html)
- [WebSocket规范](https://websocket.org/)
- [PostgreSQL性能优化](https://www.postgresql.org/docs/current/performance-tips.html)

### 11.2 开发规范

**代码规范**:
- 遵循阿里巴巴Java开发手册
- 使用Lombok减少样板代码
- 统一异常处理
- 统一日志格式

**Git规范**:
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具链相关

**提交格式**:
```
feat(device): 实现设备注册功能

- 添加Device实体
- 实现设备注册API
- 添加单元测试

Closes #123
```

### 11.3 项目时间表

```
Week 1-6:  P0阶段（核心云盘功能）
  Week 1:  设备管理
  Week 2-3: 文件夹功能
  Week 3-4: 文件分享
  Week 4-5: 分页和搜索
  Week 5-6: 事务保护、测试

Week 7-10: P1阶段（用户体验提升）
  Week 7-8: 文件同步机制
  Week 8-9: 文件版本控制
  Week 9-10: 批量操作、最近访问、存储统计

Week 11-13: P2阶段（安全与稳定性）
  Week 11: 安全增强
  Week 12: 性能优化
  Week 13: 自动清理、数据对账

Week 14-19: P3阶段（性能优化与高级功能）
  Week 14-15: 离线支持
  Week 16: 并发控制
  Week 17: 流量控制
  Week 18-19: 高级同步功能、自动化对账

Week 20: 全面测试和上线准备
```

---

## 总结

本开发路线图按照 **P0 → P1 → P2 → P3** 的顺序，详细规划了从基础云盘功能到企业级功能的完整开发路径。

**关键里程碑**:
1. **P0完成后**: 用户可以基本使用多设备文件管理
2. **P1完成后**: 设备间可以自动同步，体验显著提升
3. **P2完成后**: 系统安全稳定，可以试运行
4. **P3完成后**: 功能完善，可以正式上线

**建议**: 优先完成P0和P1阶段，这两个阶段是实现个人云盘核心功能的关键。P2和P3可以根据实际情况灵活调整优先级。
