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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author magibney
 */
@ChannelHandler.Sharable
class RedirectHandler extends SimpleChannelInboundHandler<HttpRequest>{

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String REDIRECT_PARAM_KEY = "redirect";
    private static final Pattern PRE_PATH = Pattern.compile("^.*?[^/](?=(/[^/])|(/?$))");
    private static final CharSequence LOCATION = HttpHeaders.newNameEntity(HttpHeaders.Names.LOCATION);
    private static final CharSequence CONNECTION = HttpHeaders.newNameEntity(HttpHeaders.Names.CONNECTION);
    private static final CharSequence CLOSE = HttpHeaders.newValueEntity(HttpHeaders.Values.CLOSE);
    
    private final Set<String> validHosts;
    
    public RedirectHandler(Set<String> validHosts) {
        this.validHosts = validHosts;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        QueryStringDecoder orig = new QueryStringDecoder(msg.getUri());
        List<String> vals = orig.parameters().get(REDIRECT_PARAM_KEY);
        String val;
        if (vals == null || vals.isEmpty() || (val = vals.get(0)) == null) {
            badRequest("no redirect specified", ctx);
            return;
        }
        Matcher m = PRE_PATH.matcher(val);
        if (!m.find() || !validHosts.contains(m.group())) {
            badRequest("redirect not permitted: "+val+", "+m.group(), ctx);
            return;
        }
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        resp.headers().add(LOCATION, val);
        resp.headers().add(CONNECTION, CLOSE);
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void badRequest(String message, ChannelHandlerContext ctx) {
        ByteBuf content = Unpooled.wrappedBuffer(message.getBytes(UTF8));
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, content);
        HttpHeaders.setContentLength(resp, content.readableBytes());
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
                .addListener(ChannelFutureListener.CLOSE);
    }
    
    
    
}
