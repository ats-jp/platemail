package jp.ats.platemail;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jp.ats.platemail.mail.DevelopUtils;

public class LmtpClientTest {

	public static void main(String[] args) throws Exception {
		Path testDir = DevelopUtils.getDesktopPath().resolve("mailtest");

		InputStream input = Files.newInputStream(testDir.resolve("input.txt"));
		InputStream message = Files.newInputStream(testDir.resolve("input.eml"));
		OutputStream output = Files.newOutputStream(testDir.resolve("output.txt"));

		LmtpClient.send(input, output, "localhost", "test-sender@example", "test-recipient@example", message);
	}
}
