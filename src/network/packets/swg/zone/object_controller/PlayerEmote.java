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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class PlayerEmote extends ObjectController {
	
	public static final int CRC = 0x012E;
	
	private long sourceId;
	private long targetId;
	private short emoteId;
	
	public PlayerEmote(long objectId) {
		super(objectId, CRC);
	}
	
	public PlayerEmote(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public PlayerEmote(long objectId, long sourceId, long targetId, short emoteId) {
		super(objectId, CRC);
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.emoteId = emoteId;
	}
	
	public PlayerEmote(long objectId, PlayerEmote emote) {
		super(objectId, CRC);
		this.sourceId = emote.sourceId;
		this.targetId = emote.targetId;
		this.emoteId = emote.emoteId;
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		sourceId = getLong(data);
		targetId = getLong(data);
		emoteId = getShort(data);
		getShort(data); // Should be 0
		getByte(data); // Should be 3
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 21);
		encodeHeader(data);
		addLong(data, sourceId);
		addLong(data, targetId);
		addShort(data, emoteId);
		addShort(data, (short) 0);
		addByte(data, (byte) 3);
		return data;
	}
	
}
