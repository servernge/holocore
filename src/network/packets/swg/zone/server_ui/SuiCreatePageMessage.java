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
package network.packets.swg.zone.server_ui;

import network.packets.swg.SWGPacket;
import resources.sui.SuiBaseWindow;

import java.nio.ByteBuffer;

public class SuiCreatePageMessage extends SWGPacket {
	public static final int CRC = getCrc("SuiCreatePageMessage");

	private SuiBaseWindow window;
	
	public SuiCreatePageMessage() {}
	
	public SuiCreatePageMessage(SuiBaseWindow window) {
		this.window = window;
	}
	
	public SuiCreatePageMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		window	= getEncodable(data, SuiBaseWindow.class);
	}
	
	public ByteBuffer encode() {
		byte[] windowData = window.encode();
		ByteBuffer data = ByteBuffer.allocate(6 + windowData.length);
		addShort(data, 2);
		addInt(  data, CRC);
		addData(data, windowData);
		return data;
	}

	public SuiBaseWindow getWindow() {
		return window;
	}
}
