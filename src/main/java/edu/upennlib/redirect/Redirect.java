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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author magibney
 */
public class Redirect implements Runnable {

    private static final int DEFAULT_LISTEN_PORT = 8080;
    private static final String DEFAULT_PROPFILE = "etc" + File.separatorChar + "redirect.properties";
    private static final String DEFAULT_VALID_HOSTS_FILE = "etc" + File.separatorChar + "validHosts.txt";

    public static final String LISTEN_PORT_PROPERTY_NAME = "listenPort";
    public static final String LISTEN_INTERFACE_PROPERTY_NAME = "listenInterface";
    public static final String REDIRECT_PREFIX_PROPERTY_NAME = "redirectPrefix";
    public static final String VALID_HOSTS_FILE_PROPERTY_NAME = "validHostsFile";

    private final InetAddress listenInterface;
    private final int listenPort;
    private final String redirectPrefix;
    private final Set<String> validHosts;

    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        String propFileName = args.length < 1 ? DEFAULT_PROPFILE : args[0];
        Redirect sp = new Redirect(loadProperties(propFileName));
        sp.run();
    }

    private static Properties loadProperties(String fileName) throws IOException {
        File propFile = new File(fileName);
        Properties props = new Properties();
        if (!propFile.exists()) {
            return props;
        }
        FileReader fr = new FileReader(propFile);
        try {
            props.load(fr);
        } finally {
            fr.close();
        }
        return props;
    }

    private static Set<String> loadValidHosts(String fileName) throws IOException {
        File hostsFile = new File(fileName);
        if (!hostsFile.exists()) {
            throw new RuntimeException("hosts file \""+hostsFile+"\" not found; redirect impossible with no configured hosts");
        }
        Set<String> ret = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(hostsFile));
        try {
            String host;
            while ((host = br.readLine()) != null) {
                String trimmedHost = host.trim();
                if (!trimmedHost.isEmpty() && !trimmedHost.startsWith("#")) {
                    ret.add(trimmedHost);
                }
            }
        } finally {
            br.close();
        }
        if (ret.isEmpty()) {
            throw new RuntimeException("hosts file \""+hostsFile+"\" contains no configured hosts; redirect impossible");
        }
        return ret;
    }

    private Redirect(Properties props) {
        listenPort = Integer.parseInt(props.getProperty(LISTEN_PORT_PROPERTY_NAME, Integer.toString(DEFAULT_LISTEN_PORT)));
        String interfaceProp = props.getProperty(LISTEN_INTERFACE_PROPERTY_NAME);
        if (interfaceProp == null) {
            listenInterface = InetAddress.getLoopbackAddress();
        } else {
            try {
                listenInterface = InetAddress.getByName(interfaceProp);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
        redirectPrefix = props.getProperty(REDIRECT_PREFIX_PROPERTY_NAME);
        String validHostsFile = props.getProperty(VALID_HOSTS_FILE_PROPERTY_NAME, DEFAULT_VALID_HOSTS_FILE);
        try {
            validHosts = loadValidHosts(validHostsFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void run() {
        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new RedirectInitializer(validHosts, redirectPrefix))
                    .childOption(ChannelOption.AUTO_READ, true)
                    .bind(listenInterface, listenPort)
                    .sync().channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
