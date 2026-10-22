# Ozon 跨境电商店铺管理 ERP 系统

面向 Ozon 卖家的单店/多店运营后台，覆盖 **商品上传与更新、价格与库存、退货、FBS/rFBS 订单、聊天、运单、财务分析、属性特征** 八大业务。
前后端不分离：Spring Boot + MyBatis-Plus 提供 REST 与 Thymeleaf 页面，打包为单个 jar 即可运行。

## 技术栈

| 项目 | 选择 | 说明 |
|---|---|---|
| 语言 / 运行时 | Java 21 | |
| 框架 | Spring Boot 3.5.6（web + validation + thymeleaf） | 内嵌 Tomcat，单 jar 部署 |
| 持久层 | MyBatis-Plus 3.5.12（starter + jsqlparser 分页） | 逻辑删除关闭，`created_at/updated_at` 由 `MetaFieldHandler` 自动填充 |
| 数据库 | 开发：H2 文件库（MySQL 兼容模式）；生产：MySQL 8 | 一键切换 profile，见下文 |
| 页面 | Thymeleaf + 自研 `app.css` / `app.js` | Bootstrap 风格但零外部 CDN 依赖，图表为原生 canvas 实现 |
| Ozon 集成 | `OzonApiClient`（基于 Spring `RestClient`） | Seller API 基址 `https://api-seller.ozon.ru`，请求头 `Client-Id` / `Api-Key` |
| 接口文档 | springdoc-openapi 2.8 | `/swagger-ui.html`、`/v3/api-docs` |
| 测试 | JUnit 5 + Mockito | 40 个用例（含 2 个默认禁用的冒烟测试），见「测试」章节 |

## 快速开始

```bash
# 1. 确认 JDK 21（JAVA_HOME 指向 JDK 21）
java -version

# 2. 配置 Ozon 密钥（三选一，见「密钥配置」章节）

# 3. 启动
mvn spring-boot:run
# 或先打包再运行
mvn -DskipTests package && java -jar target/ozon-erp-system.jar
```

启动后访问：

| 入口 | 地址 |
|---|---|
| 管理后台首页（仪表盘） | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 控制台（仅 dev） | http://localhost:8080/h2-console（JDBC URL 见 `application-dev.yml`，用户 `sa`，空密码） |

**首次使用建议顺序**：`店铺设置` 填密钥并「连通性测试」→ `商品管理` 点「同步」→ `价格库存` 同步 → `订单管理` 同步 → 其余模块按需同步。
仪表盘与所有列表页都只读本地表，不会一进页面就消耗 Ozon 配额。

## 密钥配置

`OzonCredentialProvider` 的取值优先级：**配置文件 / 环境变量 → `shop_config` 表中启用的店铺**；
占位符 `your-client-id`、`your-api-key` 视为「未配置」。三种方式任选其一：

```bash
# 方式一：环境变量（推荐，密钥不进版本库）
set OZON_CLIENT_ID=你的ClientId        # Windows cmd
set OZON_API_KEY=你的ApiKey
mvn spring-boot:run

# 方式二：直接改 src/main/resources/application.yml 的 ozon.client-id / ozon.api-key

# 方式三：启动后在「店铺设置」页面新增店铺并启用（api_key 存库，列表页脱敏展示）
```

未配置密钥时，所有「同步」类接口会返回 `502` 与中文提示（`未找到可用的 Ozon Client-Id…`），页面仍可正常浏览。

## 功能与路由

### 页面（Thymeleaf，服务端渲染）

