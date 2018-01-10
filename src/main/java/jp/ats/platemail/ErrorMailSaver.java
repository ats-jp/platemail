package jp.ats.platemail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class ErrorMailSaver {

	public static Path saveEvidence(String processName, byte[] message) {
		return saveEvidence(processName, "error", message);
	}

	public static Path saveEvidence(String processName, String type, byte[] message) {
		try {
			Path path = Files.createTempFile(
				Paths.get(Config.getInstance().getDirectoryForErrorMail()),
				processName + "." + type + "." + timestamp() + ".",
				".eml");
			Files.write(path, message);
			return path;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String timestamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"));
	}
}
