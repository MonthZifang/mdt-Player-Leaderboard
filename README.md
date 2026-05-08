<div align="center">
  <a href="https://github.com/MonthZifang/YUEYUEDAO-TECH">
    <img src="./md/logo.png" alt="YUEYUEDAO TECH Logo" width="720" />
  </a>

  <p><strong>YUEYUEDAO TECH 维护 MDT 玩家排行榜</strong></p>

  <p>
    <a href="https://github.com/MonthZifang/YUEYUEDAO-TECH"><strong>查看月月岛科技详情</strong></a>
  </p>
</div>

# MDT 玩家排行榜

通过调用列表数据系统读取某个值并进行排行榜计算，谁的对应素质值更高谁就排得更前。

## 市场固定识别文件

仓库根目录固定提供以下文件，供插件市场识别：

```text
market.plugin.json
plugin.json
```

## 依赖

- `mdt-list-data-system`

## 配置文件

首次启动后建议维护以下配置文件：

```text
config/mods/config/mdt-player-leaderboard/player-leaderboard.properties
```

- 支持配置排行榜读取的字段名。
- 支持升序或降序排序。
- 支持配置每页数量与刷新频率。
- 支持切换第一名、第二名、第三名的不同颜色。

## 功能说明

- 从列表数据系统读取任意可比较数值。
- 支持以素质值、经验值、货币值等字段生成排行榜。
- 支持分页显示排行榜内容。
- 适合和玩家经济系统、等级系统联动。

## 数据与写入说明

- 建议默认字段使用 `quality`，可按配置改成 `experience` 或其他字段。
- 排行只负责读值和排序，不负责写入数据。

## 命令

- `leaderboard-show [page]`：输出当前排行榜指定页。
- `leaderboard-field <field>`：切换当前排行榜使用的数据字段。
- `leaderboard-reload`：重新加载排行榜配置。
- `/leaderboard [page]`：查看玩家排行榜。

## Help 注册备注

- `help mdt-player-leaderboard`：查看 MDT 玩家排行榜 的独立命令说明。
- 中文备注建议写为“排行榜查看、排行榜字段切换、排行榜重载”。

## 插件入口

```text
com.mdt.leaderboard.PlayerLeaderboardPlugin
```

## 版本规则

- 当前插件版本：`v1`
- 当前需求市场版本：`v1`
