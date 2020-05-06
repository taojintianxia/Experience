package com.github.taojintianxia.consensus.ratis.mysqlreplicated.client;

import com.github.taojintianxia.consensus.ratis.mysqlreplicated.message.SQLMessage;
import com.github.taojintianxia.consensus.ratis.mysqlreplicated.util.JVMParamUtil;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.ClientId;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

import java.io.IOException;

/**
 * @author Nianjun Sun
 * @date 2020/5/6 14:45
 */
public class MySQLReplicatedClient {

    public void run() throws IOException {
        String raftGroupId = "demoRaftGroup123";
        RaftProperties raftProperties = new RaftProperties();
        final RaftGroup raftGroup = RaftGroup
                .valueOf(RaftGroupId.valueOf(ByteString.copyFromUtf8(raftGroupId)), JVMParamUtil.getPeers());

        RaftClient.Builder builder = RaftClient.newBuilder().setProperties(raftProperties);
        builder.setRaftGroup(raftGroup);
        builder.setClientRpc(new GrpcFactory(new Parameters()).newRaftClientRpc(ClientId.randomId(), raftProperties));
        RaftClient client = builder.build();
        SQLMessage sqlMessage = new SQLMessage(
                "INSERT INTO t_order(order_id,user_id,address_id,status) VALUES(1, 1, 1, 'INIT')");
        client.send(sqlMessage);
    }

}
