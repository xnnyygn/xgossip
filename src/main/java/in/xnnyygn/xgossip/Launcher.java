package in.xnnyygn.xgossip;

import java.util.ArrayList;
import java.util.List;

public class Launcher {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("usage <port> <seed-endpoint> <seed-endpoint>...");
            return;
        }
        int port = Integer.parseInt(args[0]);
        MemberManager memberManager = new MemberManagerBuilder(new MemberEndpoint("localhost", port)).build();
        memberManager.addListener(new MemberEventListener() {
            @Override
            public void onChanged(MemberEvent event) {
                System.out.println("member " + event.getEndpoint() + " " + event.getKind() + ", available endpoints " + memberManager.listAvailableEndpoints());
            }

            @Override
            public void onMerged() {
                System.out.println("member list merged, available endpoints " + memberManager.listAvailableEndpoints());
            }
        });
        memberManager.initialize();

        List<MemberEndpoint> seedEndpoints = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            seedEndpoints.add(parseEndpoint(args[i]));
        }
        memberManager.join(seedEndpoints);
        System.in.read();
        memberManager.leave();
        memberManager.shutdown();
    }

    private static MemberEndpoint parseEndpoint(String rawEndpoint) {
        int index = rawEndpoint.indexOf(':');
        if (index <= 0) {
            throw new IllegalArgumentException("illegal endpoint [" + rawEndpoint + "]");
        }
        String host = rawEndpoint.substring(0, index);
        int port;
        try {
            port = Integer.parseInt(rawEndpoint.substring(index + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port in endpoint [" + rawEndpoint + "]");
        }
        return new MemberEndpoint(host, port);
    }

}
