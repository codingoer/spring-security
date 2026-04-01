# Spring Security 工程目录总览（忽略 archive）

> 基于当前 `spring-security` 源码仓库（分支 `6.3.x-study`）的顶层目录梳理。  
> 目标：先建立“目录级认知”，后续再按模块深入源码。

## 一、核心功能模块目录

- `core`：安全框架基础能力（认证模型、授权基础、上下文、核心接口与实现）。
- `config`：安全配置模块（Java 配置、XML 命名空间相关支持、配置装配）。
- `web`：Servlet Web 安全能力（过滤器链、请求匹配、会话与 Web 场景安全控制）。
- `crypto`：密码学相关工具（编码器、加密/摘要等基础能力）。
- `acl`：ACL（细粒度对象权限）支持。
- `aspects`：与 AOP/AspectJ 集成相关安全支持。
- `data`：与 Spring Data 生态的安全集成支持。
- `messaging`：消息场景（如 messaging）安全支持。
- `test`：测试支持模块（测试工具、测试注解、Mock 安全上下文等）。
- `taglibs`：JSP Taglib 安全标签支持。
- `cas`：CAS（Central Authentication Service）集成支持。
- `ldap`：LDAP 认证/授权相关支持。
- `rsocket`：RSocket 场景安全支持。
- `oauth2`：OAuth 2.0 相关模块聚合目录，包含：
  - `oauth2-core`：OAuth2 抽象模型与核心能力。
  - `oauth2-client`：OAuth2 Client 支持。
  - `oauth2-resource-server`：Resource Server 支持。
  - `oauth2-jose`：JWT/JOSE 等令牌处理支持。
- `saml2`：SAML2 相关模块聚合目录，当前主要为：
  - `saml2-service-provider`：SAML2 SP（服务提供方）支持。

## 二、文档、示例与集成验证

- `docs`：官方参考文档源码（Antora 文档体系、文档构建配置）。
- `samples`：示例工程集合，覆盖多种典型安全场景与配置方式。
- `itest`：集成测试工程集合，用于跨模块/跨场景验证（如 web、ldap、context 等）。

## 三、构建与依赖管理

- `buildSrc`：Gradle 自定义构建逻辑与插件实现（约定、任务、构建辅助代码）。
- `bom`：既用于本仓库统一 Spring Security 各模块版本，也会发布为外部可导入的 `spring-security-bom`（Boot 项目通常由 Boot BOM 间接管理而不手动引入）。
- `dependencies`：依赖版本与依赖管理相关模块（供多模块统一使用）。
- `gradle`：Gradle Wrapper 与构建基础设施文件。
- `build`：构建产物目录（你执行 `gradle build` 后产生，通常不作为源码阅读重点）。

## 四、工程规范与脚本工具

- `etc`：工程级规范与工具配置（如 checkstyle、nohttp、eclipse、s101）。
- `scripts`：构建/发布/依赖更新等辅助脚本。
- `git`：仓库内辅助的 Git 相关脚本与 hooks 资源。

## 五、其它目录说明

- `openid`：当前仓库中主要是历史/迁移相关遗留内容（你本地该目录下仅看到 `build` 产物，源码阅读优先级较低）。

## 六、顶层文件用途说明（补充）

### 1) 项目说明与治理

- `README.adoc`：项目总入口说明（简介、构建方式、文档入口、支持渠道）。
- `CONTRIBUTING.adoc`：贡献流程与规范（提 PR、代码风格、开发约定）。
- `RELEASE.adoc`：发布相关说明与流程信息。
- `LICENSE.txt`：开源许可证（Apache License 2.0）。
- `notice.txt`：NOTICE 信息（版权与第三方声明）。

### 2) Gradle 构建入口文件

- `settings.gradle`：多模块装配入口。该仓库通过扫描 `*.gradle` 动态 include 子项目。
- `build.gradle`：根工程构建逻辑（全局插件、公共配置、任务、质量检查等）。
- `gradle.properties`：Gradle 全局属性（版本、构建参数等）。
- `gradlew` / `gradlew.bat`：Gradle Wrapper 启动脚本（Unix / Windows）。

### 3) 历史与辅助说明类文件

- `class_mapping_from_2.0.x.txt`：历史版本类映射信息（用于迁移/兼容参考）。

---

## 建议的源码阅读顺序（入门）

1. `core`（先理解认证、授权、上下文等核心抽象）
2. `config`（看配置如何装配核心能力）
3. `web`（看过滤器链如何落地到请求处理）
4. 再按兴趣进入 `oauth2` / `saml2` / `ldap` / `cas` 等专项模块
5. 最后看 `test` 与 `itest`，反向理解“正确使用姿势”

