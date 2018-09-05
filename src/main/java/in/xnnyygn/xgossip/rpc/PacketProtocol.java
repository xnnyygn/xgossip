package in.xnnyygn.xgossip.rpc;

import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import in.xnnyygn.xgossip.Member;
import in.xnnyygn.xgossip.MemberEndpoint;
import in.xnnyygn.xgossip.MemberNotification;
import in.xnnyygn.xgossip.rpc.messages.*;
import in.xnnyygn.xgossip.updates.AbstractUpdate;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import in.xnnyygn.xgossip.updates.MemberLeavedUpdate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

class PacketProtocol {

    private static final int MSG_TYPE_MEMBER_JOIN_RPC = 1;
    private static final int MSG_TYPE_MEMBER_JOIN_RESPONSE = 2;
    private static final int MSG_TYPE_MEMBER_LEAVED_RPC = 3;
    private static final int MSG_TYPE_MEMBER_UPDATES_RPC = 10;
    private static final int MSG_TYPE_MEMBER_UPDATES_AGREED_RESPONSE = 11;
    private static final int MSG_TYPE_MEMBER_UPDATES_RESPONSE = 12;
    private static final int MSG_TYPE_MEMBERS_MERGE_RESPONSE = 13;
    private static final int MSG_TYPE_MEMBERS_MERGED_RESPONSE = 14;
    private static final int MSG_TYPE_PING_RPC = 20;
    private static final int MSG_TYPE_PING_RESPONSE = 21;
    private static final int MSG_TYPE_PING_REQUEST_RPC = 22;
    private static final int MSG_TYPE_PROXY_PING_RPC = 23;
    private static final int MSG_TYPE_PROXY_PING_RESPONSE = 24;
    private static final int MSG_TYPE_PROXY_PING_DONE_RESPONSE = 25;

    DatagramPacket toPacket(MemberEndpoint sender, AbstractMessage message, MemberEndpoint recipient) {
        byte[] bytes;
        try {
            bytes = toBytes(sender, message);
        } catch (IOException e) {
            throw new ProtocolException(e);
        }
        return new DatagramPacket(bytes, bytes.length, new InetSocketAddress(recipient.getHost(), recipient.getPort()));
    }

