package jp.ats.platemail.mail;

import static jp.ats.platemail.mail.DevelopUtils.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jp.ats.platemail.mail.PartListener;

public class DumpPartListener implements PartListener {

	private final Path home;

	public DumpPartListener(Path parent, int index) {
		try {
			home = Files.createDirectory(parent.resolve("part-" + format(index)));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void notifyContentType(String contentType) {
		DevelopUtils.write(home.resolve("CONTENT_TYPE"), contentType);
	}

	@Override
	public void notifyHeader(String header, String value) {
		DevelopUtils.append(home.resolve("HEADERS"), header + "\t" + value + "\r\n");
	}

	@Override
	public void notifyMultipartMixed(InputStream input) {
		DevelopUtils.save(home.resolve("MULTIPART_MIXED"), input);
	}

	@Override
	public void notifyMultipartAlternative(InputStream input) {
		DevelopUtils.save(home.resolve("MULTIPART_ALTERNATIVE"), input);
	}

	@Override
	public void notifyPlainText(String text) {
		DevelopUtils.write(home.resolve("TEXT_PLAIN"), text);
	}

	@Override
	public void notifyHtmlText(String text, String charset) {
		DevelopUtils.write(home.resolve("TEXT_HTML"), text, charset);
	}

	@Override
	public void notifyOtherText(String text) {
		DevelopUtils.write(home.resolve("TEXT_OTHER"), text);
	}

	@Override
	public void notifyAttachment(String fileName, InputStream attachment) {
		DevelopUtils.write(home.resolve("FILE_NAME"), fileName);

		String ext = "";
		if (fileName != null) {
			int index = fileName.lastIndexOf(".");
			ext = index >= 0 ? fileName.substring(index) : "";
		}

		DevelopUtils.save(home.resolve("ATTACHMENT" + ext), attachment);
	}

	@Override
	public PartListener createMultipartListener(int multipartIndex) {
		return new DumpPartListener(home, multipartIndex);
	}
}
