package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.objects.swf.VectorInt;

import static com.github.manolo8.darkbot.Main.API;

public class PingManager implements Manager {

    private long pingAddress;
    private VectorInt lastPings;

    public int ping;

    private int currentIndex;
    private long lastCheck;
    private long time;

    public PingManager() {
        this.lastPings = new VectorInt(0);
    }

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.invalid.add(value -> pingAddress = 0);
    }

    public void tick() {
        if (pingAddress == 0) {

            searchPingManager();

        } else {

            if (System.currentTimeMillis() - lastCheck >= 1000) {
                lastPings.update();

                if (currentIndex == lastPings.size)
                    ping += 1000;
                else if (lastPings.size > 0)
                    ping = lastPings.elements[lastPings.size - 1];

                currentIndex = lastPings.size;

                lastCheck = System.currentTimeMillis();
            }
        }
    }

    private void searchPingManager() {

        if (System.currentTimeMillis() - time > 3000) {

            time = System.currentTimeMillis();

            long[] result = API.queryMemoryLong(1000, 10_000);

            for (long value : result) {

                if (API.readMemoryInt(value + 16) != 0)
                    continue;

                long address = API.readMemoryLong(value + 8);

                if (address == 0)
                    continue;

                if (API.readMemoryLong(address) == 0)
                    continue;

                if (API.readMemoryInt(address + 12) != 0 || API.readMemoryInt(address + 36) != 0)
                    continue;

                pingAddress = API.readMemoryLong(address + 40);
                lastPings.update(pingAddress);
            }
        }
    }
}
