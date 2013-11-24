package eu.phiwa.dt.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import eu.phiwa.dt.DragonTravelMain;

public class Utils {

	public static void deployDefaultFile(String name, DragonTravelMain plugin) {
		try {
			File target = new File(plugin.getDataFolder(), name);
			InputStream source = plugin.getResource(name);

			if (!target.exists()) {
				OutputStream output = new FileOutputStream(target);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = source.read(buffer)) > 0)
					output.write(buffer, 0, len);
				output.close();
			}
			source.close();
			plugin.info("Deployed " + name);
		} catch (Exception e) {
			plugin.warning("Could not save default file for " + name);
		}
	}

}
