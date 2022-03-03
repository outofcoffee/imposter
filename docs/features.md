# Features

Imposter allows certain features to be enabled or disabled. Fewer features generally lowers resource requirements.

> Also see the [Plugins](./plugins.md) documentation for a list of available and built-in plugins.

## List of features

| Feature name    | Purpose                              | Details                                | Enabled by default |
|-----------------|--------------------------------------|----------------------------------------|--------------------|
| `metrics`       | Collects and exposes telemetry.      | [Metrics](./metrics_logs_telemetry.md) | `true`             |
| `stores`        | Persistent or semi-persistent state. | [Stores](./stores.md)                  | `true`             |

These can be controlled by setting the environment variable `IMPOSTER_FEATURES`:

    IMPOSTER_FEATURES="stores=false,metrics=true"

...or Java system property `imposter.features`:

    -Dimposter.features="stores=false,metrics=true"
