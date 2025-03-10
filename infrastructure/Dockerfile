FROM squidfunk/mkdocs-material

COPY infrastructure/constraints.txt \
     infrastructure/requirements.txt \
     /build/

ENV PIP_CONSTRAINT=/build/constraints.txt

RUN pip install -r /build/requirements.txt
