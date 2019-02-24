package com.github.manolo8.darkbot.core.objects.swf;

import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.utils.ByteUtils;
import com.github.manolo8.darkbot.core.utils.Lazy;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.manolo8.darkbot.Main.API;

public class Dictionary extends Updatable {

    private HashMap<String, Lazy<Long>> lazy;

    private Entry[] elements;
    private String[] checks;

    public int size;

    private int lastFix;

    public Dictionary(long address) {
        this.address = address;
        this.elements = new Entry[0];
    }

    public void addLazy(String key, Consumer<Long> consumer) {
        if (lazy == null) lazy = new HashMap<>();

        Lazy<Long> lazy = this.lazy.get(key);

        if (lazy == null) {
            lazy = new Lazy<>();
            this.lazy.put(key, lazy);
        }

        lazy.add(consumer);
    }

    @Override
    public void update() {


        long tableInfo = API.readMemoryLong(address + 32);
        int size = API.readMemoryInt(tableInfo + 16);
        long table = API.readMemoryLong(tableInfo + 8) - 4;
        int length = (int) (Math.pow(2, API.readMemoryInt(tableInfo + 20)) * 4) + lastFix + 4;

        if (length > 4096 || length < 0 || size < 0 || size > 1024) return;

        if (elements.length < size) {
            checks = new String[size];
            Entry[] temp = new Entry[size];
            System.arraycopy(elements, 0, temp, 0, elements.length);
            elements = temp;
        }

        byte[] bytes = API.readMemory(table, length);

        int current = 0;
        int check = 0;

        for (int i = searchFix(bytes); i < length; i += 16) {

            long valueAddress = ByteUtils.getLong(bytes, i + 8) - 1;

            if (valueAddress >= -1 && valueAddress <= 9) continue;

            Entry entry;

            if (current >= size) {
                break;
            } else if (elements[current] == null) {
                entry = elements[current] = new Entry(API.readMemoryString(ByteUtils.getLong(bytes, i) - 2), valueAddress);
                send(entry);
            } else if ((entry = elements[current]).value != valueAddress) {
                checks[check++] = entry.key;

                entry.key = API.readMemoryString(ByteUtils.getLong(bytes, i) - 2);
                entry.value = valueAddress;

                //send update
                send(entry);
            }

            current++;
        }

        if (this.size > current) {
            while (this.size != current) {
                checks[check++] = elements[--this.size].key;
                elements[this.size] = null;
            }
        }

        this.size = current;

        if (check > 0) {
            for (check--; check >= 0; check--) {
                String str;

                if (!hasKey(str = checks[check]))
                    send(str, 0);

                checks[check] = null;
            }
        }
    }

    private int searchFix(byte[] bytes) {

        if (lastFix >= 0 && bytes.length > lastFix && ByteUtils.getInt(bytes, lastFix) == BotInstaller.SEP) {
            return lastFix + 4;
        } else {
            for (int i = 0; i < bytes.length; i++) {
                if (ByteUtils.getInt(bytes, i) == BotInstaller.SEP) {
                    lastFix = i;
                    return bytes.length - 1;
                }
            }
        }

        return bytes.length - 1;
    }

    private void send(Entry entry) {
        send(entry.key, entry.value);
    }

    private void send(String key, long value) {
        if (lazy != null) {
            Lazy<Long> lazy = this.lazy.get(key);

            if (lazy != null && !(Objects.equals(lazy.value, value))) {
                lazy.send(value);
            }
        }
    }

    private boolean hasKey(String str) {
        for (int i = 0; i < size; i++) {
            Entry entry = elements[i];
            if (entry.key.equals(str))
                return true;
        }

        return false;
    }

    @Override
    public void update(long address) {
        super.update(address);
        this.lastFix = 0;
    }

    private class Entry {

        String key;
        long value;

        public Entry(String key, long value) {
            this.key = key;
            this.value = value;
        }
    }
}
