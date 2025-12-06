# IP-counter

Counts a number of distinct IP addresses in given file and prints it to console.

## How to run

To provide the file you need to pass an absolute path to it as an app argument.
File should contain only valid IPv4 addresses, each address on a new line.

## Optimisations

1. The most obvious implementation and a starting point is reading file line by line and storing each line into hashSet,
which by design can contain only distinct elements. This works pretty fast for relatively small files (tested on ~850Mb), 
but required resources are growing linearly when file size grows (considering that a number of IP addresses is finite, 
with some maximum, but anyway it's too much). The main bottleneck here is memory, 850Mb file already uses up to 3Gb heap.

   70Gb test file: unknown (>1.5h, >20Gb heap, presumably will throw OOM at some point)

2. To decrease memory consumption we need to utilize information about file's content - it's not random strings, but IPv4 addresses.
And IPv4 is by design a 32-bit number.

    Theoretically we need to create an array Boolean[2^32] (or Boolean[][], because Java doesn't allow to have array size 2^32)
and store in each element a boolean value, indicating if we already had this number.

   It is an improvement for big files, but doesn't make sense for small,
because it allocates this huge array at the beginning regardless how many IP addresses it really needs to handle.

    70Gb test file: ~24min, 15Gb heap

3. Looks like the memory overhead for these inner arrays is still too big.

    According to memory profiler boolean[32] takes 48 byte, but the size of really valuable information there
is 1 bit per each boolean, i.e. 4 byte. It means we can use Int instead of boolean array, and each bit will play the same role
as a separate boolean value before.

   70Gb test file: ~15min, 1.5Gb heap

4. Time to focus on the line's processing.

    Split using regex seems to be unnecessary complicated way since line's structure is easy. Besides, it allocates
additional String objects. To avoid this and speed up processing we can parse IP from string manually.

   70Gb test file: ~5.5min, 1Gb heap

5. Actually, it would be better to get rid of all string allocations, including strings that are created by readLine.
File can be read by FileChannel and parsed from bytes manually.

   70Gb test file: 222034ms ~3.7min, 500Mb heap