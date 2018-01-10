package jp.ats.platemail.account;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jp.ats.platemail.Config;
import jp.ats.platemail.U;

public class AccountDataStoreImpl implements AccountDataStore {

	private static final Charset jsonCharset = StandardCharsets.UTF_8;

	private final Path lockDir;

	private final Path jsonPath;

	private final Path vadPath;

	private final boolean saveHistory;

	private final Path historyDir;

	public AccountDataStoreImpl(Config config) {
		lockDir = Paths.get(config.getLockdirForAccount());
		jsonPath = Paths.get(config.getAccountRepository());
		vadPath = Paths.get(config.getVirtualAliasDomains());

		saveHistory = Boolean.parseBoolean(config.getSaveAccountRepositoryHistory());
		if (saveHistory) {
			historyDir = Paths.get(config.getDirectoryForAccountRepositoryHistory());
		} else {
			historyDir = null;
		}
	}

	@Override
	public boolean lock() {
		try {
			//ロックを取得
			Files.createDirectory(lockDir);
		} catch (FileAlreadyExistsException e) {
			//既にロックが取得されていた場合
			return false;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	@Override
	public boolean unlock() {
		try {
			return Files.deleteIfExists(lockDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String readJson() {
		if (!Files.exists(jsonPath)) return "";

		try {
			byte[] bytes = Files.readAllBytes(jsonPath);
			return new String(bytes, jsonCharset);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeJson(String json) {
		try {
			if (saveHistory) {
				if (!Files.exists(historyDir))
					Files.createDirectories(historyDir);

				if (Files.exists(jsonPath))
					Files.move(jsonPath, getHistoryFilePath());
			}

			Files.write(jsonPath, json.getBytes(jsonCharset));

			setJsonFilePermissions();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getJsonLastModified() {
		try {
			return Files.getLastModifiedTime(jsonPath).toMillis();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setJsonFilePermissions() {
		try {
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);
			Files.setPosixFilePermissions(jsonPath, perms);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getDomains() {
		try {
			return Files.readAllLines(vadPath).stream().map(d -> d.trim()).filter(d -> U.presents(d)).collect(Collectors.toSet());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HHmmss.SSS");

	private Path getHistoryFilePath() throws IOException {
		String fileName = jsonPath.getFileName().toString();
		String timestamp = formatter.format(
			LocalDateTime.ofInstant(
				Files.getLastModifiedTime(jsonPath).toInstant(),
				ZoneId.systemDefault()));
		for (int i = 0; i < 100; i++) {
			Path target = historyDir.resolve(fileName + "." + timestamp + "." + i++);
			if (!Files.exists(target)) return target;
		}

		throw new IllegalStateException("履歴が作成できませんでした。");
	}
}
