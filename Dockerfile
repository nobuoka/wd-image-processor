FROM openjdk:8

COPY ./build/install/wdip /wdip/

ENV PATH $PATH:/wdip/bin
CMD ["wdip"]