| 路径 | 模块 | 说明 |
|---|---|---|
| `/` | 仪表盘 | 商品数 / 待发货 / 退货数 / 零库存 / 未读聊天 + 待办清单 |
| `/products` | 商品管理 | 列表（关键字、状态筛选、分页、同步） |
| `/products/upload` | 商品管理 | 按类目属性模板上传/更新商品，展示导入 `task_id` 与任务状态 |
| `/products/{id}` | 商品管理 | 商品详情（本地缓存 + 原始报文） |
| `/price-stock` | 价格库存 | 行内编辑价格/库存并批量提交 |
| `/attributes` | 属性特征 | 类目树 → 属性模板 → 字典值翻页；同步商品已填属性 |
| `/orders` | 订单管理 | FBS/rFBS 列表（状态、scheme、关键字筛选） |
| `/orders/{postingNumber}` | 订单管理 | 订单详情、包装标签下载、取消订单（含取消原因） |
| `/shipments` | 运单处理 | 待填单号订单、批量录入 `tracking_number`、发货标签、仓库列表 |
| `/returns` | 退货管理 | 退货列表与状态筛选 |
| `/chats`、`/chats/{chatId}` | 聊天管理 | 聊天室（未读标记）与消息收发 |
| `/finance` | 财务分析 | 交易流水 + analytics 看板（折线图） |
| `/settings` | 店铺设置 | 多店铺增删改、启停、连通性测试 |

### REST 接口（页面 fetch 调用，统一 `Result<T>`）

| 前缀 | 主要接口 |
|---|---|
| `/api/dashboard` | `GET /` |
| `/api/products` | `POST /sync`、`GET /`、`GET /{id}`、`POST /import`、`GET /import/status/{taskId}` |
| `/api/price-stock` | `POST /sync`、`GET /`、`POST /prices`、`POST /stocks` |
| `/api/attributes` | `GET /category-tree`、`GET /category/{categoryId}/{typeId}`、`GET /values?attr_id=&category_id=&type_id=&last_id=`、`POST /sync`、`GET /product/{productId}` |
| `/api/orders` | `POST /sync?scheme=fbs 或 rfbs`、`GET /`、`GET /remote/{postingNumber}`、`GET /{postingNumber}/cancel-reasons`、`POST /cancel` |
| `/api/shipments` | `GET /warehouses`、`GET /pending`、`POST /tracking`、`POST /label` |
| `/api/returns` | `POST /sync`、`GET /` |
| `/api/chats` | `POST /sync`、`GET /rooms`、`POST /{chatId}/sync`、`GET /{chatId}/messages`、`POST /send`、`GET /unread` |
| `/api/finance` | `POST /transactions`、`POST /analytics` |
| `/api/shops` | `GET /`、`POST /`、`DELETE /{id}`、`GET /ready`、`POST /test` |

返回体统一为：

```json
{ "code": 0, "message": "success", "data": { "records": [], "total": 0, "current": 1, "size": 20 } }
```

异常映射（`GlobalExceptionHandler` 只作用于 `controller.api` 包）：

| 场景 | HTTP | code |
|---|---|---|
| 业务校验失败（`DomainException`） | 400 | 400 |
| Ozon 接口失败 / 密钥缺失（`OzonApiException`） | 502 | 502 |
| 参数校验或参数绑定失败（缺参、类型错误） | 400 | 400 |
| 未预期异常 | 500 | 500（仅记录堆栈） |

页面路由的异常走 Spring Boot 错误视图 `templates/error.html`。

> JSON 字段一律 **snake_case**（`spring.jackson.property-naming-strategy: SNAKE_CASE` + `default-property-inclusion: non_null`），
> 与 Ozon 报文风格一致，前端提交的请求体也按 snake_case 组装。

## 数据模型

`schema.sql` 在启动时自动执行（`CREATE TABLE IF NOT EXISTS`，H2/MySQL 通用 DDL）：

| 表 | 用途 |
|---|---|
| `shop_config` | 店铺与密钥（为多店铺预留，`enabled=1` 的启用店铺参与凭证回退） |
| `product` | 商品缓存（sku、offer_id、名称、品牌、类目、状态、主图、原始报文） |
| `product_attribute` | 商品已填属性/特征 |
| `price_stock` | 价格与库存快照（price/old_price/min_price/stock/warehouse） |
| `posting` | FBS/rFBS 订单（状态、scheme、运单号、商品行/财务/客户 JSON） |
| `return_item` | 退货记录 |
| `chat_room` / `chat_message` | 聊天室与消息（含未读数） |

**列表页数据策略：本地表优先 + 手动/定时同步**。同步任务把 Ozon 数据落到本地表，页面查询与分页全部走 MyBatis-Plus，
既保证翻页与筛选体验，也避免反复触碰 Ozon 限流。

