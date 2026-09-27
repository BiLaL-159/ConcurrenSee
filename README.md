# ConcurrenSee

Three Java TCP servers that do the same job with different concurrency models, load-tested with Apache JMeter to see how each one behaves as the request rate grows.

| Server | Concurrency model |
|---|---|
| [`SingleThreaded`](SingleThreaded/Server.java) | One loop handles one connection at a time |
| [`Multithreaded`](Multithreaded/Server.java) | A new thread for every connection |
| [`ThreadPool`](ThreadPool/Server.java) | A fixed pool of worker threads (`Executors.newFixedThreadPool`) |

## Key results

With a simulated **10 ms of I/O per request** (think database call):

| Server | Handles 100k requests/min? | Highest rate handled | Latency at 1,667 req/s (p50 / p99) |
|---|:---:|---:|---|
| SingleThreaded | ❌ | ~76 req/s (85% of requests timed out) | n/a |
| ThreadPool, 10 threads | ❌ | 500 req/s; caps near 755 req/s | 660 ms / 707 ms |
| ThreadPool, 100 threads | ✅ | 2,500 req/s (150k/min) | 12 ms / 29 ms |
| Multithreaded | ✅ | 2,500 req/s (150k/min) | 14 ms / 44 ms |

- **Concurrency only matters when requests wait.** With no I/O delay, every server, the single-threaded one included, handled 100k requests/min with p99 at 31 ms or less, because each request finishes in well under a millisecond.
- **With 10 ms of I/O, the single-threaded server falls apart.** It can serve at most one request per ~12 ms, which is over 30x less than the concurrent servers.
- **A thread pool is only as good as its size.** A pool of N threads with a per-request time of T tops out at about N ÷ T. Ten threads at ~13 ms each gives ~755 req/s, below the 1,667 req/s target. With 100 threads, the pool matches thread-per-connection while keeping the number of threads bounded.
- **2,500 req/s is the highest rate measured cleanly, not the servers' limit.** At 5,000 req/s JMeter ran out of local ports (`BindException`) before the servers failed. See [Limitations](#limitations).

## What was measured

Each Request is one TCP connection: the client sends one line, the server replies with one line, and the connection closes.

A server **handles** a load when, for 60 seconds, it:

- achieves at least 97% of the offered rate,
- answers at least 99.9% of requests successfully, and
- keeps p99 response time under 100 ms.

Each server configuration gets a **stepped run**: 60 s at 500 → 1,000 → 1,667 → 2,500 → 5,000 → 10,000 req/s, stopping at the first step it fails. The 1,667 req/s step (100k requests/min) is always run. Every step starts a fresh server and warms it up for 5 s before measuring.

Load comes from a pool of 500 JMeter threads, paced by JMeter's Precise Throughput Timer so requests arrive at a fixed rate whether or not the server keeps up.

## Running it

Requirements: Java 11+ and Apache JMeter 5.6+ (`brew install jmeter`). Python 3 is used to summarize results.

**Run one server by hand:**

```sh
cd ThreadPool && javac Server.java
java Server [port] [delayMs] [poolSize]      # defaults: 8010 0 10
```

`SingleThreaded` and `Multithreaded` take `[port] [delayMs]`. With no arguments every server behaves like the original demo. The clients in `SingleThreaded/` and `Multithreaded/` still work against them; the Multithreaded client fires 100 concurrent requests.


