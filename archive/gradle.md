# Gradle 缓存目录说明 & 修改Jar下载路径 存档文档
## 一、修改 Gradle 依赖 Jar 包下载/缓存位置（含Wrapper生效）
### 1. 核心原理
所有 Gradle（含 Gradle Wrapper：`gradlew/gradlew.bat`）的依赖Jar、插件缓存、运行缓存，都统一由 `GRADLE_USER_HOME` / `gradle.user.home` 控制；
Jar包实际存放目录：`${gradle.user.home}/caches/modules-2/files-2.1`

### 2. 三种修改方式（优先级从高→低）
#### 方式① 系统环境变量（全局所有项目生效）
- 变量名：`GRADLE_USER_HOME`
- 变量值（示例）：
    - Mac/Linux：`/Users/你的用户名/Develop/gradle-cache`
    - Windows：`D:\Develop\gradle-cache`

配置后：本机所有 Gradle/Wrapper 项目统一走该缓存目录。

#### 方式② 项目内配置（团队共享、只当前项目生效）
在项目根目录 `gradle.properties` 写入：
```properties
# 自定义全局缓存主目录（Jar、daemon、wrapper、jdk都在这里）
gradle.user.home=/Users/你的用户名/Develop/gradle-cache
```
提交Git后，团队所有人自动统一缓存路径，对 IDEA / 命令行 `./gradlew` 都生效。

#### 方式③ 临时单次生效（仅当前构建）
Mac/Linux：
```bash
./gradlew build -g /Users/xxx/Develop/gradle-cache
```
Windows：
```cmd
gradlew build -g D:\Develop\gradle-cache
```

### 3. 验证是否修改成功
终端执行：
```bash
./gradlew gradleUserHome
```
输出路径 = 你配置的路径，即为生效。

### 4. 迁移旧缓存
把原 `.gradle/caches` 整个拷贝到新 `gradle-user-home/caches`，避免重新下载所有Jar。

---

## 二、Mac ~/.gradle 目录全文件/文件夹详解 & 能否删除对照表
### 目录总览
默认路径：`/Users/你的用户名/.gradle`

| 目录/文件 | 作用说明 | 是否可安全删除 | 补充影响 |
|----------|----------|----------------|----------|
| `build-scan-data` | Gradle构建分析（Build Scan）本地缓存、日志 | ✅ 完全可删 | 仅离线查看构建分析失效，不影响编译运行 |
| `caches` | **核心缓存**：依赖Jar、插件、依赖解析元数据、转换缓存 | ⚠️ 谨慎全删 | 1. `modules-2/files-2.1`：存放所有下载Jar，删了会全量重新下载<br>2. 可删`transforms-3/journal-1`等临时缓存，别删modules-2 |
| `daemon` | Gradle守护进程：后台JVM常驻日志、PID、运行状态 | ✅ 可删旧日志/旧版本 | 删除后自动重启daemon；清理旧日志可瘦身 |
| `jdks` | Gradle Toolchains自动下载的JDK本体 | ⚠️ 按需删 | 删掉不用的JDK版本；正在使用的删掉会重新自动下载 |
| `kotlin-profile` | Kotlin编译性能分析临时数据 | ✅ 完全可删 | 不影响代码编译、打包 |
| `native` | Gradle/插件依赖的原生库（dylib/本地二进制） | ✅ 可删 | 下次构建自动重新拉取原生依赖 |
| `workers` | Gradle并行构建工作进程临时文件 | ✅ 完全可删 | 仅临时缓存，删除无业务影响 |
| `wrapper` | 存放Wrapper自动下载的Gradle发行压缩包&解压缓存 | ✅ 可删 | 删后首次用gradlew会重新下载对应Gradle版本 |
| `android` | AGP安卓专属缓存：AAPT2、资源编译、构建中间缓存 | ⚠️ 谨慎删 | 纯Java项目无此目录；安卓删后首次构建极慢，会重建所有资源缓存 |
| `android.lock` | 安卓构建并发锁，防止多进程抢缓存损坏 | ✅ 可删 | 构建卡死/报占用时直接删掉即可 |
| `notifications` | Gradle桌面通知记录、弹窗配置缓存 | ✅ 完全可删 | 无任何构建影响 |
| `task-cache` | 增量构建/跨机器构建任务输出缓存 | ⚠️ 谨慎删 | 删后会全量重新执行所有编译任务，构建变慢 |

---

## 三、安全清理推荐命令（不删核心Jar）
### 1、项目内日常清理（最稳）
```bash
./gradlew clean
./gradlew cleanBuildCache
./gradlew --stop
```

### 2、全局安全瘦身（只删无用垃圾，保留Jar依赖）
```bash
# 停进程防占用
gradle --stop && ./gradlew --stop

# 清理无用缓存目录
rm -rf ~/.gradle/build-scan-data/*
rm -rf ~/.gradle/kotlin-profile/*
rm -rf ~/.gradle/notifications/*
rm -rf ~/.gradle/workers/*
rm -f ~/.gradle/android.lock

# 清理daemon旧日志（30天前）
find ~/.gradle/daemon -name "*.log" -mtime +30 -delete

# 清理依赖元数据临时缓存（不删Jar包）
rm -rf ~/.gradle/caches/journal-1/*
rm -rf ~/.gradle/caches/transforms-3/*
```

### 3、深度清理（磁盘爆满才用，会重下Jar）
> 警告：执行后下次构建会重新下载所有依赖，耗时久
```bash
./gradlew --stop
rm -rf ~/.gradle/caches/*
rm -rf ~/.gradle/android/*
rm -rf ~/.gradle/wrapper/dists/*
```

---

## 四、关键备注存档
1. 所有第三方Jar包永久存放：`caches/modules-2/files-2.1`，不要手动暴力删除；
2. 修改 `gradle.user.home` 后，IDEA、终端Wrapper、原生Gradle全部统一生效；
3. 日常瘦身优先清日志、临时缓存、分析数据，不动 `modules-2`；
4. 切换电脑/重装系统：备份 `.gradle/caches/modules-2` 即可免重复下载。
