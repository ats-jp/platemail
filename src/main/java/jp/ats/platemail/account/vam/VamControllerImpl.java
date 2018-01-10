package jp.ats.platemail.account.vam;

import java.nio.file.Paths;

import jp.ats.platemail.Config;
import jp.ats.relay.CommandExecutor;

public class VamControllerImpl implements VamController {

	private final VirtualAliasMap vam = new VirtualAliasMap();

	private final Config config;

	private final String realUser;

	public VamControllerImpl(Config config) {
		this.config = config;
		realUser = config.getRealMailUser();
	}

	@Override
	public void add(String address) {
		vam.add(address, realUser);
	}

	@Override
	public VamResult commit() {
		String file = config.getVirtualAliasMap();

		VamResult result = vam.write(Paths.get(file));

		try {
			CommandExecutor.getInstance(config.getCommandExecutorClass()).execute(config.getMailAccountRefreshCommand(), file);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return result;
	}
}
