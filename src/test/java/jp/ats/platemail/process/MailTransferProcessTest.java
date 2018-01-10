package jp.ats.platemail.process;

import jp.ats.platemail.MailTransferProcess;
import jp.ats.relay.Shell;
import jp.ats.relay.TestFileSystemResourceLoader;

public class MailTransferProcessTest {

	public static void main(String[] args) throws Exception {
		TestFileSystemResourceLoader loader = new TestFileSystemResourceLoader();
		Shell.dispatch(args, loader, new MailTransferProcess());
	}
}
