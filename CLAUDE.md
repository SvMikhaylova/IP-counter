# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A performance-optimized Java utility for counting distinct IPv4 addresses in large files (tested up to 70GB). The project demonstrates progressive optimization from naive approaches to a highly efficient parallel bitset implementation.

## Commands

```bash
./gradlew build          # Compile and package
./gradlew test           # Run all tests
./gradlew clean          # Clean build artifacts

# Run the application
java -cp build/classes/java/main Application <path-to-ip-file>
```

**Run a single test class:**
```bash
./gradlew test --tests "IpCounterServiceTest"
```

## Architecture

### Core Flow
1. `Application.java` — entry point; detects available processors, calls `IpCounterService`, reports timing
2. `IpCounterService.countDistinctIpAddressesOptimized()` — splits the file into line-aligned `Chunk` ranges, dispatches each to a thread pool worker
3. Each worker reads its chunk via `FileChannel` into 64KB byte buffers, parses IPs manually at the byte level (no `String` allocations), converts each IP to a 32-bit integer, and marks it in a shared bitset
4. `Chunk.java` — simple record `(long start, long end)` representing a byte range

### Bitset Design
- `AtomicIntegerArray[1 << 27]` (134M integers = 512MB) covers all 2^32 IPv4 addresses
- Each integer stores 32 bits; setting a bit uses a CAS loop for thread safety
- `LongAdder` counts distinct IPs (incremented only on the 0→1 bit transition)
- No global locks; contention is minimal because the CAS is per-element

### Why Byte-Level Parsing?
Avoiding `String` creation for each IP address was the single largest performance win (~3× speedup). The parser reads octets directly from `ByteBuffer` without allocating objects.

### Optimization History (documented in README.md)
| Approach | Heap | Time (70GB file) |
|---|---|---|
| `HashSet<String>` | 3 GB | >1.5 h |
| `boolean[2^32]` | 15 GB | ~24 min |
| `int[]` bitset | 1.5 GB | ~15 min |
| Manual string parsing | 1 GB | ~5.5 min |
| `FileChannel` + byte parsing | 500 MB | ~3.7 min |
| + parallel chunks | 500 MB | ~2.05 min |
| + `LongAdder` | 500 MB | **~1.15 min** |

## Test Data
`src/test/resources/test_ip` — 22 lines, 21 distinct IPs (one duplicate). Tests run with a single thread to keep results deterministic.