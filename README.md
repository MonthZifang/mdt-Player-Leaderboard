# MDT 玩家排行榜

从 `player_profile` 列表数据读取指定字段并生成排行榜，适合展示等级、经验、金币、稀有货币等数值。

## 依赖

- `mdt-list-data-system`

## 配置文件

首次启动后会生成：

```text
config/mods/config/mdt-player-leaderboard/player-leaderboard.properties
```

关键配置项：

- `leaderboard.enabled`：是否启用插件
- `leaderboard.listName`：数据来源列表名
- `leaderboard.field`：默认排行字段
- `leaderboard.sortOrder`：`asc` 或 `desc`
- `leaderboard.displayCount`：每页显示数量
- `display.title`：标题
- `display.firstColor` / `display.secondColor` / `display.thirdColor`：前三名颜色

## 命令

- `leaderboard-show [page]`：查看当前排行榜页
- `leaderboard-field <field>`：切换当前排行字段
- `leaderboard-fields [limit]`：扫描并列出当前列表里可用的字段名
- `leaderboard-reload`：重载配置
- `/leaderboard [page]`：客户端查看排行榜

## 排名规则

- 字段值能解析为整数时按数值排序
- 否则按文本排序
- 同分时按显示名排序
- 显示名优先使用 `lastName`，其次 `comid`，最后回退到对象键

## 插件入口

```text
com.mdt.leaderboard.PlayerLeaderboardPlugin
```
