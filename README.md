# RemoteLogin

RemoteLogin 是一个基于 LSPosed 框架的系统级工具，旨在通过 WebSocket 长连接，实现跨设备的 Android Intent 远程安全转发，完美解决“多设备间的授权拉起”等痛点场景。

## 🏗 架构设计

系统由 **Android LSPosed 模块** 和 **Node.js WebSocket 转发网关** 两部分组成。

* **Android 端**：基于底层的 `system_server` 进程 Hook。
* **Server 端**：采用 **Caddy (反向代理 + 边缘鉴权) + Node.js (状态机转发)** 的零信任安全架构。
* **序列化与数据传输**：在跨设备转发 Intent 的过程中，采用了**手动深度序列化 Intent 和 Bundle** 的方案。将复杂的 Intent 结构转换为一致的标准格式进行网络传输，这彻底规避了因不同 Android 版本之间 Intent 内部字段增删、以及 Bundle LazyValue 差异所导致的跨设备反序列化崩溃风险，极大提升了多设备协同的兼容性。

---

## 📱 模块安装与配置 (客户端)

### 1. LSPosed 作用域设置 (⚠️ 必看)
安装完模块后，在 LSPosed 管理器中启用模块时，**必须勾选 `系统框架` (system)**。
> **原因：** 本模块需要在系统框架进程中（如 `ActivityStarter` 环节）介入 Intent 调度。如果不勾选系统框架，模块将无法生效。

### 2. 角色配置 (Role)
本系统将设备划分为两种角色。请在模块的 UI 界面中为不同设备分配对应的角色：

* **CLIENT (发起端)**：
    * **职责**：将本地的 Intent 序列化后，发送给远端的 `SUPER_HOST` 去执行。
    * **配置**：默认即为此配置。
* **SUPER_HOST (主机端)**：
    * **职责**：负责实际拉起目标应用，处理业务逻辑。
    * **配置**：在root权限的shell上执行`setprop persist.wrlu.rl.config.role super_host`

---

## ☁️ 服务端部署 (Node.js + Caddy)

服务端强烈建议采用 **Caddy 网关边缘鉴权** 方案，这能彻底保护你的 Node.js 进程免受公网恶意流量的攻击，并自动配置 `wss://` 全链路加密。

### 1. Caddyfile 参考配置
在服务器上安装 Caddy v2，并使用以下配置（请将 `api.wrlu.net` 替换为你的真实域名，并将 `xxxx` 替换为你生成的强 Token）：

```
api.wrlu.net {
    encode gzip

    # 拦截所有没有携带正确 Bearer Token 的请求
    @unauthorized {
        not header Authorization "Bearer xxxx"
    }
    respond @unauthorized "Unauthorized: Invalid Bearer Token" 401

    @intent_websocket path /ws/intent/*
    reverse_proxy @intent_websocket localhost:8888 {
        header_up Host {host}
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        
        flush_interval -1
    }
}
```

---

## 🔒 安全与鉴权机制 (Token 配置)

由于 Intent 中可能包含极其敏感的账号授权凭证，本系统实现了严格的“纵深防御”体系：

1. **传输层安全**：强制依赖 Caddy 提供的 TLS 加密（WSS），杜绝物理设备 ID（UUID）在公网明文传输被抓包。
2. **标准 OAuth 2.0 格式鉴权**：
   在 Android 设备上，你需要填入与 Caddy 服务端一致的 Token，在root权限的shell上执行`setprop persist.wrlu.rl.config.token xxxx`。
   App 在底层网络请求时，会自动在 HTTP 握手阶段附加标准请求头：
   `Authorization: Bearer <Your_Token>`
3. **互斥防劫持**：
   Node.js 内部实现了单设备唯一性校验。如果检测到同一个 DeviceId 在异地重复登录，服务端会主动发送 `4001` 状态码踢除旧连接。

## ⚠️ 局限性与已知风险 (Limitations)

当前的 `RemoteIntent` 序列化机制存在特定的技术限制。在设计和处理复杂业务 Intent 时，请注意以下数据未被序列化引发的丢失与崩溃风险：

### 1. `ClipData` 与物理 URI 授权失效风险
* **问题表现**：当前序列化机制未处理 `intent.getClipData()` 中的数据，同时忽略了针对文件的读写授权标志位。
* **技术后果**：当发起端尝试通过 Intent 分享本地图片、文档或媒体文件（如通过 `FileProvider` 生成的 `content://` 协议 URI）时，远端接收到的 Intent 将不包含这些文件引用。
* **原因说明**：跨设备传递本地文件 URI 在技术逻辑上是无效的。远端设备的物理存储中不存在发起端的文件；同时，Android 系统的 `FLAG_GRANT_READ_URI_PERMISSION` 临时读取授权仅在单台设备的当前运行生命周期内有效，无法跨物理设备转移。目标应用在远端尝试解析该 URI 时，必然引发 `SecurityException` 或 `FileNotFoundException`。

### 2. 自定义 `Parcelable` 对象的解析异常风险
* **问题表现**：若 Intent 的 `Extras (Bundle)` 中夹带了第三方应用特有的 `Parcelable` 或 `Serializable` 自定义类对象，这些数据在序列化或反序列化阶段会丢失。
* **技术后果**：远端目标应用在执行 `getParcelableExtra()` 提取该对象时将获取到 `null`，导致应用逻辑异常或直接引发 `NullPointerException`。
* **原因说明**：跨设备还原自定义对象要求反序列化的执行环境拥有完全一致的类定义（即相同的 `ClassLoader`）。作为通用的底层系统转发组件，本模块无法获取并加载目标第三方应用的私有类代码。在缺少类定义的情况下强行解析未知的二进制数据块，会触发 Android 框架底层的 `BadParcelableException`。