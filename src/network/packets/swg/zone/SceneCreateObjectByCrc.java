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
package network.packets.swg.zone;

import network.packets.swg.SWGPacket;
import resources.Location;

import java.nio.ByteBuffer;

public class SceneCreateObjectByCrc extends SWGPacket {
	public static final int CRC = getCrc("SceneCreateObjectByCrc");

	private long objId = 0;
	private Location l = new Location();
	private int objCrc = 0;
	private boolean hyperspace;
	
	public SceneCreateObjectByCrc() {
		
	}
	
	public SceneCreateObjectByCrc(long objId, Location l, int objCrc, boolean hyperspace) {
		this.objId = objId;
		this.l = l;
		this.objCrc = objCrc;
		this.hyperspace = hyperspace;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
		l = getEncodable(data, Location.class);
		objCrc = getInt(data);
		hyperspace = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(47);
		addShort(data, 5);
		addInt(  data, CRC);
		addLong( data, objId);
		addEncodable(data, l);
		addInt(  data, objCrc);
		addBoolean(data, hyperspace);
		return data;
	}
	
	public void setObjectId(long objId) { this.objId = objId; }
	public void setLocation(Location l) { this.l = l; }
	public void setObjectCrc(int objCrc) { this.objCrc = objCrc; }
	public void setHyperspace(boolean hyperspace) { this.hyperspace = hyperspace; }
	
	public long getObjectId() { return objId; }
	public Location getLocation() { return l; }
	public int getObjectCrc() { return objCrc; }
	public boolean isHyperspace() { return hyperspace; }
}
