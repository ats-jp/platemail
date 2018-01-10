package jp.ats.platemail.mail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import jp.ats.platemail.SystemProperty;
import jp.ats.platemail.mail.MessageParser;

public class MessageParserTest {

	public static void main(String[] args) throws Exception {
		Path testDir = DevelopUtils.getDesktopPath().resolve("mailtest");

		SystemProperty.set();

		IntStream.range(1, 2).forEach(i -> {
			System.out.println(i);
			try {
				Path outDir = testDir.resolve(Long.toString(System.currentTimeMillis()));
				Files.createDirectories(outDir);

				MessageParser p = new MessageParser(new DumpMessageListener(outDir, 0));
				p.start(testDir.resolve("test.eml"));
			} catch (Exception e) {
				throw new Error(e);
			}
		});
	}
}
