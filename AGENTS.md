## 开发规范
- 重要方法上加中文注释
- 重要方法要有单元测试

## 项目结构
- doc/:，存放文档
    - story/: 存放需求，格式: 需求版本号-功能名-yyyyMMdd.md
- script/: 存放脚本
    - http/: 存放http调试请求方法（IDEA编辑器用）
    - curl/: 存放curl调试命令（命令行用）
    - sql/: 存放sql
- server/: 存放后端项目（SpringBoot，包根 dev.vincent.chaindocproof）
    - src/main/java/dev/vincent/chaindocproof/: 后端代码
        - config/: Web 配置（拦截器注册等）
        - dao/: 数据访问层
        - interceptor/: 请求拦截器（登录鉴权）
        - model/: 实体类（model/dto 存放请求/响应 DTO）
        - service/: 业务逻辑
        - support/: 通用支撑（业务异常、token、加密、注册限流）
        - web/: Controller 与全局异常处理
    - src/main/resources/: application.yml、schema.sql
    - src/test/java/: 单元测试，包结构与 main 对应
- client/: 存放用户端项目
- admin/: 存放管理端项目
- contract/: 存放合约文件

## 技术选型
### 后端
- 语言使用Java，JDK版本使用21
- 使用maven进行项目依赖管理，尽可能少的添加依赖包
- 数据库使用Mysql 8.x
- 框架使用SpringBoot
- 持久层框架使用SpringJDBC
- 使用web3j与链交互

### 数据库设计
- 表名开头为t_

### 合约
- 使用solidity开发
- 每个函数加上注释

### 前端
- 使用vueJs实现

### 管理端
- 使用vueJs实现

## 测试要求
- 根据入参，做一些边界值的测试
- 代码内有抛出异常的，需要进行异常情况的测试

## 注意事项
- 保持代码简洁，可读性高，避免过度设计
- 优先实现核心功能

## 需求
- 需求完成后，在README.md文档上写上变更信息和日期时间