### 定时同步（默认关闭）

```yaml
ozon:
  sync:
    enabled: true          # 打开后按周期同步 商品/价格库存/订单/退货/聊天
    interval-ms: 1800000   # 30 分钟
    initial-delay-ms: 60000
```

## 切换 MySQL

1. 建库：`CREATE DATABASE ozon_erp DEFAULT CHARACTER SET utf8mb4;`
2. 按需修改 `application-mysql.yml` 中的 url / 账号 / 密码（表结构由 `schema.sql` 自动创建）。
3. 启动时指定 profile：

```bash
java -jar target/ozon-erp-system.jar --spring.profiles.active=mysql
```

## 架构分层

```
com.kw
├── Application                         ← 启动类（@MapperScan("com.kw.erp.mapper")）
└── erp
    ├── client        ← OzonApiClient（鉴权头/超时/限流重试/错误解析）+ OzonCredentialProvider + dto.<模块>
    ├── entity        ← MyBatis-Plus 实体
    ├── mapper        ← BaseMapper
    ├── service       ← 业务编排（同步 / 分页查询 / 动作提交）+ SyncScheduler
    │   └── impl
    ├── controller
    │   ├── page      ← Thymeleaf 视图路由（模型注入）
    │   └── api       ← REST（供页面 fetch 调用）
    ├── common        ← Result / PageResult
    ├── exception     ← DomainException / OzonApiException / GlobalExceptionHandler
    ├── config        ← OzonProperties / MyBatisPlusConfig / MetaFieldHandler
    └── util          ← JsonUtils（Ozon 报文容错解析）
```

一次典型请求：`浏览器 → page controller 渲染列表 → 页面 fetch 调 /api/... → service → OzonApiClient → 落本地表 → 页面刷新`。

`OzonApiClient` 对 `429`、`5xx` 与 IO 异常各重试一次（间隔 1.2s），并把 Ozon 响应里的 `message` / `code` 提取为中文可读的错误信息。
`JsonUtils` 负责兼容 Ozon 多变的字段命名与日期格式（含 `0001-01-01T00:00:00Z` 之类的零值时间一律忽略）。

## 测试

```bash
mvn test
```

| 测试类 | 覆盖内容 |
|---|---|
| `JsonUtilsTest` | 报文容错解析：多候选字段、数值字符串、日期格式、零值时间、snake_case 序列化 |
| `DtoJsonTest` | DTO 与 Ozon 报文形状对齐（请求体字段名、响应体样本反序列化） |
| `ProductServiceImplTest` | 同步落库（新增/更新）、详情补全失败不阻断、游标不前进时终止翻页、上传参数校验 |
| `ShipmentServiceImplTest` | 运单号录入校验、本地订单同步更新、标签请求校验 |
| `OzonCredentialProviderTest` | 配置优先 / 占位符回退店铺 / 完全缺失时 `isReady()=false` 并抛业务异常 |
| `ApiErrorContractTest` | REST 错误契约：缺参/参数类型错误/业务异常 → 400，Ozon 异常 → 502，未预期异常 → 500 |
| `OzonApiClientSmokeTest` | **默认 `@Disabled`**：需真实密钥与外网，本地自测时设置 `OZON_CLIENT_ID` / `OZON_API_KEY` 后去掉注解运行 |

## 已知限制与设计取舍

- **无登录鉴权**：定位为本地/内网单人使用的运营工具，未引入 Spring Security；对外暴露前请自行加认证与 HTTPS。
- **密钥存储**：`shop_config` 中的 `api_key` 为明文存储（页面列表脱敏展示），如需更高安全级别请只使用环境变量方式。
- **单店铺起步**：多店铺数据表已预留，但同步时全局使用「当前启用店铺」的凭证，尚未做按店铺隔离。
- **金额不做汇率换算**：卢布/人民币按 Ozon 返回原样展示。
- **聊天/退货接口分页**：Ozon 部分接口为游标或增量分页，同步采用「有限次数循环 + 上限保护」，超大数据量建议开启定时同步而非反复手动同步。
- **图片与 PDF**：发货标签以 base64 返回并由浏览器下载，不在服务端留存文件。
