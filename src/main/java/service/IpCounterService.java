package service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class IpCounterService {

    public int countDistinctIpAddressesNaive(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader(name))) {
            Set<String> set = new HashSet<>();
            reader.lines().forEach(set::add);
            return set.size();
        } catch (IOException e){
            e.printStackTrace();
            return -1;
        }
    }

    public long countDistinctIpAddressesOptimized(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader(name))) {
            var elemSize = 32;
            var array = new int[Integer.MAX_VALUE/elemSize * 2 + 1];
            AtomicLong count = new AtomicLong(0);
            reader.lines().forEach( line -> {
                var intValue = parseIpToInt(line);
                var index = Integer.divideUnsigned(intValue, elemSize);
                var rest = Integer.remainderUnsigned(intValue, elemSize);
                var bitMask = 1 << rest;
                if ((array[index] & bitMask) == 0) {
                    array[index] = array[index] | bitMask;
                    count.getAndIncrement();
                }
            });
            return count.longValue();
        } catch (IOException e){
            return -1;
        }
    }

    private int parseIpToInt(String line) {
        int a = 0, b = 0, c = 0, d = 0;
        int num = 0;
        int part = 0;

        for (int i = 0, n = line.length(); i < n; i++) {
            char ch = line.charAt(i);
            if (ch == '.') {
                switch (part) {
                    case 0: a = num; break;
                    case 1: b = num; break;
                    case 2: c = num; break;
                }
                num = 0;
                part++;
            } else {
                num = num * 10 + (ch - '0');
            }
        }
        d = num;
        return (a << 24) | (b << 16) | (c << 8) | d;
    }
}
