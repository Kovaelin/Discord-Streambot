package ovh.gyoo.bot;

import net.dv8tion.jda.entities.impl.JDAImpl;
import net.dv8tion.jda.utils.SimpleLog;
import ovh.gyoo.bot.data.DiscordInstance;
import ovh.gyoo.bot.data.LocalServer;
import ovh.gyoo.bot.data.ServerList;
import ovh.gyoo.bot.handlers.TwitchChecker;
import ovh.gyoo.bot.listeners.MessageConsumer;
import ovh.gyoo.bot.writer.Logger;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        boolean startup = true;
        try {
            SimpleLog.addFileLogs(null,new File("err" + System.currentTimeMillis() + ".txt"));
        } catch (IOException e) {
            Logger.writeToErr(e.getMessage());
        }
        // Twitch
        TwitchChecker.getInstance();
        // Discord
        DiscordInstance.getInstance();
        // Backup data after restart
        File f = new File("ServerList.xml");
        if(f.exists()){
            List<LocalServer> servers = Logger.loadData("ServerList.xml");
            for(LocalServer server : servers){
                ServerList.getInstance().addServer(server.getServerID(), server);
            }
        }
        // Message Consumer (anti rate limiter)
        Thread messageConsumer = new MessageConsumer(DiscordInstance.getInstance().getQueue());
        messageConsumer.start();

        int ticks = 0;
        while(true){

            if (!((JDAImpl) DiscordInstance.getInstance().getDiscord()).getClient().isConnected())
            {
                Thread.sleep(1000);
                continue;
            }

            if(ticks < 180) {
                TwitchChecker.getInstance().checkStreams(startup);
                ticks++;
            }
            else {
                TwitchChecker.getInstance().checkStillOnline();
                ticks = 0;
            }

            startup = false;

            Logger.saveData(ServerList.getInstance().getMap(), "ServerList.xml");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Logger.writeToErr(e.getMessage());
                break;
            }
        }
    }


}
