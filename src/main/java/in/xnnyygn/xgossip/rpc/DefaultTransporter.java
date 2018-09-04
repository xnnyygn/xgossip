package in.xnnyygn.xgossip.rpc;

import in.xnnyygn.xgossip.MemberEndpoint;
import in.xnnyygn.xgossip.MessageDispatcher;
import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.RemoteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class DefaultTransporter implements Transporter {

    private static Logger logger = LoggerFactory.getLogger(DefaultTransporter.class);
    private static final PacketProtocol packetProtocol = new PacketProtocol();
    private final MessageDispatcher messageDispatcher;
    private final MemberEndpoint selfEndpoint;

    private Thread udpServerThread;
    private DatagramSocket datagramSocket;
    private volatile boolean running = false;


    public DefaultTransporter(MemberEndpoint selfEndpoint, MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
        this.selfEndpoint = selfEndpoint;
    }

    @Override
    public void initialize() {
        logger.info("start udp server at port {}", selfEndpoint.getPort());
        try {
            datagramSocket = new DatagramSocket(selfEndpoint.getPort());
        } catch (SocketException e) {
            throw new TransporterException(e);
        }
        udpServerThread = new Thread(this::udpServer, "udp-server");
        udpServerThread.start();
    }

    private void udpServer() {
        running = true;
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        RemoteMessage<? extends AbstractMessage> message;
        while (running) {
            try {
                datagramSocket.receive(packet);
                message = packetProtocol.fromPacket(packet);
            } catch (SocketException ignored) {
                // socket is closed
                break;
            } catch (IOException | ParserException e) {
                logger.warn("failed to receive to parse packet", e);
                continue;
            }
            logger.debug("<= {}, {}", message.getSender(), message.get());
            messageDispatcher.post(message);
        }
    }

    @Override
    public <T extends AbstractMessage> void send(MemberEndpoint endpoint, T message) {
        logger.debug("=> {}, {}", endpoint, message);
        try {
            datagramSocket.send(packetProtocol.toPacket(selfEndpoint, message, endpoint));
        } catch (IOException | ProtocolException e) {
            logger.warn("failed to send", e);
        }
    }

    @Override
    public <M extends AbstractMessage, R extends AbstractMessage> void reply(RemoteMessage<M> remoteMessage, R response) {
        send(remoteMessage.getSender(), response);
    }

    @Override
    public void close() {
        logger.info("stop transporter");
        if (!running) {
            return;
        }
        running = false;
        datagramSocket.close();
        try {
            udpServerThread.join();
        } catch (InterruptedException ignored) {
        }
    }

}
