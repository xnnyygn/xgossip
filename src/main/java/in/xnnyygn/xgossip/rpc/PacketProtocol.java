package in.xnnyygn.xgossip.rpc;

import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import in.xnnyygn.xgossip.Member;
import in.xnnyygn.xgossip.MemberEndpoint;
import in.xnnyygn.xgossip.messages.*;
import in.xnnyygn.xgossip.updates.AbstractUpdate;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;

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
    private static final int MSG_TYPE_MEMBER_UPDATES_RPC = 3;
    private static final int MSG_TYPE_MEMBER_UPDATES_AGREED_RESPONSE = 4;
    private static final int MSG_TYPE_MEMBER_UPDATES_RESPONSE = 5;
    private static final int MSG_TYPE_MEMBERS_MERGE_RESPONSE = 6;
    private static final int MSG_TYPE_MEMBERS_MERGED_RESPONSE = 7;

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

    private void writeMessage(AbstractMessage message, DataOutputStream dataOutput) throws IOException {
        if (message instanceof MemberJoinRpc) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_JOIN_RPC);
            MemberJoinRpc memberJoinRpc = (MemberJoinRpc) message;
            Protos.MemberJoinRpc.newBuilder()
                    .setEndpoint(toProtoMemberEndpoint(memberJoinRpc.getEndpoint()))
                    .setTimeJoined(memberJoinRpc.getTimeJoined())
                    .build()
                    .writeTo(dataOutput);
        } else if (message instanceof MemberJoinResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_JOIN_RESPONSE);
            MemberJoinResponse memberJoinResponse = (MemberJoinResponse) message;
            Protos.MemberJoinResponse.newBuilder()
                    .addAllMembers(toProtoMembers(memberJoinResponse.getMembers()))
                    .build()
                    .writeTo(dataOutput);
        } else if (message instanceof MemberUpdatesRpc) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_UPDATES_RPC);
            MemberUpdatesRpc memberUpdatesRpc = (MemberUpdatesRpc) message;
            Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> updateMap = groupUpdates(
                    Iterables.concat(memberUpdatesRpc.getUpdates(), memberUpdatesRpc.getNotifications())
            );
            Protos.MemberUpdatesRpc.newBuilder()
                    .setMemberDigest(ByteString.copyFrom(memberUpdatesRpc.getMembersDigest()))
                    .addAllMemberJoinedUpdate(toProtoUpdates(updateMap, MemberJoinedUpdate.class))
                    .build()
                    .writeTo(dataOutput);
        } else if (message instanceof MemberUpdatesAgreedResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_UPDATES_AGREED_RESPONSE);
            MemberUpdatesAgreedResponse memberUpdatesAgreedResponse = (MemberUpdatesAgreedResponse) message;
            Protos.MemberUpdatesAgreedResponse.newBuilder()
                    .setExchangeAt(memberUpdatesAgreedResponse.getExchangeAt())
                    .putAllUpdatedMap(memberUpdatesAgreedResponse.getUpdatedMap())
                    .build()
                    .writeTo(dataOutput);
        } else if (message instanceof MemberUpdatesResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBER_UPDATES_RESPONSE);
            MemberUpdatesResponse memberUpdatesResponse = (MemberUpdatesResponse) message;
            Map<Class<? extends AbstractUpdate>, Collection<AbstractUpdate>> updateMap = groupUpdates(
                    memberUpdatesResponse.getUpdates()
            );
            Protos.MemberUpdatesResponse.newBuilder()
                    .setExchangeAt(memberUpdatesResponse.getExchangeAt())
                    .putAllUpdatedMap(memberUpdatesResponse.getUpdatedMap())
                    .addAllMemberJoinedUpdate(toProtoUpdates(updateMap, MemberJoinedUpdate.class))
                    .setMemberDigest(ByteString.copyFrom(memberUpdatesResponse.getMembersDigest()))
                    .setHopCount(memberUpdatesResponse.getHopCount())
                    .build()
                    .writeTo(dataOutput);
        } else if (message instanceof MembersMergeResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBERS_MERGE_RESPONSE);
            MembersMergeResponse membersMergeResponse = (MembersMergeResponse) message;
            Protos.MembersMergeResponse.newBuilder()
                    .setExchangeAt(membersMergeResponse.getExchangeAt())
                    .putAllUpdatedMap(membersMergeResponse.getUpdatedMap())
                    .addAllMembers(toProtoMembers(membersMergeResponse.getMembers()))
                    .setMembersDigest(ByteString.copyFrom(membersMergeResponse.getMembersDigest()))
                    .setHopCount(membersMergeResponse.getHopCount())
                    .build()
                    .writeTo(dataOutput);
        } else if (message instanceof MembersMergedResponse) {
            dataOutput.writeInt(MSG_TYPE_MEMBERS_MERGED_RESPONSE);
            MembersMergedResponse membersMergedResponse = (MembersMergedResponse) message;
            Protos.MembersMergedResponse.newBuilder()
                    .setExchangeAt(membersMergedResponse.getExchangeAt())
                    .build()
                    .writeTo(dataOutput);
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
                case MSG_TYPE_MEMBER_UPDATES_RPC:
                    Protos.MemberUpdatesRpc protoMemberUpdatesRpc = Protos.MemberUpdatesRpc.parseFrom(input);
                    return new MemberUpdatesRpc(
                            toUpdates(protoMemberUpdatesRpc.getMemberJoinedUpdateList()),
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
                            toUpdates(protoMemberUpdatesResponse.getMemberJoinedUpdateList()),
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
                default:
                    throw new ParserException("unexpected message type " + messageType);
            }
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
