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
            var pwr2 = 256*256;
            var pwr3 = 256*pwr2;
            var array = new boolean[Integer.MAX_VALUE/16 + 1][32];
            AtomicLong count = new AtomicLong(0);
            reader.lines().forEach( line -> {
                var splitted = line.split("\\.");
                var intValue = pwr3 * Integer.parseInt(splitted[0]) + pwr2 * Integer.parseInt(splitted[1]) +
                        256 * Integer.parseInt(splitted[2]) + Integer.parseInt(splitted[3]);
                var index = Integer.divideUnsigned(intValue, 32);
                var rest = Integer.remainderUnsigned(intValue, 32);
                if (!array[index][rest] ) {
                    array[index][rest] = true;
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
}
