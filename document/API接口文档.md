# 个人健康笔记API接口文档
## 一、统一规范
### 1. 基础URL
`http://{服务器地址}:8080/api`
### 2. 请求头
| 字段名 | 说明 | 必填 |
|--------|------|------|
| Content-Type | application/json | 是 |
| Authorization | Bearer {Token} | 业务接口必填，登录注册接口不需要 |
### 3. 统一返回格式
```json
{
    "code": 200, // 状态码：200成功 400参数错误 401未授权 403禁止访问 500服务器错误
    "message": "操作成功", // 提示信息
    "data": {} // 返回数据
}
```
### 4. 错误码说明
| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或Token已过期 |
| 403 | 无权限访问 |
| 404 | 请求资源不存在 |
| 500 | 服务器内部错误 |
## 二、用户接口
### 1. 用户注册
- **接口地址**：`POST /user/register`
- **请求参数**：
```json
{
    "username": "testuser", // 用户名，长度4-20位
    "password": "123456"   // 密码，长度6-20位
}
```
- **返回数据**：
```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "userId": 1,
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
}
```
### 2. 用户登录
- **接口地址**：`POST /user/login`
- **请求参数**：
```json
{
    "username": "testuser",
    "password": "123456"
}
```
- **返回数据**：
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "userId": 1,
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
}
```
## 三、健康记录同步接口
### 1. 拉取健康记录增量数据
- **接口地址**：`POST /health/pull`
- **请求参数**：
```json
{
    "lastSyncTime": 1713456789000 // 客户端最后同步时间戳，0表示首次同步
}
```
- **返回数据**：
```json
{
    "code": 200,
    "message": "拉取成功",
    "data": {
        "records": [
            {
                "id": 1,
                "recordDate": "2024-04-18",
                "weight": 65.5,
                "systolicPressure": 120,
                "diastolicPressure": 80,
                "heartRate": 72,
                "bloodSugar": 5.6,
                "sleepDuration": 7.5,
                "waterIntake": 1500,
                "steps": 8000,
                "notes": "今天感觉不错",
                "imagePaths": "[\"/storage/emulated/0/xxx.jpg\"]",
                "updateTime": 1713456789000,
                "deleteFlag": 0
            }
        ]
    }
}
```
### 2. 上传健康记录增量数据
- **接口地址**：`POST /health/push`
- **请求参数**：
```json
{
    "records": [
        {
            "id": 1, // 客户端本地ID，服务端会重新生成
            "recordDate": "2024-04-18",
            "weight": 65.5,
            "systolicPressure": 120,
            "diastolicPressure": 80,
            "heartRate": 72,
            "bloodSugar": 5.6,
            "sleepDuration": 7.5,
            "waterIntake": 1500,
            "steps": 8000,
            "notes": "今天感觉不错",
            "imagePaths": "[\"/storage/emulated/0/xxx.jpg\"]",
            "updateTime": 1713456789000,
            "deleteFlag": 0
        }
    ]
}
```
- **返回数据**：
```json
{
    "code": 200,
    "message": "上传成功",
    "data": {
        "successCount": 1 // 成功同步的数量
    }
}
```
## 四、用药提醒同步接口
### 1. 拉取用药提醒增量数据
- **接口地址**：`POST /medicine/pull`
- **请求参数**：
```json
{
    "lastSyncTime": 1713456789000 // 客户端最后同步时间戳，0表示首次同步
}
```
- **返回数据**：
```json
{
    "code": 200,
    "message": "拉取成功",
    "data": {
        "reminders": [
            {
                "id": 1,
                "medicineName": "感冒药",
                "dosage": "1粒",
                "unit": "粒",
                "frequency": 3,
                "remindTime1": "08:00",
                "remindTime2": "12:00",
                "remindTime3": "18:00",
                "remindTime4": null,
                "remindTime5": null,
                "isEnabled": true,
                "startDate": "2024-04-18",
                "endDate": "2024-04-25",
                "beforeAfterMeal": 2,
                "notes": "饭后服用",
                "updateTime": 1713456789000,
                "deleteFlag": 0
            }
        ]
    }
}
```
### 2. 上传用药提醒增量数据
- **接口地址**：`POST /medicine/push`
- **请求参数**：
```json
{
    "reminders": [
        {
            "id": 1, // 客户端本地ID，服务端会重新生成
            "medicineName": "感冒药",
            "dosage": "1粒",
            "unit": "粒",
            "frequency": 3,
            "remindTime1": "08:00",
            "remindTime2": "12:00",
            "remindTime3": "18:00",
            "remindTime4": null,
            "remindTime5": null,
            "isEnabled": true,
            "startDate": "2024-04-18",
            "endDate": "2024-04-25",
            "beforeAfterMeal": 2,
            "notes": "饭后服用",
            "updateTime": 1713456789000,
            "deleteFlag": 0
        }
    ]
}
```
- **返回数据**：
```json
{
    "code": 200,
    "message": "上传成功",
    "data": {
        "successCount": 1 // 成功同步的数量
    }
}
```
