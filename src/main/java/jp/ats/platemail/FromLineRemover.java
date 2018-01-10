package jp.ats.platemail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ats.platemail.LineSpliterator.Brake;

class FromLineRemover {

	//メールアドレス形式(xxx@xxx)ではない場合(MAILER-DAEMON)があるのでFromの後に@を入れない
	private static final Pattern fromLinePattern = Pattern.compile("^From +[^: ]+ +(.+)$");

	//先頭行がmboxのFrom行である場合、除去する
	static Result remove(byte[] messageBytes) throws IOException {
		LineSpliterator spliterator = new LineSpliterator();
		int[] firstLineLength = { 0 };
		String[] firstLine = { "" };
		spliterator.split(new ByteArrayInputStream(messageBytes), (buffer, length) -> {
			firstLineLength[0] = length;
			firstLine[0] = new String(buffer, 0, length);
			//先頭行のみ取得して終了
			throw new Brake();
		});

		Result result = new Result();

		Matcher matcher = fromLinePattern.matcher(firstLine[0]);
		if (matcher.find()) {
			//先頭行がmboxのFrom行だった
			byte[] exculdeFirstLineBytes = new byte[messageBytes.length - firstLineLength[0]];
			System.arraycopy(messageBytes, firstLineLength[0], exculdeFirstLineBytes, 0, exculdeFirstLineBytes.length);
			result.messageBytes = exculdeFirstLineBytes;
			result.fromLineTimestamp = Optional.ofNullable(matcher.group(1));
		} else {
			result.messageBytes = messageBytes;
			result.fromLineTimestamp = Optional.empty();
		}

		return result;
	}

	static class Result {

		byte[] messageBytes;

		Optional<String> fromLineTimestamp;
	}
}
