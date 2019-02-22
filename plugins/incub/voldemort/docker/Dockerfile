#
# Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
# <p>
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# http://www.apache.org/licenses/LICENSE-2.0
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM lwieske/java-8:jdk-8u112-slim
RUN apk update && apk add ca-certificates wget && update-ca-certificates && apk add openssl && apk add --update bash
ENV VOLDEMORT_VERSION=release-1.10.23-cutoff
RUN wget https://github.com/voldemort/voldemort/archive/$VOLDEMORT_VERSION.zip
RUN unzip $VOLDEMORT_VERSION.zip && mv voldemort-* voldemort
WORKDIR /voldemort/
ENV VOLDEMORT_HOME /voldemort/config/single_node_cluster
RUN ./gradlew clean build -x test
EXPOSE 6666 6667 8081
CMD ./bin/voldemort-server.sh