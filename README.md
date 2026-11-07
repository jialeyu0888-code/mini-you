# 小小你 · Mini You 孕期管理

一款面向准妈妈的 Android 原生孕期管理 App（中文界面）。基于 Kotlin + Jetpack Compose + Material 3，数据全部保存在手机本地（Room），并接入智谱 **GLM-5.3-Flash** 视觉理解模型，拍照即可自动填写产检单、购物记录等表单。

> 界面针对 **OPPO Find N5** 折叠屏做了适配：外屏（约 434dp 宽）使用底部导航；展开内屏（约 856dp 宽，接近方屏）自动切换为侧边导航 + 双栏/多列布局（列表 + 详情并排）。折叠、展开时页面不重建，正在填写的表单不会丢失。

## 功能介绍

| 模块 | 功能 |
| --- | --- |
| 首页 | 插画风格首页（适配 OPPO Find N5 外屏和展开后的内屏）：孕周、距预产期天数、宝宝大小、下次产检、本阶段产检计划、今日打卡，以及产检、日记、购物、打卡、攻略、课堂入口。首次启动填写末次月经或预产期（也可拍 B 超单让 AI 推算） |
| 产检记录 | 增删改查；**AI 识图**：拍照或从相册选择产检单（最多 5 张），自动解析检查日期、医院、孕周、检查项目、体重/血压/胎心、各项指标（血常规、尿常规、B 超数据等，含参考范围与偏高/偏低标记）、医生建议、下次产检日期与准备事项，生成可编辑记录；保存后在下次产检**前一天 20:00 和当天 8:00 本地通知提醒**；体重变化曲线；内置孕期产检时间表参考 |
| 孕期日记 | 心情（9 种表情）、标签（可自定义）、文字、照片；月历视图标注每天心情；**AI 看图写日记**草稿 |
| 购物记录 | 三种状态：计划购买 / 决定未买 / 已购买；点进商品先看详情，再进入编辑。分类可自定义；记录价格、数量、渠道、图片；统计已花费与待购预算。**AI 识图**一次最多 8 张：一张订单截图里的多件商品会逐条列出来，核对后批量导入 |
| 孕期打卡 | 默认叶酸、钙片、DHA、维生素 D3，打卡项可增删改（名称、表情、用法备注）；每日打卡、连续天数、累计天数、近 7 天记录；每个打卡项可单独设置每日提醒（已打卡则当天不再提醒）；**AI 识别药瓶/包装**自动填写名称和用法 |
| 孕肚记录 | 上传孕肚照片，自动带上拍摄当天的孕周，可写一句纪念文字（也能让 AI 看照片写）；按时间排列，有两张以上时显示最早和最近的对比 |
| 缓解攻略 | 18 种常见孕期不适、99 个缓解方法；按症状分类、孕期筛选和关键词搜索；药物类方法醒目标注「需遵医嘱」；每个症状附「何时就医」与通用危险信号；首次进入弹出免责声明，详情页底部常驻声明。每个方法可以标记「对我有效」；也可以自己记录有效的方法（症状、做法、效果），支持拍笔记或医嘱截图让 AI 整理 |
| 准爸妈课堂 | 0–6 个月新生儿照护（喂养、日常护理、睡眠安抚、健康就医、成长发育）+ 产后妈妈，共 6 章 15 节课；分步骤讲解、小贴士、常见误区、何时就医、疫苗接种参考表；常驻 120 急救提示 |
| 设置 | 修改孕期信息与称呼；查看/替换/恢复智谱 API Key 并测试连接；通知权限状态 |

凡是有表单的地方都提供「AI 识图快速填写」入口；AI 结果只会填进表单，需要你核对后手动保存。

## 下载与安装 APK

### 方式一：直接下载（推荐）

