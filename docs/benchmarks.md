# Performance benchmarks

Imposter supports hundreds to thousands of requests per second, on a single CPU core and small memory footprint.

This section provides representative performance tests, including test set up and configuration.

> See [Performance tuning](./performance_tuning.md) to learn how to get the most out of Imposter.

## Benchmarks

Test configuration:

- 1 CPU core
- 256MB RAM
- Load injector: Apache Bench 2.3

> See _Benchmark set up_ section for full details.

### Warm-up

5 x 2 second load, at concurrency of 50

### Scenario 1: Static, configuration-driven

| Threads | Requests/sec | HTTP Keep-alive |
|---------|--------------|-----------------|
| 50      | 1075         | Disabled        |
| 100     | 1399         | Disabled        |
| 200     | 1733         | Disabled        |
| 200     | 2712         | Enabled         |

### Scenario 2: Conditional, configuration-driven

| Threads | Requests/sec | HTTP Keep-alive |
|---------|--------------|-----------------|
| 50      | 939          | Disabled        |
| 100     | 1216         | Disabled        |
| 200     | 1619         | Disabled        |
| 200     | 2027         | Enabled         |

### Scenario 3: Dynamic, script-driven

| Threads | Requests/sec | HTTP Keep-alive |
|---------|--------------|-----------------|
| 50      | 669          | Disabled        |
| 100     | 733          | Disabled        |
| 200     | 869          | Disabled        |
| 200     | 1028         | Enabled         |

### Benchmark set up

Configuration and commands to allow benchmarks to be independently reproduced.

#### Scenarios

- Scenario 1: Static, configuration-driven: [examples/rest/simple](https://github.com/imposter-project/examples/blob/main/rest/simple)
- Scenario 2: Conditional, configuration-driven: [examples/rest/conditional-config](https://github.com/imposter-project/examples/blob/main/rest/conditional-config)
- Scenario 3: Dynamic, script-driven: [examples/rest/conditional-scripted](https://github.com/imposter-project/examples/blob/main/rest/conditional-scripted)

#### Start command

     docker run --rm -it \
        -v /path/to/config:/opt/imposter/config \
        -p 8080:8080 \
        -e IMPOSTER_LOG_LEVEL=info \
        --cpus=1 \
        --memory=256m \
        outofcoffee/imposter:3.42.0

Notes:

- limits to 1 CPU core
- limits to 256 MB RAM

#### Warmup command

    for i in {1..10}; do ab -t 2 -c 50 http://localhost:8080/example ; sleep 5 ; done

Notes:

- actual URL depends on the scenario (see above)

#### Benchmark command

     ab -n 2000 -c 200 http://localhost:8080/example

Notes:

- actual URL depends on the scenario (see above)
- concurrency value (`-c` flag) depends on test case (see above)
