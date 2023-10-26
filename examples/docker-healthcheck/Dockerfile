FROM outofcoffee/imposter as imposter

# docker healthcheck using embedded CLI
HEALTHCHECK --interval=5s --timeout=5s --start-period=5s --retries=3 CMD imposter list -qx
