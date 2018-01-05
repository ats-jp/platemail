package deploy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 千葉 哲嗣
 */
public class PropertiesReplacer {

	private static final Pattern pattern = Pattern.compile("\\[\\[([^\\[\\]]+)\\]\\]");

	private PropertiesReplacer() {}

	public static void main(String[] args) throws Exception {
		Reader reader = new InputStreamReader(
			Files.newInputStream(Paths.get(args[0])),
			"UTF-8");
		Writer writer = new OutputStreamWriter(
			Files.newOutputStream(Paths.get(args[1])),
			"UTF-8");
		Reader propReader = new InputStreamReader(
			Files.newInputStream(Paths.get(args[2])),
			"UTF-8");

		Properties prop = new Properties();
		prop.load(propReader);

		replace(reader, writer, prop);
	}

	public static void replace(Reader in, Writer out, Properties prop)
		throws IOException {
		BufferedReader reader = new BufferedReader(in);
		BufferedWriter writer = new BufferedWriter(out);
		for (String line; (line = reader.readLine()) != null;) {
			StringBuilder builder = new StringBuilder();
			replace(line, builder, prop);
			writer.write(builder.toString());
			writer.write('\r');
			writer.write('\n');
			writer.flush();
		}
	}

	private static void replace(
		String line,
		StringBuilder builder,
		Properties prop) {
		Matcher matcher = pattern.matcher(line);
		while (matcher.find()) {
			builder.append(line.substring(0, matcher.start()));
			String key = matcher.group(1).trim();
			String replaced = prop.getProperty(key);
			if (replaced == null) {
				builder.append(matcher.group());
			} else {
				builder.append(replaced);
			}
			line = line.substring(matcher.end());
			matcher.reset(line);
		}
		builder.append(line);
	}
}
