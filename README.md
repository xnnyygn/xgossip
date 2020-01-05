# xgossip

Gossip based membership management and failure detection.

## Introduction

xgossip is inspired by the paper `Epidemic Algorithms for Replicatied Database Maintenance`, which maybe the first paper of [gossip protocol](https://en.wikipedia.org/wiki/Gossip_protocol), to manage members in the cluster. It employs gossip protocol to spread members' change and exchange member list between members using push-pull strategy.

Failure detection, which is based on the algorithm in the paper `On Scalable and Efficient Distributed Failure Detectors`, will mark member as `suspected` when ping and proxy ping are failed, or `backed` when ping response is not timeout. Gossip protocol here is used to broadcast the failure or recover of members and then xgossip will suppress sending rpc to suspected members.

## Demonstration

To make a 3 nodes cluster

* localhost:5302
* localhost:5303
* localhost:5304

Start node `localhost:5302`

```
$ mvn exec:java -Dexec.mainClass=in.xnnyygn.xgossip.Launcher -Dexec.args=5302
```

Start node `localhost:5303` with seed member `localhost:5302`

```
$ mvn exec:java -Dexec.mainClass=in.xnnyygn.xgossip.Launcher -Dexec.args="5303 localhost:5302"
```

Start node `localhost:5304` with seed member `localhost:5302`

```
$ mvn exec:java -Dexec.mainClass=in.xnnyygn.xgossip.Launcher -Dexec.args="5304 localhost:5302"
```

Now you can stop `localhost:5304` with `Ctrl+C` and see what will happen. 

`localhost:5302` and `localhost:5303` should print some message like `member localhost:5304 SUSPECTED`.

Then restart `localhost:5304`

```
$ mvn exec:java -Dexec.mainClass=in.xnnyygn.xgossip.Launcher -Dexec.args="5304 localhost:5302"
```

`localhost:5302` and `localhost:5303` should print some message like `member localhost:5304 BACKED`.

Finally, press enter in the console of member `localhost:5303` to shutdown. 

You should see something like `member localhost:5303 LEAVED`.

Example log of `localhost:5302`

```
2018-09-08 16:22:58.756 [in.xnnyygn.xgossip.Launcher.main()] INFO  rpc.DefaultTransporter - start udp server at port 5302
member localhost:5302 JOINED, available endpoints [localhost:5302]
2018-09-08 16:23:25.250 [udp-server] INFO  xgossip.MemberManagerImpl - member localhost:5303 joined
member localhost:5303 JOINED, available endpoints [localhost:5303, localhost:5302]
2018-09-08 16:23:49.061 [udp-server] INFO  xgossip.MemberManagerImpl - member localhost:5304 joined
member localhost:5304 JOINED, available endpoints [localhost:5303, localhost:5302, localhost:5304]
2018-09-08 16:24:01.859 [scheduler] INFO  xgossip.FailureDetector - member localhost:5304 suspected
member localhost:5304 SUSPECTED, available endpoints [localhost:5303, localhost:5302]
2018-09-08 16:24:06.259 [udp-server] INFO  xgossip.MemberManagerImpl - member localhost:5304 backed
2018-09-08 16:24:07.872 [udp-server] INFO  xgossip.FailureDetector - member localhost:5304 backed
member localhost:5304 BACKED, available endpoints [localhost:5303, localhost:5302, localhost:5304]
member localhost:5303 LEAVED, available endpoints [localhost:5302, localhost:5304]
```

## How to use

The interface of xgossip is `MemberManager`.

* initialize()
* join(seedEndpoint: Collection<MemberEndpoint>)
* listAvailableEndpoints(): Set<MemberEndpoint>
* leave()
* addListener(listener: MemberEventListener)
* shutdown()

Service should call `initialize` when start and `shutdown` when stop. 

To join the cluster, call `join` with seed endpoints. Technically, you should provide some seed endpoints unless current member is the first member in the cluster.

When service is running, available endpoints in the cluster can be retrieved by calling `listAvailableEndpoints` at any time. Listener is also supported with notification of new members, suspected members etc.

## Build

xgossip uses [maven](https://maven.apache.org/) as the build system.

```
$ mvn clean compile install
```

## License

This project is licensed under the Apache 2.0 License.