1. 打开仓库的 [Releases 页面](https://github.com/jialeyu0888-code/mini-you/releases)，在最新版本 `v0.2.2` 下载 `mini-you-v0.2.2.apk`。
2. 手机上打开下载的 APK，如提示「禁止安装未知来源应用」，按提示允许浏览器/文件管理器安装即可（OPPO/ColorOS：设置 → 安全 → 安装外部来源应用）。
3. 首次打开时请允许「通知」权限，用于产检和打卡提醒。

### 方式二：用电脑 + 数据线安装

```bash
# 1. 在电脑上克隆仓库
git clone https://github.com/jialeyu0888-code/mini-you.git
cd mini-you

# 2. 下载 Release 中的 APK（或按下文自行构建）
# 3. 手机开启「开发者选项 → USB 调试」，连上电脑后执行：
adb install -r mini-you-v0.2.2.apk
```

`adb` 来自 Android SDK Platform-Tools（[下载地址](https://developer.android.com/tools/releases/platform-tools)）。

## 从源码构建

### 环境要求

- JDK 17 或更高版本（已在 JDK 21 上验证）
- Android SDK：Platform 35、Build-Tools 35.0.0（用 Android Studio 打开项目会自动安装，或用 `sdkmanager` 安装）
- 无需单独安装 Gradle，项目自带 Gradle Wrapper（8.11.1）

### 1. 配置 SDK 路径和智谱 API Key

在仓库根目录创建 `local.properties`（该文件已被 `.gitignore` 忽略，**不要提交**）：

```properties
sdk.dir=/path/to/Android/sdk
ZHIPU_API_KEY=你的智谱APIKey
```

- Key 在 [智谱开放平台](https://open.bigmodel.cn/) 的「API Keys」页面获取。
- 构建时 Key 会被注入 `BuildConfig.ZHIPU_API_KEY` 作为内置 Key；也可以用环境变量 `ZHIPU_API_KEY` 提供。
- 不配置也能正常构建，此时 App 内没有内置 Key，需要在「设置 → AI 识图」里填写自己的 Key。
- App 里「设置 → AI 识图」可以随时替换为自己的 Key（保存在本机），也可以一键恢复内置 Key，并能测试连接是否可用。

### 2.（可选）配置正式签名

在仓库根目录创建 `keystore.properties`（已被忽略，**不要提交**；`.jks` 文件也不要放进仓库）：

```properties
storeFile=/absolute/path/to/your-release.jks
storePassword=******
keyAlias=your-alias
keyPassword=******
```

生成自签名 keystore：

```bash
keytool -genkeypair -v -keystore ~/keys/miniyou-release.jks -alias miniyou \
  -keyalg RSA -keysize 2048 -validity 10000
```

没有 `keystore.properties` 时，release 包会自动使用本机 debug 签名，依然可以直接安装。

### 3. 构建

```bash
./gradlew assembleRelease      # 输出 app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug        # 调试包
```

Windows 下使用 `gradlew.bat`。

## 自动发布（GitHub Actions）

推送 `v*` 格式的 tag 时，`.github/workflows/release.yml` 会自动创建 GitHub Release：

- 如果 `release-assets` 分支里有 `<tag>/` 目录下预先构建好的 APK，直接把它作为 Release 附件；
- 否则在 CI 中从源码构建。可在仓库 *Settings → Secrets and variables → Actions* 中配置：
  - `ZHIPU_API_KEY`：内置的智谱 Key（不配置则用户需在设置页填写）
  - `KEYSTORE_BASE64`（`base64 -w0 your.jks` 的结果）、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`：正式签名（不配置则使用 CI 临时生成的 debug 签名）

## AI 识图实现说明

- 接口：`POST https://open.bigmodel.cn/api/paas/v4/chat/completions`（Chat Completion API），`Authorization: Bearer <API Key>`
- 模型：`glm-5.3-flash`；图片以 Base64 Data URL 放在 `messages[].content[]` 的 `image_url` 块中，一次最多发送 5 张
- 发送前会按 EXIF 纠正方向并压缩（长边 ≤ 2560px，单张 < 4MB），满足官方限制：单张 < 5MB、像素 ≤ 6000×6000、jpg/png
- `thinking.type` 只支持 `enabled`，通过 `reasoning_effort` 控制思考强度（产检单用 `high`，其余用 `low` 以加快速度）
- `response_format: {"type": "json_object"}` + 提示词中给出 JSON 结构，模型返回结构化 JSON 后再填入表单
- 实测（模拟器 + 真实请求）：一张产检单约 27–30 秒完成解析，订单截图约 10–15 秒

## 项目结构

```
app/src/main/
├── assets/content/          # 内置内容：remedies.json（缓解攻略）、classes.json（准爸妈课堂）
└── java/com/miniyou/app/
    ├── ai/                  # 智谱接口封装（ZhipuClient）与各表单的提示词/解析（AiService）
    ├── data/                # Room 实体与 DAO、设置存储、孕周计算、产检时间表、内置内容解析、图片存储
    ├── notify/              # 本地通知：产检提醒、打卡提醒、开机后重新注册
    └── ui/                  # Compose 界面：首页、产检、日记、购物、打卡、百科、设置，及自适应布局
```

## 隐私与免责

- 所有记录（产检、日记、购物、打卡、照片）只保存在手机本地，卸载 App 即清除。
- 使用 AI 识图时，你选择的图片会发送到智谱开放平台进行解析。
- 内置在 APK 里的 API Key 可能被他人从安装包中提取，公开分发时建议使用额度受限的 Key，或让用户在设置页填写自己的 Key。
- 本应用的知识内容仅供孕期健康科普参考，不能替代医生的面诊、诊断与治疗；任何药物都必须在医生或药师指导下使用。
