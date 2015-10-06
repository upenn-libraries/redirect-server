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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.internal.TypeParameterMatcher;
import java.net.URLEncoder;

/**
 *
 * @author magibney
 */
@ChannelHandler.Sharable
public class WrapRedirect extends ChannelOutboundHandlerAdapter {

    private static final TypeParameterMatcher matcher = TypeParameterMatcher.get(HttpResponse.class);
    private static final CharSequence LOCATION = HttpHeaders.newNameEntity(HttpHeaders.Names.LOCATION);
    private final String prefix;

    public WrapRedirect(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (matcher.match(msg)) {
            HttpResponse resp = (HttpResponse) msg;
            HttpHeaders headers = resp.headers();
            String rawLocation = headers.get(LOCATION);
            if (rawLocation != null) {
                headers.set(LOCATION, prefix.concat(URLEncoder.encode(rawLocation, "UTF-8")));
            }
        }
        super.write(ctx, msg, promise);
    }

}