    private byte[] toBytes(MemberEndpoint sender, AbstractMessage message) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteOutput);
        writeSender(sender, dataOutput);
        writeMessage(message, dataOutput);
        return byteOutput.toByteArray();
    }

    private void writeSender(MemberEndpoint sender, DataOutputStream dataOutput) throws IOException {
        byte[] hostBytes = sender.getHost().getBytes();
        dataOutput.writeInt(hostBytes.length);
        dataOutput.write(hostBytes);
        dataOutput.writeInt(sender.getPort());
    }

    private Protos.MemberEndpoint toProtoMemberEndpoint(MemberEndpoint endpoint) {
        return Protos.MemberEndpoint.newBuilder()
                .setHost(endpoint.getHost())
                .setPort(endpoint.getPort())
                .build();
    }

    private Protos.Member toProtoMember(Member member) {
        return Protos.Member.newBuilder()
                .setEndpoint(toProtoMemberEndpoint(member.getEndpoint()))
                .setTimeAdded(member.getTimeAdded())
                .setTimeRemoved(member.getTimeRemoved())
                .build();
    }

    private List<Protos.Member> toProtoMembers(Collection<Member> members) {
        return members.stream().map(this::toProtoMember).collect(Collectors.toList());
    }

    private Protos.MemberNotification toProtoMemberNotification(MemberNotification notification) {
        return Protos.MemberNotification.newBuilder()
                .setEndpoint(toProtoMemberEndpoint(notification.getEndpoint()))
                .setSuspected(notification.isSuspected())
                .setTimestamp(notification.getTimestamp())
                .setBy(toProtoMemberEndpoint(notification.getBy()))
                .build();
    }

    private void writeMessage(AbstractMessage message, DataOutputStream dataOutput) throws IOException {
        if (message instanceof MemberJoinRpc) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_JOIN_RPC);
            MemberJoinRpc memberJoinRpc = (MemberJoinRpc) message;
            Protos.MemberJoinRpc.newBuilder()
                    .setEndpoint(toProtoMemberEndpoint(memberJoinRpc.getEndpoint()))
                    .setTimeJoined(memberJoinRpc.getTimeJoined())
                    .build().writeTo(dataOutput);
        } else if (message instanceof MemberJoinResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_JOIN_RESPONSE);
            MemberJoinResponse memberJoinResponse = (MemberJoinResponse) message;
            Protos.MemberJoinResponse.newBuilder()
                    .addAllMembers(toProtoMembers(memberJoinResponse.getMembers()))
                    .build().writeTo(dataOutput);
        } else if (message instanceof MemberLeavedRpc) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_LEAVED_RPC);
            MemberLeavedRpc memberLeavedRpc = (MemberLeavedRpc) message;
            Protos.MemberLeavedRpc.newBuilder()
                    .setEndpoint(toProtoMemberEndpoint(memberLeavedRpc.getEndpoint()))
                    .setTimeLeaved(memberLeavedRpc.getTimeLeaved())
                    .build().writeTo(dataOutput);
        } else if (message instanceof MemberUpdatesRpc) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_UPDATES_RPC);
            MemberUpdatesRpc memberUpdatesRpc = (MemberUpdatesRpc) message;
            Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> updateMap = groupUpdates(memberUpdatesRpc.getUpdates());
            Protos.MemberUpdatesRpc.newBuilder()
                    .setExchangeAt(memberUpdatesRpc.getExchangeAt())
                    .addAllMemberJoinedUpdates(toProtoUpdates(updateMap, MemberJoinedUpdate.class))
                    .addAllMemberLeavedUpdate(toProtoUpdates(updateMap, MemberLeavedUpdate.class))
                    .addAllNotifications(memberUpdatesRpc.getNotifications().stream().map(this::toProtoMemberNotification).collect(Collectors.toList()))
                    .setMemberDigest(ByteString.copyFrom(memberUpdatesRpc.getMembersDigest()))
                    .build().writeTo(dataOutput);
        } else if (message instanceof MemberUpdatesAgreedResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_UPDATES_AGREED_RESPONSE);
            MemberUpdatesAgreedResponse memberUpdatesAgreedResponse = (MemberUpdatesAgreedResponse) message;
            Protos.MemberUpdatesAgreedResponse.newBuilder()
                    .setExchangeAt(memberUpdatesAgreedResponse.getExchangeAt())
                    .putAllUpdatedMap(memberUpdatesAgreedResponse.getUpdatedMap())
                    .build().writeTo(dataOutput);
        } else if (message instanceof MemberUpdatesResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_UPDATES_RESPONSE);
            MemberUpdatesResponse memberUpdatesResponse = (MemberUpdatesResponse) message;
            Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> updateMap = groupUpdates(memberUpdatesResponse.getUpdates());
            Protos.MemberUpdatesResponse.newBuilder()
                    .setExchangeAt(memberUpdatesResponse.getExchangeAt())
                    .putAllUpdatedMap(memberUpdatesResponse.getUpdatedMap())
                    .addAllMemberJoinedUpdates(toProtoUpdates(updateMap, MemberJoinedUpdate.class))
                    .addAllMemberLeavedUpdates(toProtoUpdates(updateMap, MemberLeavedUpdate.class))
                    .setMemberDigest(ByteString.copyFrom(memberUpdatesResponse.getMembersDigest()))
                    .setHopCount(memberUpdatesResponse.getHopCount())
                    .build().writeTo(dataOutput);
        } else if (message instanceof MembersMergeResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBERS_MERGE_RESPONSE);
            MembersMergeResponse membersMergeResponse = (MembersMergeResponse) message;
            Protos.MembersMergeResponse.newBuilder()
                    .setExchangeAt(membersMergeResponse.getExchangeAt())
                    .putAllUpdatedMap(membersMergeResponse.getUpdatedMap())
                    .addAllMembers(toProtoMembers(membersMergeResponse.getMembers()))
                    .setMembersDigest(ByteString.copyFrom(membersMergeResponse.getMembersDigest()))
                    .setHopCount(membersMergeResponse.getHopCount())
                    .build().writeTo(dataOutput);
        } else if (message instanceof MembersMergedResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBERS_MERGED_RESPONSE);
            MembersMergedResponse membersMergedResponse = (MembersMergedResponse) message;
            Protos.MembersMergedResponse.newBuilder()
                    .setExchangeAt(membersMergedResponse.getExchangeAt())
                    .build().writeTo(dataOutput);
        } else if (message instanceof PingRpc) {
            dataOutput.writeInt(MSG_TYPE_PING_RPC);
            PingRpc pingRpc = (PingRpc) message;
            Protos.PingRpc.newBuilder()
                    .setPingAt(pingRpc.getPingAt())
                    .build().writeTo(dataOutput);
        } else if (message instanceof PingResponse) {
            dataOutput.writeInt(MSG_TYPE_PING_RESPONSE);
            PingResponse pingResponse = (PingResponse) message;
            Protos.PingResponse.newBuilder()
                    .setPingAt(pingResponse.getPingAt())
                    .build().writeTo(dataOutput);
        } else if (message instanceof PingRequestRpc) {
            dataOutput.writeInt(MSG_TYPE_PING_REQUEST_RPC);
            PingRequestRpc pingRequestRpc = (PingRequestRpc) message;
            Protos.PingRequestRpc.newBuilder()
                    .setPingAt(pingRequestRpc.getPingAt())
                    .setEndpoint(toProtoMemberEndpoint(pingRequestRpc.getEndpoint()))
                    .build().writeTo(dataOutput);
        } else if (message instanceof ProxyPingRpc) {
            dataOutput.writeInt(MSG_TYPE_PROXY_PING_RPC);
            ProxyPingRpc proxyPingRpc = (ProxyPingRpc) message;
            Protos.ProxyPingRpc.newBuilder()
                    .setPingAt(proxyPingRpc.getPingAt())
                    .setSourceEndpoint(toProtoMemberEndpoint(proxyPingRpc.getSourceEndpoint()))
                    .build().writeTo(dataOutput);
        } else if (message instanceof ProxyPingResponse) {
            dataOutput.writeInt(MSG_TYPE_PROXY_PING_RESPONSE);
            ProxyPingResponse proxyPingResponse = (ProxyPingResponse) message;
            Protos.ProxyPingResponse.newBuilder()
                    .setPingAt(proxyPingResponse.getPingAt())
                    .setSourceEndpoint(toProtoMemberEndpoint(proxyPingResponse.getSourceEndpoint()))
                    .build().writeTo(dataOutput);
        } else if (message instanceof ProxyPingDoneResponse) {
            dataOutput.writeInt(MSG_TYPE_PROXY_PING_DONE_RESPONSE);
            ProxyPingDoneResponse proxyPingDoneResponse = (ProxyPingDoneResponse) message;
            Protos.ProxyPingDoneResponse.newBuilder()
                    .setPingAt(proxyPingDoneResponse.getPingAt())
                    .setEndpoint(toProtoMemberEndpoint(proxyPingDoneResponse.getEndpoint()))
                    .build().writeTo(dataOutput);
        } else {
            throw new ProtocolException("unsupported message " + message.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractUpdate, R> List<R> toProtoUpdates(
            Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> updateMap, Class<T> clazz) {
        List<R> result = new ArrayList<>();
        for (AbstractUpdate update : updateMap.getOrDefault(clazz, Collections.emptyList())) {
            result.add((R) toProtoUpdate(update));
        }
        return result;
    }

    private Object toProtoUpdate(AbstractUpdate update) {
        if (update instanceof MemberJoinedUpdate) {
            MemberJoinedUpdate memberJoinedUpdate = (MemberJoinedUpdate) update;
            return Protos.MemberJoinedUpdate.newBuilder()
                    .setId(memberJoinedUpdate.getId())
                    .setEndpoint(toProtoMemberEndpoint(memberJoinedUpdate.getEndpoint()))
                    .setTimeJoined(memberJoinedUpdate.getTimeJoined())
                    .build();
        }
        if (update instanceof MemberLeavedUpdate) {
            MemberLeavedUpdate memberLeavedUpdate = (MemberLeavedUpdate) update;
            return Protos.MemberLeavedUpdate.newBuilder()
                    .setId(memberLeavedUpdate.getId())
                    .setEndpoint(toProtoMemberEndpoint(memberLeavedUpdate.getEndpoint()))
                    .setTimeLeaved(memberLeavedUpdate.getTimeLeaved())
                    .build();
        }
        throw new ProtocolException("unsupported update " + update.getClass());
    }


    private Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> groupUpdates(Iterable<AbstractUpdate> iterable) {
        Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> result = new HashMap<>();
        for (AbstractUpdate update : iterable) {
            Collection<AbstractUpdate> sub = result.computeIfAbsent(update.getClass(), k -> new ArrayList<>());
            sub.add(update);
        }
        return result;
    }

    RemoteMessage<? extends AbstractMessage> fromPacket(DatagramPacket packet) {
        return new PacketParser(packet).parse();
    }

    private static class PacketParser {

        private final byte[] buffer;
        private final int length;
        private int position;

        PacketParser(DatagramPacket packet) {
            buffer = packet.getData();
            length = packet.getLength();
            position = packet.getOffset();
        }

        RemoteMessage<? extends AbstractMessage> parse() {
            MemberEndpoint sender = readSender();
            AbstractMessage message = readMessage(readInt());
            return new RemoteMessage<>(message, sender);
        }

        private MemberEndpoint readSender() {
            String host = readString();
            int port = readInt();
            return new MemberEndpoint(host, port);
        }

        private AbstractMessage readMessage(int messageType) {
            try {
                return doReadMessage(messageType);
            } catch (IOException e) {
                throw new ParserException("failed to parse message", e);
            }
        }

        private AbstractMessage doReadMessage(int messageType) throws IOException {
            ByteArrayInputStream input = new ByteArrayInputStream(buffer, position, length - position);
            switch (messageType) {
                case MSG_TYPE_MEMBER_JOIN_RPC:
                    Protos.MemberJoinRpc protoMemberJoinRpc = Protos.MemberJoinRpc.parseFrom(input);
                    return new MemberJoinRpc(toMemberEndpoint(protoMemberJoinRpc.getEndpoint()), protoMemberJoinRpc.getTimeJoined());
                case MSG_TYPE_MEMBER_JOIN_RESPONSE:
                    Protos.MemberJoinResponse protoMemberJoinResponse = Protos.MemberJoinResponse.parseFrom(input);
                    return new MemberJoinResponse(toMembers(protoMemberJoinResponse.getMembersList()));
                case MSG_TYPE_MEMBER_LEAVED_RPC:
                    Protos.MemberLeavedRpc protoMemberLeavedRpc = Protos.MemberLeavedRpc.parseFrom(input);
                    return new MemberLeavedRpc(toMemberEndpoint(protoMemberLeavedRpc.getEndpoint()), protoMemberLeavedRpc.getTimeLeaved());
                case MSG_TYPE_MEMBER_UPDATES_RPC:
                    Protos.MemberUpdatesRpc protoMemberUpdatesRpc = Protos.MemberUpdatesRpc.parseFrom(input);
                    return new MemberUpdatesRpc(
                            protoMemberUpdatesRpc.getExchangeAt(),
                            toUpdates(Iterables.concat(
                                    protoMemberUpdatesRpc.getMemberJoinedUpdatesList(),
                                    protoMemberUpdatesRpc.getMemberLeavedUpdateList()
                            )),
                            protoMemberUpdatesRpc.getNotificationsList().stream().map(this::toMemberNotification).collect(Collectors.toList()),
                            protoMemberUpdatesRpc.getMemberDigest().toByteArray()
                    );
                case MSG_TYPE_MEMBER_UPDATES_AGREED_RESPONSE:
                    Protos.MemberUpdatesAgreedResponse protoMemberUpdatesAgreedResponse = Protos.MemberUpdatesAgreedResponse.parseFrom(input);
                    return new MemberUpdatesAgreedResponse(
                            protoMemberUpdatesAgreedResponse.getExchangeAt(),
                            protoMemberUpdatesAgreedResponse.getUpdatedMapMap()
                    );
                case MSG_TYPE_MEMBER_UPDATES_RESPONSE:
                    Protos.MemberUpdatesResponse protoMemberUpdatesResponse = Protos.MemberUpdatesResponse.parseFrom(input);
                    return new MemberUpdatesResponse(
                            protoMemberUpdatesResponse.getExchangeAt(),
                            protoMemberUpdatesResponse.getUpdatedMapMap(),
                            toUpdates(Iterables.concat(
                                    protoMemberUpdatesResponse.getMemberJoinedUpdatesList(),
                                    protoMemberUpdatesResponse.getMemberLeavedUpdatesList()
                            )),
                            protoMemberUpdatesResponse.getMemberDigest().toByteArray(),
                            protoMemberUpdatesResponse.getHopCount()
                    );
                case MSG_TYPE_MEMBERS_MERGE_RESPONSE:
                    Protos.MembersMergeResponse protoMembersMergeResponse = Protos.MembersMergeResponse.parseFrom(input);
                    return new MembersMergeResponse(
                            protoMembersMergeResponse.getExchangeAt(),
                            protoMembersMergeResponse.getUpdatedMapMap(),
                            toMembers(protoMembersMergeResponse.getMembersList()),
                            protoMembersMergeResponse.getMembersDigest().toByteArray(),
                            protoMembersMergeResponse.getHopCount()
                    );
                case MSG_TYPE_MEMBERS_MERGED_RESPONSE:
                    Protos.MembersMergedResponse protoMembersMergedResponse = Protos.MembersMergedResponse.parseFrom(input);
                    return new MembersMergedResponse(protoMembersMergedResponse.getExchangeAt());
                case MSG_TYPE_PING_RPC:
                    Protos.PingRpc protoPingRpc = Protos.PingRpc.parseFrom(input);
                    return new PingRpc(protoPingRpc.getPingAt());
                case MSG_TYPE_PING_RESPONSE:
                    Protos.PingResponse protoPingResponse = Protos.PingResponse.parseFrom(input);
                    return new PingResponse(protoPingResponse.getPingAt());
                case MSG_TYPE_PING_REQUEST_RPC:
                    Protos.PingRequestRpc protoPingRequestRpc = Protos.PingRequestRpc.parseFrom(input);
                    return new PingRequestRpc(protoPingRequestRpc.getPingAt(), toMemberEndpoint(protoPingRequestRpc.getEndpoint()));
                case MSG_TYPE_PROXY_PING_RPC:
                    Protos.ProxyPingRpc protoProxyPingRpc = Protos.ProxyPingRpc.parseFrom(input);
                    return new ProxyPingRpc(protoProxyPingRpc.getPingAt(), toMemberEndpoint(protoProxyPingRpc.getSourceEndpoint()));
                case MSG_TYPE_PROXY_PING_RESPONSE:
                    Protos.ProxyPingResponse protoProxyPingResponse = Protos.ProxyPingResponse.parseFrom(input);
                    return new ProxyPingResponse(protoProxyPingResponse.getPingAt(), toMemberEndpoint(protoProxyPingResponse.getSourceEndpoint()));
                case MSG_TYPE_PROXY_PING_DONE_RESPONSE:
                    Protos.ProxyPingDoneResponse proxyPingDoneResponse = Protos.ProxyPingDoneResponse.parseFrom(input);
                    return new ProxyPingDoneResponse(proxyPingDoneResponse.getPingAt(), toMemberEndpoint(proxyPingDoneResponse.getEndpoint()));
                default:
                    throw new ParserException("unexpected message type " + messageType);
            }
        }

        private MemberNotification toMemberNotification(Protos.MemberNotification protoMemberNotification) {
            return new MemberNotification(
                    toMemberEndpoint(protoMemberNotification.getEndpoint()),
                    protoMemberNotification.getSuspected(),
                    protoMemberNotification.getTimestamp(),
                    toMemberEndpoint(protoMemberNotification.getBy())
            );
        }

        private List<AbstractUpdate> toUpdates(Iterable<?> rawUpdates) {
            List<AbstractUpdate> updates = new ArrayList<>();
            for (Object rawUpdate : rawUpdates) {
                updates.add(toUpdate(rawUpdate));
            }
            return updates;
        }

        private AbstractUpdate toUpdate(Object rawUpdate) {
            if (rawUpdate instanceof Protos.MemberJoinedUpdate) {
                Protos.MemberJoinedUpdate protoMemberJoinedUpdate = (Protos.MemberJoinedUpdate) rawUpdate;
                return new MemberJoinedUpdate(
                        protoMemberJoinedUpdate.getId(),
                        toMemberEndpoint(protoMemberJoinedUpdate.getEndpoint()),
                        protoMemberJoinedUpdate.getTimeJoined()
                );
            }
            if (rawUpdate instanceof Protos.MemberLeavedUpdate) {
                Protos.MemberLeavedUpdate protoMemberLeavedUpdate = (Protos.MemberLeavedUpdate) rawUpdate;
                return new MemberLeavedUpdate(
                        protoMemberLeavedUpdate.getId(),
                        toMemberEndpoint(protoMemberLeavedUpdate.getEndpoint()),
                        protoMemberLeavedUpdate.getTimeLeaved()
                );
            }
            throw new ParserException("unsupported update " + rawUpdate.getClass());
        }

        private MemberEndpoint toMemberEndpoint(Protos.MemberEndpoint protoMemberEndpoint) {
            return new MemberEndpoint(protoMemberEndpoint.getHost(), protoMemberEndpoint.getPort());
        }

        private List<Member> toMembers(List<Protos.Member> protoMembers) {
            return protoMembers.stream().map(this::toMember).collect(Collectors.toList());
        }

        private Member toMember(Protos.Member protoMember) {
            return new Member(
                    toMemberEndpoint(protoMember.getEndpoint()),
                    protoMember.getTimeAdded(),
                    protoMember.getTimeRemoved()
            );
        }

        private String readString() {
            return new String(readBytes(readInt()));
        }

        private byte[] readBytes(int n) {
            if (position + n > length) {
                throw new ParserException("eof");
            }
            byte[] bytes = new byte[n];
            System.arraycopy(buffer, position, bytes, 0, n);
            position += n;
            return bytes;
        }

        private int readInt() {
            if (position + 4 > length) {
                throw new ParserException("eof");
            }
            int n = ((buffer[position]) << 24) |
                    ((buffer[position + 1] & 0xFF) << 16) |
                    ((buffer[position + 2] & 0xFF) << 8) |
                    (buffer[position + 3] & 0xFF);
            position += 4;
            return n;
        }

    }

}
