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
package services.galaxy;

import resources.control.Manager;
import services.chat.ChatService;
import services.commands.CommandService;
import services.sui.SuiService;

public class GameManager extends Manager {
	
	private ChatService chatService;
	private CommandService commandService;
	private ConnectionService connectionService;
	private SuiService suiService;
	private WeatherService weatherService;
	
	public GameManager() {
		chatService = new ChatService();
		commandService = new CommandService();
		connectionService = new ConnectionService();
		suiService = new SuiService();
		weatherService = new WeatherService();
		
		addChildService(chatService);
		addChildService(commandService);
		addChildService(connectionService);
		addChildService(suiService);
		addChildService(weatherService);
	}
}
