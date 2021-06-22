Prometheus metrics for Imposter
===============================

A worked example of using Prometheus to scrape metrics from an Imposter instance.

## Prerequisites

- Docker

## Steps

Start Prometheus using the configuration in this directory:

	docker run --rm -it -p9090:9090 -v $PWD/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus
