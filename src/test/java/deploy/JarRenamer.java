package deploy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class JarRenamer {

	public static void main(String[] args) throws Exception {
		Path src = Files.list(Paths.get(args[0]))
			//一応versionの一番最後のものを使用する
			.sorted((file1, file2) -> file1.toString().compareTo(file2.toString()) * -1)
			.filter(file -> file.toString().endsWith("-jar-with-dependencies.jar"))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Jar file not found."));

		Path dest = src.getParent().resolve("platemail.jar");

		System.out.println("Copy jar: src:[" + src + "] dest:[" + dest + "]");

		Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
	}
}
