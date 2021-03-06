/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.network;

import intents.network.CloseConnectionIntent;
import intents.network.InboundPacketIntent;
import intents.network.OutboundPacketIntent;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.NetworkClient;
import network.PacketReceiver;
import network.PacketSender;
import network.packets.Packet;
import network.packets.soe.Disconnect;
import network.packets.soe.SessionRequest;
import network.packets.soe.SessionResponse;
import network.packets.soe.Disconnect.DisconnectReason;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;
import utilities.ThreadUtilities;

public class NetworkClientManager extends Manager implements PacketReceiver {
	
	private final Map <InetAddress, List <NetworkClient>> clients;
	private final Map <Long, NetworkClient> networkClients;
	private final Queue<ReceivedPacket> receivedPackets;
	private final ScheduledExecutorService packetResender;
	private final ExecutorService packetProcessor;
	private final Random crcGenerator;
	private final PacketSender packetSender;
	private final Runnable processPacketRunnable;
	private final Runnable packetResendRunnable;
	private long networkId;
	
	public NetworkClientManager(PacketSender packetSender) {
		this.packetSender = packetSender;
		clients = new HashMap<InetAddress, List<NetworkClient>>();
		networkClients = new HashMap<Long, NetworkClient>();
		receivedPackets = new LinkedList<>();
		packetResender = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("packet-resender"));
		packetProcessor = Executors.newCachedThreadPool(ThreadUtilities.newThreadFactory("packet-processor-%d"));
		crcGenerator = new Random();
		processPacketRunnable = new Runnable() {
			public void run() {
				synchronized (receivedPackets) {
					ReceivedPacket recv = receivedPackets.poll();
					if (recv == null)
						return;
					handlePacket(recv.getType(), recv.getPacket());
				}
			}
		};
		packetResendRunnable = new Runnable() {
			public void run() {
				resendOldUnacknowledged();
			}
		};
		networkId = 0;
		
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(OutboundPacketIntent.TYPE);
		registerForIntent(CloseConnectionIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		packetResender.scheduleAtFixedRate(packetResendRunnable, 0, 200, TimeUnit.MILLISECONDS);
		return super.initialize();
	}
	
	@Override
	public boolean stop() {
		for (NetworkClient client : networkClients.values()) {
			client.sendPacket(new Disconnect(client.getConnectionId(), DisconnectReason.APPLICATION));
		}
		return super.stop();
	}
	
	@Override
	public boolean terminate() {
		packetProcessor.shutdownNow();
		packetResender.shutdownNow();
		boolean success = true;
		try {
			success = packetProcessor.awaitTermination(5, TimeUnit.SECONDS);
			success = packetResender.awaitTermination(5, TimeUnit.SECONDS) && success;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return super.terminate() && success;
	}
	
	@Override
	public void receivePacket(ServerType type, UDPPacket packet) {
		synchronized (receivedPackets) {
			receivedPackets.add(new ReceivedPacket(type, packet));
		}
		packetProcessor.submit(processPacketRunnable);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof OutboundPacketIntent) {
			Packet p = ((OutboundPacketIntent)i).getPacket();
			if (p != null)
				handleOutboundPacket(((OutboundPacketIntent) i).getNetworkId(), p);
		} else if (i instanceof InboundPacketIntent) {
			Packet p = ((InboundPacketIntent) i).getPacket();
			if (p != null) {
				if (p instanceof SessionRequest)
					initializeSession((SessionRequest) p);
				if (p instanceof Disconnect)
					disconnectSession(((InboundPacketIntent) i).getNetworkId(), (Disconnect) p);
			}
		} else if (i instanceof CloseConnectionIntent) {
			int connId = ((CloseConnectionIntent)i).getConnectionId();
			long netId = ((CloseConnectionIntent)i).getNetworkId();
			DisconnectReason reason = ((CloseConnectionIntent)i).getReason();
			removeClient(netId);
			sendPacket(netId, new Disconnect(connId, reason));
		}
	}
	
	private void initializeSession(SessionRequest req) {
		NetworkClient client = getClient(req.getAddress(), req.getPort());
		if (client == null) {
			return;
		}
		SessionResponse outPacket = new SessionResponse();
		outPacket.setConnectionID(client.getConnectionId());
		outPacket.setCrcSeed(client.getCrc());
		outPacket.setCrcLength(2);
		outPacket.setEncryptionFlag((short) 1);
		outPacket.setXorLength((byte) 4);
		outPacket.setUdpSize(getConfig(ConfigFile.NETWORK).getInt("MAX-PACKET-SIZE", 496));
		sendPacket(client.getNetworkId(), outPacket);
	}
	
