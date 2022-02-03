# Prometheus metrics for Imposter

A worked example of using Prometheus to scrape metrics from an Imposter instance.

## Prerequisites

- Docker

## Steps

Start Prometheus using the configuration in this directory:

	docker run --rm -it -p9090:9090 -v $PWD/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus

(Optional) start Grafana:

    docker run --rm -it -p3000:3000 -v $PWD/grafana.yml:/etc/grafana/provisioning/datasources/imposter.yml grafana/grafana

## Example

You can use the Docker Compose file in this directory to quickly stand up Prometheus and Grafana.
