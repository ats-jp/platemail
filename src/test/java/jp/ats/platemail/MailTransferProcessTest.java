package jp.ats.platemail;

import jp.ats.relay.Shell;
import jp.ats.relay.TestFileSystemResourceLoader;

public class MailTransferProcessTest {

	public static void main(String[] args) throws Exception {
		TestFileSystemResourceLoader loader = new TestFileSystemResourceLoader();
		Shell.dispatch(args, loader, new MailTransferProcess());
	}
}