	private void disconnectSession(long networkId, Disconnect d) {
		disconnectSession(networkId, d.getAddress(), d.getPort(), d.getReason());
	}
	
	private void disconnectSession(long networkId, InetAddress addr, int port, DisconnectReason reason) {
		removeClient(networkId, addr, port);
	}
	
	private int generateCrc() {
		int crc = 0;
		do {
			crc = crcGenerator.nextInt();
		} while (crc == 0);
		return crc;
	}
	
	private void resendOldUnacknowledged() {
		synchronized (clients) {
			for (NetworkClient client : networkClients.values()) {
				client.resendOldUnacknowledged();
				flushPackets();
			}
		}
	}
	
	private void handleOutboundPacket(long networkId, Packet p) {
		synchronized (clients) {
			NetworkClient client = networkClients.get(networkId);
			if (client != null)
				client.sendPacket(p);
		}
	}
	
	private void handlePacket(ServerType type, UDPPacket p) {
		InetAddress addr = p.getAddress();
		if (addr == null)
			return;
		if (p.getData().length == 14 && p.getData()[0] == 0 && p.getData()[1] == 1) {
			handleSessionRequest(type, p);
			return;
		}
		if (type == ServerType.LOGIN || type == ServerType.ZONE)
			handlePacket(p.getAddress(), p.getPort(), type, p.getData());
	}
	
	private void handlePacket(InetAddress addr, int port, ServerType type, byte [] data) {
		synchronized (clients) {
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList != null) {
				synchronized (ipList) {
					for (NetworkClient c : ipList) {
						if (c.processPacket(type, data)) {
							c.updateNetworkInfo(addr, port);
						}
					}
				}
			}
		}
	}
	
	private void handleSessionRequest(ServerType type, UDPPacket p) {
		SessionRequest req = new SessionRequest(ByteBuffer.wrap(p.getData()));
		req.setAddress(p.getAddress());
		req.setPort(p.getPort());
		NetworkClient client = createSession(type, req);
		if (client != null)
			client.processPacket(type, p.getData());
	}
	
	private NetworkClient createSession(ServerType type, SessionRequest req) {
		NetworkClient client = getClient(req.getAddress(), req.getPort());
		if (client != null) {
			if (client.getConnectionId() == req.getConnectionID()) {
				client.resetNetwork();
				client.updateNetworkInfo(req.getAddress(), req.getPort());
				return client;
			} else 
				return null;
		}
		client = createClient(type, req.getAddress(), req.getPort());
		client.setCrc(generateCrc());
		client.setConnectionId(req.getConnectionID());
		return client;
	}
	
	private NetworkClient createClient(ServerType type, InetAddress addr, int port) {
		synchronized (clients) {
			NetworkClient client = new NetworkClient(type, addr, port, networkId++, packetSender);
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList == null) {
				ipList = new ArrayList<NetworkClient>();
				clients.put(addr, ipList);
			}
			synchronized (ipList) {
				ipList.add(client);
			}
			networkClients.put(client.getNetworkId(), client);
			return client;
		}
	}
	
	private NetworkClient getClient(InetAddress addr, int port) {
		synchronized (clients) {
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList != null) {
				synchronized (ipList) {
					for (NetworkClient c : ipList) {
						if (c.getPort() == port)
							return c;
					}
				}
			}
		}
		return null;
	}
	
	private boolean removeClient(long networkId) {
		synchronized (clients) {
			NetworkClient client = networkClients.remove(networkId);
			if (client != null) {
				InetAddress addr = client.getAddress();
				int port = client.getPort();
				List <NetworkClient> ipList = clients.get(addr);
				if (ipList != null) {
					synchronized (ipList) {
						for (NetworkClient c : ipList) {
							if (c.getPort() == port) {
								ipList.remove(c);
								return true;
							}
						}
					}
				}
				client.resetNetwork();
			}
		}
		return false;
	}
	
	private boolean removeClient(long networkId, InetAddress addr, int port) {
		synchronized (clients) {
			networkClients.remove(networkId);
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList != null) {
				synchronized (ipList) {
					for (NetworkClient c : ipList) {
						if (c.getPort() == port) {
							ipList.remove(c);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private static class ReceivedPacket {
		private final ServerType type;
		private final UDPPacket packet;
		
		public ReceivedPacket(ServerType type, UDPPacket packet) {
			this.type = type;
			this.packet = packet;
		}
		
		public ServerType getType() {
			return type;
		}
		
		public UDPPacket getPacket() {
			return packet;
		}
	}
	
}
