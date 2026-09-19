# 数据库脚本说明

默认数据库名：**`test`**（与 `application.yml` 一致）。

## 安装

```bash
mysql -u root -p < schema.sql
```

或在客户端中执行 `schema.sql` 全文。会创建库表、管理员账号（`admin`/`123456`）、15 道入门题和一场示例比赛。随后可执行同目录的 `question_bank_expansion.sql`，额外导入 25 道原创基础算法题；脚本可重复执行且不会覆盖已有题目。

| 项 | 值 |
|----|-----|
| 管理员账号 | `admin` |
| 管理员密码 | `123456` |

> 仅当 `app.security.pepper` 为空（默认）时密码有效；生产环境请尽快改密。

配置好数据库连接（`.env` 或 `application-local.yml`）后：

1. 在 **Linux + Docker** 机器上启动 **Sandbox**（判题必需，见仓库 [Sandbox/README.md](../../../Sandbox/README.md)）
2. 再启动主服务 `spingboot-init`，即可用 `admin` / `123456` 登录并提交代码

## 重置

```sql
DROP DATABASE IF EXISTS `test`;
```

然后重新执行 `schema.sql`。
