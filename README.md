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