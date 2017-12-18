FROM debian:latest

COPY azul.gpg /etc/apt/trusted.gpg.d
RUN echo 'deb http://repos.azulsystems.com/debian stable main' > /etc/apt/sources.list.d/azul.list && apt-get update

RUN apt-get install -y zulu-8 zulu-9
