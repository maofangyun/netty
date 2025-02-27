/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketPermission;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Provides socket operations with privileges enabled. This is necessary for applications that use the
 * {@link SecurityManager} to restrict {@link SocketPermission} to their application. By asserting that these
 * operations are privileged, the operations can proceed even if some code in the calling chain lacks the appropriate
 * {@link SocketPermission}.
 */
public final class SocketUtils {

    private static final Enumeration<Object> EMPTY = Collections.enumeration(Collections.emptyList());

    private SocketUtils() {
    }

    @SuppressWarnings("unchecked")
    private static <T> Enumeration<T> empty() {
        return (Enumeration<T>) EMPTY;
    }

    public static void connect(final Socket socket, final SocketAddress remoteAddress, final int timeout)
            throws IOException {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                socket.connect(remoteAddress, timeout);
                return null;
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    public static void bind(final Socket socket, final SocketAddress bindpoint) throws IOException {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                socket.bind(bindpoint);
                return null;
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    public static boolean connect(final SocketChannel socketChannel, final SocketAddress remoteAddress)
            throws IOException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>) () -> socketChannel.connect(remoteAddress));
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    @SuppressJava6Requirement(reason = "Usage guarded by java version check")
    public static void bind(final SocketChannel socketChannel, final SocketAddress address) throws IOException {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                socketChannel.bind(address);
                return null;
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    public static SocketChannel accept(final ServerSocketChannel serverSocketChannel) throws IOException {
        try {
            // doPrivileged()方法的返回值,就是所执行方法的返回值,例如serverSocketChannel.accept()
            return AccessController.doPrivileged((PrivilegedExceptionAction<SocketChannel>) () -> serverSocketChannel.accept());
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    @SuppressJava6Requirement(reason = "Usage guarded by java version check")
    public static void bind(final DatagramChannel networkChannel, final SocketAddress address) throws IOException {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                networkChannel.bind(address);
                return null;
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    public static SocketAddress localSocketAddress(final ServerSocket socket) {
        return AccessController.doPrivileged(new PrivilegedAction<SocketAddress>() {
            @Override
            public SocketAddress run() {
                return socket.getLocalSocketAddress();
            }
        });
    }

    public static InetAddress addressByName(final String hostname) throws UnknownHostException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<InetAddress>) () -> InetAddress.getByName(hostname));
        } catch (PrivilegedActionException e) {
            throw (UnknownHostException) e.getCause();
        }
    }

    public static InetAddress[] allAddressesByName(final String hostname) throws UnknownHostException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<InetAddress[]>) () -> InetAddress.getAllByName(hostname));
        } catch (PrivilegedActionException e) {
            throw (UnknownHostException) e.getCause();
        }
    }

    public static InetSocketAddress socketAddress(final String hostname, final int port) {
        return AccessController.doPrivileged((PrivilegedAction<InetSocketAddress>) () -> new InetSocketAddress(hostname, port));
    }

    public static Enumeration<InetAddress> addressesFromNetworkInterface(final NetworkInterface intf) {
        Enumeration<InetAddress> addresses =
                AccessController.doPrivileged((PrivilegedAction<Enumeration<InetAddress>>) () -> intf.getInetAddresses());
        // Android seems to sometimes return null even if this is not a valid return value by the api docs.
        // Just return an empty Enumeration in this case.
        // See https://github.com/netty/netty/issues/10045
        if (addresses == null) {
            return empty();
        }
        return addresses;
    }

    @SuppressJava6Requirement(reason = "Usage guarded by java version check")
    public static InetAddress loopbackAddress() {
        return AccessController.doPrivileged((PrivilegedAction<InetAddress>) () -> {
            if (PlatformDependent.javaVersion() >= 7) {
                return InetAddress.getLoopbackAddress();
            }
            try {
                return InetAddress.getByName(null);
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static byte[] hardwareAddressFromNetworkInterface(final NetworkInterface intf) throws SocketException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<byte[]>) () -> intf.getHardwareAddress());
        } catch (PrivilegedActionException e) {
            throw (SocketException) e.getCause();
        }
    }
}
