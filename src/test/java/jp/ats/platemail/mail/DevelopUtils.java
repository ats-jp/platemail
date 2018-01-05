package jp.ats.platemail.mail;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;

public class DevelopUtils {

	static void write(Path path, String content) {
		write(path, content, "UTF-8");
	}

	static void write(Path path, String content, String charset) {
		path = addTxt(path);

		if (Files.exists(path)) throw new IllegalStateException(path + " is already exists.");

		try {
			Files.write(path, care(content).getBytes(charset), StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static void append(Path path, String content) {
		path = addTxt(path);

		try {
			Files.write(path, care(content).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static void save(Path path, InputStream input) {
		if (Files.exists(path)) throw new IllegalStateException(path + " is already exists.");

		try {
			Files.write(path, readBytes(new BufferedInputStream(input)), StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static String format(int number) {
		return new DecimalFormat("000000").format(number);
	}

	private static Path addTxt(Path base) {
		return base.getParent().resolve(base.getFileName() + ".txt");
	}

	private static final int BUFFER_SIZE = 8192;

	public static byte[] readBytes(InputStream in) throws IOException {
		byte[] concat = {};
		byte[] b = new byte[BUFFER_SIZE];
		int readed;
		while ((readed = in.read(b, 0, BUFFER_SIZE)) > 0) {
			concat = concatByteArray(concat, concat.length, b, readed);
		}
		return concat;
	}

	static String care(String target) {
		if (target == null) return "";
		return target;
	}

	private static byte[] concatByteArray(byte[] array1, int lengthof1, byte[] array2, int lengthof2) {
		byte[] concat = new byte[lengthof1 + lengthof2];
		System.arraycopy(array1, 0, concat, 0, lengthof1);
		System.arraycopy(array2, 0, concat, lengthof1, lengthof2);
		return concat;
	}

	public static Path getDesktopPath() {
		return Paths.get(
			System.getProperty("user.home"),
			"Desktop");
	}

	public static Path getThunderbirdProfilePath() throws IOException {
		Path base = Paths.get(
			System.getProperty("user.home"),
			"AppData/Roaming/Thunderbird");

		String profile = Files.readAllLines(base.resolve("profiles.ini"))
			.stream()
			.filter(l -> l.startsWith("Path="))
			.findFirst()
			.get()
			.substring(5);

		return base.resolve(profile);
	}
}
