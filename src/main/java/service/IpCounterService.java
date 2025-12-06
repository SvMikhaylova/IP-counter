package service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
        var bitset = new int[1 << 27];
        AtomicLong count = new AtomicLong(0);
        processFile(Paths.get(name), bitset, count);
        return count.longValue();
    }

    private void processFile(Path path, int[] bitset,
                                    AtomicLong count) {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {

            final int BUFFER_SIZE = 1 << 18;  // 64 KB
            ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);

            long pos = 0;
            long remaining = ch.size();

            int a = 0, b = 0, c = 0, d = 0;
            int part = 0; // which octet 0..3

            while (remaining > 0) {
                int readSize = (int)Math.min(remaining, BUFFER_SIZE);

                buf.clear();
                buf.limit(readSize);

                int n = ch.read(buf, pos);
                if (n == -1) break;
                buf.flip();

                for (int i = 0; i < n; i++) {
                    byte chb = buf.get(i);

                    // DIGITS
                    if (chb >= '0' && chb <= '9') {
                        int digit = chb - '0';
                        switch (part) {
                            case 0 -> a = a * 10 + digit;
                            case 1 -> b = b * 10 + digit;
                            case 2 -> c = c * 10 + digit;
                            case 3 -> d = d * 10 + digit;
                        }
                        continue;
                    }

                    // DOT
                    if (chb == '.') {
                        part++;
                        continue;
                    }

                    // NEWLINE or CARRIAGE RETURN
                    if (chb == '\n' || chb == '\r') {
                        if (part == 3) {
                            int ip = (a << 24) | (b << 16) | (c << 8) | d;
                            setBitAndCount(bitset, count, ip);
                        }
                        a = b = c = d = 0;
                        part = 0;
                        continue;
                    }

                    // Invalid char -> reset
                    a = b = c = d = 0;
                    part = 0;
                }

                pos += n;
                remaining -= n;
            }

            // FINAL FLUSH (last line without newline)
            if (part == 3) {
                int ip = (a << 24) | (b << 16) | (c << 8) | d;
                setBitAndCount(bitset, count, ip);
            }
        } catch (Throwable t) {
            throw new RuntimeException("File processing failed", t);
        }
    }

    private static void setBitAndCount(
            int[] bitset,
            AtomicLong count,
            int ip
    ) {
        int index = ip >>> 5;     // / 32
        int bit = ip & 31;      // % 32
        int bitMask = 1 << bit;

        if ((bitset[index] & bitMask) == 0) {
            bitset[index] = bitset[index] | bitMask;
            count.getAndIncrement();
        }
    }
}
