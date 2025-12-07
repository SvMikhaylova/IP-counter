package service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;
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

    public long countDistinctIpAddressesOptimized(String filePath, int workersNum) {
        var bitset = new AtomicIntegerArray(1 << 27);
        var path = Paths.get(filePath);
        AtomicLong count = new AtomicLong(0);

        List<Chunk> chunks;
        try {
            chunks = splitIntoLineChunks(path, workersNum);
        } catch (IOException e) {
            throw new RuntimeException("Failed to split file into chunks", e);
        }

        try (ExecutorService pool = Executors.newFixedThreadPool(workersNum)) {
            List<Future<?>> futures = new ArrayList<>(chunks.size());
            for (Chunk chunk : chunks) {
                futures.add(pool.submit(() -> processChunk(path, chunk, bitset, count)));
            }
            // wait and propagate exceptions
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            throw new RuntimeException("File processing failed", e);
        }
        return count.longValue();
    }

    private void processChunk(Path path, Chunk chunk, AtomicIntegerArray bitset,
                                    AtomicLong count) {
        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {

            final int BUFFER_SIZE = 1 << 18;  // 64 KB
            ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);

            long pos = chunk.start();
            long remaining = chunk.end() - chunk.start();

            int a = 0, b = 0, c = 0, d = 0;
            int part = 0; // which octet 0..3

            while (remaining > 0) {
                int readSize = (int)Math.min(remaining, BUFFER_SIZE);

                buf.clear();
                buf.limit(readSize);

                int n = fileChannel.read(buf, pos);
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
            throw new RuntimeException("File processing failed: chunk " + chunk, t);
        }
    }

    private void setBitAndCount(
            AtomicIntegerArray bitset,
            AtomicLong count,
            int ip
    ) {
        int index = ip >>> 5;     // / 32
        int bit = ip & 31;      // % 32
        int bitMask = 1 << bit;

        while (true) {
            int current = bitset.get(index);
            if ((current & bitMask) != 0)
                return; // already counted

            int newValue = current | bitMask;
            if (bitset.compareAndSet(index, current, newValue)) {
                count.getAndIncrement();
                return;
            }
            // retry on CAS failure
        }
    }

    // find newline-forward from pos (inclusive) returning position of byte AFTER newline
    private long findNextNewline(FileChannel ch, long pos, long fileSize) throws IOException {
        long p = pos;
        ByteBuffer buf = ByteBuffer.allocate(32);
        while (p < fileSize) {
            buf.clear();
            int toRead = (int)Math.min(buf.capacity(), fileSize - p);
            ch.position(p);
            int r = ch.read(buf.limit(toRead));
            if (r <= 0) return fileSize;
            buf.flip();
            for (int i = 0; i < r; i++) {
                byte newLine = (byte) '\n';
                if (buf.get() == newLine) {
                    return p + i + 1; // position after newline
                }
            }
            p += r;
        }
        return fileSize;
    }

    private List<Chunk> splitIntoLineChunks(Path path, int workers) throws IOException {
        long size = Files.size(path);
        long approx = size / workers;
        List<Chunk> chunks = new ArrayList<>(workers);

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long start = 0;
            for (int i = 0; i < workers; i++) {
                long rawEnd = (i == workers - 1) ? size : Math.min(size, start + approx);
                long end;

                if (rawEnd == size) {
                    end = size;
                } else {
                    // find newline at or after rawEnd
                    end = findNextNewline(ch, rawEnd, size);
                }

                chunks.add(new Chunk(start, end));
                start = end;
            }
        }

        return chunks;
    }
}
