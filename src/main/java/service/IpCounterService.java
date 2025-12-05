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
            LocalDateTime time1 = LocalDateTime.now();
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
            System.out.println("Array: " + count);
            LocalDateTime time2 = LocalDateTime.now();
            System.out.println(Duration.between(time1, time2).toMillis());
            return count.longValue();
        } catch (IOException e){
            return -1;
        }
    }

    private int parseIpToInt(String ip) {
        var splitted = ip.split("\\.");
        return (Integer.parseInt(splitted[0]) << 24) | (Integer.parseInt(splitted[1]) << 16) |
                (Integer.parseInt(splitted[2]) << 8) | Integer.parseInt(splitted[3]);
    }
}
