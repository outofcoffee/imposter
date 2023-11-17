FROM python:3

RUN pip install \
       mkdocs-material \
       mkdocs-mermaid2-plugin

RUN mkdir /site
WORKDIR /site

EXPOSE 8000

ENTRYPOINT mkdocs build && python -m http.server --directory ./site 8000
