/*
 * Copyright 2015 The Trustees of the University of Pennsylvania
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package edu.upennlib.redirect;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.util.Set;

/**
 *
 * @author magibney
 */
class RedirectInitializer  extends ChannelInitializer<SocketChannel> {

    private final LoggingHandler lh = new LoggingHandler(LogLevel.DEBUG);
    private final RedirectHandler rh;
    private final WrapRedirect wr;
    
    public RedirectInitializer(Set<String> validHosts, String prefix) {
        this.rh = new RedirectHandler(validHosts);
        this.wr = prefix == null ? null : new WrapRedirect(prefix);
    }

    public RedirectInitializer(Set<String> validHosts) {
        this(validHosts, null);
    }
    
    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(lh);
        pipeline.addLast(new HttpServerCodec());
        if (wr != null) {
            pipeline.addLast(wr);
        }
        pipeline.addLast(rh);
    }

}
