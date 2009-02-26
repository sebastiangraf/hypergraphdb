package org.hypergraphdb.peer.bootstrap;

import java.util.Map;

import org.hypergraphdb.peer.BootstrapPeer;
import org.hypergraphdb.peer.HGDBOntology;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.protocol.Performative;
import org.hypergraphdb.peer.workflow.QueryTaskServer;
import org.hypergraphdb.peer.workflow.replication.CatchUpTaskServer;
import org.hypergraphdb.peer.workflow.replication.GetInterestsTask;
import org.hypergraphdb.peer.workflow.replication.PublishInterestsTask;
import org.hypergraphdb.peer.workflow.replication.RememberTaskServer;
import org.hypergraphdb.query.AnyAtomCondition;

public class ReplicationBootstrap implements BootstrapPeer
{
	public void bootstrap(HyperGraphPeer peer, Map<String, Object> config)
	{
/*		peer.getPeerInterface().registerTaskFactory(Performative.CallForProposal, 
												    HGDBOntology.REMEMBER_ACTION, 
												    new RememberTaskServer.RememberTaskServerFactory());
		peer.getPeerInterface().registerTaskFactory(Performative.Request, 
												    HGDBOntology.ATOM_INTEREST, 
												    new PublishInterestsTask.PublishInterestsFactory());
		peer.getPeerInterface().registerTaskFactory(Performative.Request, 
													HGDBOntology.QUERY, 
													new QueryTaskServer.QueryTaskFactory());
		peer.getPeerInterface().registerTaskFactory(Performative.Request, 
													HGDBOntology.CATCHUP, 
													new CatchUpTaskServer.CatchUpTaskServerFactory());
		peer.getPeerInterface().registerTaskFactory(Performative.Inform, 
													HGDBOntology.ATOM_INTEREST, 
													new GetInterestsTask.GetInterestsFactory()); */
//    	peer.setAtomInterests(new AnyAtomCondition());                
    	peer.catchUp();          				
	}
}