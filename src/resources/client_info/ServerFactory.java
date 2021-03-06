/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.client_info;

import resources.client_info.visitors.DatatableData;
import resources.server_info.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Waverunner on 6/9/2015
 */
public final class ServerFactory extends DataFactory {
	private static ServerFactory instance;

	public static DatatableData getDatatable(String file) {
		ClientData data = getInstance().readFile(file);
		// Safe type conversion as ServerFactory can only create DatatableData ClientData objects
		return (data != null ? (DatatableData) data : null);
	}

	public void updateServerIffs() throws IOException {
		File root = new File(getFolder());

		Files.walkFileTree(root.toPath(), new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
				if (path.toString().endsWith("sdf")) {
					String name = path.toString();
					name = name.substring(0, name.length() - 4) + ".iff";

					File iff = new File(name);

					if (!iff.exists()) {
						convertSdf(path, name);
						System.out.println("Created Server Datatable: " + name);
						Log.i("ServerFactory", "Created Server Datatable: %s", name);
					} else {
						File sif = path.toFile();
						if (sif.lastModified() > iff.lastModified()) {
							convertSdf(path, name);
							System.out.println("Updated Server Datatable: " + name);
							Log.i("ServerFactory", "Updated Server Datatable: %s", name);
						}
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void convertSdf(Path sif, String newPath) {
		SWGFile swgFile = new SWGFile(newPath, "DTII");

		DatatableData data = (DatatableData) createDataObject(swgFile);

		String[] columnTypes = null;
		String[] columnNames = null;
		Object[][] table = null;

		List<String> defaultValues = new ArrayList<>();
		try {
			int lineNum = -1;

			List<String> rows = Files.readAllLines(sif);
			Iterator<String> itr = rows.iterator();
			while (itr.hasNext()) {
				String row = itr.next();
				if (row == null || row.isEmpty() || row.startsWith("#") || row.startsWith("//")) {
					itr.remove();
					continue;
				}

				// Don't break out of the loop, make sure we remove all the commented/null/empty rows
				if (!(lineNum <= 1))
					continue;

				lineNum++;
				if (lineNum == 0) {
					columnNames = row.split("\t");
					itr.remove();
				} else if (lineNum == 1) {
					columnTypes = row.split("\t");
					for (int i = 0; i < columnTypes.length; i++) {
						String columnType = columnTypes[i];
						if (columnType.contains("[")) {
							String[] split = columnType.split("\\[");
							columnTypes[i] = split[0].toLowerCase();
							defaultValues.add(split[1].replace("]", ""));
						} else {
							columnTypes[i] = columnType.toLowerCase();
							defaultValues.add("");
						}
					}
					itr.remove();
				}
			}

			if (columnNames == null || columnTypes == null) {
				System.err.println("Failed to convert sdf " + sif.getFileName());
				return;
			}

			table = new Object[rows.size()][columnTypes.length];

			for (int i = 0; i < rows.size(); i++) {
				createDatatableRow(i, rows.get(i), columnTypes, table, defaultValues);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		data.setColumnNames(columnNames);
		data.setColumnTypes(columnTypes);
		data.setTable(table);

		writeFile(swgFile, data);
	}

	private void createDatatableRow(int rowNum, String line, String[] columnTypes, Object[][] table, List<String> defValues) {
		String[] values = line.split("\t", -1);

		for (int t = 0; t < columnTypes.length; t++) {
			String type = columnTypes[t];
			String val = values[t];

			if (val.isEmpty() && !defValues.get(t).isEmpty())
				val = defValues.get(t);

			try {
				switch(type) {
					case "b": table[rowNum][t] = Boolean.valueOf(val); break;
					case "h":
					case "i": table[rowNum][t] = Integer.valueOf(val); break;
					case "f": table[rowNum][t] = Float.valueOf(val); break;
					case "s": table[rowNum][t] = val; break;
					default: System.err.println("Don't know how to parse type " + type); break;
				}
			} catch (NumberFormatException e) {
				Log.e("ServerFactory:createDatableRow", "Cannot format string %s to a number", val);
				e.printStackTrace();
			}

		}
	}

	@Override
	protected ClientData createDataObject(String type) {
		switch(type) {
			case "DTII": return new DatatableData();
			default: return null;
		}
	}

	@Override
	protected String getFolder() {
		return "./serverdata/";
	}

	public static ServerFactory getInstance() {
		if (instance == null)
			instance = new ServerFactory();
		return instance;
	}
}
