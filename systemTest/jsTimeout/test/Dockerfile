FROM rust:1.28

COPY . /test
WORKDIR /test

RUN cargo build

CMD ["cargo", "run"]
