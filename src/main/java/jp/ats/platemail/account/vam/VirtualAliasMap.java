package jp.ats.platemail.account.vam;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jp.ats.platemail.account.AddressUtils;

/**
 * Virtual Alias Mapの操作を定義したクラス
 */
class VirtualAliasMap {

	private static final String separator = System.getProperty("line.separator");

	//key: mail address
	//value: aliases
	private final Map<String, Set<String>> flatAddresses = new TreeMap<>();

	void add(String address, String alias) {
		Set<String> aliases = flatAddresses.get(address);
		if (aliases == null) {
			aliases = new TreeSet<>();
			flatAddresses.put(address, aliases);
		}

		aliases.add(alias);
	}

	VamResult write(Path file) {
		try (Writer writer = Files.newBufferedWriter(file)) {
			//key: domain
			//value: address
			Map<String, Set<String>> map = new TreeMap<>();
			flatAddresses.forEach((address, aliases) -> {
				put(map, address);
			});

			VamResult result = new VamResult();
			map.forEach((domain, addresses) -> {
				println(writer, domain + " anything");

				addresses.forEach(address -> {
					Set<String> aliases = flatAddresses.get(address);
					println(writer, address + " " + String.join(",", aliases));
					result.incrementAt(domain);
				});

				println(writer, "");
			});

			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void put(Map<String, Set<String>> map, String address) {
		String domain = AddressUtils.splitMailAddress(address)[1];

		Set<String> addresses = map.get(domain);
		if (addresses == null) {
			addresses = new TreeSet<>();
			map.put(domain, addresses);
		}

		addresses.add(address);
	}

	private static void println(Writer writer, String line) {
		try {
			writer.write(line + separator);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
