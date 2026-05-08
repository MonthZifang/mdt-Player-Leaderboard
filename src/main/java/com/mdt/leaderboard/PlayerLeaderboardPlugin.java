package com.mdt.leaderboard;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

public final class PlayerLeaderboardPlugin extends Plugin {
    @Override
    public void init() {
        Log.info("MDT 玩家排行榜 loaded.");
        Log.info("配置目录建议: config/mods/config/mdt-player-leaderboard");
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("leaderboard-show", "[page]", "输出当前排行榜指定页。", args -> {
            Log.info("MDT 玩家排行榜 命令占位已触发: leaderboard-show");
        });

        handler.register("leaderboard-field", "<field>", "切换当前排行榜使用的数据字段。", args -> {
            Log.info("MDT 玩家排行榜 命令占位已触发: leaderboard-field");
        });

        handler.register("leaderboard-reload", "重新加载排行榜配置。", args -> {
            Log.info("MDT 玩家排行榜 命令占位已触发: leaderboard-reload");
        });

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("leaderboard", "[page]", "查看玩家排行榜。", (args, player) -> {
            player.sendMessage("[accent]MDT 玩家排行榜[] 命令占位已触发: leaderboard");
        });

    }
}
