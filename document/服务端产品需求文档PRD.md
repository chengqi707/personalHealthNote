# 个人健康笔记服务端产品需求文档
## 一、产品定位
为个人健康笔记APP提供后端服务支持，实现用户账号管理、健康数据云端存储、多端数据同步能力，保障用户数据安全不丢失。
## 二、核心功能清单
### 1. 用户管理模块
- 用户注册：支持用户名密码注册，用户名唯一
- 用户登录：支持用户名密码登录，返回JWT Token
- Token鉴权：所有业务接口需要携带有效Token才能访问
- 用户数据隔离：不同用户的数据完全隔离，只能访问自己的数据
### 2. 健康记录同步模块
- 增量同步拉取：客户端上传最后同步时间，服务端返回该时间之后的所有变更数据
- 增量同步上传：客户端上传本地未同步的变更数据，服务端批量保存
- 冲突解决策略：采用「最新修改时间覆盖」原则，修改时间晚的数据优先保留
### 3. 用药提醒同步模块
- 同健康记录同步逻辑，支持增量拉取和上传
## 三、核心同步逻辑
1. 同步触发时机：用户手动点击APP端「同步」按钮触发
2. 同步方向：双向同步，先拉取云侧最新数据到本地，再上传本地未同步数据到云侧
3. 增量同步规则：
   - 所有数据都有`updateTime`（最后修改时间戳）和`deleteFlag`（删除标记）字段
   - 客户端每次同步时记录最后同步时间戳`lastSyncTime`
   - 拉取时：客户端上传`lastSyncTime`，服务端返回所有`updateTime > lastSyncTime`的数据（包括新增、修改、删除的数据）
   - 上传时：客户端上传所有`isSync = 0`（未同步）的数据，服务端保存后返回同步成功状态
4. 删除同步规则：
   - 客户端删除数据时不物理删除，仅标记`deleteFlag = 1`，同步时上传到服务端
   - 服务端删除数据时同样标记`deleteFlag = 1`，客户端拉取到后本地删除对应数据
## 四、数据表结构设计
### 1. 用户表（user）
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键，自增 |
| username | varchar(50) | 用户名，唯一 |
| password | varchar(255) | 密码，BCrypt加密存储 |
| create_time | bigint | 创建时间戳 |
| update_time | bigint | 最后修改时间戳 |
### 2. 健康记录表（health_record）
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键，自增 |
| user_id | bigint | 关联用户ID |
| record_date | varchar(20) | 记录日期，格式：yyyy-MM-dd |
| weight | float | 体重，单位kg |
| systolic_pressure | int | 收缩压 |
| diastolic_pressure | int | 舒张压 |
| heart_rate | int | 心率 |
| blood_sugar | float | 血糖 |
| sleep_duration | float | 睡眠时长，单位小时 |
| water_intake | int | 饮水量，单位ml |
| steps | int | 步数 |
| notes | text | 备注 |
| image_paths | text | 图片路径JSON数组 |
| update_time | bigint | 最后修改时间戳 |
| delete_flag | tinyint | 删除标记：0未删除 1已删除 |
### 3. 用药提醒表（medicine_reminder）
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键，自增 |
| user_id | bigint | 关联用户ID |
| medicine_name | varchar(100) | 药品名称 |
| dosage | varchar(50) | 用药剂量 |
| unit | varchar(20) | 单位，如 "mg", "g", "ml" |
| frequency | int | 每日次数，1-5次 |
| remind_time1 | varchar(20) | 第一次提醒时间，格式：HH:mm |
| remind_time2 | varchar(20) | 第二次提醒时间，可为空 |
| remind_time3 | varchar(20) | 第三次提醒时间，可为空 |
| remind_time4 | varchar(20) | 第四次提醒时间，可为空 |
| remind_time5 | varchar(20) | 第五次提醒时间，可为空 |
| is_enabled | tinyint | 是否启用：0禁用 1启用 |
| start_date | varchar(20) | 开始日期，格式：yyyy-MM-dd |
| end_date | varchar(20) | 结束日期，格式：yyyy-MM-dd，可为空表示长期 |
| before_after_meal | int | 饭前饭后：0-不限，1-饭前，2-饭后，3-饭中 |
| notes | text | 备注 |
| update_time | bigint | 最后修改时间戳 |
| delete_flag | tinyint | 删除标记：0未删除 1已删除 |
## 五、非功能需求
1. 安全性：
   - 密码必须BCrypt加密存储，禁止明文存储
   - 所有接口采用JWT鉴权，Token有效期7天
   - 用户数据完全隔离，禁止跨用户访问
2. 性能：
   - 同步接口支持批量操作，单次最多支持100条数据同步
   - 接口响应时间不超过2秒
3. 兼容性：
   - 接口设计保持向后兼容，版本迭代不影响旧版本APP使用
