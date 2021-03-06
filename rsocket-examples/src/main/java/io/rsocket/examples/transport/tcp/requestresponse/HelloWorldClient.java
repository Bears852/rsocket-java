/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.examples.transport.tcp.requestresponse;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public final class HelloWorldClient {

  private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

  public static void main(String[] args) {

    RSocket rsocket =
        new RSocket() {
          boolean fail = true;

          @Override
          public Mono<Payload> requestResponse(Payload p) {
            if (fail) {
              fail = false;
              return Mono.error(new Throwable("Simulated error"));
            } else {
              return Mono.just(p);
            }
          }
        };

    RSocketServer.create(SocketAcceptor.with(rsocket))
        .bindNow(TcpServerTransport.create("localhost", 7000));

    RSocket socket =
        RSocketConnector.connectWith(TcpClientTransport.create("localhost", 7000)).block();

    for (int i = 0; i < 3; i++) {
      socket
          .requestResponse(DefaultPayload.create("Hello"))
          .map(Payload::getDataUtf8)
          .onErrorReturn("error")
          .doOnNext(logger::debug)
          .block();
    }

    socket.dispose();
  }
}
