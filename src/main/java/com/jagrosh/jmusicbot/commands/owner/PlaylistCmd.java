/*
 * Copyright 2018-2020 Cosgy Dev
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.OwnerCommand;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;

import java.io.IOException;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistCmd extends OwnerCommand {
    private final Bot bot;

    public PlaylistCmd(Bot bot) {
        this.bot = bot;
        this.guildOnly = false;
        this.name = "playlist";
        this.arguments = "<append|delete|make|setdefault>";
        this.help = "再生リスト管理";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new OwnerCommand[]{
                new ListCmd(),
                new AppendlistCmd(),
                new DeletelistCmd(),
                new MakelistCmd(),
                new DefaultlistCmd(bot)
        };
    }

    @Override
    public void execute(CommandEvent event) {
        StringBuilder builder = new StringBuilder(event.getClient().getWarning() + " 再生リスト管理コマンド:\n");
        for (Command cmd : this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }

    public class MakelistCmd extends OwnerCommand {
        public MakelistCmd() {
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "新しい再生リストを作る";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if (bot.getPlaylistLoader().getPlaylist(pname) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " `" + pname + "`という名前で再生リストを作成しました!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " 再生リストを作成できませんでした。:" + e.getLocalizedMessage());
                }
            } else
                event.reply(event.getClient().getError() + " 再生リスト `" + pname + "` はすでに存在しています!");
        }
    }

    public class DeletelistCmd extends OwnerCommand {
        public DeletelistCmd() {
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "既存の再生リストを削除します";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if (bot.getPlaylistLoader().getPlaylist(pname) == null)
                event.reply(event.getClient().getError() + " 再生リスト `" + pname + "` は存在しません!");
            else {
                try {
                    bot.getPlaylistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " 再生リスト `" + pname + "`を削除しました。!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " 再生リストを削除できませんでした: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public class AppendlistCmd extends OwnerCommand {
        public AppendlistCmd() {
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = "既存の再生リストに曲を追加します";
            this.arguments = "<name> <URL> | <URL> | ...";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) {
            String[] parts = event.getArgs().split("\\s+", 2);
            if (parts.length < 2) {
                event.reply(event.getClient().getError() + " 追加先の再生リスト名とURLを含めてください。");
                return;
            }
            String pname = parts[0];
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + " 再生リスト `" + pname + "` は存在しません!");
            else {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = parts[1].split("\\|");
                for (String url : urls) {
                    String u = url.trim();
                    if (u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    builder.append("\r\n").append(u);
                }
                try {
                    bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + " 項目を再生リスト `" + pname + "`に追加しました!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " 再生リストに追加できませんでした: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public class DefaultlistCmd extends AutoplaylistCmd {
        public DefaultlistCmd(Bot bot) {
            super(bot);
            this.name = "setdefault";
            this.aliases = new String[]{"default"};
            this.arguments = "<playlistname|NONE>";
            this.guildOnly = true;
        }
    }

    public class ListCmd extends OwnerCommand {
        public ListCmd() {
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = "利用可能なすべての再生リストを表示します。";
            this.guildOnly = true;
        }

        @Override
        protected void execute(CommandEvent event) {
            if (!bot.getPlaylistLoader().folderExists())
                bot.getPlaylistLoader().createFolder();
            if (!bot.getPlaylistLoader().folderExists()) {
                event.reply(event.getClient().getWarning() + " 再生リストフォルダが存在しないため作成できませんでした。");
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames();
            if (list == null)
                event.reply(event.getClient().getError() + " 利用可能な再生リストを読み込めませんでした。");
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + " 再生リストフォルダに再生リストがありません。");
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " 利用可能な再生リスト:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }
    }
}
