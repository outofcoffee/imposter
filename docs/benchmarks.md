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
| 50      | 1099         | Disabled        |
| 100     | 1330         | Disabled        |
| 200     | 1438         | Disabled        |
| 200     | 1904         | Enabled         |

### Scenario 2: Conditional, configuration-driven

| Threads | Requests/sec | HTTP Keep-alive |
|---------|--------------|-----------------|
| 50      | 901          | Disabled        |
| 100     | 1159         | Disabled        |
| 200     | 1416         | Disabled        |
| 200     | 1855         | Enabled         |

### Scenario 3: Dynamic, script-driven

| Threads | Requests/sec | HTTP Keep-alive |
|---------|--------------|-----------------|
| 50      | 3364         | Disabled        |
| 100     | 3590         | Disabled        |
| 200     | 3710         | Disabled        |
| 200     | 7236         | Enabled         |

### Benchmark set up

Configuration and commands to allow benchmarks to be independently reproduced.

#### Scenarios

- Scenario 1: Static, configuration-driven: [examples/rest/simple](https://github.com/outofcoffee/imposter/blob/main/examples/rest/simple)
- Scenario 2: Conditional, configuration-driven: [examples/rest/conditional-config](https://github.com/outofcoffee/imposter/blob/main/examples/rest/conditional-config)
- Scenario 3: Dynamic, script-driven: [examples/rest/conditional-scripted](https://github.com/outofcoffee/imposter/blob/main/examples/rest/conditional-scripted)

#### Start command

     docker run --rm -it -v /path/to/config:/opt/imposter/config -p 8080:8080 --cpus=1 --memory=256m outofcoffee/imposter:1.23.3

Notes:

- limits to 1 CPU core
- limits to 256 MB RAM

#### Warmup command

    for i in {1..5}; do ab -t 2 -c 50 http://localhost:8080/example ; sleep 5 ; done

Notes:

- actual URL depends on the scenario (see above)

#### Benchmark command

     ab -n 2000 -c 200 http://localhost:8080/example

Notes:

- actual URL depends on the scenario (see above)
- concurrency value (`-c` flag) depends on test case (see above)
