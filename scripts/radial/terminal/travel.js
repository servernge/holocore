function getOptions(options) {
	options.add(new RadialOption(RadialItem.ITEM_USE));
	options.add(new RadialOption(RadialItem.EXAMINE));
}

function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			Log.d("travel.js", "Travel Selection: ITEM_USE");
			var TravelPointSelectionIntent = Java.type("intents.travel.TravelPointSelectionIntent");
			new TravelPointSelectionIntent(player.getCreatureObject(), false).broadcast();
			break;
	}
}