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

import java.nio.ByteBuffer;

public class EnterTicketPurchaseModeMessage extends SWGPacket {
	public static final int CRC = getCrc("EnterTicketPurchaseModeMessage");
	
	private String planetName;
	private String nearestPointName;
	private boolean instant;
	
	public EnterTicketPurchaseModeMessage() {
		
	}
	
	public EnterTicketPurchaseModeMessage(String planetName, String nearestPointName, boolean instant) {
		this.planetName = planetName;
		this.nearestPointName = nearestPointName;
		this.instant = instant;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		planetName = getAscii(data);
		nearestPointName = getAscii(data);
		instant = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(11 + planetName.length() + nearestPointName.length());	// 2x ascii length shorts, 1x opcount short, 1x boolean, int CRC = 11
		addShort(data, 3);	// Operand count of 3
		addInt(data, CRC);
		addAscii(data, planetName);
		addAscii(data, nearestPointName);
		addBoolean(data, instant);
		return data;
	}
	

}
